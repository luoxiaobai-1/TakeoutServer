package com.sky.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "添加员工时传递的数据模型")
public class EmployeeDTO implements Serializable {
@NotNull
    private Long id;
@NotEmpty
    private String username;
    @NotEmpty
    private String name;
    @NotEmpty
    private String phone;
    @NotEmpty
    private String sex;
    @NotEmpty
    private String idNumber;

}
