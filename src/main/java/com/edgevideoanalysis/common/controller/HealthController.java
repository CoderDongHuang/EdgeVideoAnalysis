package com.edgevideoanalysis.common.controller;

import com.edgevideoanalysis.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "健康检查")
@RestController
@RequestMapping("/api")
public class HealthController {

    @ApiOperation("服务健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("服务运行正常");
    }
}
