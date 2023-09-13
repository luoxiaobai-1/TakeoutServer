package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.config.RedisCache;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Autowired
    RedisCache redisCache;
    @Override
    @Transactional
    public void save(DishDTO dishDTO) {
        String key="dish_"+dishDTO.getCategoryId();
        redisCache.deleteObject(key);
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();


        if (flavors!=null&&flavors.size()>0)
        {
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dish.getId());
            }

            dishfavorMapper.InsertList(flavors);
        }//todo 待改正


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
       redisCache.deleteall("dish_*");
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

    @Override
    public DishVO getbyid(Long id) {

        Dish dish = dishMapper.selectById(id);
        DishVO dishVO1 = BeanCopyutil.copyBean(dish, DishVO.class);
        LambdaQueryWrapper<DishFlavor> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,id);
        List<DishFlavor> dishFlavors = dishfavorMapper.selectList(queryWrapper);
        dishVO1.setFlavors(dishFlavors);
        return dishVO1;
    }

    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        String key="dish_"+dishDTO.getCategoryId();
        redisCache.deleteObject(key);
        LambdaQueryWrapper<DishFlavor> queryWrapper1=new LambdaQueryWrapper<>();
        queryWrapper1.eq(DishFlavor::getDishId,dishDTO.getId());
        dishfavorMapper.delete(queryWrapper1);
        Dish dish = BeanCopyutil.copyBean(dishDTO, Dish.class);

        dishMapper.updateById(dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        for (int i = 0; i < flavors.size(); i++) {
            DishFlavor dishFlavor = flavors.get(i);
            dishFlavor.setId(null);
            dishFlavor.setDishId(dishDTO.getId());
            dishfavorMapper.insert(dishFlavor);
        }


    }

    @Override
    public List<Dish> list(Long categoryId) {
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId,categoryId);
        queryWrapper.eq(Dish::getStatus,StatusConstant.ENABLE);
        return dishMapper.selectList(queryWrapper);

    }

    @Override
    public List<DishVO> listWithFlavor(Long categoryId) {

        String key="dish_"+categoryId;
        List<DishVO> cacheObject =redisCache.getCacheObject(key);

       if (cacheObject!=null&&cacheObject.size()>0)
       {
           return cacheObject;
       }
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
         LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId()!=null, Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.eq(dish.getStatus()!=null, Dish::getStatus,dish.getStatus());
        List<Dish> dishes = dishMapper.selectList(queryWrapper);
        List<DishVO> dishVOS = BeanCopyutil.copyBeanList(dishes, DishVO.class);

        List<DishVO> collect = dishVOS.stream().map(dishVO -> dishVO.setFlavors(dishfavorMapper.selectListbyid(dishVO.getId()))).collect(Collectors.toList());
        redisCache.setCacheObject(key,collect);
        return
                collect;


    }

    @Override
    public void startOrStop(Integer status, Long id) {
        redisCache.deleteall("dish_*");
        Dish dish=new Dish();
        dish.setStatus(status);
        dish.setId(id);
        dishMapper.updateById(dish);

    }


}
