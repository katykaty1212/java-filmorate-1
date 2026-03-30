package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Genre {
    private Long id;
    @NotBlank
    private String name;
}