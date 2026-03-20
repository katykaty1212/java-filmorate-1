package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.service.MPAService;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MPAController {

    private final MPAService mpaService;

    @GetMapping
    public List<MPA> findAll() {
        return mpaService.findAll();
    }

    @GetMapping("/{id}")
    public MPA findById(@PathVariable Integer id) {
        return mpaService.findById(id);
    }
}