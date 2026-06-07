package com.watermonitor.service;

import com.watermonitor.dao.TableStatsMapper;
import com.watermonitor.entity.PipeFlowRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 千万级水利管网模拟数据生成服务
 * 使用JDBC原生批量插入 + count缓存表优化
 * 设计: 500个管网 × 3年 × 365天 × 24小时 ≈ 1314万条记录
 */
@Service
public class DataGenerateService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TableStatsMapper tableStatsMapper;

    /** 管网数量 */
    private static final int PIPE_COUNT = 500;
    /** 模拟年数 */
    private static final int YEARS = 3;
    /** 每批插入条数 */
    private static final int BATCH_SIZE = 5000;
    /** 插入SQL */
    private static final String INSERT_SQL =
        "INSERT INTO pipe_flow_record (pipe_no, pipe_name, monitor_date, monitor_hour, " +
        "flow_rate, water_pressure, anomaly_level, water_quality) VALUES (?,?,?,?,?,?,?,?)";

    private static final String[] WATER_QUALITY_OPTIONS = {"良好", "良好", "良好", "良好", "良好", "良好", "良好", "一般", "一般", "较差"};
    private static final String[] PIPE_AREAS = {
        "城北", "城南", "城东", "城西", "高新", "经开", "滨江", "老城", "新城", "开发区",
        "朝阳", "海淀", "丰台", "石景山", "大兴", "通州", "顺义", "昌平", "房山", "密云"
    };
    private static final String[] PIPE_TYPES = {"供水主管", "供水支管", "排水主管", "排水支管", "雨水管", "污水管", "中水管", "消防管"};
    private static final Random RANDOM = new Random();

    /**
     * 生成千万级模拟数据（一键执行）
     */
    public Map<String, Object> generateData(String startDateStr, int pipeCountOverride) {
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();

        int actualPipeCount = pipeCountOverride > 0 ? pipeCountOverride : PIPE_COUNT;
        LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate endDate = startDate.plusYears(YEARS);
        long totalDays = startDate.until(endDate).getDays();
        long totalRecords = (long) actualPipeCount * totalDays * 24L;

        result.put("预计总记录数", totalRecords);
        result.put("管网数量", actualPipeCount);
        result.put("起始日期", startDateStr);
        result.put("结束日期", endDate.toString());

        System.out.println("========================================");
        System.out.println("  开始生成模拟数据...");
        System.out.println("  管网数量: " + actualPipeCount);
        System.out.println("  时间跨度: " + startDate + " ~ " + endDate + " (" + totalDays + "天)");
        System.out.println("  预计生成: " + totalRecords + " 条记录");
        System.out.println("========================================");

        // 预生成管网信息
        List<String[]> pipeInfos = new ArrayList<>();
        for (int i = 1; i <= actualPipeCount; i++) {
            String pipeNo = "PIPE" + String.format("%04d", i);
            String area = PIPE_AREAS[RANDOM.nextInt(PIPE_AREAS.length)];
            String type = PIPE_TYPES[RANDOM.nextInt(PIPE_TYPES.length)];
            String pipeName = area + "-" + type + "-" + i;
            pipeInfos.add(new String[]{pipeNo, pipeName});
        }

        long insertedCount = 0;
        List<PipeFlowRecord> batch = new ArrayList<>(BATCH_SIZE);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // 按天数循环，每一天为一批管网生成24小时数据
            LocalDate currentDate = startDate;
            int dayCounter = 0;

            while (currentDate.isBefore(endDate)) {
                java.sql.Date sqlDate = java.sql.Date.valueOf(currentDate);
                int dayOfWeek = currentDate.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
                boolean isWeekend = (dayOfWeek >= 6);

                for (String[] pipeInfo : pipeInfos) {
                    // 每个管网的基础流量（管网不同，基础流量不同）
                    int pipeIndex = Integer.parseInt(pipeInfo[0].substring(4));
                    double baseFlow = 50 + (pipeIndex % 50) * 10 + RANDOM.nextDouble() * 30;
                    double basePressure = 0.2 + RANDOM.nextDouble() * 0.4;

                    for (int hour = 0; hour < 24; hour++) {
                        PipeFlowRecord record = new PipeFlowRecord();
                        record.setPipeNo(pipeInfo[0]);
                        record.setPipeName(pipeInfo[1]);
                        record.setMonitorDate(sqlDate);
                        record.setMonitorHour(hour);

                        // 流量计算（模拟早晚高峰）
                        double hourFactor = getHourFactor(hour);
                        double weekendFactor = isWeekend ? 0.75 : 1.0;
                        double seasonalFactor = getSeasonalFactor(currentDate.getMonthValue());
                        double randomNoise = 0.85 + RANDOM.nextDouble() * 0.3;

                        double flow = baseFlow * hourFactor * weekendFactor * seasonalFactor * randomNoise;
                        record.setFlowRate(BigDecimal.valueOf(flow).setScale(2, RoundingMode.HALF_UP));

                        // 水压计算（流量大时水压略降）
                        double pressure = basePressure * (1.0 - (hourFactor - 1.0) * 0.3) * (0.9 + RANDOM.nextDouble() * 0.2);
                        record.setWaterPressure(BigDecimal.valueOf(Math.max(0.05, pressure)).setScale(2, RoundingMode.HALF_UP));

                        // 异常等级（10%概率异常）
                        int anomalyLevel = determineAnomalyLevel(flow, baseFlow, pressure, basePressure);
                        record.setAnomalyLevel(anomalyLevel);

                        // 水质状态
                        String quality = WATER_QUALITY_OPTIONS[RANDOM.nextInt(WATER_QUALITY_OPTIONS.length)];
                        record.setWaterQuality(quality);

                        batch.add(record);

                        if (batch.size() >= BATCH_SIZE) {
                            insertedCount += executeBatch(conn, batch);
                            batch.clear();
                        }
                    }
                }

                dayCounter++;
                if (dayCounter % 30 == 0) {
                    System.out.printf("  进度: %s, 已插入 %,d 条记录 (%.1f%%)%n",
                        currentDate, insertedCount,
                        insertedCount * 100.0 / totalRecords);
                }

                currentDate = currentDate.plusDays(1);
            }

            // 处理剩余记录
            if (!batch.isEmpty()) {
                insertedCount += executeBatch(conn, batch);
            }

            conn.commit();

            // 从原始表构建聚合表（替后续分析查询加速）
            buildAggregationTables(conn);

            // 更新count缓存表，避免后续大表COUNT(*)全表扫描
            tableStatsMapper.upsertRowCount("pipe_flow_record", insertedCount);
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("错误", e.getMessage());
            return result;
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        result.put("实际插入数", insertedCount);
        result.put("耗时(秒)", elapsed);
        result.put("平均速度(条/秒)", elapsed > 0 ? insertedCount / elapsed : insertedCount);

        System.out.println("========================================");
        System.out.println("  数据生成完成!");
        System.out.println("  总记录数: " + insertedCount);
        System.out.println("  耗时: " + elapsed + " 秒");
        System.out.println("========================================");

        return result;
    }

    private int executeBatch(Connection conn, List<PipeFlowRecord> batch) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            for (PipeFlowRecord r : batch) {
                ps.setString(1, r.getPipeNo());
                ps.setString(2, r.getPipeName());
                ps.setDate(3, new java.sql.Date(r.getMonitorDate().getTime()));
                ps.setInt(4, r.getMonitorHour());
                ps.setBigDecimal(5, r.getFlowRate());
                ps.setBigDecimal(6, r.getWaterPressure());
                ps.setInt(7, r.getAnomalyLevel());
                ps.setString(8, r.getWaterQuality());
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            return results.length;
        }
    }

    /** 小时因子：模拟早晚用水高峰 */
    private double getHourFactor(int hour) {
        if (hour >= 6 && hour <= 9) return 1.8 + RANDOM.nextDouble() * 0.4;   // 早高峰 6-9点
        if (hour >= 11 && hour <= 13) return 1.3 + RANDOM.nextDouble() * 0.3;  // 午间小高峰
        if (hour >= 17 && hour <= 21) return 1.9 + RANDOM.nextDouble() * 0.5;  // 晚高峰 17-21点
        if (hour >= 22 || hour <= 4) return 0.3 + RANDOM.nextDouble() * 0.2;   // 深夜低谷
        return 0.8 + RANDOM.nextDouble() * 0.4; // 其他时段正常
    }

    /** 季节因子：夏季用水量更大 */
    private double getSeasonalFactor(int month) {
        if (month >= 6 && month <= 8) return 1.3;   // 夏季高峰
        if (month == 12 || month <= 2) return 0.8;   // 冬季低谷
        return 1.0; // 春秋正常
    }

    /** 判定异常等级 */
    private int determineAnomalyLevel(double flow, double baseFlow, double pressure, double basePressure) {
        double flowDeviation = Math.abs(flow - baseFlow) / baseFlow;
        double pressureDeviation = Math.abs(pressure - basePressure) / basePressure;

        if (flowDeviation > 1.5 || pressureDeviation > 0.6) return 3; // 严重异常
        if (flowDeviation > 1.0 || pressureDeviation > 0.4) return 2; // 中度异常
        if (flowDeviation > 0.5 || pressureDeviation > 0.2) return 1; // 轻微异常
        return 0; // 正常
    }

    /** 从原始表构建聚合表（日汇总 + 小时汇总），供分析查询加速 */
    private void buildAggregationTables(Connection conn) throws SQLException {
        System.out.println("  正在构建聚合表...");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE daily_flow_summary");
            stmt.execute("TRUNCATE TABLE hourly_flow_summary");

            stmt.execute(
                "INSERT INTO daily_flow_summary " +
                "(pipe_no, monitor_date, total_flow, avg_flow, avg_pressure, " +
                "anomaly_0, anomaly_1, anomaly_2, anomaly_3) " +
                "SELECT pipe_no, monitor_date, " +
                "SUM(flow_rate), AVG(flow_rate), AVG(water_pressure), " +
                "SUM(CASE WHEN anomaly_level=0 THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN anomaly_level=1 THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN anomaly_level=2 THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN anomaly_level=3 THEN 1 ELSE 0 END) " +
                "FROM pipe_flow_record GROUP BY pipe_no, monitor_date"
            );
            System.out.println("    daily_flow_summary 已构建");

            stmt.execute(
                "INSERT INTO hourly_flow_summary " +
                "(pipe_no, monitor_hour, avg_flow, max_flow, record_count) " +
                "SELECT pipe_no, monitor_hour, AVG(flow_rate), MAX(flow_rate), COUNT(*) " +
                "FROM pipe_flow_record GROUP BY pipe_no, monitor_hour"
            );
            System.out.println("    hourly_flow_summary 已构建");
        }
    }
}
