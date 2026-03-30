package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final UserService userService;
    private final FilmService filmService;
    private final EventService eventService;

    public Review create(Review review) {
        validateReview(review);
        Review created = reviewStorage.create(review);
        eventService.createEvent(created.getUserId(), EventType.REVIEW, Operation.ADD, created.getReviewId());
        return created;
    }

    public Review update(Review review) {
        if (review.getReviewId() == null) {
            throw new ValidationException("Id отзыва обязателен для обновления");
        }
        validateReviewExists(review.getReviewId());
        validateReview(review);
        Review updated = reviewStorage.update(review);
        eventService.createEvent(updated.getUserId(), EventType.REVIEW, Operation.UPDATE, updated.getReviewId());
        return updated;
    }

    public void delete(Long reviewId) {
        Review review = getReviewById(reviewId);

        boolean deleted = reviewStorage.delete(reviewId);
        if (deleted) {
            eventService.createEvent(review.getUserId(), EventType.REVIEW, Operation.REMOVE, reviewId);
        }
    }

    public Review getReviewById(Long reviewId) {
        return reviewStorage.getReviewById(reviewId)
                .orElseThrow(() -> {
                    log.error("Отзыв с ID {} не найден", reviewId);
                    return new NotFoundException("Отзыв с ID " + reviewId + " не найден");
                });
    }

    public void validateReviewExists(Long reviewId) {
        reviewStorage.getReviewById(reviewId)
                .orElseThrow(() -> new NotFoundException("Отзыв с ID " + reviewId + " не найден"));
    }

    public List<Review> getReviews(Long filmId, Integer count) {
        int reviewsCount = count == null ? 10 : count;

        if (reviewsCount <= 0) {
            throw new ValidationException("Параметр count должен быть положительным");
        }

        if (filmId != null) {
            filmService.validateFilmExists(filmId);
        }

        return reviewStorage.getReviews(filmId, reviewsCount);
    }

    public void addLike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        userService.validateUserExists(userId);
        reviewStorage.addLike(reviewId, userId);
    }

    public void addDislike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        userService.validateUserExists(userId);
        reviewStorage.addDislike(reviewId, userId);
    }

    public void deleteLike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        userService.validateUserExists(userId);
        reviewStorage.deleteLike(reviewId, userId);
    }

    public void deleteDislike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        userService.validateUserExists(userId);
        reviewStorage.deleteDislike(reviewId, userId);
    }

    private void validateReview(Review review) {
        userService.validateUserExists(review.getUserId());
        filmService.validateFilmExists(review.getFilmId());
    }
}
