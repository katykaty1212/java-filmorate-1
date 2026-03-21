package ru.yandex.practicum.filmorate.storage.director;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.util.List;

@Component
@Slf4j
public class DirectorDbStorage extends BaseDbStorage<Director> implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;
    private final DirectorRowMapper directorRowMapper;

    public DirectorDbStorage(JdbcTemplate jdbcTemplate,
                             DirectorRowMapper directorRowMapper) {
        super(jdbcTemplate, directorRowMapper);
        this.jdbcTemplate = jdbcTemplate;
        this.directorRowMapper = directorRowMapper;
    }

    @Override
    public List<Director> findAll() {
        String sql = "SELECT * FROM directors ORDER BY director_id";
        return jdbcTemplate.query(sql, directorRowMapper);
    }

    @Override
    public Director create(Director director) {
        String sql = "INSERT INTO directors (name) " +
                "VALUES (?)";

        long id = insert(sql, director.getName());

        director.setId(id);
        log.info("Создан режиссер с ID: {} ", director.getId());

        return director;
    }

    @Override
    public Director update(Director director) {
        return null;
    }

    @Override
    public Director delete(Long directorId) {
        return null;
    }

    @Override
    public Director findById(Long directorId) {
        String sql = "SELECT * FROM directors WHERE director_id = ?";
        return jdbcTemplate.query(sql, directorRowMapper, directorId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Режиссер с id " + directorId + " не найден"));
    }
}