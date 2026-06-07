package com.watermonitor.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 纯Java实现的线性回归预测模型
 * 使用多元线性回归（正规方程法）预测管网水流量
 *
 * 特征维度:
 * - hour: 小时 (0-23, 归一化)
 * - dayOfWeek: 星期几 (1-7, one-hot)
 * - recentAvgFlow: 近期平均流量
 * - seasonFactor: 季节因子
 */
public class LinearRegressionModel {

    private double[] weights;
    private double bias;
    private boolean trained = false;
    private double trainScore;

    /**
     * 使用正规方程训练模型: W = (X^T X)^(-1) X^T y
     */
    public void train(double[][] features, double[] targets) {
        int n = features.length;    // 样本数
        int m = features[0].length; // 特征数

        // 构建设计矩阵 X (添加偏置列)
        double[][] X = new double[n][m + 1];
        for (int i = 0; i < n; i++) {
            X[i][0] = 1.0; // 偏置项
            System.arraycopy(features[i], 0, X[i], 1, m);
        }

        // 计算 X^T X
        double[][] XtX = new double[m + 1][m + 1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m + 1; j++) {
                for (int k = 0; k < m + 1; k++) {
                    XtX[j][k] += X[i][j] * X[i][k];
                }
            }
        }

        // 计算 (X^T X)^(-1) 使用高斯-约旦消元法
        double[][] inv = invertMatrix(XtX);
        if (inv == null) {
            // 矩阵不可逆，使用梯度下降作为备用
            gradientDescent(features, targets, 0.01, 5000);
            return;
        }

        // 计算 X^T y
        double[] Xty = new double[m + 1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m + 1; j++) {
                Xty[j] += X[i][j] * targets[i];
            }
        }

        // W = (X^T X)^(-1) X^T y
        double[] W = new double[m + 1];
        for (int i = 0; i < m + 1; i++) {
            for (int j = 0; j < m + 1; j++) {
                W[i] += inv[i][j] * Xty[j];
            }
        }

        bias = W[0];
        weights = new double[m];
        System.arraycopy(W, 1, weights, 0, m);
        trained = true;

        // 计算R²评分
        trainScore = computeR2(features, targets);
    }

    /** 预测单个样本 */
    public double predict(double[] features) {
        if (!trained) throw new IllegalStateException("模型未训练");
        double result = bias;
        for (int i = 0; i < weights.length && i < features.length; i++) {
            result += weights[i] * features[i];
        }
        return Math.max(0, result);
    }

    /** 批量预测 */
    public double[] predictBatch(double[][] featuresBatch) {
        double[] results = new double[featuresBatch.length];
        for (int i = 0; i < featuresBatch.length; i++) {
            results[i] = predict(featuresBatch[i]);
        }
        return results;
    }

    public boolean isTrained() { return trained; }
    public double getTrainScore() { return trainScore; }

    /** R²评分 */
    private double computeR2(double[][] features, double[] targets) {
        double ssRes = 0, ssTot = 0;
        double mean = 0;
        for (double t : targets) mean += t;
        mean /= targets.length;

        for (int i = 0; i < targets.length; i++) {
            double pred = predict(features[i]);
            ssRes += Math.pow(targets[i] - pred, 2);
            ssTot += Math.pow(targets[i] - mean, 2);
        }
        return ssTot == 0 ? 0 : 1 - ssRes / ssTot;
    }

    /** 梯度下降法（备用） */
    private void gradientDescent(double[][] features, double[] targets, double lr, int epochs) {
        int m = features[0].length;
        weights = new double[m];
        bias = 0;
        int n = features.length;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double[] dw = new double[m];
            double db = 0;
            for (int i = 0; i < n; i++) {
                double pred = bias;
                for (int j = 0; j < m; j++) pred += weights[j] * features[i][j];
                double error = pred - targets[i];
                for (int j = 0; j < m; j++) dw[j] += error * features[i][j];
                db += error;
            }
            for (int j = 0; j < m; j++) weights[j] -= lr * dw[j] / n;
            bias -= lr * db / n;
        }
        trained = true;
        trainScore = computeR2(features, targets);
    }

    /** 矩阵求逆（高斯-约旦消元法） */
    private double[][] invertMatrix(double[][] matrix) {
        int n = matrix.length;
        double[][] aug = new double[n][2 * n];

        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, aug[i], 0, n);
            aug[i][n + i] = 1.0;
        }

        for (int i = 0; i < n; i++) {
            // 选主元
            int maxRow = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(aug[j][i]) > Math.abs(aug[maxRow][i])) maxRow = j;
            }
            if (Math.abs(aug[maxRow][i]) < 1e-10) return null; // 奇异矩阵

            double[] temp = aug[i];
            aug[i] = aug[maxRow];
            aug[maxRow] = temp;

            double pivot = aug[i][i];
            for (int j = 0; j < 2 * n; j++) aug[i][j] /= pivot;

            for (int j = 0; j < n; j++) {
                if (j != i) {
                    double factor = aug[j][i];
                    for (int k = 0; k < 2 * n; k++) {
                        aug[j][k] -= factor * aug[i][k];
                    }
                }
            }
        }

        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(aug[i], n, inv[i], 0, n);
        }
        return inv;
    }

    // ====== 静态工厂方法：为管网流量预测构建训练特征 ======

    /**
     * 从历史流量数据构建训练特征
     * @param historyData 历史数据 [monitorDate, monitorHour, flowRate]
     * @return 特征矩阵和目标值
     */
    public static class FeatureBuilder {
        /**
         * 构建24小时预测特征
         * @param hourlyAvgMap 各小时近30天平均流量 Map<hour, avgFlow>
         * @param recentFlow 最近24小时流量数组
         * @return Map with "features", "targets", "predictFeatures", "hours"
         */
        public static Map<String, Object> build24HourPredictionData(
                Map<Integer, Double> hourlyAvgMap,
                List<Double> recent24Flow) {

            // 按小时分组获取平均流量，构建训练数据
            // 特征: [hour_normalized, day_segment, avg_hourly_flow]
            // 目标: flow_rate

            List<double[]> featureList = new ArrayList<>();
            List<Double> targetList = new ArrayList<>();
            List<double[]> predictFeatures = new ArrayList<>();
            List<Integer> predictHours = new ArrayList<>();

            for (int hour = 0; hour < 24; hour++) {
                double avgFlow = hourlyAvgMap.getOrDefault(hour, 100.0);
                double hourNorm = hour / 24.0;
                int daySegment = getDaySegment(hour);

                double[] feats = new double[]{
                    hourNorm,
                    daySegment == 1 ? 1.0 : 0.0,  // 早高峰
                    daySegment == 2 ? 1.0 : 0.0,  // 午间
                    daySegment == 3 ? 1.0 : 0.0,  // 晚高峰
                    avgFlow / 200.0                 // 归一化平均流量
                };

                featureList.add(feats);
                targetList.add(avgFlow);

                predictFeatures.add(feats);
                predictHours.add(hour);
            }

            double[][] features = featureList.toArray(new double[0][]);
            double[] targets = targetList.stream().mapToDouble(Double::doubleValue).toArray();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("features", features);
            result.put("targets", targets);
            result.put("predictFeatures", predictFeatures.toArray(new double[0][]));
            result.put("hours", predictHours);
            return result;
        }

        /** 构建时序预测训练数据（短期预测） */
        public static Map<String, Object> buildTimeSeriesData(
                List<Map<String, Object>> historyData, int lookback) {

            if (historyData.size() < lookback + 1) {
                throw new IllegalArgumentException("历史数据不足，需要至少" + (lookback + 1) + "条");
            }

            List<double[]> featureList = new ArrayList<>();
            List<Double> targetList = new ArrayList<>();

            for (int i = 0; i < historyData.size() - lookback; i++) {
                double[] feats = new double[lookback + 2]; // lookback个历史值 + hour + dayOfWeek
                for (int j = 0; j < lookback; j++) {
                    Object val = historyData.get(i + j + 1).get("flowRate");
                    feats[j] = val instanceof BigDecimal ? ((BigDecimal) val).doubleValue() / 200.0
                              : ((Number) val).doubleValue() / 200.0;
                }
                Object hourVal = historyData.get(i).get("monitorHour");
                feats[lookback] = hourVal instanceof Integer ? (Integer) hourVal / 24.0 : 0.5;

                Object targetVal = historyData.get(i).get("flowRate");
                double target = targetVal instanceof BigDecimal ? ((BigDecimal) targetVal).doubleValue()
                              : ((Number) targetVal).doubleValue();

                featureList.add(feats);
                targetList.add(target);
            }

            double[][] features = featureList.toArray(new double[0][]);
            double[] targets = targetList.stream().mapToDouble(Double::doubleValue).toArray();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("features", features);
            result.put("targets", targets);
            return result;
        }

        private static int getDaySegment(int hour) {
            if (hour >= 6 && hour <= 9) return 1;    // 早高峰
            if (hour >= 11 && hour <= 13) return 2;  // 午间
            if (hour >= 17 && hour <= 21) return 3;  // 晚高峰
            return 0; // 正常时段
        }
    }
}
