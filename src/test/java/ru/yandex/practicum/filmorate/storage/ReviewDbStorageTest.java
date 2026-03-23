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

        filmStorage = new FilmDbStorage(jdbcTemplate, filmRowMapper, mpaRowMapper, genreRowMapper);
        userStorage = new UserDbStorage(jdbcTemplate, userRowMapper, friendshipRowMapper);
        reviewStorage = new ReviewDbStorage(jdbcTemplate, reviewRowMapper);
    }

    private User createTestUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.create(user);
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

        return filmStorage.create(film);
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
    void shouldCreateReview() {
        User user = createTestUser("create-review@mail.ru", "createReview");
        Film film = createTestFilm("Create review film");

        Review createdReview = reviewStorage.create(createTestReview(user.getId(), film.getId(), "Хорошо", true));

        assertNotNull(createdReview.getReviewId());
        assertEquals(0, createdReview.getUseful());
    }

    @Test
    void shouldFindReviewById() {
        User user = createTestUser("find-review@mail.ru", "findReview");
        Film film = createTestFilm("Find review film");

        Review createdReview = reviewStorage.create(createTestReview(user.getId(), film.getId(), "Ищем отзыв", true));
        Optional<Review> foundReview = reviewStorage.getReviewById(createdReview.getReviewId());

        assertTrue(foundReview.isPresent());
        assertEquals(createdReview.getReviewId(), foundReview.get().getReviewId());
    }

    @Test
    void shouldUpdateReview() {
        User user = createTestUser("update-review-storage@mail.ru", "updateReviewStorage");
        Film film = createTestFilm("Update review storage film");

        Review createdReview = reviewStorage.create(createTestReview(user.getId(), film.getId(), "Было", true));
        createdReview.setContent("Стало");
        createdReview.setIsPositive(false);

        Review updatedReview = reviewStorage.update(createdReview);

        assertEquals("Стало", updatedReview.getContent());
        assertFalse(updatedReview.getIsPositive());
    }

    @Test
    void shouldDeleteReview() {
        User user = createTestUser("delete-review-storage@mail.ru", "deleteReviewStorage");
        Film film = createTestFilm("Delete review storage film");

        Review createdReview = reviewStorage.create(createTestReview(user.getId(), film.getId(), "Удаляем", true));
        reviewStorage.delete(createdReview.getReviewId());

        Optional<Review> foundReview = reviewStorage.getReviewById(createdReview.getReviewId());
        assertFalse(foundReview.isPresent());
    }

    @Test
    void shouldAddLikeAndDislike() {
        User author = createTestUser("author-storage@mail.ru", "authorStorage");
        User likeUser = createTestUser("like-storage@mail.ru", "likeStorage");
        User dislikeUser = createTestUser("dislike-storage@mail.ru", "dislikeStorage");
        Film film = createTestFilm("Reaction review storage film");

        Review createdReview = reviewStorage.create(createTestReview(author.getId(), film.getId(), "Реакции", true));

        reviewStorage.addLike(createdReview.getReviewId(), likeUser.getId());
        reviewStorage.addDislike(createdReview.getReviewId(), dislikeUser.getId());

        Optional<Review> foundReview = reviewStorage.getReviewById(createdReview.getReviewId());
        assertTrue(foundReview.isPresent());
        assertEquals(0, foundReview.get().getUseful());
    }

    @Test
    void shouldDeleteLike() {
        User author = createTestUser("delete-like-storage-author@mail.ru", "deleteLikeStorageAuthor");
        User likeUser = createTestUser("delete-like-storage-user@mail.ru", "deleteLikeStorageUser");
        Film film = createTestFilm("Delete like storage film");

        Review createdReview = reviewStorage.create(createTestReview(author.getId(), film.getId(), "Лайк для удаления", true));

        reviewStorage.addLike(createdReview.getReviewId(), likeUser.getId());
        assertEquals(1, reviewStorage.getReviewById(createdReview.getReviewId()).get().getUseful());

        reviewStorage.deleteLike(createdReview.getReviewId(), likeUser.getId());

        Optional<Review> foundReview = reviewStorage.getReviewById(createdReview.getReviewId());
        assertTrue(foundReview.isPresent());
        assertEquals(0, foundReview.get().getUseful());
    }

    @Test
    void shouldDeleteDislike() {
        User author = createTestUser("delete-dislike-storage-author@mail.ru", "deleteDislikeStorageAuthor");
        User dislikeUser = createTestUser("delete-dislike-storage-user@mail.ru", "deleteDislikeStorageUser");
        Film film = createTestFilm("Delete dislike storage film");

        Review createdReview = reviewStorage.create(createTestReview(author.getId(), film.getId(), "Дизлайк для удаления", true));

        reviewStorage.addDislike(createdReview.getReviewId(), dislikeUser.getId());
        assertEquals(-1, reviewStorage.getReviewById(createdReview.getReviewId()).get().getUseful());

        reviewStorage.deleteDislike(createdReview.getReviewId(), dislikeUser.getId());

        Optional<Review> foundReview = reviewStorage.getReviewById(createdReview.getReviewId());
        assertTrue(foundReview.isPresent());
        assertEquals(0, foundReview.get().getUseful());
    }

    @Test
    void shouldGetReviewsByFilmSortedByUseful() {
        User author = createTestUser("sorted-storage-author@mail.ru", "sortedStorageAuthor");
        User likeUser = createTestUser("sorted-storage-like@mail.ru", "sortedStorageLike");
        Film film = createTestFilm("Sorted review storage film");

        Review firstReview = reviewStorage.create(createTestReview(author.getId(), film.getId(), "Первый", true));
        Review secondReview = reviewStorage.create(createTestReview(author.getId(), film.getId(), "Второй", true));

        reviewStorage.addLike(secondReview.getReviewId(), likeUser.getId());

        List<Review> reviews = reviewStorage.getReviews(film.getId(), 10);

        assertEquals(2, reviews.size());
        assertEquals(secondReview.getReviewId(), reviews.get(0).getReviewId());
        assertEquals(firstReview.getReviewId(), reviews.get(1).getReviewId());
    }

    @Test
    void shouldGetAllReviewsWithoutFilmId() {
        User author = createTestUser("all-storage-author@mail.ru", "allStorageAuthor");
        User likeUser = createTestUser("all-storage-like@mail.ru", "allStorageLike");
        Film firstFilm = createTestFilm("All storage first film");
        Film secondFilm = createTestFilm("All storage second film");

        for (int i = 1; i <= 9; i++) {
            reviewStorage.create(createTestReview(author.getId(), firstFilm.getId(), "Отзыв " + i, true));
        }

        Review likedReview = reviewStorage.create(createTestReview(author.getId(), secondFilm.getId(), "Отзыв с лайком", true));
        Review skippedReview = reviewStorage.create(createTestReview(author.getId(), secondFilm.getId(), "Лишний отзыв", true));

        reviewStorage.addLike(likedReview.getReviewId(), likeUser.getId());

        List<Review> reviews = reviewStorage.getReviews(null, 10);

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
