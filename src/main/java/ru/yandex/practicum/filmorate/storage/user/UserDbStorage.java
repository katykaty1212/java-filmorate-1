package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;
import ru.yandex.practicum.filmorate.storage.film.friendship.FriendshipRowMapper;

import java.util.*;

@Component
@Qualifier("userDbStorage")
@Slf4j
public class UserDbStorage extends BaseDbStorage<User> implements UserStorage {

    FriendshipRowMapper friendshipRowMapper;

    public UserDbStorage(JdbcTemplate jdbcTemplate,
                         UserRowMapper userRowMapper,
                         FriendshipRowMapper friendshipRowMapper) {
        super(jdbcTemplate, userRowMapper);
        this.friendshipRowMapper = friendshipRowMapper;
    }

    @Override
    public Collection<User> findAll() {
        String sql = "SELECT * FROM users";

        List<User> users = findMany(sql);
        log.info("Получен список всех пользователей.");

        return users;
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) " +
                "VALUES (?, ?, ?, ?)";

        long id = insert(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday()
        );

        user.setId(id);
        log.info("Создан пользователь с ID: {} ", user.getId());

        return user;
    }

    @Override
    public User update(User newUser) {

        Optional<User> existingUser = getUserById(newUser.getId());

        if (existingUser.isEmpty()) {
            log.warn("Попытка обновления несуществующего пользователя с ID: {}", newUser.getId());
            throw new NotFoundException("Пользователь с ID " + newUser.getId() + " не найден");

        }

        String sql = "UPDATE users SET " +
                "email = ?, " +
                "login = ?, " +
                "name = ?, " +
                "birthday = ? " +
                "WHERE user_id = ?";

        update(sql,
                newUser.getEmail(),
                newUser.getLogin(),
                newUser.getName(),
                newUser.getBirthday(),
                newUser.getId()
        );

        log.info("Обновлён пользователь с ID: {}", newUser.getId());

        return newUser;
    }

    @Override
    public User delete(Long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        User user = getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден."));

        update(sql, userId);
        log.info("Удалён пользователь с ID: {}", userId);
        return user;
    }

    @Override
    public Optional<User> getUserById(Long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        return findOne(sql, userId);
    }

    public void addFriend(Long userId, Long friendId) {
        String sql = "INSERT INTO friendship (user_id, friend_id, status) VALUES (?, ?, ?)";

        update(sql, userId, friendId, true);
        log.info("Пользователь {} подписался на пользователя {}", userId, friendId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";

        int deleteString = jdbcTemplate.update(sql, userId, friendId);

        if (deleteString > 0) {
            log.info("Пользователь {} удалил дружбу с пользователем {}", userId, friendId);
        } else {
            log.info("Дружба между {} и {} не существовала", userId, friendId);
        }
    }

    public List<User> getUserFriends(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ? AND f.status = ?";

        return jdbcTemplate.query(sql, mapper, userId, true);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f1 ON u.user_id = f1.friend_id " +
                "JOIN friendship f2 ON u.user_id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f1.status = ? " +
                "AND f2.user_id = ? AND f2.status = ?";

        return jdbcTemplate.query(sql, mapper,
                userId, true, otherId, true);
    }
}