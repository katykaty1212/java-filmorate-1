package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.film.friendship.FriendshipRowMapper;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRowMapper;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserRowMapper;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReviewControllerTest {

    private ReviewController reviewController;
    private UserController userController;
    private FilmController filmController;
    private JdbcTemplate jdbcTemplate;
    private EmbeddedDatabase embeddedDatabase;

    @BeforeEach
    void setUp() {
        embeddedDatabase = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .addScript("classpath:data.sql")
                .build();

        jdbcTemplate = new JdbcTemplate(embeddedDatabase);

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

        MpaDbStorage mpaDbStorage = new MpaDbStorage(jdbcTemplate, mpaRowMapper);
        GenreDbStorage genreDbStorage = new GenreDbStorage(jdbcTemplate, genreRowMapper);
        FilmDbStorage filmStorage = new FilmDbStorage(jdbcTemplate, filmRowMapper, mpaRowMapper, genreRowMapper);
        UserDbStorage userStorage = new UserDbStorage(jdbcTemplate, userRowMapper, friendshipRowMapper);
        ReviewDbStorage reviewStorage = new ReviewDbStorage(jdbcTemplate, reviewRowMapper);

        UserService userService = new UserService(userStorage);
        FilmService filmService = new FilmService(filmStorage, userService, mpaDbStorage, genreDbStorage);
        ReviewService reviewService = new ReviewService(reviewStorage, userService, filmService);

        userController = new UserController(userService);
        filmController = new FilmController(filmService);
        reviewController = new ReviewController(reviewService);
    }

    @AfterEach
    void tearDown() {
        embeddedDatabase.shutdown();
    }

    @Test
    void createAndGetReviewTest() {
        User user = userController.create(createUser("test12@mail.ru", "user12"));
        Film film = filmController.create(createFilm("Film 8"));

        Review createdReview = reviewController.create(createReview(user.getId(), film.getId(), "Очень хороший фильм", true));
        Review foundReview = reviewController.getReviewById(createdReview.getReviewId());

        assertNotNull(foundReview);
        assertEquals(createdReview.getReviewId(), foundReview.getReviewId());
        assertEquals("Очень хороший фильм", foundReview.getContent());
    }

    @Test
    void updateReviewTest() {
        User user = userController.create(createUser("test13@mail.ru", "user13"));
        Film film = filmController.create(createFilm("Film 9"));
        Review createdReview = reviewController.create(createReview(user.getId(), film.getId(), "Старый отзыв", true));

        createdReview.setContent("Новый отзыв");
        createdReview.setIsPositive(false);
        Review updatedReview = reviewController.update(createdReview);

        assertEquals("Новый отзыв", updatedReview.getContent());
        assertFalse(updatedReview.getIsPositive());
    }

    @Test
    void deleteReviewTest() {
        User user = userController.create(createUser("test14@mail.ru", "user14"));
        Film film = filmController.create(createFilm("Film 10"));
        Review createdReview = reviewController.create(createReview(user.getId(), film.getId(), "Удаляем отзыв", true));

        reviewController.delete(createdReview.getReviewId());

        assertThrows(RuntimeException.class, () -> reviewController.getReviewById(createdReview.getReviewId()));
    }

    @Test
    void likeAndDislikeReviewTest() {
        User author = userController.create(createUser("test15@mail.ru", "user15"));
        User voter = userController.create(createUser("test16@mail.ru", "user16"));
        Film film = filmController.create(createFilm("Film 11"));
        Review review = reviewController.create(createReview(author.getId(), film.getId(), "Полезный отзыв", true));

        reviewController.addLike(review.getReviewId(), voter.getId());
        Review reviewAfterLike = reviewController.getReviewById(review.getReviewId());
        assertEquals(1, reviewAfterLike.getUseful());

        reviewController.deleteLike(review.getReviewId(), voter.getId());
        reviewController.addDislike(review.getReviewId(), voter.getId());
        Review reviewAfterDislike = reviewController.getReviewById(review.getReviewId());
        assertEquals(-1, reviewAfterDislike.getUseful());
    }

    @Test
    void getReviewsByFilmTest() {
        User user = userController.create(createUser("test17@mail.ru", "user17"));
        Film film = filmController.create(createFilm("Film 12"));
        Review firstReview = reviewController.create(createReview(user.getId(), film.getId(), "Первый отзыв", true));
        Review secondReview = reviewController.create(createReview(user.getId(), film.getId(), "Второй отзыв", true));

        reviewController.addLike(secondReview.getReviewId(), user.getId());

        List<Review> reviews = reviewController.getReviews(film.getId(), 10L);

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
