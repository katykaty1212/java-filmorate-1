package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
@RequiredArgsConstructor
public class FilmController {

    private final FilmService filmService;

    @GetMapping
    public Collection<Film> findAll() {
        return filmService.findAll();
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        return filmService.create(film);
    }

    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable Long id) {
        return filmService.getFilmById(id);
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film newFilm) {
        return filmService.update(newFilm);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.deleteLike(id, userId);
    }

    @DeleteMapping("/{filmId}")
    public void delete(@PathVariable Long filmId) {
        filmService.delete(filmId);
    }

    @GetMapping("/common")
    public List<Film> getCommonFilms(@RequestParam Long userId, @RequestParam Long friendId) {
        return filmService.getCommonFilms(userId, friendId);
    }

    @GetMapping("/director/{directorId}")
    public List<Film> allFilmsByDirector(@PathVariable Long directorId, @RequestParam String sortBy) {
        return filmService.allFilmsByDirector(directorId, sortBy);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(
            @RequestParam(defaultValue = "10") @Positive int count,
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Integer year) {

        if (genreId != null && year != null) {
            return filmService.popularFilmsByGenreAndYear(genreId, count, year);
        }

        if (year != null) {
            return filmService.popularFilmsByYear(year, count);
        }

        if (genreId != null) {
            return filmService.popularFilmsByGenre(genreId, count);
        }

        return filmService.getPopularFilm((long) count);
    }

    @GetMapping("/search")
    public List<Film> search(@RequestParam String query, @RequestParam String by) {
        return filmService.search(query, by);
    }
}