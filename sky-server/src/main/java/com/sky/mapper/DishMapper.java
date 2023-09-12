package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    Integer countByMap(Map map);
}
