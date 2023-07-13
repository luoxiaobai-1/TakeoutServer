package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.DishfavorMapper;
import com.sky.mapper.SetmaldishMapper;
import com.sky.result.PageResult;
import com.sky.service.Dishservice;
import com.sky.utils.BeanCopyutil;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class Dishserviceimpl implements Dishservice {
    @Autowired
    DishfavorMapper dishfavorMapper;
    @Autowired
    DishMapper dishMapper;
    @Autowired
    CategoryMapper categoryMapper;
    @Autowired
    SetmaldishMapper setmaldishMapper;
    @Override
    @Transactional
    public void save(DishDTO dishDTO) {
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors!=null&&flavors.size()>0)
        {
            dishfavorMapper.InsertList(flavors);
        }


    }

    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        PageResult pageResult =new PageResult();
        Page<Dish> page=new Page<>(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(dishPageQueryDTO.getName()),Dish::getName,dishPageQueryDTO.getName());

        queryWrapper.eq(dishPageQueryDTO.getCategoryId()!=null, Dish::getCategoryId,dishPageQueryDTO.getCategoryId());
        queryWrapper.eq(dishPageQueryDTO.getStatus()!=null, Dish::getStatus,dishPageQueryDTO.getStatus());
       queryWrapper.orderByDesc(Dish::getCreateTime);


        Page<Dish> page1 =dishMapper.selectPage(page, queryWrapper);
        List<Dish> records = page1.getRecords();
        log.info(records.toString());
        List<DishVO> dishVOS = BeanCopyutil.copyBeanList(records, DishVO.class);
        List<DishVO> collect = dishVOS.stream().map(dishVO -> dishVO.setFlavors(dishfavorMapper.selectListbyid(dishVO.getId()))).collect(Collectors.toList());


        List<DishVO> collect1 = collect.stream().map(dishVO -> dishVO.setCategoryName(categoryMapper.selectById(dishVO.getCategoryId()).getName())).collect(Collectors.toList());

        pageResult.setRecords(collect1);
        pageResult.setTotal(page1.getSize());
        return pageResult ;
    }

    @Override
    @Transactional
    public void Delete(Integer[] id) {
        for (Integer integer : id) {
        if (StatusConstant.ENABLE.equals(dishMapper.selectById(integer).getStatus()))
        {
            throw new DeletionNotAllowedException("菜品正在售卖，不允许删除");
        }
        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getDishId,integer);
            Integer integer1 = setmaldishMapper.selectCount(queryWrapper);
            if (integer1>0){
                throw new DeletionNotAllowedException("关联了套餐，不允许删除");
            }


            dishMapper.deleteById(integer);
           LambdaQueryWrapper<DishFlavor> queryWrapper1=new LambdaQueryWrapper<>();
           queryWrapper1.eq(DishFlavor::getDishId,integer);
            dishfavorMapper.delete(queryWrapper1);

    }
    }
}
