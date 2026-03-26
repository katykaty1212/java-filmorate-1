package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventDbStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    EventDbStorage eventDbStorage;

    public List<Event> eventFeed(Long userId) {
        return eventDbStorage.eventFeed(userId);
    }

    public void createEvent(Long userId, EventType eventType, Operation operation, Long entityId) {
        eventDbStorage.createEvent(userId, eventType, operation, entityId);
    }
}
