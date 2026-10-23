package ru.itmo.soa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDto {
    private String name;
    private String parentLegion;
    private String world;
}
