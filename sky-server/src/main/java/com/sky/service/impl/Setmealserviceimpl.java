package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Employee;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmaldishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.utils.BeanCopyutil;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class Setmealserviceimpl implements SetmealService {
    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    SetmaldishMapper setmaldishMapper;
    @Autowired
    CategoryMapper categoryMapper;
    @Autowired
    DishMapper dishMapper;

    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = BeanCopyutil.copyBean(setmealDTO, Setmeal.class);
        setmealMapper.insert(setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        List<SetmealDish> collect = setmealDishes.stream().
                map(setmealDish -> setmealDish.setSetmealId(setmeal.getId())).
                collect(Collectors.toList());
        for (int i = 0; i < collect.size(); i++) {
            setmaldishMapper.insert(collect.get(i));

        }

    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageResult pageResult =new PageResult();
        Page<Setmeal> page=new Page<>(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(setmealPageQueryDTO.getName()),Setmeal::getName,setmealPageQueryDTO.getName());
        queryWrapper.eq(setmealPageQueryDTO.getCategoryId()!=null,Setmeal::getCategoryId,setmealPageQueryDTO.getCategoryId());
        queryWrapper.eq(setmealPageQueryDTO.getStatus()!=null,Setmeal::getStatus,setmealPageQueryDTO.getStatus());
        queryWrapper.orderByDesc(Setmeal::getCreateTime);
        Page<Setmeal> page1 = setmealMapper.selectPage(page, queryWrapper);
        List<Setmeal> records = page1.getRecords();
        List<SetmealVO> setmealVOS = BeanCopyutil.copyBeanList(records, SetmealVO.class);
        Stream<SetmealVO> setmealVOStream = setmealVOS.stream().map(setmealVO -> setmealVO.setCategoryName(categoryMapper.selectById(setmealVO.getCategoryId()).getName()));
        pageResult.setRecords(setmealVOStream.collect(Collectors.toList()));
        pageResult.setTotal(page1.getTotal());
        return pageResult ;
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        ids.forEach(id -> {
            Setmeal setmeal = setmealMapper.selectById(id);
            if(StatusConstant.ENABLE == setmeal.getStatus()){
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        ids.forEach(setmealId -> {
            //删除套餐表中的数据
            setmealMapper.deleteById(setmealId);
            //删除套餐菜品关系表中的数据
            queryWrapper.clear();
            queryWrapper.eq(SetmealDish::getSetmealId,setmealId);
            setmaldishMapper.delete(queryWrapper);
        });
    }

    @Override
    public SetmealVO getByIdWithDish(Long id) {
        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,id);
        Setmeal setmeal = setmealMapper.selectById(id);
        List<SetmealDish> setmealDishes = setmaldishMapper.selectList(queryWrapper);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //1、修改套餐表，执行update
        setmealMapper.updateById(setmeal);

        //套餐id
        Long setmealId = setmealDTO.getId();
        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealId);
        //2、删除套餐和菜品的关联关系，操作setmeal_dish表，执行delete
        setmaldishMapper.delete(queryWrapper);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        //3、重新插入套餐和菜品的关联关系，操作setmeal_dish表，执行insert
        for (SetmealDish setmealDish : setmealDishes) {
            setmaldishMapper.insert(setmealDish);
        }

    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal=new Setmeal();
        setmeal.setId(id);
        setmeal.setStatus(status);
        setmealMapper.updateById(setmeal);
    }

    @Override
    public List<Setmeal> list(Long categoryId) {
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getCreateTime);
        return setmealMapper.selectList(queryWrapper);
    }

    @Override
    public List<DishItemVO> getDishItemById(Long id) {
      return setmealMapper.getDishItemBySetmealId(id);


    }
}
