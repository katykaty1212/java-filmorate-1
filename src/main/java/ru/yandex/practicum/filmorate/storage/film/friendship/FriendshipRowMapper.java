package ru.yandex.practicum.filmorate.storage.film.friendship;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class FriendshipRowMapper implements RowMapper<Friendship> {
    @Override
    public Friendship mapRow(ResultSet rs, int rowNum) throws SQLException {
        Friendship fr = new Friendship();
        fr.setUserId(rs.getLong("user_id"));
        fr.setFriendId(rs.getLong("friend_id"));

        boolean status = rs.getBoolean("status");
        fr.setStatus(status ? FriendshipStatus.CONFIRMED : FriendshipStatus.PENDING);

        return fr;
    }
}