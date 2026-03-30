package ru.yandex.practicum.filmorate.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;
    private final DirectorService directorService;
    private final MPAService mpaService;
    private final GenreDbStorage genreDbStorage;
    private final EventService eventService;

    public Film create(Film film) {
        mpaService.validateMpaExists(film.getMpa().getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                genreDbStorage.getGenreById(genre.getId());
            }
        }

        return filmStorage.create(film);
    }

    public Film update(Film newFilm) {
        validateFilmExists(newFilm.getId());
        return filmStorage.update(newFilm);
    }

    public void delete(Long filmId) {
        validateFilmExists(filmId);
        filmStorage.delete(filmId);
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film getFilmById(Long filmId) {
        return filmStorage.getFilmById(filmId)
                .orElseThrow(() -> {
                    log.error("Фильм с ID {} не найден", filmId);
                    return new NotFoundException("Фильм с ID " + filmId + " не найден");
                });
    }

    public void validateFilmExists(Long filmId) {
        filmStorage.getFilmById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + filmId + " не найден"));
    }

    public void addLike(Long filmId, Long userId) {
        validateFilmExists(filmId);
        userService.validateUserExists(userId);
        filmStorage.addLike(filmId, userId);
        eventService.createEvent(userId, EventType.LIKE, Operation.ADD, filmId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void deleteLike(Long filmId, Long userId) {
        validateFilmExists(filmId);
        userService.validateUserExists(userId);
        filmStorage.deleteLike(filmId, userId);
        eventService.createEvent(userId, EventType.LIKE, Operation.REMOVE, filmId);
        log.info("Пользователь {} удалил лайк фильму {}", userId, filmId);
    }

    public List<Film> getPopularFilm(int count, Integer genreId, Integer year) {
        if (genreId != null && year != null) {
            return filmStorage.popularFilmsByGenreAndYear(genreId, count, year);
        }

        if (year != null) {
            return filmStorage.popularFilmsByYear(year, count);
        }

        if (genreId != null) {
            return filmStorage.popularFilmsByGenre(genreId, count);
        }

        return filmStorage.getPopularFilms((long) count);
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        userService.validateUserExists(userId);
        userService.validateUserExists(friendId);
        return filmStorage.getCommonFilms(userId, friendId);
    }

    public List<Film> allFilmsByDirector(Long directorId, String sortBy) {
        directorService.validateDirectorExists(directorId);
        return filmStorage.allFilmsByDirector(directorId, sortBy);
    }

    public List<Film> search(String query, String by) {
        validateSearchFields(by);
        return filmStorage.search(query, by);
    }

    private void validateSearchFields(String by) {
        Set<String> allowedFields = Set.of("director", "title");

        for (String field : by.split(",")) {
            if (!allowedFields.contains(field)) {
                throw new ValidationException("Поиск можно осуществить только по режиссеру и/или названию фильма. Неверный параметр поиска: " + field);
            }
        }
    }

    public List<Film> popularFilmsByGenreAndYear(int genreId, int limit, int year) {
        return filmStorage.popularFilmsByGenreAndYear(genreId, limit, year);
    }

    public List<Film> popularFilmsByGenre(int genreId, int count) {
        return filmStorage.popularFilmsByGenre(genreId, count);
    }

    public List<Film> popularFilmsByYear(int year, int count) {
        return filmStorage.popularFilmsByYear(year, count);
    }


}