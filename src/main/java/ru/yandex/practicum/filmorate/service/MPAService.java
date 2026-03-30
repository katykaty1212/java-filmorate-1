package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MPAService {
    private final MpaDbStorage mpaDbStorage;

    public List<MPA> findAll() {
        return mpaDbStorage.findAll();
    }

    public MPA getMpaById(Long mpaId) {
        return mpaDbStorage.getMpaById(mpaId)
                .orElseThrow(() -> {
                    log.error("Рейтинг с ID {} не найден", mpaId);
                    return new NotFoundException("Рейтинг с ID " + mpaId + " не найден");
                });
    }

    public void validateMpaExists(Long mpaId) {
        mpaDbStorage.getMpaById(mpaId)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + mpaId + " не найден"));
    }
}