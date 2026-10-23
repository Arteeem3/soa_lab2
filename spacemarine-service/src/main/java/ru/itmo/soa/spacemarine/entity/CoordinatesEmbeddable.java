package ru.itmo.soa.spacemarine.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CoordinatesEmbeddable {

    @Column(name = "coordinate_x", nullable = false)
    private Integer x;

    @Column(name = "coordinate_y", nullable = false)
    private Integer y;
}
