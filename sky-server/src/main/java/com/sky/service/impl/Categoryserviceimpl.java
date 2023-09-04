package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.annotation.jkl;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Employee;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class Categoryserviceimpl implements CategoryService {
    @Autowired
    CategoryMapper categoryMapper;
    @Autowired
    DishMapper dishMapper;
    @Autowired
    SetmealMapper setmealMapper;


    @Override
    public PageResult page(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageResult pageResult =new PageResult();
        Page<Category> page=new Page<>(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Category> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(categoryPageQueryDTO.getName()),Category::getName,categoryPageQueryDTO.getName());

        queryWrapper.eq(categoryPageQueryDTO.getType()!=null,Category::getType,categoryPageQueryDTO.getType());
        queryWrapper.orderByAsc(Category::getSort);
        Page<Category> page1 = categoryMapper.selectPage(page, queryWrapper);
        pageResult.setRecords(page1.getRecords());
        pageResult.setTotal(page1.getTotal());
        return pageResult ;
    }

    @Override
    public void addcategory(CategoryDTO categoryDTO) {

        Category category=new Category();
        BeanUtils.copyProperties(categoryDTO,category);
        category.setStatus(StatusConstant.ENABLE);
        categoryMapper.insert(category);

    }

    @Override
    public void changestatus(Integer status, Long id) {
        Category category=new Category();
        category.setId(id);
        category.setStatus(status);
        categoryMapper.updateById(category);
    }

    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category=new Category();
        BeanUtils.copyProperties(categoryDTO,category);
        categoryMapper.updateById(category);
    }

    @Override
    @jkl(value = "1")
    public List<Category> list(Integer type) {
        LambdaQueryWrapper<Category> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getStatus,StatusConstant.ENABLE);
        queryWrapper.eq(type!=null,Category::getType,type);
//        queryWrapper.orderByAsc(Category::getCreateTime);
//        queryWrapper.orderByAsc(Category::getSort);
        return categoryMapper.selectList(queryWrapper);
    }

    @Override
    public void Delete(Integer id) {
        LambdaQueryWrapper<Dish> queryWrapper =new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId,id);
        Integer integer = dishMapper.selectCount(queryWrapper);
        if (integer>=1)
        {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        LambdaQueryWrapper<Setmeal> queryWrapper1 =new LambdaQueryWrapper<>();
        queryWrapper1.eq(Setmeal::getCategoryId,id);
        Integer integer1 = setmealMapper.selectCount(queryWrapper1);
        if (integer1>=1)
        {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        categoryMapper.deleteById(id);

    }


}
