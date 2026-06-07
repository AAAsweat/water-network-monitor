package com.watermonitor.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.Date;

public class PipeFlowRecord {
    private Long id;
    private String pipeNo;
    private String pipeName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date monitorDate;

    private Integer monitorHour;
    private BigDecimal flowRate;
    private BigDecimal waterPressure;
    private Integer anomalyLevel;
    private String waterQuality;

    // ====== getters & setters ======

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPipeNo() { return pipeNo; }
    public void setPipeNo(String pipeNo) { this.pipeNo = pipeNo; }

    public String getPipeName() { return pipeName; }
    public void setPipeName(String pipeName) { this.pipeName = pipeName; }

    public Date getMonitorDate() { return monitorDate; }
    public void setMonitorDate(Date monitorDate) { this.monitorDate = monitorDate; }

    public Integer getMonitorHour() { return monitorHour; }
    public void setMonitorHour(Integer monitorHour) { this.monitorHour = monitorHour; }

    public BigDecimal getFlowRate() { return flowRate; }
    public void setFlowRate(BigDecimal flowRate) { this.flowRate = flowRate; }

    public BigDecimal getWaterPressure() { return waterPressure; }
    public void setWaterPressure(BigDecimal waterPressure) { this.waterPressure = waterPressure; }

    public Integer getAnomalyLevel() { return anomalyLevel; }
    public void setAnomalyLevel(Integer anomalyLevel) { this.anomalyLevel = anomalyLevel; }

    public String getWaterQuality() { return waterQuality; }
    public void setWaterQuality(String waterQuality) { this.waterQuality = waterQuality; }
}
