package com.example.focus_flow.feature.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.focus_flow.R;
import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.DistractionReason;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.TimeSegment;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.domain.rules.Advice;
import com.example.focus_flow.domain.rules.AdviceEngine;
import com.example.focus_flow.domain.stats.RecentStats;
import com.example.focus_flow.domain.stats.StatsCalculator;
import com.example.focus_flow.domain.stats.SummaryStats;
import com.example.focus_flow.domain.stats.TimeSegmentStats;
import com.example.focus_flow.feature.tasks.TaskCards;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {
    private enum Period {
        TODAY, WEEK, MONTH
    }

    private LinearLayout content;
    private RepositoryProvider provider;
    private final StatsCalculator calculator = new StatsCalculator();
    private Period selectedPeriod = Period.WEEK;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        provider = RepositoryProvider.get(requireContext());
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        content = TaskUi.vertical(requireContext(), 22);
        scrollView.addView(content);
        return scrollView;
    }

    @Override
    public void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        provider.focusSessionRepository.refresh();
        provider.taskRepository.refresh();
        content.removeAllViews();

        long now = System.currentTimeMillis();
        List<FocusSessionRecord> periodSessions = terminalSessions(sessionsForPeriod(now));
        List<FocusSessionRecord> recentSessions = provider.focusSessionRepository.getRecentSessions(80);
        List<TaskRecord> tasks = provider.taskRepository.observeAllVisibleTasks().getValue();
        if (tasks == null) {
            tasks = Collections.emptyList();
        }
        SummaryStats summary = calculator.calculateSummary(periodSessions);

        content.addView(TaskUi.text(requireContext(), "专注统计", 30,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        content.addView(TaskUi.text(requireContext(), "周期洞察、趋势图和本地规则建议。", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        content.addView(TaskUi.spacer(requireContext(), 16));
        addPeriodSelector();
        addMetricGrid(summary);
        addTrendChart(now);
        addSubjectChart(periodSessions);
        addInsightCards(periodSessions, recentSessions);
        addAdviceCards(recentSessions, tasks, now);
    }

    private void addPeriodSelector() {
        MaterialButtonToggleGroup group = new MaterialButtonToggleGroup(requireContext());
        group.setSingleSelection(true);
        group.setSelectionRequired(true);

        MaterialButton today = toggleButton("今日", R.id.stats_filter_today);
        MaterialButton week = toggleButton("本周", R.id.stats_filter_week);
        MaterialButton month = toggleButton("本月", R.id.stats_filter_month);
        group.addView(today, toggleParams());
        group.addView(week, toggleParams());
        group.addView(month, toggleParams());
        group.check(idForPeriod(selectedPeriod));
        group.addOnButtonCheckedListener((buttonGroup, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            selectedPeriod = periodForId(checkedId);
            render();
        });
        content.addView(group);
        content.addView(TaskUi.spacer(requireContext(), 14));
    }

    private MaterialButton toggleButton(String text, int id) {
        MaterialButton button = TaskUi.button(requireContext(), text, false);
        button.setId(id);
        button.setCheckable(true);
        button.setMinHeight(TaskUi.dp(requireContext(), 48));
        return button;
    }

    private LinearLayout.LayoutParams toggleParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMarginEnd(TaskUi.dp(requireContext(), 8));
        return params;
    }

    private void addMetricGrid(SummaryStats summary) {
        TaskCards cards = new TaskCards(requireContext());
        LinearLayout rowOne = TaskUi.horizontal(requireContext());
        rowOne.addView(cards.metricCard("总时长", formatMetricDuration(summary.totalFocusSeconds)), metricParams());
        rowOne.addView(cards.metricCard("完成数", String.valueOf(summary.completedCount)), metricParams());
        rowOne.addView(cards.metricCard("完成率", percent(summary.completionRate)), metricParams());
        content.addView(rowOne);

        LinearLayout rowTwo = TaskUi.horizontal(requireContext());
        rowTwo.addView(cards.metricCard("平均质量", String.format(Locale.getDefault(), "%.1f/5", summary.averageQuality)), metricParams());
        rowTwo.addView(cards.metricCard("放弃率", percent(summary.abandonmentRate)), metricParams());
        rowTwo.addView(cards.metricCard("平均暂停", String.format(Locale.getDefault(), "%.1f次", summary.averagePauseCount)), metricParams());
        content.addView(rowTwo);
        content.addView(TaskUi.spacer(requireContext(), 10));
    }

    private void addTrendChart(long now) {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "最近 7 天趋势", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), "按天统计有效专注时长，用来观察学习节奏是否稳定。", 13,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));

        StatsLineChartView chart = new StatsLineChartView(requireContext());
        chart.setId(R.id.stats_trend_chart);
        chart.setData(lastSevenDaySeconds(now), lastSevenDayLabels(now));
        body.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(requireContext(), 220)));
        content.addView(card);
    }

    private void addSubjectChart(List<FocusSessionRecord> sessions) {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "科目占比", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));

        List<StatsRingChartView.Segment> segments = subjectSegments(sessions);
        StatsRingChartView ring = new StatsRingChartView(requireContext());
        ring.setId(R.id.stats_subject_chart);
        ring.setSegments(segments);
        body.addView(ring, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(requireContext(), 220)));
        if (segments.isEmpty()) {
            body.addView(TaskUi.text(requireContext(), "完成一次专注后，这里会显示不同科目的时间分布。", 13,
                    requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        } else {
            for (StatsRingChartView.Segment segment : segments) {
                body.addView(legendLine(segment.label, DateTimeUtils.formatDurationShort(segment.value), segment.color));
            }
        }
        content.addView(card);
    }

    private void addInsightCards(List<FocusSessionRecord> periodSessions, List<FocusSessionRecord> recentSessions) {
        content.addView(TaskUi.text(requireContext(), "洞察", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        content.addView(infoCard("高效时段洞察", bestTimeSegmentText(recentSessions)));
        content.addView(infoCard("分心原因", distractionText(periodSessions)));
        content.addView(infoCard("最常用白噪音", noiseText(periodSessions)));
    }

    private void addAdviceCards(List<FocusSessionRecord> recentSessions, List<TaskRecord> tasks, long now) {
        content.addView(TaskUi.text(requireContext(), "智能建议", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        RecentStats stats = calculator.calculateRecentStats(recentSessions, now);
        List<Advice> advices = new AdviceEngine().generate(stats, tasks, recentSessions, now, false);
        if (advices.isEmpty()) {
            content.addView(infoCard("继续记录", "再完成几次专注后，系统会根据完成率、质量和中断原因给出更明确的建议。"));
            return;
        }
        int limit = Math.min(5, advices.size());
        for (int i = 0; i < limit; i++) {
            Advice advice = advices.get(i);
            content.addView(infoCard(advice.title, advice.content));
        }
    }

    private MaterialCardView infoCard(String title, String bodyText) {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 16);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), title, 16,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), bodyText, 13,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        return card;
    }

    private TextView legendLine(String label, String value, int color) {
        TextView text = TaskUi.text(requireContext(), "● " + label + "  " + value, 13,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.BOLD);
        text.setTextColor(color);
        return text;
    }

    private List<FocusSessionRecord> sessionsForPeriod(long now) {
        long start;
        if (selectedPeriod == Period.TODAY) {
            start = DateTimeUtils.startOfDayMillis(now);
        } else if (selectedPeriod == Period.MONTH) {
            start = DateTimeUtils.startOfMonthMillis(now);
        } else {
            start = DateTimeUtils.startOfWeekMillis(now);
        }
        return provider.focusSessionRepository.getSessionsBetween(start, now);
    }

    private List<FocusSessionRecord> terminalSessions(List<FocusSessionRecord> sessions) {
        List<FocusSessionRecord> result = new ArrayList<>();
        for (FocusSessionRecord session : sessions) {
            if (session.status != FocusSessionStatus.RUNNING) {
                result.add(session);
            }
        }
        return result;
    }

    private int[] lastSevenDaySeconds(long now) {
        int[] values = new int[7];
        long firstDay = DateTimeUtils.startOfDayMillis(now - 6L * 24L * 60L * 60L * 1000L);
        List<FocusSessionRecord> sessions = provider.focusSessionRepository.getSessionsBetween(firstDay, now);
        for (FocusSessionRecord session : sessions) {
            if (session.status == FocusSessionStatus.RUNNING) {
                continue;
            }
            int index = (int) ((DateTimeUtils.startOfDayMillis(session.startedAt) - firstDay) / (24L * 60L * 60L * 1000L));
            if (index >= 0 && index < values.length) {
                values[index] += Math.max(0, session.actualFocusSeconds);
            }
        }
        return values;
    }

    private String[] lastSevenDayLabels(long now) {
        String[] labels = new String[7];
        long firstDay = DateTimeUtils.startOfDayMillis(now - 6L * 24L * 60L * 60L * 1000L);
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < labels.length; i++) {
            calendar.setTimeInMillis(firstDay + i * 24L * 60L * 60L * 1000L);
            labels[i] = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        }
        return labels;
    }

    private List<StatsRingChartView.Segment> subjectSegments(List<FocusSessionRecord> sessions) {
        Map<String, Integer> raw = calculator.subjectSeconds(sessions);
        List<Map.Entry<String, Integer>> entries = sortedEntries(raw);
        List<StatsRingChartView.Segment> segments = new ArrayList<>();
        int[] colors = new int[]{
                requireContext().getColor(R.color.focus_cyan),
                requireContext().getColor(R.color.focus_purple),
                requireContext().getColor(R.color.focus_green),
                requireContext().getColor(R.color.focus_orange),
                requireContext().getColor(R.color.focus_pink)
        };
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            if (entry.getValue() <= 0) {
                continue;
            }
            String label = entry.getKey() == null || entry.getKey().trim().isEmpty() ? "未分类" : entry.getKey();
            segments.add(new StatsRingChartView.Segment(label, entry.getValue(), colors[i % colors.length]));
        }
        return segments;
    }

    private String bestTimeSegmentText(List<FocusSessionRecord> sessions) {
        TimeSegment best = null;
        double bestScore = -1;
        TimeSegmentStats bestStats = null;
        for (TimeSegment segment : TimeSegment.values()) {
            TimeSegmentStats stats = calculator.calculateTimeSegmentStats(sessions, segment);
            if (stats.sampleCount == 0) {
                continue;
            }
            double score = stats.successRate * 0.6 + stats.averageQuality / 5.0 * 0.4;
            if (score > bestScore) {
                bestScore = score;
                best = segment;
                bestStats = stats;
            }
        }
        if (best == null || bestStats == null) {
            return "历史样本还不足。先完成几次专注后，会显示更适合你的高效时段。";
        }
        return labelForTimeSegment(best) + "表现最好，完成率 " + percent(bestStats.successRate)
                + "，平均质量 " + String.format(Locale.getDefault(), "%.1f/5", bestStats.averageQuality) + "。";
    }

    private String distractionText(List<FocusSessionRecord> sessions) {
        Map<DistractionReason, Integer> counts = calculator.distractionCounts(sessions);
        List<Map.Entry<DistractionReason, Integer>> entries = sortedEntries(counts);
        if (entries.isEmpty()) {
            return "当前周期没有明显分心记录，保持现在的环境准备。";
        }
        Map.Entry<DistractionReason, Integer> top = entries.get(0);
        return labelForDistraction(top.getKey()) + "出现 " + top.getValue() + " 次。下次开始前可以先处理这个干扰源。";
    }

    private String noiseText(List<FocusSessionRecord> sessions) {
        Map<String, Integer> seconds = new HashMap<>();
        for (FocusSessionRecord session : sessions) {
            if (session.noiseMixNameSnapshot == null || session.noiseMixNameSnapshot.trim().isEmpty()) {
                continue;
            }
            String name = session.noiseMixNameSnapshot;
            int value = seconds.containsKey(name) ? seconds.get(name) : 0;
            seconds.put(name, value + Math.max(0, session.actualFocusSeconds));
        }
        List<Map.Entry<String, Integer>> entries = sortedEntries(seconds);
        if (entries.isEmpty()) {
            return "还没有带白噪音的专注记录。可以在专注页或白噪音页选择一个混音。";
        }
        Map.Entry<String, Integer> top = entries.get(0);
        return top.getKey() + " 使用最多，累计 " + DateTimeUtils.formatDurationShort(top.getValue()) + "。";
    }

    private <T> List<Map.Entry<T, Integer>> sortedEntries(Map<T, Integer> map) {
        List<Map.Entry<T, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<T, Integer> entry) -> entry.getValue()).reversed());
        return entries;
    }

    private String percent(double value) {
        return Math.round(Math.max(0, Math.min(1, value)) * 100) + "%";
    }

    private String formatMetricDuration(int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        if (minutes >= 60) {
            return (minutes / 60) + "小时" + (minutes % 60) + "分";
        }
        return minutes + "分钟";
    }

    private int idForPeriod(Period period) {
        if (period == Period.TODAY) {
            return R.id.stats_filter_today;
        }
        if (period == Period.MONTH) {
            return R.id.stats_filter_month;
        }
        return R.id.stats_filter_week;
    }

    private Period periodForId(int id) {
        if (id == R.id.stats_filter_today) {
            return Period.TODAY;
        }
        if (id == R.id.stats_filter_month) {
            return Period.MONTH;
        }
        return Period.WEEK;
    }

    private String labelForTimeSegment(TimeSegment segment) {
        switch (segment) {
            case MORNING:
                return "上午";
            case NOON:
                return "中午";
            case AFTERNOON:
                return "下午";
            case EVENING:
                return "晚上";
            case NIGHT:
            default:
                return "深夜";
        }
    }

    private String labelForDistraction(DistractionReason reason) {
        switch (reason) {
            case PUSH_MESSAGE:
                return "消息提醒";
            case FATIGUE_SLEEPY:
                return "疲劳犯困";
            case TASK_TOO_HARD:
                return "任务过难";
            case TASK_UNCLEAR:
                return "目标不清";
            case ENVIRONMENT_NOISE:
                return "环境噪声";
            case DEVICE_DISTRACTION:
                return "设备分心";
            case PHYSICAL_NEED:
                return "身体需求";
            case EMOTIONAL_STRESS:
                return "情绪压力";
            case OTHER:
                return "其他原因";
            case NONE:
            default:
                return "无明显分心";
        }
    }

    private LinearLayout.LayoutParams metricParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMarginEnd(TaskUi.dp(requireContext(), 6));
        return params;
    }
}
