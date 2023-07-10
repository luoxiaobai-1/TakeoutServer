package com.sky.controller.admin;

import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/category")
@Slf4j
@Api("分类接口操作类")
public class CategoryController {
    @Autowired
    CategoryService categoryService;

    @GetMapping("/page")
    @ApiOperation("分类分页操作")
    public Result<PageResult> page(@Validated CategoryPageQueryDTO categoryPageQueryDTO)
    {
        log.info("分类分页，{}",categoryPageQueryDTO);
        PageResult pageResult=  categoryService.page(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

}
