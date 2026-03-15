package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class User {
    private Long id;

    @NotBlank(message = "Строка не может быть пустой.")
    @Email(message = "Строка должна соответствовать формату электронного адреса.")
    private String email;

    @NotBlank(message = "Строка не может быть пустой.")
    @Pattern(regexp = "\\S+", message = "Логин не должен содержать пробелов")
    private String login;

    private String name;

    @PastOrPresent
    private LocalDate birthday;
}