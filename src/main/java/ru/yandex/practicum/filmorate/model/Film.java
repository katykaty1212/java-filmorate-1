package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.yandex.practicum.filmorate.annotation.DurationPositive;
import ru.yandex.practicum.filmorate.annotation.ReleaseDate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class Film {
    private Long id;

    @NotBlank(message = "Название не может быть пустым.")
    private String name;

    @Size(max = 200, message = "Максимальная длина описания — 200 символов;")
    private String description;

    @NotNull
    @ReleaseDate
    private LocalDate releaseDate;

    @NotNull
    @DurationPositive
    private Duration duration;

    private Set<Genre> genres = new LinkedHashSet<>();

    @NotNull
    private MPA mpa;

    public long getDuration() {
        return duration.toMinutes();
    }

    public void setDuration(long minutes) {
        this.duration = Duration.ofMinutes(minutes);
    }
}