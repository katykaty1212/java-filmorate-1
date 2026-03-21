package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReviewValidationTest {
    private Validator validator;
    private Review review;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        review = new Review();
        review.setContent("Нормальный фильм");
        review.setIsPositive(true);
        review.setUserId(1L);
        review.setFilmId(1L);
    }

    @Test
    public void validReviewShouldPassTest() {
        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void contentNotBlankTest() {
        review.setContent("   ");
        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void isPositiveNotNullTest() {
        review.setIsPositive(null);
        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void userIdNotNullTest() {
        review.setUserId(null);
        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void filmIdNotNullTest() {
        review.setFilmId(null);
        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertFalse(violations.isEmpty());
    }
}
