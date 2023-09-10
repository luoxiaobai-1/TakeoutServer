package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {
    @Select("select * from orders where status=#{status} and order_time<#{time}")
    public List<Orders> selectovertime(Integer status, LocalDateTime time);

    @Select("select * from orders where number=#{outTradeNo} and user_id=#{userId}")
    Orders getByNumberAndUserId(String outTradeNo, Long userId);
}
