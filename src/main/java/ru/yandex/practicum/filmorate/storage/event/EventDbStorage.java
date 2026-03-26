package ru.yandex.practicum.filmorate.storage.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
public class EventDbStorage extends BaseDbStorage<Event> {

    public EventDbStorage(JdbcTemplate jdbcTemplate, EventRowMapper eventRowMapper) {
        super(jdbcTemplate, eventRowMapper);
    }

    public List<Event> eventFeed(Long userId) {
        String sql = "SELECT e.* " +
                "FROM events e " +
                "WHERE user_id = ? OR e.user_id IN (" +
                "SELECT friend_id " +
                "FROM friendship " +
                "WHERE user_id = ?) " +
                "ORDER BY time_stamp ASC ";

        return jdbcTemplate.query(sql, mapper, userId, userId);
    }

    public void createEvent(Long userId, EventType eventType, Operation operation, Long entityId) {

        String sql = "INSERT INTO events (user_id, event_type, operation, entity_id, time_stamp) " +
                "VALUES (?, ?, ?, ?, ?)";

        Event event = new Event();
        Long timeStamp = Instant.now().toEpochMilli();

        long eventId = insert(sql, userId, eventType.name(), operation.name(), entityId, timeStamp);
        log.info("Создано событие с ID: {}", eventId);

        event.setEventId(eventId);
    }
}
