package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserDbStorageTest {

    private UserDbStorage userStorage;
    private JdbcTemplate jdbcTemplate;

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

        // Сбрасываем счетчик
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");

        // Создаем мапперы
        UserRowMapper userRowMapper = new UserRowMapper();
        FriendshipRowMapper friendshipRowMapper = new FriendshipRowMapper();

        // Создаем хранилище
        userStorage = new UserDbStorage(jdbcTemplate, userRowMapper, friendshipRowMapper);
    }

    private User createTestUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    @Test
    void shouldCreateUser() {
        User user = createTestUser("test@mail.ru", "testlogin");
        User created = userStorage.create(user);

        assertNotNull(created.getId());
        assertEquals("test@mail.ru", created.getEmail());
        assertEquals("testlogin", created.getLogin());
        assertEquals("Test User", created.getName());
    }

    @Test
    void shouldFindUserById() {
        User user = createTestUser("find@mail.ru", "findlogin");
        User created = userStorage.create(user);

        Optional<User> found = userStorage.getUserById(created.getId());

        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals("find@mail.ru", found.get().getEmail());
    }

    @Test
    void shouldUpdateUser() {
        User user = createTestUser("update@mail.ru", "updatelogin");
        User created = userStorage.create(user);

        created.setName("Updated Name");
        User updated = userStorage.update(created);

        assertEquals("Updated Name", updated.getName());

        Optional<User> found = userStorage.getUserById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
    }

    @Test
    void shouldDeleteUser() {
        User user = createTestUser("delete@mail.ru", "deletelogin");
        User created = userStorage.create(user);

        userStorage.delete(created.getId());

        Optional<User> found = userStorage.getUserById(created.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void shouldAddFriend() {
        User user1 = createTestUser("friend1@mail.ru", "friend1");
        User user2 = createTestUser("friend2@mail.ru", "friend2");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        userStorage.addFriend(created1.getId(), created2.getId());

        // Проверяем через прямой SQL запрос
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendship WHERE user_id = ? AND friend_id = ?",
                Integer.class, created1.getId(), created2.getId()
        );
        assertEquals(1, count);
    }

    @Test
    void shouldDeleteFriend() {
        User user1 = createTestUser("friend5@mail.ru", "friend5");
        User user2 = createTestUser("friend6@mail.ru", "friend6");

        User created1 = userStorage.create(user1);
        User created2 = userStorage.create(user2);

        userStorage.addFriend(created1.getId(), created2.getId());

        // Проверяем что добавилось
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendship WHERE user_id = ? AND friend_id = ?",
                Integer.class, created1.getId(), created2.getId()
        );
        assertEquals(1, count);

        userStorage.deleteFriend(created1.getId(), created2.getId());

        // Проверяем что удалилось
        count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendship WHERE user_id = ? AND friend_id = ?",
                Integer.class, created1.getId(), created2.getId()
        );
        assertEquals(0, count);
    }
}