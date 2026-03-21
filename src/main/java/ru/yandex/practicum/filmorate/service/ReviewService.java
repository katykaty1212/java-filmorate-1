package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;

import java.util.List;

@Service
@Slf4j
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final UserService userService;
    private final FilmService filmService;

    public ReviewService(@Qualifier("reviewDbStorage") ReviewStorage reviewStorage,
                         UserService userService,
                         FilmService filmService) {
        this.reviewStorage = reviewStorage;
        this.userService = userService;
        this.filmService = filmService;
    }

    public Review create(Review review) {
        validateReview(review);
        return reviewStorage.create(review);
    }

    public Review update(Review review) {
        if (review.getReviewId() == null) {
            throw new ValidationException("Id отзыва обязателен для обновления");
        }
        getReviewById(review.getReviewId());
        validateReview(review);
        return reviewStorage.update(review);
    }

    public void delete(Long reviewId) {
        getReviewById(reviewId);
        reviewStorage.delete(reviewId);
    }

    public Review getReviewById(Long reviewId) {
        return reviewStorage.getReviewById(reviewId)
                .orElseThrow(() -> {
                    log.error("Отзыв с ID {} не найден", reviewId);
                    return new NotFoundException("Отзыв с ID " + reviewId + " не найден");
                });
    }

    public List<Review> getReviews(Long filmId, Integer count) {
        int reviewsCount = count == null ? 10 : count;

        if (reviewsCount <= 0) {
            throw new ValidationException("Параметр count должен быть положительным");
        }

        if (filmId != null) {
            filmService.getFilmById(filmId);
        }

        return reviewStorage.getReviews(filmId, reviewsCount);
    }

    public void addLike(Long reviewId, Long userId) {
        getReviewById(reviewId);
        userService.getUserById(userId);
        reviewStorage.addLike(reviewId, userId);
    }

    public void addDislike(Long reviewId, Long userId) {
        getReviewById(reviewId);
        userService.getUserById(userId);
        reviewStorage.addDislike(reviewId, userId);
    }

    public void deleteLike(Long reviewId, Long userId) {
        getReviewById(reviewId);
        userService.getUserById(userId);
        reviewStorage.deleteLike(reviewId, userId);
    }

    public void deleteDislike(Long reviewId, Long userId) {
        getReviewById(reviewId);
        userService.getUserById(userId);
        reviewStorage.deleteDislike(reviewId, userId);
    }

    private void validateReview(Review review) {
        userService.getUserById(review.getUserId());
        filmService.getFilmById(review.getFilmId());
    }
}
