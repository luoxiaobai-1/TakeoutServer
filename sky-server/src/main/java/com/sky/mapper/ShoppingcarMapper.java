package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingcarMapper extends BaseMapper<ShoppingCart> {

    List<ShoppingCart> list(ShoppingCart shoppingCart);
    boolean deleteone (ShoppingCartDTO shoppingCartDTO);
    boolean deleteByUserId(long userid);

    void insertBatch(List<ShoppingCart> shoppingCartList);
}
