package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MPA {
    private Long id;
    @NotBlank
    private String name;
}