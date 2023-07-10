package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Employee;
import com.sky.mapper.CategoryMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Categoryserviceimpl implements CategoryService {
    @Autowired
    CategoryMapper categoryMapper;


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
        pageResult.setTotal(page1.getSize());
        return pageResult ;
    }
}
