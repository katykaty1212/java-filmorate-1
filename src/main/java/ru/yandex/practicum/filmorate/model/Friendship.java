package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Friendship {
    private Long userId; // кто добавил
    private Long friendId; //кого добавили
    private FriendshipStatus status;
}