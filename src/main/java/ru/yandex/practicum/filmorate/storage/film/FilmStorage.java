package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

public interface FilmStorage {
    Film create(Film film);

    Film update(Film newFilm);

    void delete(Long filmId);

    Collection<Film> findAll();

    Optional<Film> getFilmById(Long filmId);

    void addLike(Long filmId, Long userId);

    void deleteLike(Long filmId, Long userId);

    Set<Long> getLikes(Long filmId);

    List<Film> getPopularFilms(Long count);

    List<Film> getCommonFilms(Long userId, Long friendId);

    Set<Long> getLikedFilmIds(Long userId);

    Map<Long, Set<Long>> getAllUserLikes();

    List<Film> getFilmsByIds(Set<Long> ids);

    List<Film> allFilmsByDirector(Long directorId, String sortBy);

    List<Film> search(String query, String by);

    List<Film> popularFilmsByGenreAndYear(int genreId, int limit, int year);

    List<Film> popularFilmsByGenre(int genreId, int count);

    List<Film> popularFilmsByYear(int year, int count);
}