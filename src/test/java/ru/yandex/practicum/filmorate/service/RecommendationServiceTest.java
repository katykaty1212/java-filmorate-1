package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.director.DirectorRowMapper;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRowMapper;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecommendationServiceTest {
    private FilmDbStorage filmDbStorage;
    private RecommendationService recommendationService;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .addScript("classpath:data.sql")
                .build();

        jdbcTemplate = new JdbcTemplate(dataSource);

        // Очищаем таблицы перед каждым тестом
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM likes");
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");

        // Сбрасываем счетчик ID
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");

        FilmRowMapper filmRowMapper = new FilmRowMapper();
        MpaRowMapper mpaRowMapper = new MpaRowMapper();
        GenreRowMapper genreRowMapper = new GenreRowMapper();
        DirectorRowMapper directorRowMapper = new DirectorRowMapper();

        filmDbStorage = new FilmDbStorage(jdbcTemplate, filmRowMapper, mpaRowMapper, genreRowMapper, directorRowMapper);
        recommendationService = new RecommendationService(filmDbStorage);

        jdbcTemplate.update("INSERT INTO USERS (USER_ID, EMAIL, LOGIN, NAME, BIRTHDAY) VALUES (?, ?, ?, ?, ?)",
                1L, "user1@mail.ru", "user1login", "User One", java.sql.Date.valueOf("1990-01-01"));
        jdbcTemplate.update("INSERT INTO USERS (USER_ID, EMAIL, LOGIN, NAME, BIRTHDAY) VALUES (?, ?, ?, ?, ?)",
                2L, "user2@mail.ru", "user2login", "User Two", java.sql.Date.valueOf("1990-01-02"));
        jdbcTemplate.update("INSERT INTO USERS (USER_ID, EMAIL, LOGIN, NAME, BIRTHDAY) VALUES (?, ?, ?, ?, ?)",
                3L, "user3@mail.ru", "user3login", "User Three", java.sql.Date.valueOf("1990-01-03"));

        jdbcTemplate.update("INSERT INTO FILMS (FILM_ID, NAME, DESCRIPTION, RELEASE_DATE, DURATION, MPA_ID) VALUES (?, ?, ?, ?, ?, ?)",
                1L, "Film 1", "Desc 1", java.sql.Date.valueOf("2020-01-01"), 100, 1);
        jdbcTemplate.update("INSERT INTO FILMS (FILM_ID, NAME, DESCRIPTION, RELEASE_DATE, DURATION, MPA_ID) VALUES (?, ?, ?, ?, ?, ?)",
                2L, "Film 2", "Desc 2", java.sql.Date.valueOf("2020-01-01"), 120, 1);
        jdbcTemplate.update("INSERT INTO FILMS (FILM_ID, NAME, DESCRIPTION, RELEASE_DATE, DURATION, MPA_ID) VALUES (?, ?, ?, ?, ?, ?)",
                3L, "Film 3", "Desc 3", java.sql.Date.valueOf("2020-01-01"), 90, 1);
        jdbcTemplate.update("INSERT INTO FILMS (FILM_ID, NAME, DESCRIPTION, RELEASE_DATE, DURATION, MPA_ID) VALUES (?, ?, ?, ?, ?, ?)",
                4L, "Film 4", "Desc 4", java.sql.Date.valueOf("2020-01-01"), 110, 1);
    }

    @Test
    void getRecommendations_returnsEmpty_whenUserHasNoLikes() {
        Long userId = 1L;
        List<Film> recommendations = recommendationService.getRecommendations(userId);
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }

    @Test
    void getRecommendations_returnsCorrectFilms() {
        Long user1 = 1L;
        Long user2 = 2L;

        jdbcTemplate.update("INSERT INTO LIKES (USER_ID, FILM_ID) VALUES (?, ?)", user1, 1);
        jdbcTemplate.update("INSERT INTO LIKES (USER_ID, FILM_ID) VALUES (?, ?)", user1, 2);

        jdbcTemplate.update("INSERT INTO LIKES (USER_ID, FILM_ID) VALUES (?, ?)", user2, 2);
        jdbcTemplate.update("INSERT INTO LIKES (USER_ID, FILM_ID) VALUES (?, ?)", user2, 3);

        List<Film> recommendations = recommendationService.getRecommendations(user1);
        assertNotNull(recommendations);
        assertEquals(1, recommendations.size());
        assertEquals(3L, recommendations.get(0).getId());
    }

    @Test
    void getRecommendations_returnsEmpty_whenNoSimilarUsers() {
        Long user3 = 3L;

        jdbcTemplate.update("INSERT INTO LIKES (USER_ID, FILM_ID) VALUES (?, ?)", user3, 4);

        List<Film> recommendations = recommendationService.getRecommendations(user3);
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }
}
