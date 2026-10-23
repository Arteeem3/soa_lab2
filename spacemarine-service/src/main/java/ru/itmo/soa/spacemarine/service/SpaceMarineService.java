package ru.itmo.soa.spacemarine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.common.dto.SpaceMarineDto;
import ru.itmo.soa.common.dto.SpaceMarineInputDto;
import ru.itmo.soa.common.dto.SpaceMarinesResponse;
import ru.itmo.soa.spacemarine.converter.SpaceMarineConverter;
import ru.itmo.soa.spacemarine.entity.SpaceMarineEntity;
import ru.itmo.soa.spacemarine.exception.NotFoundException;
import ru.itmo.soa.spacemarine.exception.ValidationException;
import ru.itmo.soa.spacemarine.repository.SpaceMarineRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceMarineService {

    private final SpaceMarineRepository repository;
    private final SpaceMarineConverter converter;

    @Transactional(readOnly = true)
    public SpaceMarinesResponse getAll(Map<String, String> filters,
                                       String sortBy,
                                       String sortOrder,
                                       Integer page,
                                       Integer pageSize) {
        if (page == null) page = 0;
        if (pageSize == null) pageSize = 10;

        if (page < 0) {
            throw new ValidationException(400, "Параметр 'page' должен быть неотрицательным числом");
        }
        if (pageSize < 1) {
            throw new ValidationException(400, "Параметр 'pageSize' должен быть больше 0");
        }

        if (sortBy != null && !repository.isValidSortField(sortBy)) {
            throw new ValidationException(400, "Недопустимое поле сортировки: " + sortBy);
        }

        if (sortOrder != null && !"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
            throw new ValidationException(400, "Недопустимый порядок сортировки: " + sortOrder + ". Допустимы: asc, desc");
        }

        long totalCount = repository.countFiltered(filters);
        int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);

        List<SpaceMarineEntity> entities = repository.getAllFiltered(filters, sortBy, sortOrder, page, pageSize);
        List<SpaceMarineDto> dtos = entities.stream()
                .map(converter::toDto)
                .collect(Collectors.toList());

        return new SpaceMarinesResponse(dtos, page, pageSize, totalPages, (int) totalCount);
    }

    @Transactional(readOnly = true)
    public SpaceMarineDto getById(Integer id) {
        validateId(id);
        SpaceMarineEntity entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Космодесантник с ID=" + id + " не найден"));
        return converter.toDto(entity);
    }

    @Transactional
    public SpaceMarineDto create(SpaceMarineInputDto inputDto) {
        validateInput(inputDto);
        SpaceMarineEntity entity = converter.toEntity(inputDto);
        SpaceMarineEntity saved = repository.save(entity);
        log.info("Created SpaceMarine with id={}", saved.getId());
        return converter.toDto(saved);
    }

    @Transactional
    public SpaceMarineDto update(Integer id, SpaceMarineInputDto inputDto) {
        validateId(id);
        validateInput(inputDto);

        SpaceMarineEntity entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Космодесантник с ID=" + id + " не найден"));

        converter.updateEntityFromDto(entity, inputDto);
        SpaceMarineEntity updated = repository.save(entity);
        log.info("Updated SpaceMarine with id={}", updated.getId());
        return converter.toDto(updated);
    }

    @Transactional
    public void delete(Integer id) {
        validateId(id);
        SpaceMarineEntity entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Космодесантник с ID=" + id + " не найден"));
        repository.delete(entity);
        log.info("Deleted SpaceMarine with id={}", id);
    }

    @Transactional(readOnly = true)
    public Double getAverageHealth() {
        return repository.getAverageHealth();
    }

    @Transactional(readOnly = true)
    public List<SpaceMarineDto> searchByName(String substring) {
        if (substring == null || substring.trim().isEmpty()) {
            throw new ValidationException(400, "Параметр 'substring' обязателен и не может быть пустым");
        }
        return repository.findByNameContaining(substring.trim()).stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SpaceMarineDto> getByHealthGreaterThan(Double threshold) {
        if (threshold == null) {
            throw new ValidationException(400, "Параметр 'threshold' обязателен");
        }
        return repository.findByHealthGreaterThan(threshold).stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new ValidationException(400, "Идентификатор должен быть положительным числом (> 0)");
        }
    }

    private void validateInput(SpaceMarineInputDto input) {
        if (input == null) {
            throw new ValidationException(400, "Тело запроса не может быть пустым");
        }

        if (input.getName() == null || input.getName().trim().isEmpty()) {
            throw new ValidationException(422, "Поле 'name' не может быть пустым");
        }

        if (input.getCoordinates() == null) {
            throw new ValidationException(422, "Координаты обязательны");
        }
        if (input.getCoordinates().getX() == null) {
            throw new ValidationException(422, "Координата 'x' не может быть null");
        }
        if (input.getCoordinates().getX() > 811) {
            throw new ValidationException(422, "Координата 'x' не может превышать 811");
        }
        if (input.getCoordinates().getY() == null) {
            throw new ValidationException(422, "Координата 'y' не может быть null");
        }

        if (input.getHealth() <= 0) {
            throw new ValidationException(422, "Поле 'health' должно быть строго больше 0");
        }

        if (input.getHeartCount() != null && (input.getHeartCount() < 1 || input.getHeartCount() > 3)) {
            throw new ValidationException(422, "Поле 'heartCount' должно быть в диапазоне от 1 до 3");
        }

        if (input.getWeaponType() == null) {
            throw new ValidationException(422, "Поле 'weaponType' обязательно");
        }

        if (input.getChapter() != null) {
            if (input.getChapter().getName() == null || input.getChapter().getName().trim().isEmpty()) {
                throw new ValidationException(422, "Поле 'chapter.name' не может быть пустым, если указан chapter");
            }
        }
    }
}
