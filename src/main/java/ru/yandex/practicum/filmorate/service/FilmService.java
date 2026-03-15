package ru.yandex.practicum.filmorate.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;
    private final MpaDbStorage mpaDbStorage;
    private final GenreService genreDbStorage;

    public FilmService(@Qualifier("dbFilmStorage") FilmStorage filmStorage, UserService userService, MpaDbStorage mpaDbStorage1, GenreService genreDbStorage) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.mpaDbStorage = mpaDbStorage1;
        this.genreDbStorage = genreDbStorage;
    }

    public Film create(Film film) {

        try {
            mpaDbStorage.findById(film.getMpa().getId());
        } catch (NotFoundException e) {
            throw new NotFoundException("MPA с id " + film.getMpa().getId() + " не найден");
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                try {
                    genreDbStorage.findById(genre.getId());
                } catch (NotFoundException e) {
                    throw new NotFoundException("Жанр с id " + genre.getId() + " не найден");
                }
            }
        }

        return filmStorage.create(film);
    }

    public Film update(Film newFilm) {
        return filmStorage.update(newFilm);
    }

    public Film delete(Long id) {
        return filmStorage.delete(id);
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
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void deleteLike(Long filmId, Long userId) {
        getFilmById(filmId);
        userService.getUserById(userId);
        filmStorage.deleteLike(filmId, userId);

        log.info("Пользователь {} удалил лайк фильму {}", userId, filmId);
    }

    public List<Film> getPopularFilm(Long count) {
        return filmStorage.getPopularFilms(count);
    }
}