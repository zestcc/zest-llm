package cn.zest.www.zestllm.admin;

import cn.zest.www.zestllm.infra.config.LocalDotEnvLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("cn.zest.www.zestllm.admin.mapper")
@EnableScheduling
public class ZestLlmAdminApplication {

    public static void main(String[] args) {
        LocalDotEnvLoader.loadIfPresent();
        SpringApplication.run(ZestLlmAdminApplication.class, args);
    }
}
