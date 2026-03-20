package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.film.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.storage.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRowMapper;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ReviewDbStorageTest {

    private ReviewDbStorage reviewStorage;
    private UserDbStorage userStorage;
    private FilmDbStorage filmStorage;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .addScript("classpath:data.sql")
                .build();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("DELETE FROM review_reactions");
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM likes");
        jdbcTemplate.execute("DELETE FROM film_genre");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");

        jdbcTemplate.execute("ALTER TABLE reviews ALTER COLUMN review_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");

        FilmRowMapper filmRowMapper = new FilmRowMapper();
        MpaRowMapper mpaRowMapper = new MpaRowMapper();
        GenreRowMapper genreRowMapper = new GenreRowMapper();
        UserRowMapper userRowMapper = new UserRowMapper();
        FriendshipRowMapper friendshipRowMapper = new FriendshipRowMapper();
        ReviewRowMapper reviewRowMapper = new ReviewRowMapper();

        filmStorage = new FilmDbStorage(jdbcTemplate, filmRowMapper, mpaRowMapper, genreRowMapper);
        userStorage = new UserDbStorage(jdbcTemplate, userRowMapper, friendshipRowMapper);
        reviewStorage = new ReviewDbStorage(jdbcTemplate, reviewRowMapper);
    }

    @Test
    void shouldCreateReview() {
        User user = userStorage.create(createUser("test1@mail.ru", "user1"));
        Film film = filmStorage.create(createFilm("Film 1"));

        Review createdReview = reviewStorage.create(createReview(user.getId(), film.getId(), "Хороший фильм", true));

        assertNotNull(createdReview.getReviewId());
        assertEquals("Хороший фильм", createdReview.getContent());
        assertEquals(0, createdReview.getUseful());
    }

    @Test
    void shouldFindReviewById() {
        User user = userStorage.create(createUser("test2@mail.ru", "user2"));
        Film film = filmStorage.create(createFilm("Film 2"));
        Review createdReview = reviewStorage.create(createReview(user.getId(), film.getId(), "Нормальный фильм", true));

        Optional<Review> foundReview = reviewStorage.getReviewById(createdReview.getReviewId());

        assertTrue(foundReview.isPresent());
        assertEquals(createdReview.getReviewId(), foundReview.get().getReviewId());
    }

    @Test
    void shouldUpdateReview() {
        User user = userStorage.create(createUser("test3@mail.ru", "user3"));
        Film film = filmStorage.create(createFilm("Film 3"));
        Review createdReview = reviewStorage.create(createReview(user.getId(), film.getId(), "Старый текст", true));

        createdReview.setContent("Новый текст");
        createdReview.setIsPositive(false);
        Review updatedReview = reviewStorage.update(createdReview);

        assertEquals("Новый текст", updatedReview.getContent());
        assertFalse(updatedReview.getIsPositive());
    }

    @Test
    void shouldDeleteReview() {
        User user = userStorage.create(createUser("test4@mail.ru", "user4"));
        Film film = filmStorage.create(createFilm("Film 4"));
        Review createdReview = reviewStorage.create(createReview(user.getId(), film.getId(), "Удаляемый отзыв", true));

        reviewStorage.delete(createdReview.getReviewId());

        assertTrue(reviewStorage.getReviewById(createdReview.getReviewId()).isEmpty());
    }

    @Test
    void shouldAddLikeToReview() {
        User author = userStorage.create(createUser("test5@mail.ru", "user5"));
        User voter = userStorage.create(createUser("test6@mail.ru", "user6"));
        Film film = filmStorage.create(createFilm("Film 5"));
        Review createdReview = reviewStorage.create(createReview(author.getId(), film.getId(), "Полезный отзыв", true));

        reviewStorage.addLike(createdReview.getReviewId(), voter.getId());

        Review savedReview = reviewStorage.getReviewById(createdReview.getReviewId()).orElseThrow();
        assertEquals(1, savedReview.getUseful());
    }

    @Test
    void shouldChangeDislikeToLike() {
        User author = userStorage.create(createUser("test7@mail.ru", "user7"));
        User voter = userStorage.create(createUser("test8@mail.ru", "user8"));
        Film film = filmStorage.create(createFilm("Film 6"));
        Review createdReview = reviewStorage.create(createReview(author.getId(), film.getId(), "Спорный отзыв", true));

        reviewStorage.addDislike(createdReview.getReviewId(), voter.getId());
        reviewStorage.addLike(createdReview.getReviewId(), voter.getId());

        Review savedReview = reviewStorage.getReviewById(createdReview.getReviewId()).orElseThrow();
        assertEquals(1, savedReview.getUseful());
    }



    @Test
    void shouldDeleteDislikeFromReview() {
        User author = userStorage.create(createUser("test12@mail.ru", "user12"));
        User voter = userStorage.create(createUser("test13@mail.ru", "user13"));
        Film film = filmStorage.create(createFilm("Film 8"));
        Review createdReview = reviewStorage.create(createReview(author.getId(), film.getId(), "Неполезный отзыв", true));

        reviewStorage.addDislike(createdReview.getReviewId(), voter.getId());
        reviewStorage.deleteDislike(createdReview.getReviewId(), voter.getId());

        Review savedReview = reviewStorage.getReviewById(createdReview.getReviewId()).orElseThrow();
        assertEquals(0, savedReview.getUseful());
    }

    @Test
    void shouldReturnAllReviewsWhenFilmIdIsNull() {
        User author = userStorage.create(createUser("test14@mail.ru", "user14"));
        User voter = userStorage.create(createUser("test15@mail.ru", "user15"));
        Film firstFilm = filmStorage.create(createFilm("Film 9"));
        Film secondFilm = filmStorage.create(createFilm("Film 10"));

        Review firstReview = reviewStorage.create(createReview(author.getId(), firstFilm.getId(), "Первый общий отзыв", true));
        Review secondReview = reviewStorage.create(createReview(author.getId(), secondFilm.getId(), "Второй общий отзыв", true));

        reviewStorage.addLike(secondReview.getReviewId(), voter.getId());

        List<Review> reviews = reviewStorage.getReviews(null, 10L);

        assertEquals(2, reviews.size());
        assertEquals(secondReview.getReviewId(), reviews.get(0).getReviewId());
        assertEquals(firstReview.getReviewId(), reviews.get(1).getReviewId());
    }

    @Test
    void shouldRespectCountLimit() {
        User author = userStorage.create(createUser("test16@mail.ru", "user16"));
        User voter = userStorage.create(createUser("test17@mail.ru", "user17"));
        Film film = filmStorage.create(createFilm("Film 11"));

        reviewStorage.create(createReview(author.getId(), film.getId(), "Первый отзыв с лимитом", true));
        Review secondReview = reviewStorage.create(createReview(author.getId(), film.getId(), "Второй отзыв с лимитом", true));

        reviewStorage.addLike(secondReview.getReviewId(), voter.getId());

        List<Review> reviews = reviewStorage.getReviews(film.getId(), 1L);

        assertEquals(1, reviews.size());
        assertEquals(secondReview.getReviewId(), reviews.get(0).getReviewId());
    }
    @Test
    void shouldReturnReviewsSortedByUseful() {
        User author = userStorage.create(createUser("test9@mail.ru", "user9"));
        User voter1 = userStorage.create(createUser("test10@mail.ru", "user10"));
        User voter2 = userStorage.create(createUser("test11@mail.ru", "user11"));
        Film film = filmStorage.create(createFilm("Film 7"));

        Review firstReview = reviewStorage.create(createReview(author.getId(), film.getId(), "Первый отзыв", true));
        Review secondReview = reviewStorage.create(createReview(author.getId(), film.getId(), "Второй отзыв", true));

        reviewStorage.addLike(secondReview.getReviewId(), voter1.getId());
        reviewStorage.addLike(secondReview.getReviewId(), voter2.getId());

        List<Review> reviews = reviewStorage.getReviews(film.getId(), 10L);

        assertEquals(2, reviews.size());
        assertEquals(secondReview.getReviewId(), reviews.get(0).getReviewId());
        assertEquals(firstReview.getReviewId(), reviews.get(1).getReviewId());
    }

    private User createUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    private Film createFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание " + name);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        MPA mpa = new MPA();
        mpa.setId(1);
        film.setMpa(mpa);
        return film;
    }

    private Review createReview(Long userId, Long filmId, String content, boolean isPositive) {
        Review review = new Review();
        review.setUserId(userId);
        review.setFilmId(filmId);
        review.setContent(content);
        review.setIsPositive(isPositive);
        return review;
    }
}
