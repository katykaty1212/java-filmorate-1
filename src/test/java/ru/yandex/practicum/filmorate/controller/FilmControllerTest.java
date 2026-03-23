package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.RecommendationService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.director.DirectorRowMapper;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.film.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FilmControllerTest {

    private FilmController filmController;
    private FilmDbStorage filmStorage;
    private UserDbStorage userStorage;
    private DirectorService directorService;
    UserController userController;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .addScript("classpath:data.sql")
                .build();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

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
        UserRowMapper userRowMapper = new UserRowMapper();
        FriendshipRowMapper friendshipRowMapper = new FriendshipRowMapper();
        DirectorRowMapper directorRowMapper = new DirectorRowMapper();

        MpaDbStorage mpaDbStorage = new MpaDbStorage(jdbcTemplate, mpaRowMapper);  // добавить
        GenreDbStorage genreDbStorage = new GenreDbStorage(jdbcTemplate, genreRowMapper);  // добавить
        filmStorage = new FilmDbStorage(jdbcTemplate, filmRowMapper, mpaRowMapper, genreRowMapper, directorRowMapper);
        userStorage = new UserDbStorage(jdbcTemplate, userRowMapper, friendshipRowMapper);

        UserService userService = new UserService(userStorage);
        FilmService filmService = new FilmService(filmStorage, userService, directorService, mpaDbStorage, genreDbStorage);
        filmController = new FilmController(filmService);
        userController = new UserController(userService, new RecommendationService(filmStorage));
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
        assertEquals(120, createdFilm.getDuration());
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
        assertEquals(150, resultFilm.getDuration());
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

    @Test
    public void popularFilmsByGenreAndYearTest() {
        Film film1 = createFilmTest("Драма 2020 много лайков", 2020, List.of(2));
        Film film2 = createFilmTest("Драма 2020 средне", 2020, List.of(2));
        Film film3 = createFilmTest("Драма 2021", 2021, List.of(2));
        Film film4 = createFilmTest("Комедия 2020", 2020, List.of(1));
        Film film5 = createFilmTest("Драма 2020 без лайков", 2020, List.of(2));
        Film film6 = createFilmTest("Драма 2019", 2019, List.of(2));
        Film film7 = createFilmTest("Драма 2020 тоже много", 2020, List.of(2));

        User user1 = createUserTest("user1");
        User user2 = createUserTest("user2");
        User user3 = createUserTest("user3");
        User user4 = createUserTest("user4");
        User user5 = createUserTest("user5");

        filmController.addLike(film1.getId(), user1.getId());
        filmController.addLike(film1.getId(), user2.getId());
        filmController.addLike(film1.getId(), user3.getId());
        filmController.addLike(film1.getId(), user4.getId());
        filmController.addLike(film1.getId(), user5.getId());

        filmController.addLike(film2.getId(), user1.getId());
        filmController.addLike(film2.getId(), user2.getId());
        filmController.addLike(film2.getId(), user3.getId());

        filmController.addLike(film3.getId(), user1.getId());

        filmController.addLike(film4.getId(), user1.getId());
        filmController.addLike(film4.getId(), user2.getId());

        filmController.addLike(film6.getId(), user1.getId());
        filmController.addLike(film6.getId(), user2.getId());

        filmController.addLike(film7.getId(), user1.getId());
        filmController.addLike(film7.getId(), user2.getId());
        filmController.addLike(film7.getId(), user3.getId());
        filmController.addLike(film7.getId(), user4.getId());

        Collection<Film> filmsFromDb = filmController.findAll();
        assertEquals(7, filmsFromDb.size());

        List<Film> result = filmController.popularFilmsByGenreAndYear(2, 3, 2020);

        assertEquals(3, result.size());

        for (Film film : result) {
            assertTrue(film.getGenres().stream().anyMatch(g -> g.getId() == 2));
        }

        for (Film film : result) {
            assertEquals(2020, film.getReleaseDate().getYear());
        }

        List<Long> expectedIds = List.of(film1.getId(), film7.getId(), film2.getId());
        List<Long> actualIds = result.stream().map(Film::getId).toList();
        assertEquals(expectedIds, actualIds);

        assertFalse(result.stream().anyMatch(f -> f.getId().equals(film3.getId())));
        assertFalse(result.stream().anyMatch(f -> f.getId().equals(film4.getId())));
        assertFalse(result.stream().anyMatch(f -> f.getId().equals(film6.getId())));
        assertFalse(result.stream().anyMatch(f -> f.getId().equals(film5.getId())));

    }

    private Film createFilmTest(String name, int year, List<Integer> genreIds) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(year, 1, 1));
        film.setDuration(120);

        MPA mpa = new MPA();
        mpa.setId(1);
        film.setMpa(mpa);

        Set<Genre> genres = new LinkedHashSet<>();
        for (Integer id : genreIds) {
            Genre genre = new Genre();
            genre.setId(id);
            genres.add(genre);
        }
        film.setGenres(genres);

        return filmController.create(film);
    }

    private User createUserTest(String login) {
        User user = new User();
        user.setEmail(login + "@test.com");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));

        return userController.create(user);
    }
}