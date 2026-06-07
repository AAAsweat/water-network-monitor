# 城市水利管网流量监测与预测系统

基于 SpringBoot + MyBatis + ECharts 的海量数据可视化分析系统。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.7 |
| Web 层 | Spring MVC |
| 持久层 | MyBatis 3.0.3 |
| 数据库 | MySQL 8.0 |
| 连接池 | Druid 1.2.23 |
| 前端可视化 | ECharts 5.4.3 |
| AI 预测 | 纯 Java 线性回归 |
| 构建工具 | Maven 3.9+ |
| 运行环境 | JDK 17 |

## 数据规模

- 管网数量：500 个
- 时间跨度：3 年（1095 天）
- 采样频率：每小时 1 次
- 总记录数：**13,140,000 条**
- 原始表大小：约 5 GB

## 功能模块

- 千万级模拟数据生成（500 管网 × 3 年 × 365 天 × 24 小时）
- 数据概览仪表盘
- 水流量趋势曲线图
- 异常状态占比饼图
- 24 小时高峰时段分析
- 工作日/周末流量对比
- 多管网流量对比
- 实时监测数据面板
- AI 线性回归 24 小时流量预测

## 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- 建议 16 GB 以上内存，20 GB 磁盘空间

## 快速启动

1. 执行 `sql/schema.sql` 建表
2. 修改 `src/main/resources/application.yml` 中的数据库连接
3. 运行 `WaterMonitorApplication.main()`
4. 访问 `http://localhost:8088`
5. 点击「生成模拟数据」生成千万级数据
6. 点击「查询分析」查看可视化图表

## 项目结构

```
src/main/java/com/watermonitor/
├── WaterMonitorApplication.java        # 应用入口
├── ai/
│   └── LinearRegressionModel.java      # 线性回归 AI 模型
├── config/
│   └── CorsConfig.java                 # 跨域配置
├── controller/
│   └── PipeFlowController.java         # REST API
├── dao/
│   ├── PipeFlowMapper.java             # MyBatis Mapper
│   └── TableStatsMapper.java           # 统计缓存 Mapper
├── entity/
│   ├── PipeFlowRecord.java             # 监测记录实体
│   └── TableStats.java                 # 统计实体
├── service/
│   ├── PipeFlowService.java            # 业务接口
│   ├── DataGenerateService.java        # 千万级数据生成
│   └── impl/
│       └── PipeFlowServiceImpl.java    # 业务实现
└── resources/
    ├── application.yml                 # 配置
    ├── mybatis/
    │   ├── PipeFlowMapper.xml          # SQL 映射
    │   └── TableStatsMapper.xml
    └── static/
        └── index.html                  # 前端仪表盘
```
