package ru.yandex.practicum.filmorate.storage.review;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.util.List;
import java.util.Optional;

@Component
@Qualifier("reviewDbStorage")
@Slf4j
public class ReviewDbStorage extends BaseDbStorage<Review> implements ReviewStorage {

    public ReviewDbStorage(JdbcTemplate jdbcTemplate, ReviewRowMapper reviewRowMapper) {
        super(jdbcTemplate, reviewRowMapper);
    }

    @Override
    public Review create(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id, useful) VALUES (?, ?, ?, ?, 0)";

        long id = insert(sql,
                review.getContent(),
                review.getIsPositive(),
                review.getUserId(),
                review.getFilmId()
        );

        review.setReviewId(id);
        review.setUseful(0);
        log.info("Создан отзыв с ID {}", id);
        return review;
    }

    @Override
    public Review update(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ?, user_id = ?, film_id = ? WHERE review_id = ?";
        update(sql,
                review.getContent(),
                review.getIsPositive(),
                review.getUserId(),
                review.getFilmId(),
                review.getReviewId());

        log.info("Обновлён отзыв с ID {}", review.getReviewId());
        return getReviewById(review.getReviewId()).orElseThrow();
    }

    @Override
    public void delete(Long reviewId) {
        String sql = "DELETE FROM reviews WHERE review_id = ?";
        jdbcTemplate.update(sql, reviewId);
    }

    @Override
    public Optional<Review> getReviewById(Long reviewId) {
        String sql = "SELECT * FROM reviews WHERE review_id = ?";
        return findOne(sql, reviewId);
    }

    @Override
    public List<Review> getReviews(Long filmId, Long count) {
        if (filmId == null) {
            String sql = "SELECT * FROM reviews ORDER BY useful DESC, review_id ASC LIMIT ?";
            return findMany(sql, count);
        }

        String sql = "SELECT * FROM reviews WHERE film_id = ? ORDER BY useful DESC, review_id ASC LIMIT ?";
        return findMany(sql, filmId, count);
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        addReaction(reviewId, userId, true);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        addReaction(reviewId, userId, false);
    }

    @Override
    public void deleteLike(Long reviewId, Long userId) {
        deleteReaction(reviewId, userId, true);
    }

    @Override
    public void deleteDislike(Long reviewId, Long userId) {
        deleteReaction(reviewId, userId, false);
    }

    private void addReaction(Long reviewId, Long userId, boolean isLike) {
        Optional<Boolean> currentReaction = getReaction(reviewId, userId);

        if (currentReaction.isEmpty()) {
            String sql = "INSERT INTO review_reactions (review_id, user_id, is_like) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, reviewId, userId, isLike);
            updateUseful(reviewId, isLike ? 1 : -1);
            return;
        }

        if (currentReaction.get() == isLike) {
            return;
        }

        String sql = "UPDATE review_reactions SET is_like = ? WHERE review_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, isLike, reviewId, userId);
        updateUseful(reviewId, isLike ? 2 : -2);
    }

    private void deleteReaction(Long reviewId, Long userId, boolean isLike) {
        Optional<Boolean> currentReaction = getReaction(reviewId, userId);

        if (currentReaction.isEmpty() || currentReaction.get() != isLike) {
            return;
        }

        String sql = "DELETE FROM review_reactions WHERE review_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, reviewId, userId);
        updateUseful(reviewId, isLike ? -1 : 1);
    }

    private Optional<Boolean> getReaction(Long reviewId, Long userId) {
        String sql = "SELECT is_like FROM review_reactions WHERE review_id = ? AND user_id = ?";

        try {
            Boolean reaction = jdbcTemplate.queryForObject(sql, Boolean.class, reviewId, userId);
            return Optional.ofNullable(reaction);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    private void updateUseful(Long reviewId, int delta) {
        String sql = "UPDATE reviews SET useful = useful + ? WHERE review_id = ?";
        jdbcTemplate.update(sql, delta, reviewId);
        log.info("Обновлена полезность отзыва {} на {}", reviewId, delta);
    }
}
