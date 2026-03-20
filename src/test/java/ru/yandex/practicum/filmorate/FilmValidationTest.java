package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MPA;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FilmValidationTest {
    private Validator validator;
    private Film validFilm;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        MPA mpa = new MPA();
        mpa.setId(1);
        mpa.setName("0+");

        validFilm = new Film();
        validFilm.setName("Valid Film");
        validFilm.setDescription("Valid description");
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(120);
        validFilm.setMpa(mpa);
    }

    @Test
    public void shouldPassForValidFilm() {
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void nameAnnotationNotBlankTest() {
        validFilm.setName("   ");
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void descriptionSizeTest() {
        validFilm.setDescription("F".repeat(201));
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertFalse(violations.isEmpty());

        validFilm.setDescription("F".repeat(200));
        violations = validator.validate(validFilm);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void releaseDateValidationTest() {
        validFilm.setReleaseDate(LocalDate.of(1895, 12, 27));
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertFalse(violations.isEmpty());

        validFilm.setReleaseDate(LocalDate.of(2001, 1, 1));
        violations = validator.validate(validFilm);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void durationPositiveTest() {
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertTrue(violations.isEmpty());

        validFilm.setDuration(-10);
        violations = validator.validate(validFilm);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void mpaNotNullTest() {
        validFilm.setMpa(null);
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertFalse(violations.isEmpty());
    }
}