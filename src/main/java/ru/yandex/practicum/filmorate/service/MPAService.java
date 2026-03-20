package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    public MPA findById(@PathVariable Integer id) {
        return mpaDbStorage.findById(id);
    }
}