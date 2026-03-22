package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {
    private final FilmStorage filmStorage;

    public List<Film> getRecommendations(Long userId) {
        Set<Long> currentUserLikes = filmStorage.getLikedFilmIds(userId);
        Map<Long, Set<Long>> allLikes = filmStorage.getAllUserLikes();

        if (currentUserLikes.isEmpty()) {
            return List.of();
        }

        Long similarUserId = null;
        int maxIntersection = 0;

        for (Map.Entry<Long, Set<Long>> entry : allLikes.entrySet()) {
            Long otherUserId = entry.getKey();

            if (otherUserId.equals(userId)) continue;

            Set<Long> otherUserLikes = entry.getValue();

            Set<Long> intersection = new HashSet<>(currentUserLikes);
            intersection.retainAll(otherUserLikes);

            if (intersection.size() > maxIntersection) {
                maxIntersection = intersection.size();
                similarUserId = otherUserId;
            }
        }

        if (similarUserId == null) {
            return List.of();
        }

        Set<Long> similarUserLikes = allLikes.get(similarUserId);

        Set<Long> recommendations = new HashSet<>(similarUserLikes);
        recommendations.removeAll(currentUserLikes);

        return filmStorage.getFilmsByIds(recommendations);
    }

}
