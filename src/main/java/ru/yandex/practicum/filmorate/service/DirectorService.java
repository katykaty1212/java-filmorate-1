package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    public Director findById(Long directorId) {
        return directorStorage.findById(directorId);
    }

    public Director create(Director newDirector) {
        return directorStorage.create(newDirector);
    }

    public Director update(Director newDirector) {
        return directorStorage.update(newDirector);
    }

    public Director delete(Long directorId) {
        return directorStorage.delete(directorId);
    }
}