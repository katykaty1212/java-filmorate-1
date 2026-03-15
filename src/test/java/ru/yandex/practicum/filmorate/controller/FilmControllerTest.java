package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.film.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.storage.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class FilmControllerTest {

    private FilmController filmController;
    private FilmDbStorage filmStorage;
    private UserDbStorage userStorage;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .addScript("classpath:data.sql")
                .build();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        FilmRowMapper filmRowMapper = new FilmRowMapper();
        MpaRowMapper mpaRowMapper = new MpaRowMapper();
        GenreRowMapper genreRowMapper = new GenreRowMapper();
        UserRowMapper userRowMapper = new UserRowMapper();
        FriendshipRowMapper friendshipRowMapper = new FriendshipRowMapper();

        filmStorage = new FilmDbStorage(jdbcTemplate, filmRowMapper, mpaRowMapper, genreRowMapper);
        userStorage = new UserDbStorage(jdbcTemplate, userRowMapper, friendshipRowMapper);

        UserService userService = new UserService(userStorage);
        FilmService filmService = new FilmService(filmStorage, userService);
        filmController = new FilmController(filmService);
    }

    private Film createTestFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description for " + name);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        MPA mpa = new MPA();
        mpa.setId(1);
        film.setMpa(mpa);
        return film;
    }

    @Test
    public void createAndFindAllFilmsTest() {
        Film film1 = createTestFilm("Film One");
        Film film2 = createTestFilm("Film Two");

        filmController.create(film1);
        filmController.create(film2);

        Collection<Film> allFilms = filmController.findAll();

        assertEquals(2, allFilms.size());
        assertTrue(allFilms.stream().anyMatch(f -> f.getName().equals("Film One")));
        assertTrue(allFilms.stream().anyMatch(f -> f.getName().equals("Film Two")));
    }

    @Test
    public void createFilmTest() {
        Film film = createTestFilm("Valid Film");

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm.getId());
        assertEquals("Valid Film", createdFilm.getName());
        assertEquals(Duration.ofMinutes(120), createdFilm.getDuration());
        assertNotNull(createdFilm.getMpa());
    }

    @Test
    public void updateFilmTest() {
        Film film = createTestFilm("Original Film");
        Film createdFilm = filmController.create(film);
        Long filmId = createdFilm.getId();

        Film updatedFilm = new Film();
        updatedFilm.setId(filmId);
        updatedFilm.setName("Updated Film");
        updatedFilm.setDescription("Updated description");
        updatedFilm.setReleaseDate(LocalDate.of(2010, 5, 15));
        updatedFilm.setDuration(150);

        MPA mpa = new MPA();
        mpa.setId(1);
        updatedFilm.setMpa(mpa);

        Film resultFilm = filmController.update(updatedFilm);

        assertEquals(filmId, resultFilm.getId());
        assertEquals("Updated Film", resultFilm.getName());
        assertEquals("Updated description", resultFilm.getDescription());
        assertEquals(LocalDate.of(2010, 5, 15), resultFilm.getReleaseDate());
        assertEquals(Duration.ofMinutes(150), resultFilm.getDuration());
    }

    @Test
    public void getFilmByIdTest() {
        Film film = createTestFilm("Test Film");
        Film createdFilm = filmController.create(film);

        Film foundFilm = filmController.getFilmById(createdFilm.getId());

        assertNotNull(foundFilm);
        assertEquals(createdFilm.getId(), foundFilm.getId());
        assertEquals("Test Film", foundFilm.getName());
    }

    @Test
    public void deleteFilmTest() {
        Film film = createTestFilm("Film to Delete");
        Film createdFilm = filmController.create(film);

        filmController.delete(createdFilm.getId());

        assertThrows(RuntimeException.class, () -> filmController.getFilmById(createdFilm.getId()));
    }
}