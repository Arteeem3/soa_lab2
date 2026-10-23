package ru.itmo.soa.spacemarine.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmo.soa.common.enums.MeleeWeapon;
import ru.itmo.soa.common.enums.Weapon;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "space_marine")
public class SpaceMarineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sm_seq")
    @SequenceGenerator(name = "sm_seq", sequenceName = "space_marine_seq", allocationSize = 1)
    private int id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private CoordinatesEmbeddable coordinates;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @Column(nullable = false)
    private double health;

    @Column(name = "heart_count")
    private Integer heartCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "weapon_type", nullable = false)
    private Weapon weaponType;

    @Enumerated(EnumType.STRING)
    @Column(name = "melee_weapon")
    private MeleeWeapon meleeWeapon;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "chapter_name")),
            @AttributeOverride(name = "parentLegion", column = @Column(name = "chapter_parent_legion")),
            @AttributeOverride(name = "world", column = @Column(name = "chapter_world"))
    })
    private ChapterEmbeddable chapter;
}
