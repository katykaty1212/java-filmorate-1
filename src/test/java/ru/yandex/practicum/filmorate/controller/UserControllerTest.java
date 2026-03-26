package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.EventService;
import ru.yandex.practicum.filmorate.service.RecommendationService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.director.DirectorRowMapper;
import ru.yandex.practicum.filmorate.storage.event.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.event.EventRowMapper;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.storage.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserControllerTest {

    private UserController userController;
    private JdbcTemplate jdbcTemplate;
    private EmbeddedDatabase embeddedDatabase;

    @BeforeEach
    void setUp() {
        embeddedDatabase = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .addScript("classpath:data.sql")
                .build();

        jdbcTemplate = new JdbcTemplate(embeddedDatabase);

        // Очищаем таблицы перед каждым тестом
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM likes");
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");

        // Сбрасываем счетчик ID
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");

        UserRowMapper userRowMapper = new UserRowMapper();
        FilmRowMapper filmRowMapper = new FilmRowMapper();
        MpaRowMapper mpaRowMapper = new MpaRowMapper();
        GenreRowMapper genreRowMapper = new GenreRowMapper();
        FriendshipRowMapper friendshipRowMapper = new FriendshipRowMapper();
        DirectorRowMapper directorRowMapper = new DirectorRowMapper();
        EventRowMapper eventRowMapper = new EventRowMapper();

        UserDbStorage userStorage = new UserDbStorage(jdbcTemplate, userRowMapper, friendshipRowMapper);
        FilmStorage filmStorage = new FilmDbStorage(
                jdbcTemplate, filmRowMapper, mpaRowMapper, genreRowMapper, directorRowMapper);
        EventDbStorage eventDbStorage = new EventDbStorage(jdbcTemplate, eventRowMapper);

        UserService userService = new UserService(userStorage, eventDbStorage);
        RecommendationService recommendationService = new RecommendationService(filmStorage);
        EventService eventService = new EventService();
        userController = new UserController(userService, recommendationService, eventService);
    }

    @AfterEach
    void tearDown() {
        embeddedDatabase.shutdown();
    }

    private User createTestUser(String email, String login, String name) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    @Test
    public void createAndFindAllUsersTest() {
        User user1 = createTestUser("user1@mail.ru", "user1login", "User One");
        User user2 = createTestUser("user2@mail.ru", "user2login", "User Two");

        userController.create(user1);
        userController.create(user2);

        Collection<User> allUsers = userController.findAll();

        assertEquals(2, allUsers.size());
        assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals("user1@mail.ru")));
        assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals("user2@mail.ru")));
    }

    @Test
    public void createUserTest() {
        User user = createTestUser("test@mail.ru", "validlogin", "Valid Name");

        User createdUser = userController.create(user);

        assertNotNull(createdUser.getId());
        assertEquals("test@mail.ru", createdUser.getEmail());
        assertEquals("validlogin", createdUser.getLogin());
        assertEquals("Valid Name", createdUser.getName());
    }

    @Test
    public void createUserWithEmptyNameTest() {
        User user = createTestUser("empty@mail.ru", "validlogin", "");

        User createdUser = userController.create(user);

        assertEquals("validlogin", createdUser.getName());
    }

    @Test
    public void createUserWithNullNameTest() {
        User user = createTestUser("null@mail.ru", "validlogin", null);

        User createdUser = userController.create(user);

        assertEquals("validlogin", createdUser.getName());
    }

    @Test
    public void updateUserWithValidDataTest() {
        User user = createTestUser("original@mail.ru", "originallogin", "Original Name");
        User createdUser = userController.create(user);
        Long userId = createdUser.getId();

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setEmail("updated@mail.ru");
        updatedUser.setLogin("updatedlogin");
        updatedUser.setName("Updated Name");
        updatedUser.setBirthday(LocalDate.of(1995, 5, 15));

        User resultUser = userController.update(updatedUser);

        assertEquals(userId, resultUser.getId());
        assertEquals("updated@mail.ru", resultUser.getEmail());
        assertEquals("updatedlogin", resultUser.getLogin());
        assertEquals("Updated Name", resultUser.getName());
        assertEquals(LocalDate.of(1995, 5, 15), resultUser.getBirthday());
    }

    @Test
    public void updateUserWithEmptyNameTest() {
        User user = createTestUser("test@mail.ru", "originallogin", "Original Name");
        User createdUser = userController.create(user);

        User updatedUser = new User();
        updatedUser.setId(createdUser.getId());
        updatedUser.setEmail("test@mail.ru");
        updatedUser.setLogin("updatedlogin");
        updatedUser.setName("");
        updatedUser.setBirthday(LocalDate.of(2000, 1, 1));

        User resultUser = userController.update(updatedUser);

        assertEquals("updatedlogin", resultUser.getName());
    }

    @Test
    public void getUserByIdTest() {
        User user = createTestUser("find@mail.ru", "findlogin", "Find User");
        User createdUser = userController.create(user);

        User foundUser = userController.getUserById(createdUser.getId());

        assertNotNull(foundUser);
        assertEquals(createdUser.getId(), foundUser.getId());
        assertEquals("find@mail.ru", foundUser.getEmail());
    }

    @Test
    public void deleteUserTest() {
        User user = createTestUser("delete@mail.ru", "deletelogin", "Delete User");
        User createdUser = userController.create(user);

        userController.delete(createdUser.getId());

        assertThrows(RuntimeException.class, () -> {
            userController.getUserById(createdUser.getId());
        });
    }

    @Test
    public void getRecommendationsTest() {
        User user1 = createTestUser("user1@mail.ru", "user1login", "User One");
        User user2 = createTestUser("user2@mail.ru", "user2login", "User Two");

        user1 = userController.create(user1);
        user2 = userController.create(user2);

        Long userId1 = user1.getId();
        Long userId2 = user2.getId();

        jdbcTemplate.update("INSERT INTO FILMS (FILM_ID, NAME, DESCRIPTION, RELEASE_DATE, DURATION, MPA_ID) VALUES (?, ?, ?, ?, ?, ?)",
                1L, "Film 1", "Desc 1", java.sql.Date.valueOf("2020-01-01"), 100, 1);
        jdbcTemplate.update("INSERT INTO FILMS (FILM_ID, NAME, DESCRIPTION, RELEASE_DATE, DURATION, MPA_ID) VALUES (?, ?, ?, ?, ?, ?)",
                2L, "Film 2", "Desc 2", java.sql.Date.valueOf("2020-01-01"), 120, 1);
        jdbcTemplate.update("INSERT INTO FILMS (FILM_ID, NAME, DESCRIPTION, RELEASE_DATE, DURATION, MPA_ID) VALUES (?, ?, ?, ?, ?, ?)",
                3L, "Film 3", "Desc 3", java.sql.Date.valueOf("2020-01-01"), 90, 1);

        jdbcTemplate.update("INSERT INTO LIKES (USER_ID, FILM_ID) VALUES (?, ?)", userId1, 1L);
        jdbcTemplate.update("INSERT INTO LIKES (USER_ID, FILM_ID) VALUES (?, ?)", userId1, 2L);

        jdbcTemplate.update("INSERT INTO LIKES (USER_ID, FILM_ID) VALUES (?, ?)", userId2, 2L);
        jdbcTemplate.update("INSERT INTO LIKES (USER_ID, FILM_ID) VALUES (?, ?)", userId2, 3L);

        List<Film> recommendations = userController.getRecommendations(userId1);

        assertNotNull(recommendations);
        assertEquals(1, recommendations.size());
        assertEquals(3L, recommendations.get(0).getId());
        assertEquals("Film 3", recommendations.get(0).getName());
    }

}