package ru.yandex.practicum.filmorate.storage.director;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.util.List;
import java.util.Optional;

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
    public Director update(Director newDirector) {
        String sql = "UPDATE directors SET name = ? WHERE director_id = ?";

        update(sql, newDirector.getName(), newDirector.getId());
        log.info("Обновлён режиссер с ID: {}", newDirector.getId());

        return newDirector;
    }

    @Override
    public void delete(Long directorId) {
        jdbcTemplate.update("DELETE FROM directors WHERE director_id = ?", directorId);
    }

    @Override
    public Optional<Director> getDirectorById(Long directorId) {
        String sql = "SELECT * FROM directors WHERE director_id = ?";

        try {
            Director director = jdbcTemplate.queryForObject(sql, directorRowMapper, directorId);
            return Optional.of(director);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Режиссер по ID: {} не найден.", directorId);
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Режиссер при загрузке фильма ID: {}.{}", directorId, e.getMessage());
            return Optional.empty();
        }
    }

}