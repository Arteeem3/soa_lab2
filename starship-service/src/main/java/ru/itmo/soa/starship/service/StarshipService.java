package ru.itmo.soa.starship.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.soa.common.dto.SpaceMarineDto;
import ru.itmo.soa.common.dto.SpaceMarinesResponse;
import ru.itmo.soa.starship.client.SpaceMarineClient;
import ru.itmo.soa.starship.exception.ErrorDescriptions;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarshipService {

    private final SpaceMarineClient spaceMarineClient;

    public Map<String, Object> unloadMarine(Long starshipId, Integer spaceMarineId) {
        if (starshipId == null || starshipId <= 0) {
            throw ErrorDescriptions.INVALID_STARSHIP_ID.exception();
        }
        if (spaceMarineId == null || spaceMarineId <= 0) {
            throw ErrorDescriptions.INVALID_MARINE_ID.exception();
        }

        SpaceMarineDto marine = spaceMarineClient.getById(spaceMarineId);
        log.info("Marine {} found. Unloading from starship {}", marine.getName(), starshipId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", String.format("Космодесантник %s (ID=%d) успешно высажен с корабля ID=%d",
                marine.getName(), spaceMarineId, starshipId));
        return result;
    }

    public Map<String, Object> unloadAll(Long starshipId) {
        if (starshipId == null || starshipId <= 0) {
            throw ErrorDescriptions.INVALID_STARSHIP_ID.exception();
        }

        SpaceMarinesResponse response = spaceMarineClient.getAll();
        int count = (response != null && response.getSpaceMarines() != null)
                ? response.getSpaceMarines().size()
                : 0;

        log.info("Unloaded all {} marines from starship {}", count, starshipId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", String.format("Все %d космодесантников успешно высажены с корабля ID=%d",
                count, starshipId));
        return result;
    }
}
