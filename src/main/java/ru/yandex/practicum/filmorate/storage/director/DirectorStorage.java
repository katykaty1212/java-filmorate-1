package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.Collection;
import java.util.Optional;

public interface DirectorStorage {

    Collection<Director> findAll();

    Director create(Director director);

    Director update(Director newDirector);

    void delete(Long directorId);

    Optional<Director> getDirectorById(Long directorId);
}