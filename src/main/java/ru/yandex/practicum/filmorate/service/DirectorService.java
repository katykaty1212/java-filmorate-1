package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.Collection;

@Service
@Slf4j
@RequiredArgsConstructor
public class DirectorService {

    private final DirectorStorage directorStorage;

    public Collection<Director> findAll() {
        return directorStorage.findAll();
    }

    public Director getDirectorById(Long directorId) {
        return directorStorage.getDirectorById(directorId)
                .orElseThrow(() -> {
                    log.error("Режиссер с ID {} не найден", directorId);
                    return new NotFoundException("Режиссер с ID " + directorId + " не найден");
                });
    }

    public void validateDirectorExists(Long directorId) {
        directorStorage.getDirectorById(directorId)
                .orElseThrow(() -> new NotFoundException("Режиссер с ID " + directorId + " не найден"));
    }

    public Director create(Director newDirector) {
        return directorStorage.create(newDirector);
    }

    public Director update(Director newDirector) {
        validateDirectorExists(newDirector.getId());
        return directorStorage.update(newDirector);
    }

    public void delete(Long directorId) {
        validateDirectorExists(directorId);
        directorStorage.delete(directorId);
    }
}