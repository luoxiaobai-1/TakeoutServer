package com.sky.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Setmeal implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //分类id
    private Long categoryId;

    //套餐名称
    private String name;

    //套餐价格
    private BigDecimal price;

    //状态 0:停用 1:启用
    private Integer status;

    //描述信息
    private String description;

    //图片
    private String image;

    @TableField(fill = FieldFill.INSERT)    //创建时间
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    //更新时间
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    //创建人
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    //修改人
    private Long updateUser;

}
