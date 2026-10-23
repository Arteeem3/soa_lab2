package ru.itmo.soa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.itmo.soa.common.enums.MeleeWeapon;
import ru.itmo.soa.common.enums.Weapon;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMarineInputDto {
    private String name;
    private CoordinatesDto coordinates;
    private double health;
    private Integer heartCount;
    private Weapon weaponType;
    private MeleeWeapon meleeWeapon;
    private ChapterDto chapter;
}
