package com.sky.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class CategoryDTO implements Serializable {
@NotNull
    //主键
    private Long id;
    @NotNull
    //类型 1 菜品分类 2 套餐分类
    private Integer type;
@NotEmpty
    //分类名称
    private String name;
    @NotEmpty
    //排序
    private Integer sort;

}
