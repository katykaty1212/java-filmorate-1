package ru.yandex.practicum.filmorate.storage.review;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
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
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";
        long id = insert(sql, review.getContent(), review.getIsPositive(), review.getUserId(), review.getFilmId());
        review.setReviewId(id);
        log.info("Создан отзыв с ID {}", id);
        return getReviewById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с id " + id + " не найден"));
    }

    @Override
    public Review update(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ?, user_id = ?, film_id = ? WHERE review_id = ?";
        update(sql, review.getContent(), review.getIsPositive(), review.getUserId(), review.getFilmId(), review.getReviewId());
        log.info("Обновлён отзыв с ID {}", review.getReviewId());
        return getReviewById(review.getReviewId())
                .orElseThrow(() -> new NotFoundException("Отзыв с id " + review.getReviewId() + " не найден"));
    }

    @Override
    public void delete(Long reviewId) {
        String sql = "DELETE FROM reviews WHERE review_id = ?";
        jdbcTemplate.update(sql, reviewId);
        log.info("Удалён отзыв с ID {}", reviewId);
    }

    @Override
    public Optional<Review> getReviewById(Long reviewId) {
        String sql = "SELECT r.review_id, r.content, r.is_positive, r.user_id, r.film_id, " +
                "COALESCE(SUM(CASE " +
                "WHEN rl.is_like = TRUE THEN 1 " +
                "WHEN rl.is_like = FALSE THEN -1 " +
                "ELSE 0 END), 0) AS useful " +
                "FROM reviews r " +
                "LEFT JOIN review_likes rl ON r.review_id = rl.review_id " +
                "WHERE r.review_id = ? " +
                "GROUP BY r.review_id, r.content, r.is_positive, r.user_id, r.film_id";
        return findOne(sql, reviewId);
    }

    @Override
    public List<Review> getReviews(Long filmId, Integer count) {
        String baseSql = "SELECT r.review_id, r.content, r.is_positive, r.user_id, r.film_id, " +
                "COALESCE(SUM(CASE " +
                "WHEN rl.is_like = TRUE THEN 1 " +
                "WHEN rl.is_like = FALSE THEN -1 " +
                "ELSE 0 END), 0) AS useful " +
                "FROM reviews r " +
                "LEFT JOIN review_likes rl ON r.review_id = rl.review_id ";

        String tailSql = "GROUP BY r.review_id, r.content, r.is_positive, r.user_id, r.film_id " +
                "ORDER BY useful DESC, r.review_id ASC " +
                "LIMIT ?";

        if (filmId == null) {
            return findMany(baseSql + tailSql, count);
        }

        return findMany(baseSql + "WHERE r.film_id = ? " + tailSql, filmId, count);
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        saveReaction(reviewId, userId, true);
        log.info("Пользователь {} поставил лайк отзыву {}", userId, reviewId);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        saveReaction(reviewId, userId, false);
        log.info("Пользователь {} поставил дизлайк отзыву {}", userId, reviewId);
    }

    @Override
    public void deleteLike(Long reviewId, Long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = TRUE";
        jdbcTemplate.update(sql, reviewId, userId);
        log.info("Пользователь {} удалил лайк у отзыва {}", userId, reviewId);
    }

    @Override
    public void deleteDislike(Long reviewId, Long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = FALSE";
        jdbcTemplate.update(sql, reviewId, userId);
        log.info("Пользователь {} удалил дизлайк у отзыва {}", userId, reviewId);
    }

    private void saveReaction(Long reviewId, Long userId, boolean isLike) {
        String deleteSql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ?";
        jdbcTemplate.update(deleteSql, reviewId, userId);

        String insertSql = "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, ?)";
        update(insertSql, reviewId, userId, isLike);
    }
}
