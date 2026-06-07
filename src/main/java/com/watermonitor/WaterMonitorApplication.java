package com.watermonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WaterMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(WaterMonitorApplication.class, args);
        System.out.println("============================================");
        System.out.println("  城市水利管网流量监测与预测系统 启动成功!");
        System.out.println("  访问地址: http://localhost:8088");
        System.out.println("============================================");
    }
}
