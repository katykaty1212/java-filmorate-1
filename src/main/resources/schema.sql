CREATE TABLE IF NOT EXISTS mpa (
    mpa_id INTEGER PRIMARY KEY NOT NULL,
    name VARCHAR (10) NOT NULL
    );

CREATE TABLE IF NOT EXISTS genres (
    genre_id INTEGER PRIMARY KEY NOT NULL,
    name VARCHAR (100) NOT NULL
    );

CREATE TABLE IF NOT EXISTS films (
    film_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description VARCHAR (200),
    release_date DATE NOT NULL,
    duration BIGINT NOT NULL,
    mpa_id INTEGER NOT NULL,
    FOREIGN KEY (mpa_id) REFERENCES mpa(mpa_id)
    );

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(200) NOT NULL UNIQUE,
    login VARCHAR(200) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    birthday DATE
);

CREATE TABLE IF NOT EXISTS film_genre (
    film_id BIGINT NOT NULL,
    genre_id INTEGER NOT NULL,
    PRIMARY KEY (film_id, genre_id),
    FOREIGN KEY (film_id) REFERENCES films(film_id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(genre_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS likes (
    film_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (film_id, user_id),
    FOREIGN KEY (film_id) REFERENCES films(film_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS friendship (
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
        FOREIGN KEY (friend_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reviews (
    review_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content VARCHAR(1000) NOT NULL,
    is_positive BOOLEAN NOT NULL,
    user_id BIGINT NOT NULL,
    film_id BIGINT NOT NULL,
    useful INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (film_id) REFERENCES films(film_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS review_reactions (
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_like BOOLEAN NOT NULL,
    PRIMARY KEY (review_id, user_id),
    FOREIGN KEY (review_id) REFERENCES reviews(review_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
