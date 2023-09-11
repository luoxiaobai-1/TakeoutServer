package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {
    @Select("select * from orders where status=#{status} and order_time<#{time}")
    public List<Orders> selectovertime(Integer status, LocalDateTime time);

    @Select("select * from orders where number=#{outTradeNo} and user_id=#{userId}")
    Orders getByNumberAndUserId(String outTradeNo, Long userId);

    Double sumByMap(Map map);

    Integer countByMap(Map map);

    List<GoodsSalesDTO> getSalesTop10(LocalDateTime beginTime, LocalDateTime endTime);
}
