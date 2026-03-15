package ru.yandex.practicum.filmorate.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.filmorate.annotation.ReleaseDate;

import java.time.LocalDate;

@Slf4j
public class ReleaseDateValidator implements ConstraintValidator<ReleaseDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate releaseDate, ConstraintValidatorContext context) {
        log.info("Начата проверка даты релиза: {}", releaseDate);
        if (releaseDate == null) {
            log.debug("Дата не указана - пропускаем проверку");
            return true;
        }

        LocalDate firstDate = LocalDate.of(1895, 12, 28);
        LocalDate today = LocalDate.now();

        if (releaseDate.isBefore(firstDate) || releaseDate.isAfter(today)) {
            log.warn("Попытка создать фильм с некорректной датой: {}", releaseDate);
            return false;
        }

        log.info("Дата прошла валидацию: {}", releaseDate);
        return true;
    }
}