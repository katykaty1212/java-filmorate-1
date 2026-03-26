package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.event.EventRowMapper;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EventDbStorageTest {

    private EventDbStorage eventStorage;
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

        // Очистка таблиц
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM film_director");
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM likes");
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");

        // Сброс автоинкремента
        jdbcTemplate.execute("ALTER TABLE events ALTER COLUMN event_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE reviews ALTER COLUMN review_id RESTART WITH 1");

        // Пользователи
        jdbcTemplate.update("INSERT INTO users (user_id, email, login, name, birthday) VALUES (1, 'user1@test.com', 'user1', 'User One', '1990-01-01')");
        jdbcTemplate.update("INSERT INTO users (user_id, email, login, name, birthday) VALUES (2, 'user2@test.com', 'user2', 'User Two', '1991-02-02')");
        jdbcTemplate.update("INSERT INTO users (user_id, email, login, name, birthday) VALUES (3, 'user3@test.com', 'user3', 'User Three', '1992-03-03')");
        jdbcTemplate.update("INSERT INTO users (user_id, email, login, name, birthday) VALUES (4, 'user4@test.com', 'user4', 'User Four', '1993-04-04')");

        // Фильмы
        jdbcTemplate.update("INSERT INTO films (film_id, name, description, release_date, duration, mpa_id) VALUES (1, 'Film One', 'Description 1', '2020-01-01', 120, 1)");
        jdbcTemplate.update("INSERT INTO films (film_id, name, description, release_date, duration, mpa_id) VALUES (2, 'Film Two', 'Description 2', '2021-01-01', 130, 1)");

        // Жанры для фильмов
        jdbcTemplate.update("INSERT INTO film_genre (film_id, genre_id) VALUES (1, 1)");
        jdbcTemplate.update("INSERT INTO film_genre (film_id, genre_id) VALUES (2, 2)");

        // Отзывы
        jdbcTemplate.update("INSERT INTO reviews (review_id, content, is_positive, user_id, film_id) VALUES (1, 'Great film!', TRUE, 1, 1)");
        jdbcTemplate.update("INSERT INTO reviews (review_id, content, is_positive, user_id, film_id) VALUES (2, 'Not bad', TRUE, 2, 1)");
        jdbcTemplate.update("INSERT INTO reviews (review_id, content, is_positive, user_id, film_id) VALUES (3, 'Awesome!', TRUE, 3, 2)");

        // Лайки на фильмы
        jdbcTemplate.update("INSERT INTO likes (film_id, user_id) VALUES (1, 2)");
        jdbcTemplate.update("INSERT INTO likes (film_id, user_id) VALUES (1, 3)");
        jdbcTemplate.update("INSERT INTO likes (film_id, user_id) VALUES (2, 1)");

        // Лайки на отзывы (review_likes)
        jdbcTemplate.update("INSERT INTO review_likes (review_id, user_id, is_like) VALUES (1, 2, TRUE)");
        jdbcTemplate.update("INSERT INTO review_likes (review_id, user_id, is_like) VALUES (1, 3, FALSE)");
        jdbcTemplate.update("INSERT INTO review_likes (review_id, user_id, is_like) VALUES (2, 1, TRUE)");

        // Дружба
        jdbcTemplate.update("INSERT INTO friendship (user_id, friend_id, status) VALUES (1, 2, TRUE)");
        jdbcTemplate.update("INSERT INTO friendship (user_id, friend_id, status) VALUES (1, 3, TRUE)");
        jdbcTemplate.update("INSERT INTO friendship (user_id, friend_id, status) VALUES (2, 3, FALSE)");

        EventRowMapper eventRowMapper = new EventRowMapper();
        eventStorage = new EventDbStorage(jdbcTemplate, eventRowMapper);
    }

    @Test
    void addEventTest() {
        eventStorage.createEvent(2L, EventType.FRIEND, Operation.ADD, 3L);    // друг 2 добавил друга 3
        eventStorage.createEvent(2L, EventType.FRIEND, Operation.REMOVE, 3L); // друг 2 удалил друга 3

        eventStorage.createEvent(3L, EventType.FRIEND, Operation.ADD, 4L);    // друг 3 добавил друга 4

        List<Event> feed = eventStorage.eventFeed(1L);

        assertEquals(3, feed.size());

        assertTrue(feed.stream().allMatch(e -> e.getUserId() == 2L || e.getUserId() == 3L));

        for (int i = 0; i < feed.size() - 1; i++) {
            assertTrue(feed.get(i).getTimestamp() <= feed.get(i + 1).getTimestamp());
        }
    }

    @Test
    void createAndReadEventTest() {
        eventStorage.createEvent(2L, EventType.FRIEND, Operation.ADD, 3L);

        List<Event> events = eventStorage.eventFeed(1L);

        assertEquals(1, events.size());


        Event event = events.get(0);
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertTrue(event.getTimestamp() > 0);
        assertEquals(EventType.FRIEND, event.getEventType());
        assertEquals(Operation.ADD, event.getOperation());
        assertEquals(3L, event.getEntityId());
    }
}
