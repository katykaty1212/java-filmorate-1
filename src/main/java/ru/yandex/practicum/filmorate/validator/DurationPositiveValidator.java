package ru.yandex.practicum.filmorate.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.filmorate.annotation.DurationPositive;

import java.time.Duration;

@Slf4j
public class DurationPositiveValidator implements ConstraintValidator<DurationPositive, Duration> {

    @Override
    public boolean isValid(Duration duration, ConstraintValidatorContext constraintValidatorContext) {
        log.info("Проверка продолжительности фильма: {}", duration);

        if (duration == null) {
            log.debug("Продолжительность не указана - пропускаем проверку");
            return true;
        }

        // Проверяем что продолжительность положительная
        if (duration.isNegative() || duration.isZero()) {
            log.warn("Некорректная продолжительность фильма: {}", duration);
            return false;
        }

        log.info("Продолжительность прошла валидацию: {}", duration);
        return true;
    }
}
