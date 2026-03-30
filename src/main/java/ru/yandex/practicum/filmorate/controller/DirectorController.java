package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.Collection;

@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
public class DirectorController {

    private final DirectorService directorService;

    @GetMapping
    public Collection<Director> findAll() {
        return directorService.findAll();
    }

    @GetMapping("/{directorId}")
    public Director getDirectorById(@PathVariable Long directorId) {
        return directorService.getDirectorById(directorId);
    }

    @PostMapping
    public Director create(@Valid @RequestBody Director newDirector) {
        return directorService.create(newDirector);
    }

    @PutMapping
    public Director update(@Valid @RequestBody Director newDirector) {
        return directorService.update(newDirector);
    }

    @DeleteMapping("/{directorId}")
    public void delete(@PathVariable Long directorId) {
        directorService.delete(directorId);
    }
}