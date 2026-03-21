package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.Collection;

public interface DirectorStorage {

    Collection<Director> findAll();

    Director create(Director director);

    Director update(Director director);

    Director delete(Long directorId);

    Director findById(Long directorId);
}