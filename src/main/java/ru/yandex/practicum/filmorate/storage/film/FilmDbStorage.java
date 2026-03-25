package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;
import ru.yandex.practicum.filmorate.storage.director.DirectorRowMapper;
import ru.yandex.practicum.filmorate.storage.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRowMapper;

import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Qualifier("dbFilmStorage")
@Slf4j
public class FilmDbStorage extends BaseDbStorage<Film> implements FilmStorage {

    private final MpaRowMapper mpaRowMapper;
    private final GenreRowMapper genreRowMapper;
    private final DirectorRowMapper directorRowMapper;

    public FilmDbStorage(JdbcTemplate jdbcTemplate,
                         FilmRowMapper filmRowMapper,
                         MpaRowMapper mpaRowMapper,
                         GenreRowMapper genreRowMapper,
                         DirectorRowMapper directorRowMapper) {
        super(jdbcTemplate, filmRowMapper);
        this.mpaRowMapper = mpaRowMapper;
        this.genreRowMapper = genreRowMapper;
        this.directorRowMapper = directorRowMapper;
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        long id = insert(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
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

        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            String sqlDirectors = "INSERT INTO film_director (film_id, director_id) VALUES (?, ?)";

            for (Director director : film.getDirectors()) {
                update(sqlDirectors, film.getId(), director.getId());
            }

            log.info("Для фильма ID {} добавлено режиссеров: {}", id, film.getDirectors().size());
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
                newFilm.getDuration(),
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

        jdbcTemplate.update("DELETE FROM film_director WHERE film_id = ?", newFilm.getId());

        if (newFilm.getDirectors() != null && !newFilm.getDirectors().isEmpty()) {
            String insertDirectorSql = "INSERT INTO film_director (film_id, director_id) VALUES (?, ?)";
            for (Director director : newFilm.getDirectors()) {
                jdbcTemplate.update(insertDirectorSql, newFilm.getId(), director.getId());
            }
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
                "WHERE film_id = ? " +
                "ORDER BY genres.genre_id";
        try {
            List<Genre> genresList = jdbcTemplate.query(sql, genreRowMapper, filmId);
            log.info("Загружено {} жанров для фильма с ID: {}", genresList.size(), filmId);

            return new LinkedHashSet<>(genresList);
        } catch (DataAccessException e) {
            log.error("Ошибка загрузки жанров для фильма id {}: {}", filmId, e.getMessage());
            return new HashSet<>();
        }
    }

    private List<Director> directorsLoad(Long filmId) {
        String sql = "SELECT * " +
                "FROM film_director " +
                "JOIN directors ON film_director.director_id = directors.director_id " +
                "WHERE film_id = ?";

        try {
            List<Director> directorList = jdbcTemplate.query(sql, directorRowMapper, filmId);
            log.info("Загружено {} режиссеров для фильма с ID: {}", directorList.size(), filmId);

            return directorList;
        } catch (DataAccessException e) {
            log.error("Ошибка загрузки режиссеров для фильма id {}: {}", filmId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Film loadFilmData(Film film) {
        MPA mpaFilm = mpaLoadById(film.getMpa().getId());
        film.setMpa(mpaFilm);

        Set<Genre> genresFilm = genresLoad(film.getId());
        film.setGenres(genresFilm);

        List<Director> directorsFilm = directorsLoad(film.getId());
        film.setDirectors(directorsFilm);

        log.info("Добавлены МРА и жанры.");

        return film;
    }

    @Override
    public Set<Long> getLikedFilmIds(Long userId) {
        String sql = "SELECT film_id FROM likes WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("film_id"),
                userId)
        );
    }

    @Override
    public Map<Long, Set<Long>> getAllUserLikes() {
        String sql = "SELECT user_id, film_id FROM likes";

        return jdbcTemplate.query(sql, rs -> {
            Map<Long, Set<Long>> userLikes = new HashMap<>();
            while (rs.next()) {
                long userId = rs.getLong("user_id");
                long filmId = rs.getLong("film_id");
                userLikes
                        .computeIfAbsent(userId, k -> new HashSet<>())
                        .add(filmId);
            }
            return userLikes;
        });
    }

    @Override
    public List<Film> getFilmsByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        String sql = "SELECT * FROM films WHERE film_id IN (%s)";
        String placeholder = ids.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        String query = String.format(sql, placeholder);

        List<Film> films = jdbcTemplate.query(query, mapper, ids.toArray());

        films.forEach(this::loadFilmData);

        return films;
    }

    public List<Film> allFilmsByDirector(Long directorId, String sortBy) {

        switch (sortBy) {
            case "likes" -> {
                return sortedFilmDirectorByLikes(directorId);
            }
            case "year" -> {
                return sortedFilmDirectorByReleaseDate(directorId);
            }
            default -> throw new ValidationException("Не верный запрос сортировки фильмов.");
        }

    }

    private List<Film> sortedFilmDirectorByReleaseDate(Long directorId) {
        String sql = "SELECT f.* FROM films f " +
                "JOIN film_director fd ON f.film_id = fd.film_id  " +
                "WHERE fd.director_id = ? " +
                "ORDER BY f.release_date ASC";

        List<Film> films = findMany(sql, directorId);

        for (Film film : films) {
            List<Director> directors = directorsLoad(film.getId());
            film.setDirectors(directors);
        }

        return films;
    }

    private List<Film> sortedFilmDirectorByLikes(Long directorId) {
        String sql = "SELECT f.*, COUNT(l.user_id) AS likes_count " +
                "FROM films f " +
                "JOIN film_director fd ON f.film_id = fd.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "WHERE fd.director_id = ? " +
                "GROUP BY f.film_id " +
                "ORDER BY likes_count DESC";

        List<Film> films = findMany(sql, directorId);

        for (Film film : films) {
            List<Director> directors = directorsLoad(film.getId());
            film.setDirectors(directors);
        }

        return films;
    }

    @Override
    public List<Film> popularFilmsByGenreAndYear(int genreId, int count, int year) {
        String sql = "SELECT f.*, COUNT(l.user_id) AS likes_count " +
                "FROM films f " +
                "JOIN film_genre fg ON f.film_id = fg.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "WHERE fg.genre_id = ? AND EXTRACT(YEAR FROM f.release_date) = ? " +
                "GROUP BY f.film_id " +
                "ORDER BY likes_count DESC " +
                "LIMIT ? ";

        try {
            List<Film> popularFilms = jdbcTemplate.query(sql, mapper, genreId, year, count);
            popularFilms.forEach(this::loadFilmData);

            return popularFilms;
        } catch (EmptyResultDataAccessException e) {
            log.error("Ошибка при получении популярных фильмов по жанру и году: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Film> popularFilmsByGenre(int genreId, int count) {
        String sql = "SELECT f.*, COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "JOIN film_genre fg ON f.film_id = fg.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "WHERE fg.genre_id = ? " +
                "GROUP BY f.film_id " +
                "ORDER BY likes_count DESC " +
                "LIMIT ? ";

        try {
            List<Film> popularFilms = jdbcTemplate.query(sql, mapper, genreId, count);
            popularFilms.forEach(this::loadFilmData);

            return popularFilms;
        } catch (EmptyResultDataAccessException e) {
            log.error("Ошибка при получении популярных фильмов по жанру: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Film> popularFilmsByYear(int year, int count) {
        String sql = "SELECT f.*, COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "WHERE EXTRACT(YEAR FROM f.release_date) = ? " +
                "GROUP BY f.film_id " +
                "ORDER BY likes_count DESC " +
                "LIMIT ? ";

        try {
            List<Film> popularFilms = jdbcTemplate.query(sql, mapper, year, count);
            popularFilms.forEach(this::loadFilmData);

            return popularFilms;
        } catch (EmptyResultDataAccessException e) {
            log.error("Ошибка при получении популярных фильмов по году: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}