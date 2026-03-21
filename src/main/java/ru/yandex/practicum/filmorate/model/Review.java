package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Review {
    private Long reviewId;

    @NotBlank(message = "Текст отзыва не может быть пустым")
    private String content;

    @NotNull(message = "Поле isPositive обязательно")
    private Boolean isPositive;

    @NotNull(message = "Поле userId обязательно")
    private Long userId;

    @NotNull(message = "Поле filmId обязательно")
    private Long filmId;

    private Integer useful = 0;
}
