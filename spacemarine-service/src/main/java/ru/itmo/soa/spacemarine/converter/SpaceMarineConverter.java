package ru.itmo.soa.spacemarine.converter;

import org.springframework.stereotype.Component;
import ru.itmo.soa.common.dto.ChapterDto;
import ru.itmo.soa.common.dto.CoordinatesDto;
import ru.itmo.soa.common.dto.SpaceMarineDto;
import ru.itmo.soa.common.dto.SpaceMarineInputDto;
import ru.itmo.soa.spacemarine.entity.ChapterEmbeddable;
import ru.itmo.soa.spacemarine.entity.CoordinatesEmbeddable;
import ru.itmo.soa.spacemarine.entity.SpaceMarineEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SpaceMarineConverter {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public SpaceMarineDto toDto(SpaceMarineEntity entity) {
        if (entity == null) return null;

        CoordinatesDto coordinatesDto = null;
        if (entity.getCoordinates() != null) {
            coordinatesDto = new CoordinatesDto(
                    entity.getCoordinates().getX(),
                    entity.getCoordinates().getY()
            );
        }

        ChapterDto chapterDto = null;
        if (entity.getChapter() != null) {
            chapterDto = new ChapterDto(
                    entity.getChapter().getName(),
                    entity.getChapter().getParentLegion(),
                    entity.getChapter().getWorld()
            );
        }

        String creationDateStr = entity.getCreationDate() != null
                ? entity.getCreationDate().format(DATE_FORMATTER)
                : null;

        return new SpaceMarineDto(
                entity.getId(),
                entity.getName(),
                coordinatesDto,
                creationDateStr,
                entity.getHealth(),
                entity.getHeartCount(),
                entity.getWeaponType(),
                entity.getMeleeWeapon(),
                chapterDto
        );
    }

    public SpaceMarineEntity toEntity(SpaceMarineInputDto dto) {
        if (dto == null) return null;

        SpaceMarineEntity entity = new SpaceMarineEntity();
        updateEntityFromDto(entity, dto);
        entity.setCreationDate(LocalDateTime.now());
        return entity;
    }

    public void updateEntityFromDto(SpaceMarineEntity entity, SpaceMarineInputDto dto) {
        entity.setName(dto.getName());

        if (dto.getCoordinates() != null) {
            entity.setCoordinates(new CoordinatesEmbeddable(
                    dto.getCoordinates().getX(),
                    dto.getCoordinates().getY()
            ));
        } else {
            entity.setCoordinates(null);
        }

        entity.setHealth(dto.getHealth());
        entity.setHeartCount(dto.getHeartCount());
        entity.setWeaponType(dto.getWeaponType());
        entity.setMeleeWeapon(dto.getMeleeWeapon());

        if (dto.getChapter() != null) {
            entity.setChapter(new ChapterEmbeddable(
                    dto.getChapter().getName(),
                    dto.getChapter().getParentLegion(),
                    dto.getChapter().getWorld()
            ));
        } else {
            entity.setChapter(null);
        }
    }
}
