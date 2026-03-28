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
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

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
    private final MpaDbStorage mpaDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final EventService eventService;

    public Film create(Film film) {
        mpaDbStorage.findById(film.getMpa().getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                genreDbStorage.findById(genre.getId());
            }
        }

        return filmStorage.create(film);
    }

    public Film update(Film newFilm) {
        getFilmById(newFilm.getId());
        return filmStorage.update(newFilm);
    }

    public void delete(Long filmId) {
        getFilmById(filmId);
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

    public void addLike(Long filmId, Long userId) {
        getFilmById(filmId);
        userService.getUserById(userId);
        filmStorage.addLike(filmId, userId);
        eventService.createEvent(userId, EventType.LIKE, Operation.ADD, filmId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void deleteLike(Long filmId, Long userId) {
        getFilmById(filmId);
        userService.getUserById(userId);
        filmStorage.deleteLike(filmId, userId);
        eventService.createEvent(userId, EventType.LIKE, Operation.REMOVE, filmId);
        log.info("Пользователь {} удалил лайк фильму {}", userId, filmId);
    }

    public List<Film> getPopularFilm(Long count) {
        return filmStorage.getPopularFilms(count);
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        userService.getUserById(userId);
        userService.getUserById(friendId);
        return filmStorage.getCommonFilms(userId, friendId);
    }

    public List<Film> allFilmsByDirector(Long directorId, String sortBy) {
        directorService.findById(directorId);
        return filmStorage.allFilmsByDirector(directorId, sortBy);
    }

    public List<Film> search(String query, String by) {
        validateBy(by);
        return filmStorage.search(query, by);
    }

    private void validateBy(String by) {
        Set<String> options = Set.of("director", "title");

        for (String option : by.split(",")) {
            if (!options.contains(option)) {
                throw new ValidationException("Поиск можно осуществить только по режиссеру и/или названию фильма. Неверный параметр поиска: " + option);
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