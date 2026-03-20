package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Review {
    private Long reviewId;

    @NotBlank(message = "Текст отзыва не может быть пустым")
    private String content;

    @NotNull(message = "Нужно указать, позитивный отзыв или нет")
    @JsonProperty("isPositive")
    private Boolean isPositive;

    @NotNull(message = "Нужно указать идентификатор пользователя")
    private Long userId;

    @NotNull(message = "Нужно указать идентификатор фильма")
    private Long filmId;

    private Integer useful = 0;


    public Boolean getIsPositive() {
        return isPositive;
    }

    public void setIsPositive(Boolean positive) {
        this.isPositive = positive;
    }
}
