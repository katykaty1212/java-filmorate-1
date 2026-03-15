package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;

import java.sql.Date;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRowMapper;

import java.util.*;

@Component
@Qualifier("dbFilmStorage")
@Slf4j
public class FilmDbStorage extends BaseDbStorage<Film> implements FilmStorage {

    private final MpaRowMapper mpaRowMapper;
    private final GenreRowMapper genreRowMapper;

    public FilmDbStorage(JdbcTemplate jdbcTemplate,
                         FilmRowMapper filmRowMapper,
                         MpaRowMapper mpaRowMapper,
                         GenreRowMapper genreRowMapper) {
        super(jdbcTemplate, filmRowMapper);
        this.mpaRowMapper = mpaRowMapper;
        this.genreRowMapper = genreRowMapper;
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        long id = insert(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration().toMinutes(),
                film.getMpa().getId()
        );

        film.setId(id);
        log.info("Создан фильм с ID: {} ", film.getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String sqlGenres = "INSERT INTO film_genre (film_id, genre_id) VALUES (?, ?)";

            for (Genre genre : film.getGenres()) {
                update(sqlGenres, film.getId(), genre.getId());
            }
            log.info("Для фильма ID {} добавлено жанров: {}", id, film.getGenres().size());
        }

        Film savedFilm = loadFilmData(film);
        log.info("Фильм ID {} полностью загружен с MPA и жанрами", id);

        return savedFilm;
    }

    @Override
    public Film update(Film newFilm) {
        String sql = "UPDATE films SET " +
                "name = ?, " +
                "description = ?, " +
                "release_date = ?, " +
                "duration = ?, " +
                "mpa_id = ? " +
                "WHERE film_id = ?";

        update(sql,
                newFilm.getName(),
                newFilm.getDescription(),
                Date.valueOf(newFilm.getReleaseDate()),
                newFilm.getDuration().toMinutes(),
                newFilm.getMpa().getId(),
                newFilm.getId()
        );

        log.info("Обновлён фильм с ID: {}", newFilm.getId());

        String deleteGenresSql = "DELETE FROM film_genre WHERE film_id = ?";
        jdbcTemplate.update(deleteGenresSql, newFilm.getId());

        if (newFilm.getGenres() != null && !newFilm.getGenres().isEmpty()) {
            String insertGenreSql = "INSERT INTO film_genre (film_id, genre_id) VALUES (?, ?)";
            for (Genre genre : newFilm.getGenres()) {
                update(insertGenreSql, newFilm.getId(), genre.getId());
            }
            log.info("Для фильма ID {} добавлено жанров: {}", newFilm.getId(), newFilm.getGenres().size());
        } else {
            log.info("Жанры для фильма ID {} удалены", newFilm.getId());
        }

        return loadFilmData(newFilm);
    }

    @Override
    public Film delete(Long id) {
        String sql = "DELETE FROM films WHERE film_id = ?";
        Film film = getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + id + " не найден"));

        update(sql, id);
        log.info("Удалён фильм с ID: {}", id);
        return film;
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT * FROM films";

        List<Film> films = findMany(sql);
        log.info("Получен список всех фильмов.");

        films.forEach(this::loadFilmData);

        return films;
    }

    @Override
    public Optional<Film> getFilmById(Long filmId) {
        String sql = "SELECT * FROM films WHERE film_id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(sql, mapper, filmId);
            log.info("Найден фильм с ID: {}", filmId);

            return Optional.of(loadFilmData(film));
        } catch (EmptyResultDataAccessException e) {
            log.warn("Фильм по ID: {} не найден.", filmId);
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Ошибка при загрузке фильма ID: {}.{}", filmId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        update(sql, filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        update(sql, filmId, userId);
        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    @Override
    public Set<Long> getLikes(Long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    private MPA mpaLoadById(int mpaId) {
        String sql = "SELECT * FROM mpa WHERE mpa_id = ?";

        try {
            MPA mpa = jdbcTemplate.queryForObject(sql, mpaRowMapper, mpaId);
            log.info("Рейтинг с ID {} найден", mpaId);
            return mpa;
        } catch (DataAccessException e) {
            log.error("Ошибка загрузки MPA с ID: {}. {}", mpaId, e.getMessage());
            throw new RuntimeException("Не удалось загрузить MPA.", e);
        }
    }

    @Override
    public List<Film> getPopularFilms(Long count) {
        String sql = "SELECT f.*, COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "GROUP BY f.film_id " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        try {
            List<Film> popularFilmsList = jdbcTemplate.query(sql, mapper, count);
            popularFilmsList.forEach(this::loadFilmData);
            log.info("Получено {} популярных фильмов", popularFilmsList.size());

            return popularFilmsList;

        } catch (EmptyResultDataAccessException e) {
            log.error("Ошибка при получении популярных фильмов: {}", e.getMessage());
            return Collections.emptyList();
        }

    }

    private Set<Genre> genresLoad(Long filmId) {
        String sql = "SELECT * " +
                "FROM film_genre " +
                "JOIN genres ON film_genre.genre_id = genres.genre_id " +
                "WHERE film_id = ?"+
                "ORDER BY genres.genre_id";
        try {
            List<Genre> genresList = jdbcTemplate.query(sql, genreRowMapper, filmId);
            log.info("Загружено {} жанров для фильма с ID: {}", genresList.size(), filmId);

            return new HashSet<>(genresList);
        } catch (DataAccessException e) {
            log.error("Ошибка загрузки жанров для фильма id {}: {}", filmId, e.getMessage());
            return new HashSet<>();
        }
    }

    private Film loadFilmData(Film film) {
        MPA mpaFilm = mpaLoadById(film.getMpa().getId());
        film.setMpa(mpaFilm);

        Set<Genre> genresFilm = genresLoad(film.getId());
        film.setGenres(genresFilm);

        log.info("Добавлены МРА и жанры.");

        return film;
    }


}