package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;

@Service
@Slf4j
public class UserService {

    public final UserStorage userStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User create(User user) {
        validateNameAndSet(user);
        return userStorage.create(user);
    }

    public User update(User newUser) {
        validateNameAndSet(newUser);
        return userStorage.update(newUser);
    }

    public User delete(Long userId) {
        return userStorage.delete(userId);
    }

    public User getUserById(Long userId) {
        return userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден", userId);
                    return new NotFoundException("Пользователь с ID " + userId + " не найден");
                });
    }

    public void addFriend(Long userId, Long friendId) {

        if (userId.equals(friendId)) {
            log.warn("Попытка добавить себя в друзья: {}", userId);
            throw new ValidationException("Нельзя добавить себя в друзья");
        }

        try {
            userStorage.addFriend(userId, friendId);
        } catch (DuplicateKeyException e) {
            throw new ValidationException("Пользователь с ID " +
                    friendId + " уже в друзьях у пользователя с ID" + userId);
        }
    }

    public void acceptFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);

        userStorage.acceptFriend(userId, friendId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);
        log.info("Попытка пользователя с ID {} удалить из друзей пользователя с ID {}", userId, friendId);

        userStorage.deleteFriend(userId, friendId);
        log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
    }

    public List<User> getListUserFriend(Long userId) {
        getUserById(userId);
        return userStorage.getUserFriends(userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        getUserById(userId);
        getUserById(otherId);
        log.info("Попытка получить список общих друзей пользователей ID {} и ID {}", userId, otherId);

        return userStorage.getCommonFriends(userId, otherId);
    }

    private void validateNameAndSet(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Имя отсутствует, вместо него установлен логин {}.", user.getLogin());
        }
    }
}