package com.example.demo.utils;

import lombok.Data;

import java.util.List;

@Data
public class InputData {
    private Integer width;  // 箱子的宽度
    private Integer height; // 箱子的长度
    private List<List<Points>> pieces;    // 存放零件的列表
    private Integer num;    // 零件数量
}
