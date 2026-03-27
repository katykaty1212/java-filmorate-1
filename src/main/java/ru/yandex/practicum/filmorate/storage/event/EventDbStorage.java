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
        String sql = """
                     SELECT e.*
                     FROM events e
                     WHERE e.user_id = ?
                     ORDER BY e.event_id ASC;
                """;

        return jdbcTemplate.query(sql, mapper, userId);
    }

    public Event createEvent(Long userId, EventType eventType, Operation operation, Long entityId) {

        String sql = "INSERT INTO events (user_id, event_type, operation, entity_id, time_stamp) " +
                "VALUES (?, ?, ?, ?, ?)";

        Long timeStamp = Instant.now().toEpochMilli();
        long eventId = insert(sql, userId, eventType.name(), operation.name(), entityId, timeStamp);

        Event event = new Event();
        event.setEventId(eventId);
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setOperation(operation);
        event.setEntityId(entityId);
        event.setTimestamp(timeStamp);

        log.info("Создано событие с ID: {}", eventId);

        return event;
    }
}
