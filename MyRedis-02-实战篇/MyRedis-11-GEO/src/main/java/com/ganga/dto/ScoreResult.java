package com.ganga.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScoreResult {

    //分页后的数据
    private List<?> list;
    //最小时间戳 用于下次请求分页
    private Long minTime;
    //最小时间戳出现的次数 用于下次请求分页的偏移量
    private Integer offset;

}
