package ru.itmo.soa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMarinesResponse {
    private List<SpaceMarineDto> spaceMarines;
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Integer totalCount;
}
