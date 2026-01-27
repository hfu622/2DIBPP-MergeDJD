package com.example.demo.utils;

import lombok.Data;

import java.util.List;

@Data
public class Container {
    private Integer width;
    private Integer height;
    private List<Polygon> polygons;
}
