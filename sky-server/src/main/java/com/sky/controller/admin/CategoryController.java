package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.EmployeeDTO;
import com.sky.entity.Category;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PostMapping()
    @ApiOperation("添加分类操作")
    public  Result save(@RequestBody @Validated  CategoryDTO categoryDTO)
    {
        log.info("新增员工 {}",categoryDTO);
        categoryService.addcategory(categoryDTO);
        return Result.success("添加分类成功");
    }
    @PostMapping("/status/{status}")
    @ApiOperation("禁用分类操作")
    public  Result changestatus(@PathVariable Integer status,Long id)
    {
        log.info("启用或禁用分类操作{} {}",status, id);
        categoryService.changestatus( status, id);
        return Result.success("操作成功");
    }
    @PutMapping
    @ApiOperation("修改分类信息")
    public  Result<Employee> update(@RequestBody @Validated CategoryDTO categoryDTO)
    {
        log.info("通过id查询员工信息 {}", categoryDTO);
        categoryService.update(categoryDTO);
        return Result.success("修改员工信息成功");
    }
    @GetMapping("/list")
    @ApiOperation("根据类型查询分类")
    public Result<List<Category>> list(Integer type){
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }
@DeleteMapping
@ApiOperation("删除分类")
public Result Delete(Integer id){
     categoryService.Delete(id);
    return Result.success("删除成功");
}
}
