package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
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

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReviewControllerTest {

    private ReviewController reviewController;
    private UserController userController;
    private FilmController filmController;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .addScript("classpath:data.sql")
                .build();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("DELETE FROM review_likes");
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

    private User createTestUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userController.create(user);
    }

    private Film createTestFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание " + name);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        MPA mpa = new MPA();
        mpa.setId(1);
        film.setMpa(mpa);

        return filmController.create(film);
    }

    private Review createTestReview(Long userId, Long filmId, String content, boolean isPositive) {
        Review review = new Review();
        review.setContent(content);
        review.setIsPositive(isPositive);
        review.setUserId(userId);
        review.setFilmId(filmId);
        return review;
    }

    @Test
    public void createReviewTest() {
        User user = createTestUser("review@mail.ru", "reviewUser");
        Film film = createTestFilm("Review film");

        Review createdReview = reviewController.create(createTestReview(user.getId(), film.getId(), "Хороший фильм", true));

        assertNotNull(createdReview.getReviewId());
        assertEquals("Хороший фильм", createdReview.getContent());
        assertEquals(0, createdReview.getUseful());
    }

    @Test
    public void updateReviewTest() {
        User user = createTestUser("update-review@mail.ru", "updateReviewUser");
        Film film = createTestFilm("Update review film");

        Review createdReview = reviewController.create(createTestReview(user.getId(), film.getId(), "Нормально", true));
        createdReview.setContent("Стало лучше");
        createdReview.setIsPositive(false);

        Review updatedReview = reviewController.update(createdReview);

        assertEquals(createdReview.getReviewId(), updatedReview.getReviewId());
        assertEquals("Стало лучше", updatedReview.getContent());
        assertFalse(updatedReview.getIsPositive());
    }

    @Test
    public void getReviewByIdTest() {
        User user = createTestUser("get-review@mail.ru", "getReviewUser");
        Film film = createTestFilm("Get review film");

        Review createdReview = reviewController.create(createTestReview(user.getId(), film.getId(), "Можно посмотреть", true));
        Review foundReview = reviewController.getReviewById(createdReview.getReviewId());

        assertEquals(createdReview.getReviewId(), foundReview.getReviewId());
        assertEquals("Можно посмотреть", foundReview.getContent());
    }

    @Test
    public void deleteReviewTest() {
        User user = createTestUser("delete-review@mail.ru", "deleteReviewUser");
        Film film = createTestFilm("Delete review film");

        Review createdReview = reviewController.create(createTestReview(user.getId(), film.getId(), "Удаляем отзыв", true));
        reviewController.delete(createdReview.getReviewId());

        assertThrows(RuntimeException.class, () -> reviewController.getReviewById(createdReview.getReviewId()));
    }

    @Test
    public void addLikeAndDislikeTest() {
        User author = createTestUser("author-review@mail.ru", "authorReviewUser");
        User firstUser = createTestUser("first-review@mail.ru", "firstReviewUser");
        User secondUser = createTestUser("second-review@mail.ru", "secondReviewUser");
        Film film = createTestFilm("Likes review film");

        Review createdReview = reviewController.create(createTestReview(author.getId(), film.getId(), "Спорный фильм", true));

        reviewController.addLike(createdReview.getReviewId(), firstUser.getId());
        reviewController.addDislike(createdReview.getReviewId(), secondUser.getId());

        Review foundReview = reviewController.getReviewById(createdReview.getReviewId());
        assertEquals(0, foundReview.getUseful());
    }

    @Test
    public void deleteLikeTest() {
        User author = createTestUser("delete-like-author@mail.ru", "deleteLikeAuthor");
        User likeUser = createTestUser("delete-like-user@mail.ru", "deleteLikeUser");
        Film film = createTestFilm("Delete like review film");

        Review createdReview = reviewController.create(createTestReview(author.getId(), film.getId(), "Лайк для удаления", true));

        reviewController.addLike(createdReview.getReviewId(), likeUser.getId());
        assertEquals(1, reviewController.getReviewById(createdReview.getReviewId()).getUseful());

        reviewController.deleteLike(createdReview.getReviewId(), likeUser.getId());

        Review foundReview = reviewController.getReviewById(createdReview.getReviewId());
        assertEquals(0, foundReview.getUseful());
    }

    @Test
    public void deleteDislikeTest() {
        User author = createTestUser("delete-dislike-author@mail.ru", "deleteDislikeAuthor");
        User dislikeUser = createTestUser("delete-dislike-user@mail.ru", "deleteDislikeUser");
        Film film = createTestFilm("Delete dislike review film");

        Review createdReview = reviewController.create(createTestReview(author.getId(), film.getId(), "Дизлайк для удаления", true));

        reviewController.addDislike(createdReview.getReviewId(), dislikeUser.getId());
        assertEquals(-1, reviewController.getReviewById(createdReview.getReviewId()).getUseful());

        reviewController.deleteDislike(createdReview.getReviewId(), dislikeUser.getId());

        Review foundReview = reviewController.getReviewById(createdReview.getReviewId());
        assertEquals(0, foundReview.getUseful());
    }

    @Test
    public void getReviewsByFilmSortedByUsefulTest() {
        User author = createTestUser("sorted-author@mail.ru", "sortedAuthor");
        User likeUser = createTestUser("sorted-like@mail.ru", "sortedLike");
        Film film = createTestFilm("Sorted reviews film");

        Review firstReview = reviewController.create(createTestReview(author.getId(), film.getId(), "Первый отзыв", true));
        Review secondReview = reviewController.create(createTestReview(author.getId(), film.getId(), "Второй отзыв", true));

        reviewController.addLike(secondReview.getReviewId(), likeUser.getId());

        List<Review> reviews = reviewController.getReviews(film.getId(), 10);

        assertEquals(2, reviews.size());
        assertEquals(secondReview.getReviewId(), reviews.get(0).getReviewId());
        assertEquals(firstReview.getReviewId(), reviews.get(1).getReviewId());
    }

    @Test
    public void getAllReviewsWithDefaultCountTest() {
        User author = createTestUser("all-reviews-author@mail.ru", "allReviewsAuthor");
        User likeUser = createTestUser("all-reviews-like@mail.ru", "allReviewsLike");
        Film firstFilm = createTestFilm("All reviews first film");
        Film secondFilm = createTestFilm("All reviews second film");

        for (int i = 1; i <= 9; i++) {
            reviewController.create(createTestReview(author.getId(), firstFilm.getId(), "Отзыв " + i, true));
        }

        Review likedReview = reviewController.create(createTestReview(author.getId(), secondFilm.getId(), "Отзыв с лайком", true));
        Review skippedReview = reviewController.create(createTestReview(author.getId(), secondFilm.getId(), "Лишний отзыв", true));

        reviewController.addLike(likedReview.getReviewId(), likeUser.getId());

        List<Review> reviews = reviewController.getReviews(null, null);

        boolean likedReviewFound = false;
        boolean skippedReviewFound = false;

        for (Review review : reviews) {
            if (review.getReviewId().equals(likedReview.getReviewId())) {
                likedReviewFound = true;
            }

            if (review.getReviewId().equals(skippedReview.getReviewId())) {
                skippedReviewFound = true;
            }
        }

        assertEquals(10, reviews.size());
        assertEquals(likedReview.getReviewId(), reviews.get(0).getReviewId());
        assertTrue(likedReviewFound);
        assertFalse(skippedReviewFound);
    }
}
