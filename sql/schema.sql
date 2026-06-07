-- ============================================
-- 城市水利管网流量监测与预测系统 - 数据库脚本
-- MySQL 5.7+ / 8.0+
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS water_monitor
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE water_monitor;

-- 管网流量监测记录表（支持千万级数据）
DROP TABLE IF EXISTS pipe_flow_record;

CREATE TABLE pipe_flow_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    pipe_no         VARCHAR(50)     NOT NULL                 COMMENT '管网编号',
    pipe_name       VARCHAR(100)    NOT NULL                 COMMENT '管网名称',
    monitor_date    DATE            NOT NULL                 COMMENT '监测日期',
    monitor_hour    TINYINT         NOT NULL                 COMMENT '监测小时(0-23)',
    flow_rate       DECIMAL(10,2)   NOT NULL DEFAULT 0.00    COMMENT '水流量(m³/h)',
    water_pressure  DECIMAL(10,2)   NOT NULL DEFAULT 0.00    COMMENT '水压(MPa)',
    anomaly_level   TINYINT         NOT NULL DEFAULT 0       COMMENT '异常等级(0=正常,1=轻微,2=中度,3=严重)',
    water_quality   VARCHAR(20)     NOT NULL DEFAULT '良好'   COMMENT '水质状态(良好/一般/较差)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    INDEX idx_pipe_no (pipe_no),
    INDEX idx_monitor_date (monitor_date),
    INDEX idx_pipe_date (pipe_no, monitor_date),
    INDEX idx_anomaly_level (anomaly_level),
    INDEX idx_monitor_date_hour (monitor_date, monitor_hour)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管网流量监测记录表';

-- ============================================
-- 表行数缓存表（避免大表 COUNT(*) 全表扫描）
-- ============================================
DROP TABLE IF EXISTS table_stats;

CREATE TABLE table_stats (
    table_name  VARCHAR(100) NOT NULL PRIMARY KEY COMMENT '表名',
    row_count   BIGINT       NOT NULL DEFAULT 0  COMMENT '行数缓存',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '缓存更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表统计信息缓存';

-- ============================================
-- 按天聚合表（趋势/饼图/对比 查这里，代替大表全扫）
-- ============================================
DROP TABLE IF EXISTS daily_flow_summary;

CREATE TABLE daily_flow_summary (
    pipe_no         VARCHAR(50)  NOT NULL COMMENT '管网编号',
    monitor_date    DATE         NOT NULL COMMENT '监测日期',
    total_flow      DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '当日总流量',
    avg_flow        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当日平均流量',
    avg_pressure    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当日平均水压',
    anomaly_0       INT          NOT NULL DEFAULT 0 COMMENT '正常次数',
    anomaly_1       INT          NOT NULL DEFAULT 0 COMMENT '轻微异常次数',
    anomaly_2       INT          NOT NULL DEFAULT 0 COMMENT '中度异常次数',
    anomaly_3       INT          NOT NULL DEFAULT 0 COMMENT '严重异常次数',
    PRIMARY KEY (pipe_no, monitor_date),
    INDEX idx_daily_date (monitor_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管网日流量汇总表';

-- ============================================
-- 按小时聚合表（高峰时段分析查这里）
-- ============================================
DROP TABLE IF EXISTS hourly_flow_summary;

CREATE TABLE hourly_flow_summary (
    pipe_no       VARCHAR(50)  NOT NULL COMMENT '管网编号',
    monitor_hour  TINYINT      NOT NULL COMMENT '监测小时(0-23)',
    avg_flow      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '该小时平均流量',
    max_flow      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '该小时最大流量',
    record_count  INT          NOT NULL DEFAULT 0 COMMENT '样本数',
    PRIMARY KEY (pipe_no, monitor_hour)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管网小时流量汇总表';
