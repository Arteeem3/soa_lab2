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
public class ChapterEmbeddable {

    @Column(name = "chapter_name")
    private String name;

    @Column(name = "chapter_parent_legion")
    private String parentLegion;

    @Column(name = "chapter_world")
    private String world;
}
