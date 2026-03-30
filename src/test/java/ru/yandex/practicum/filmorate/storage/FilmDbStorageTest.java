package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
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
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class FilmDbStorageTest {

    private FilmDbStorage filmStorage;
    private UserDbStorage userStorage;
    private DirectorDbStorage directorStorage;
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
        directorStorage = new DirectorDbStorage(jdbcTemplate, directorRowMapper);
    }

    private Film createTestFilm(String name) {
        Film film = new Film();
        film.setName(name);
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

    private Director createTestDirector(String name) {
        Director director = new Director();
        director.setName(name);

        long id = directorStorage.create(director).getId();
        director.setId(id);

        return director;
    }

    private void linkFilmDirector(Long filmId, Long directorId) {
        Film film = filmStorage.getFilmById(filmId).orElseThrow();
        Director director = directorStorage.getDirectorById(directorId).orElseThrow();

        if (film.getDirectors() == null) {
            film.setDirectors(new ArrayList<>());
        }
        film.getDirectors().add(director);

        filmStorage.update(film);
    }

    @Test
    void shouldCreateFilm() {
        Film film = createTestFilm("Test Film");
        Film created = filmStorage.create(film);

        assertNotNull(created.getId());
        assertEquals("Test Film", created.getName());
        assertNotNull(created.getMpa());
        assertEquals(1, created.getMpa().getId());
    }

    @Test
    void shouldFindFilmById() {
        Film film = createTestFilm("Test Film");
        Film created = filmStorage.create(film);

        Optional<Film> found = filmStorage.getFilmById(created.getId());

        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals("Test Film", found.get().getName());
    }

    @Test
    void shouldUpdateFilm() {
        Film film = createTestFilm("Test Film");
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
        Film film = createTestFilm("Test Film");
        Film created = filmStorage.create(film);

        filmStorage.delete(created.getId());

        Optional<Film> found = filmStorage.getFilmById(created.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void shouldAddLike() {
        User user = createTestUser();
        User createdUser = userStorage.create(user);

        Film film = createTestFilm("Test Film");
        Film createdFilm = filmStorage.create(film);

        filmStorage.addLike(createdFilm.getId(), createdUser.getId());

        assertTrue(filmStorage.getLikes(createdFilm.getId()).contains(createdUser.getId()));
    }

    @Test
    void shouldDeleteLike() {
        User user = createTestUser();
        User createdUser = userStorage.create(user);

        Film film = createTestFilm("Test Film");
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

        Film film1 = createTestFilm("Test Film");
        Film createdFilm1 = filmStorage.create(film1);
        Film film2 = createTestFilm("Test Film");
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

        Film film1 = createTestFilm("Test Film");
        Film createdFilm1 = filmStorage.create(film1);
        Film film2 = createTestFilm("Test Film");
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
        Film film1 = createTestFilm("Test Film");
        Film createdFilm1 = filmStorage.create(film1);
        Film film2 = createTestFilm("Test Film");
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

    @Test
    void shouldDeleteFilmByIds() {
        Film film1 = createTestFilm("Test Film");
        Film createdFilm1 = filmStorage.create(film1);

        filmStorage.delete(createdFilm1.getId());

        assertTrue(filmStorage.getFilmById(createdFilm1.getId()).isEmpty());
    }

    @Test
    void shouldDeleteLikesWhenFilmDeleted() {
        Film film1 = createTestFilm("Test Film");
        Film createdFilm1 = filmStorage.create(film1);

        User user1 = createTestUser();
        User createdUser1 = userStorage.create(user1);


        filmStorage.addLike(createdFilm1.getId(), createdUser1.getId());

        filmStorage.delete(createdFilm1.getId());

        List<Film> popular = filmStorage.getPopularFilms(10L);

        assertTrue(popular.stream().noneMatch(film -> film.getId().equals(createdFilm1.getId())));
    }

    @Test
    void shouldFindFilmByTitle() {
        Film film1 = createTestFilm("Test Film");
        Film createdFilm1 = filmStorage.create(film1);

        List<Film> films = filmStorage.search("Tes", "title");

        assertFalse(films.isEmpty());
        assertEquals(createdFilm1.getId(), films.get(0).getId());
    }

    @Test
    void shouldFindFilmByDirector() {
        Film film1 = createTestFilm("Test Film");
        Film createdFilm1 = filmStorage.create(film1);

        Director createdDirector = createTestDirector("Альфред Хичкок");

        linkFilmDirector(createdFilm1.getId(), createdDirector.getId());

        List<Film> films = filmStorage.search("Хич", "director");

        assertFalse(films.isEmpty());
        assertEquals(createdFilm1.getId(), films.get(0).getId());
    }

    @Test
    void shouldFindByTitleAndDirector() {
        Film film1 = createTestFilm("UniqueTitle");
        Film titleOnlyFilm = filmStorage.create(film1); // assign ID

        Director unrelatedDirector = createTestDirector("Other Director");
        linkFilmDirector(titleOnlyFilm.getId(), unrelatedDirector.getId());

        Film film2 = createTestFilm("Other Film");
        Film directorOnlyFilm = filmStorage.create(film2); // assign ID

        Director searchDirector = createTestDirector("UniqueDirector");
        linkFilmDirector(directorOnlyFilm.getId(), searchDirector.getId());

        List<Film> results = filmStorage.search("uni", "title,director");

        assertTrue(results.stream()
                .anyMatch(f -> f.getId().equals(titleOnlyFilm.getId())));

        assertTrue(results.stream()
                .anyMatch(f -> f.getId().equals(directorOnlyFilm.getId())));
    }


    @Test
    void shouldReturnCommonFilmsSortedByPopularity() {
        User user1 = userStorage.create(createTestUser());
        User user2 = userStorage.create(createTestUser());
        User user3 = userStorage.create(createTestUser());
        User user4 = userStorage.create(createTestUser());

        Film firstFilm = createTestFilm("First Film");
        Film secondFilm = createTestFilm("Second Film");
        Film thirdFilm = createTestFilm("Second Film");

        Film createdFirstFilm = filmStorage.create(firstFilm);
        Film createdSecondFilm = filmStorage.create(secondFilm);
        Film createdThirdFilm = filmStorage.create(thirdFilm);

        filmStorage.addLike(createdFirstFilm.getId(), user1.getId());
        filmStorage.addLike(createdSecondFilm.getId(), user1.getId());

        filmStorage.addLike(createdFirstFilm.getId(), user2.getId());
        filmStorage.addLike(createdSecondFilm.getId(), user2.getId());

        filmStorage.addLike(createdFirstFilm.getId(), user3.getId());
        filmStorage.addLike(createdThirdFilm.getId(), user4.getId());

        List<Film> commonFilms = filmStorage.getCommonFilms(user1.getId(), user2.getId());

        assertEquals(2, commonFilms.size());
        assertEquals(createdFirstFilm.getId(), commonFilms.get(0).getId());
        assertEquals(createdSecondFilm.getId(), commonFilms.get(1).getId());
    }

}