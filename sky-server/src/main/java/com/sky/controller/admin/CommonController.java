package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/common")
@Slf4j
@Api("文件上传操作类")
public class CommonController {
    @PostMapping("upload")
    @ApiOperation("文件上传")
    //todo 文件上传接口未完成
    public Result<String> upload(MultipartFile file)
    {
        log.info("文件上传 {}",file);
        return Result.success("a");
    }
}
