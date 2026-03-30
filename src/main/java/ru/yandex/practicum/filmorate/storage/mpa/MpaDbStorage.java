package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MPA;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;
    private final MpaRowMapper mpaRowMapper;

    public List<MPA> findAll() {
        String sql = "SELECT * FROM mpa ORDER BY mpa_id";
        return jdbcTemplate.query(sql, mpaRowMapper);
    }

    public Optional<MPA> getMpaById(Long mpaId) {
        String sql = "SELECT * FROM mpa WHERE mpa_id = ?";

        try {
            MPA mpa = jdbcTemplate.queryForObject(sql, mpaRowMapper, mpaId);
            log.info("Рейтинг с ID: {}", mpaId);

            return Optional.of((mpa));
        } catch (EmptyResultDataAccessException e) {
            log.warn("Рейтинг по ID: {} не найден.", mpaId);
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Ошибка при загрузке рейтинга ID: {}.{}", mpaId, e.getMessage());
            return Optional.empty();
        }
    }

}