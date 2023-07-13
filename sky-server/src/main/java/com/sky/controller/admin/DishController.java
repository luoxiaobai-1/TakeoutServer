package com.sky.controller.admin;

import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.EmployeeDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.Dishservice;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api("菜品操作类")
public class DishController {
@Autowired
    Dishservice dishservice;
    @PostMapping()
    @ApiOperation("添加菜品操作")
    public Result save(@RequestBody DishDTO dishDTO)
    {
        log.info("新增菜品  {}",dishDTO);
        dishservice.save(dishDTO);
        return Result.success("添加菜品成功");
    }
    @GetMapping("/page")
    @ApiOperation("菜品分页操作")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO)
    {
        log.info("菜品分页，{}",dishPageQueryDTO);
        PageResult pageResult=  dishservice.page(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    @DeleteMapping
    @ApiOperation("删除菜品")
    public Result Delete(Integer [] ids){
        log.info(ids[2].toString());
         dishservice.Delete(ids);
        return Result.success("删除成功");
    }
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> Delete(@PathVariable Long id){

       DishVO dishVO= dishservice.getbyid(id);
        return Result.success(dishVO);
    }
@PutMapping
@ApiOperation("修改菜品")
public Result update(@RequestBody DishDTO dishDTO){

   dishservice.update(dishDTO);
    return Result.success("success");
}


}
