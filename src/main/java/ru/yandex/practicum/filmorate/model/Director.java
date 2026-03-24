package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class Director {
    private long id;

    @NotBlank(message = "Имя режиссера не может быть пустым.")
    private String name;
}