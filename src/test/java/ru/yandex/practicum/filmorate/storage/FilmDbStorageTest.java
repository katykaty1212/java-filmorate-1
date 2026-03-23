package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.director.DirectorRowMapper;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.film.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.storage.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class FilmDbStorageTest {

    private FilmDbStorage filmStorage;
    private UserDbStorage userStorage;
    private JdbcTemplate jdbcTemplate;
    private int userCounter = 1;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
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

        // Сбрасываем счетчики
        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");

        // Создаем мапперы
        FilmRowMapper filmRowMapper = new FilmRowMapper();
        MpaRowMapper mpaRowMapper = new MpaRowMapper();
        GenreRowMapper genreRowMapper = new GenreRowMapper();
        UserRowMapper userRowMapper = new UserRowMapper();
        FriendshipRowMapper friendshipRowMapper = new FriendshipRowMapper();
        DirectorRowMapper directorRowMapper = new DirectorRowMapper();

        // Создаем хранилища
        filmStorage = new FilmDbStorage(jdbcTemplate, filmRowMapper, mpaRowMapper, genreRowMapper, directorRowMapper);
        userStorage = new UserDbStorage(jdbcTemplate, userRowMapper, friendshipRowMapper);
    }

    private Film createTestFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        MPA mpa = new MPA();
        mpa.setId(1);
        film.setMpa(mpa);
        return film;
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test" + userCounter + "@film.ru");
        user.setLogin("testlogin" + userCounter);
        user.setName("Test User" + userCounter);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        userCounter++;
        return user;
    }

    @Test
    void shouldCreateFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        assertNotNull(created.getId());
        assertEquals("Test Film", created.getName());
        assertNotNull(created.getMpa());
        assertEquals(1, created.getMpa().getId());
    }

    @Test
    void shouldFindFilmById() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        Optional<Film> found = filmStorage.getFilmById(created.getId());

        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals("Test Film", found.get().getName());
    }

    @Test
    void shouldUpdateFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        created.setName("Updated Name");
        Film updated = filmStorage.update(created);

        assertEquals("Updated Name", updated.getName());

        Optional<Film> found = filmStorage.getFilmById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
    }

    @Test
    void shouldDeleteFilm() {
        Film film = createTestFilm();
        Film created = filmStorage.create(film);

        filmStorage.delete(created.getId());

        Optional<Film> found = filmStorage.getFilmById(created.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void shouldAddLike() {
        User user = createTestUser();
        User createdUser = userStorage.create(user);

        Film film = createTestFilm();
        Film createdFilm = filmStorage.create(film);

        filmStorage.addLike(createdFilm.getId(), createdUser.getId());

        assertTrue(filmStorage.getLikes(createdFilm.getId()).contains(createdUser.getId()));
    }

    @Test
    void shouldDeleteLike() {
        User user = createTestUser();
        User createdUser = userStorage.create(user);

        Film film = createTestFilm();
        Film createdFilm = filmStorage.create(film);

        filmStorage.addLike(createdFilm.getId(), createdUser.getId());
        assertTrue(filmStorage.getLikes(createdFilm.getId()).contains(createdUser.getId()));

        filmStorage.deleteLike(createdFilm.getId(), createdUser.getId());
        assertFalse(filmStorage.getLikes(createdFilm.getId()).contains(createdUser.getId()));
    }

    @Test
    void shouldReturnLikedFilmIdsForUser() {
        User user = createTestUser();
        User createdUser = userStorage.create(user);

        Film film1 = createTestFilm();
        Film createdFilm1 = filmStorage.create(film1);
        Film film2 = createTestFilm();
        Film createdFilm2 = filmStorage.create(film2);

        filmStorage.addLike(createdFilm1.getId(), createdUser.getId());
        filmStorage.addLike(createdFilm2.getId(), createdUser.getId());

        Set<Long> likedFilmIds = filmStorage.getLikedFilmIds(createdUser.getId());

        assertNotNull(likedFilmIds);
        assertEquals(2, likedFilmIds.size());
        assertTrue(likedFilmIds.contains(createdFilm1.getId()));
        assertTrue(likedFilmIds.contains(createdFilm2.getId()));
    }

    @Test
    void shouldReturnAllUserLikes() {
        User user1 = createTestUser();
        User createdUser1 = userStorage.create(user1);
        User user2 = createTestUser();
        User createdUser2 = userStorage.create(user2);

        Film film1 = createTestFilm();
        Film createdFilm1 = filmStorage.create(film1);
        Film film2 = createTestFilm();
        Film createdFilm2 = filmStorage.create(film2);

        filmStorage.addLike(createdFilm1.getId(), createdUser1.getId());
        filmStorage.addLike(createdFilm2.getId(), createdUser1.getId());
        filmStorage.addLike(createdFilm2.getId(), createdUser2.getId());

        Map<Long, Set<Long>> allUserLikes = filmStorage.getAllUserLikes();

        assertNotNull(allUserLikes);
        assertEquals(2, allUserLikes.size());
        assertTrue(allUserLikes.get(createdUser1.getId()).contains(createdFilm1.getId()));
        assertTrue(allUserLikes.get(createdUser1.getId()).contains(createdFilm2.getId()));
        assertTrue(allUserLikes.get(createdUser2.getId()).contains(createdFilm2.getId()));
    }

    @Test
    void shouldReturnFilmsByIds() {
        Film film1 = createTestFilm();
        Film createdFilm1 = filmStorage.create(film1);
        Film film2 = createTestFilm();
        Film createdFilm2 = filmStorage.create(film2);

        Set<Long> ids = Set.of(createdFilm1.getId(), createdFilm2.getId());

        List<Film> films = filmStorage.getFilmsByIds(ids);
        Set<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        assertNotNull(films);
        assertEquals(2, films.size());
        assertTrue(filmIds.contains(createdFilm1.getId()));
        assertTrue(filmIds.contains(createdFilm2.getId()));
    }

}