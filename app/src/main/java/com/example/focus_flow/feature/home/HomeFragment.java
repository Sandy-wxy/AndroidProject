package com.example.focus_flow.feature.home;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.focus_flow.R;
import com.example.focus_flow.SettingsActivity;
import com.example.focus_flow.MainActivity;
import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.RecommendationConfidence;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.core.navigation.FocusStartStore;
import com.example.focus_flow.data.local.model.FocusBlockRecord;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.domain.assistant.AiPromptBuilder;
import com.example.focus_flow.domain.assistant.AiResponseParser;
import com.example.focus_flow.domain.assistant.AssistantSuggestion;
import com.example.focus_flow.domain.assistant.StudyStrategyEngine;
import com.example.focus_flow.domain.assistant.SuggestionQueue;
import com.example.focus_flow.domain.rules.Advice;
import com.example.focus_flow.domain.rules.AdviceEngine;
import com.example.focus_flow.domain.rules.Recommendation;
import com.example.focus_flow.domain.rules.TaskSplitEngine;
import com.example.focus_flow.domain.stats.RecentStats;
import com.example.focus_flow.domain.stats.StatsCalculator;
import com.example.focus_flow.domain.stats.SummaryStats;
import com.example.focus_flow.feature.assistant.AiProxyClient;
import com.example.focus_flow.feature.assistant.AiUiTransitions;
import com.example.focus_flow.feature.tasks.TaskCards;
import com.example.focus_flow.feature.tasks.TaskFormBottomSheet;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.example.focus_flow.feature.reminder.TaskReminderScheduler;
import com.example.focus_flow.feature.widget.FocusQuickWidgetProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private LinearLayout content;
    private RepositoryProvider provider;
    private DateSwipeScrollView scrollView;
    private long selectedDate = DateTimeUtils.startOfDayMillis(System.currentTimeMillis());
    private final Handler aiHandler = new Handler(Looper.getMainLooper());
    private final AiResponseParser aiResponseParser = new AiResponseParser();
    private final AiProxyClient aiProxyClient = new AiProxyClient();
    private SuggestionQueue adviceQueue;
    private TextView adviceTitleView;
    private TextView adviceBodyView;
    private Runnable adviceRotator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        provider = RepositoryProvider.get(requireContext());
        scrollView = new DateSwipeScrollView(requireContext());
        scrollView.setFillViewport(true);
        content = TaskUi.vertical(requireContext(), 22);
        scrollView.addView(content);
        scrollView.setOnDateSwipeListener(this::shiftSelectedDate);
        return scrollView;
    }

    @Override
    public void onResume() {
        super.onResume();
        render();
        if (requireActivity() instanceof MainActivity
                && ((MainActivity) requireActivity()).consumeAddTaskRequest()) {
            showTaskForm(null);
        }
    }

    @Override
    public void onDestroyView() {
        stopAdviceRotation();
        super.onDestroyView();
    }

    private void render() {
        provider.taskRepository.refresh();
        provider.focusSessionRepository.refresh();
        provider.noiseMixRepository.refresh();
        FocusQuickWidgetProvider.refreshAll(requireContext());
        stopAdviceRotation();
        content.removeAllViews();
        List<TaskRecord> selectedTasks = provider.taskRepository.getTasksForDate(
                DateTimeUtils.formatDate(selectedDate));
        long now = System.currentTimeMillis();
        List<FocusSessionRecord> todaySessions = provider.focusSessionRepository.getSessionsBetween(
                DateTimeUtils.startOfDayMillis(now), DateTimeUtils.endOfDayMillis(now));
        SummaryStats summary = new StatsCalculator().calculateSummary(todaySessions);

        addHeader();
        content.addView(TaskUi.spacer(requireContext(), 16));

        TaskCards cards = new TaskCards(requireContext());
        LinearLayout metrics = TaskUi.horizontal(requireContext());
        metrics.addView(cards.metricCard("今日专注", DateTimeUtils.formatDurationShort(summary.totalFocusSeconds)), metricParams());
        metrics.addView(cards.metricCard("完成番茄钟", String.valueOf(summary.completedCount)), metricParams());
        metrics.addView(cards.metricCard("连续学习", "0 天"), metricParams());
        content.addView(metrics);

        content.addView(TaskUi.spacer(requireContext(), 12));
        MaterialButton statsButton = TaskUi.button(requireContext(), "查看统计", false);
        statsButton.setId(R.id.home_button_stats);
        statsButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.statsFragment));
        content.addView(statsButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(TaskUi.spacer(requireContext(), 12));
        MaterialButton add = TaskUi.button(requireContext(),
                "+ 添加" + relativeDateLabel(selectedDate) + "任务", true);
        add.setId(R.id.home_button_add_task);
        add.setOnClickListener(v -> showTaskForm(null));
        content.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(TaskUi.spacer(requireContext(), 14));

        addTaskSection(selectedTasks, cards);
        addAdviceCards(selectedTasks);
        addRecentRecord(todaySessions);
    }

    private void addHeader() {
        LinearLayout row = TaskUi.horizontal(requireContext());
        LinearLayout titleBlock = TaskUi.vertical(requireContext(), 0);
        titleBlock.addView(TaskUi.text(requireContext(), relativeDateTitle(), 30,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        titleBlock.addView(TaskUi.text(requireContext(), displayDate(selectedDate) + " · 左右滑动切换日期", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        row.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        AppCompatImageButton more = TaskUi.iconButton(requireContext(), R.drawable.ic_nav_profile);
        more.setId(R.id.home_button_settings);
        more.setContentDescription("更多");
        more.setOnClickListener(this::showHomeMenu);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                TaskUi.dp(requireContext(), 52), TaskUi.dp(requireContext(), 52));
        params.setMarginStart(TaskUi.dp(requireContext(), 12));
        row.addView(more, params);
        content.addView(row);
        addDateNavigator();
    }

    private void showHomeMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add("我的");
        menu.getMenu().add("学习报告");
        menu.getMenu().add("设置");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if ("我的".equals(title)) {
                NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
                return true;
            }
            if ("学习报告".equals(title)) {
                NavHostFragment.findNavController(this).navigate(R.id.statsFragment);
                return true;
            }
            if ("设置".equals(title)) {
                startActivity(new Intent(requireContext(), SettingsActivity.class));
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void addDateNavigator() {
        content.addView(TaskUi.spacer(requireContext(), 12));
        MaterialButton pickDate = TaskUi.button(requireContext(), "\u9009\u62e9\u65e5\u671f", false);
        pickDate.setId(R.id.home_button_pick_date);
        pickDate.setOnClickListener(v -> showDatePicker());
        content.addView(pickDate, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(TaskUi.spacer(requireContext(), 8));
        LinearLayout navigator = TaskUi.horizontal(requireContext());
        MaterialButton previous = TaskUi.button(requireContext(), "‹ 前一天", false);
        previous.setId(R.id.home_button_previous_day);
        MaterialButton today = TaskUi.button(requireContext(), "回到今天", false);
        today.setId(R.id.home_button_back_today);
        MaterialButton next = TaskUi.button(requireContext(), "后一天 ›", false);
        next.setId(R.id.home_button_next_day);
        previous.setOnClickListener(v -> shiftSelectedDate(-1));
        next.setOnClickListener(v -> shiftSelectedDate(1));
        today.setOnClickListener(v -> {
            selectedDate = DateTimeUtils.startOfDayMillis(System.currentTimeMillis());
            render();
            scrollView.scrollTo(0, 0);
        });
        today.setEnabled(!isToday(selectedDate));
        navigator.addView(previous, navigationParams(false));
        navigator.addView(today, navigationParams(true));
        navigator.addView(next, navigationParams(false));
        content.addView(navigator);
    }

    private void addAdviceCards(List<TaskRecord> tasks) {
        StatsCalculator calculator = new StatsCalculator();
        List<FocusSessionRecord> sessions = provider.focusSessionRepository.getRecentSessions(20);
        RecentStats stats = calculator.calculateRecentStats(sessions, System.currentTimeMillis());
        if (addHybridAdviceCards(tasks, sessions, stats)) {
            return;
        }
        List<Advice> advices = new AdviceEngine().generate(stats, tasks, sessions, System.currentTimeMillis(), true);
        content.addView(TaskUi.text(requireContext(), "智能建议", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        if (advices.isEmpty()) {
            content.addView(smallCard("状态稳定", "先添加一个清晰的小任务，系统会根据历史数据给出更准确的建议。"));
        } else {
            for (Advice advice : advices) {
                content.addView(smallCard(advice.title, advice.content));
            }
        }
    }

    private boolean addHybridAdviceCards(List<TaskRecord> tasks, List<FocusSessionRecord> sessions,
                                         RecentStats stats) {
        List<AssistantSuggestion> local = localStudySuggestions(stats, tasks, sessions);
        adviceQueue = new SuggestionQueue(local);

        content.addView(TaskUi.text(requireContext(), "智能建议", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 16);
        card.addView(body);
        adviceTitleView = TaskUi.text(requireContext(), "", 16,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD);
        adviceBodyView = TaskUi.text(requireContext(), "", 13,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL);
        body.addView(adviceTitleView);
        body.addView(adviceBodyView);
        content.addView(card);

        renderAdviceSuggestion(adviceQueue.current(), false);
        scheduleAdviceRotation();
        requestApiStudyAdvice(tasks, stats);
        return true;
    }

    private List<AssistantSuggestion> localStudySuggestions(RecentStats stats, List<TaskRecord> tasks,
                                                            List<FocusSessionRecord> sessions) {
        List<AssistantSuggestion> result = new ArrayList<>();
        List<Advice> advices = new AdviceEngine().generate(stats, tasks, sessions, System.currentTimeMillis(), true);
        for (Advice advice : advices) {
            result.add(AssistantSuggestion.local(advice.adviceId, AssistantSuggestion.Category.STUDY_PLAN,
                    advice.title, advice.content));
        }
        StudyStrategyEngine.Context context = new StudyStrategyEngine.Context();
        context.nowMillis = System.currentTimeMillis();
        context.completionRate = stats.completionRate;
        context.averageQuality = stats.averageQuality;
        context.bestSegment = stats.currentTimeSegment;
        context.currentSegment = com.example.focus_flow.core.model.TimeSegment.fromMillis(System.currentTimeMillis());
        context.tasks = tasks;
        result.addAll(new StudyStrategyEngine().generate(context));
        if (result.isEmpty()) {
            result.add(AssistantSuggestion.local("study-default", AssistantSuggestion.Category.STUDY_PLAN,
                    "先创建一个清晰任务", "写下可检查的完成结果，系统会在几次专注后给出更贴合的建议。"));
        }
        return result;
    }

    private void requestApiStudyAdvice(List<TaskRecord> tasks, RecentStats stats) {
        AiPromptBuilder.TaskAdviceContext context = new AiPromptBuilder.TaskAdviceContext();
        context.todayTaskCount = tasks == null ? 0 : tasks.size();
        context.urgentTaskCount = countUrgent(tasks);
        context.hardTaskCount = countHard(tasks);
        context.completionRate = stats.completionRate;
        context.averageQuality = stats.averageQuality;
        context.bestSegment = stats.currentTimeSegment;
        context.topSubjects = topSubjects(tasks);
        String prompt = new AiPromptBuilder().buildTaskAdvicePrompt(context);
        aiProxyClient.chat(prompt, new AiProxyClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                if (!isAdded() || adviceQueue == null) {
                    return;
                }
                List<AssistantSuggestion> api = aiResponseParser.parseSuggestions(
                        responseBody, AssistantSuggestion.Category.STUDY_PLAN);
                if (api.isEmpty()) {
                    return;
                }
                adviceQueue.mergeApiSuggestions(api);
                adviceQueue.moveTo(api.get(0));
                renderAdviceSuggestion(adviceQueue.current(), true);
                scheduleAdviceRotation();
            }

            @Override
            public void onError(Exception error) {
                // The local queue remains active when the proxy times out or fails.
            }
        });
    }

    private void renderAdviceSuggestion(AssistantSuggestion suggestion, boolean animate) {
        if (suggestion == null || adviceTitleView == null || adviceBodyView == null) {
            return;
        }
        AiUiTransitions.crossFadeText(adviceTitleView, suggestion.title,
                adviceBodyView, suggestion.content, animate);
    }

    private void scheduleAdviceRotation() {
        if (adviceQueue == null || adviceQueue.items().size() <= 1) {
            return;
        }
        if (adviceRotator != null) {
            aiHandler.removeCallbacks(adviceRotator);
        }
        adviceRotator = () -> {
            if (!isAdded() || adviceQueue == null || adviceQueue.items().isEmpty()) {
                return;
            }
            AssistantSuggestion next = adviceQueue.next();
            renderAdviceSuggestion(next, true);
            aiHandler.postDelayed(adviceRotator, delayFor(next));
        };
        aiHandler.postDelayed(adviceRotator, delayFor(adviceQueue.current()));
    }

    private void stopAdviceRotation() {
        if (adviceRotator != null) {
            aiHandler.removeCallbacks(adviceRotator);
        }
        adviceRotator = null;
    }

    private long delayFor(AssistantSuggestion suggestion) {
        int length = suggestion == null ? 40 : suggestion.content.length() + suggestion.title.length();
        return Math.max(4500L, Math.min(9000L, length * 85L));
    }

    private int countUrgent(List<TaskRecord> tasks) {
        int count = 0;
        if (tasks == null) return 0;
        for (TaskRecord task : tasks) {
            if (task.priority != null && task.priority.rank() >= 3) {
                count++;
            }
        }
        return count;
    }

    private int countHard(List<TaskRecord> tasks) {
        int count = 0;
        if (tasks == null) return 0;
        for (TaskRecord task : tasks) {
            if (task.difficulty != null && task.difficulty.ordinal() >= 2) {
                count++;
            }
        }
        return count;
    }

    private List<String> topSubjects(List<TaskRecord> tasks) {
        Map<String, Integer> counts = new HashMap<>();
        if (tasks != null) {
            for (TaskRecord task : tasks) {
                String subject = task.subject == null || task.subject.trim().isEmpty() ? "study" : task.subject.trim();
                counts.put(subject, counts.containsKey(subject) ? counts.get(subject) + 1 : 1);
            }
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((left, right) -> Integer.compare(right.getValue(), left.getValue()));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(3, entries.size()); i++) {
            result.add(entries.get(i).getKey());
        }
        return result;
    }

    private void addTaskSection(List<TaskRecord> tasks, TaskCards cards) {
        content.addView(TaskUi.spacer(requireContext(), 8));
        LinearLayout heading = TaskUi.horizontal(requireContext());
        heading.addView(TaskUi.text(requireContext(), relativeDateLabel(selectedDate) + "任务", 20,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        heading.addView(TaskUi.text(requireContext(), "  ·  " + DateTimeUtils.formatDate(selectedDate), 12,
                requireContext().getColor(R.color.text_weak), android.graphics.Typeface.NORMAL));
        content.addView(heading);
        if (tasks.isEmpty()) {
            content.addView(smallCard(relativeDateLabel(selectedDate) + "还没有学习任务",
                    "可以添加任务，或继续左右滑动安排其他日期。"));
        }
        TaskCards.Actions actions = actions();
        for (TaskRecord task : tasks) {
            boolean selectedIsToday = isToday(selectedDate);
            String quickLabel = selectedIsToday ? "添加任务到明日" : "添加任务到今日";
            content.addView(cards.taskCard(task, actions, quickLabel,
                    () -> moveTask(task, selectedIsToday ? dayOffset(1) : dayOffset(0))));
        }
    }

    private void addRecentRecord(List<FocusSessionRecord> sessions) {
        if (sessions.isEmpty()) {
            return;
        }
        FocusSessionRecord latest = sessions.get(sessions.size() - 1);
        content.addView(smallCard("最近专注", latest.taskTitleSnapshot + " · " + DateTimeUtils.formatDurationShort(latest.actualFocusSeconds)));
    }

    private MaterialCardView smallCard(String title, String bodyText) {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 16);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), title, 15, requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), bodyText, 13, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        return card;
    }

    private LinearLayout.LayoutParams metricParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMarginEnd(TaskUi.dp(requireContext(), 6));
        return params;
    }

    private LinearLayout.LayoutParams navigationParams(boolean middle) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, middle ? 1.15f : 1f);
        if (middle) {
            params.setMarginStart(TaskUi.dp(requireContext(), 6));
            params.setMarginEnd(TaskUi.dp(requireContext(), 6));
        }
        return params;
    }

    private TaskCards.Actions actions() {
        return new TaskCards.Actions() {
            @Override
            public void onStart(TaskRecord task) {
                if (provider.focusSessionRepository.getRunningSession() != null) {
                    NavHostFragment.findNavController(HomeFragment.this).navigate(R.id.focusFragment);
                    return;
                }
                new FocusStartStore(requireContext()).requestStart(task.id);
                Toast.makeText(requireContext(), "已进入专注舱", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(HomeFragment.this).navigate(R.id.focusFragment);
            }

            @Override
            public void onEdit(TaskRecord task) {
                showTaskForm(task);
            }

            @Override
            public void onDelete(TaskRecord task) {
                confirmDelete(task);
            }

            @Override
            public void onComplete(TaskRecord task) {
                TaskReminderScheduler.cancel(requireContext(), task.id);
                provider.taskRepository.markTaskCompleted(task.id);
                render();
            }
        };
    }

    private void showTaskForm(TaskRecord task) {
        new TaskFormBottomSheet(task,
                task == null ? DateTimeUtils.formatDate(selectedDate) : null,
                (startNow, taskId) -> {
            render();
            if (startNow) {
                new FocusStartStore(requireContext()).requestStart(taskId);
                NavHostFragment.findNavController(this).navigate(R.id.focusFragment);
            }
        }).show(getParentFragmentManager(), "task_form");
    }

    public void openTaskCreator() {
        if (isAdded()) {
            showTaskForm(null);
        }
    }

    private void moveTask(TaskRecord task, long destinationDate) {
        String destination = DateTimeUtils.formatDate(destinationDate);
        if (destination.equals(task.plannedDate)) {
            Toast.makeText(requireContext(), "任务已经在" + relativeDateLabel(destinationDate), Toast.LENGTH_SHORT).show();
            return;
        }
        if (task.status == TaskStatus.COMPLETED) {
            duplicateCompletedTask(task, destination);
            Toast.makeText(requireContext(),
                    "已复制任务到" + relativeDateLabel(destinationDate), Toast.LENGTH_SHORT).show();
            render();
            return;
        }
        task.plannedDate = destination;
        provider.taskRepository.updateTask(task);
        Toast.makeText(requireContext(),
                "已添加到" + relativeDateLabel(destinationDate), Toast.LENGTH_SHORT).show();
        render();
    }

    private void duplicateCompletedTask(TaskRecord source, String destination) {
        TaskRecord copy = new TaskRecord();
        copy.title = source.title;
        copy.subject = source.subject;
        copy.targetOutcome = source.targetOutcome;
        copy.description = source.description;
        copy.difficulty = source.difficulty;
        copy.priority = source.priority;
        copy.estimatedTotalMinutes = source.estimatedTotalMinutes;
        copy.plannedDate = destination;
        copy.deadlineAt = null;
        copy.colorTag = source.colorTag;
        copy.autoSplitEnabled = source.autoSplitEnabled;
        copy.status = TaskStatus.PENDING;
        Recommendation recommendation = new Recommendation(
                Math.max(10, Math.min(60, source.estimatedTotalMinutes)),
                5, 15, 4, RecommendationConfidence.LOW,
                java.util.Collections.singletonList("QUICK_COPY"));
        List<FocusBlockRecord> blocks = new TaskSplitEngine().splitTask(copy, recommendation, 0);
        provider.taskRepository.insertTask(copy, blocks);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            Calendar target = Calendar.getInstance();
            target.set(year, month, day, 0, 0, 0);
            target.set(Calendar.MILLISECOND, 0);
            selectedDate = DateTimeUtils.startOfDayMillis(target.getTimeInMillis());
            render();
            scrollView.post(() -> scrollView.scrollTo(0, 0));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void shiftSelectedDate(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        selectedDate = DateTimeUtils.startOfDayMillis(calendar.getTimeInMillis());
        render();
        scrollView.scrollTo(0, 0);
    }

    private long dayOffset(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(DateTimeUtils.startOfDayMillis(System.currentTimeMillis()));
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTimeInMillis();
    }

    private boolean isToday(long date) {
        return DateTimeUtils.formatDate(date).equals(DateTimeUtils.todayDateString());
    }

    private String relativeDateTitle() {
        long offset = dayDifferenceFromToday(selectedDate);
        if (offset == 0) {
            return "今天准备专注多久？";
        }
        if (offset == 1) {
            return "明天准备完成什么？";
        }
        if (offset == -1) {
            return "回顾昨天的安排";
        }
        return offset > 0 ? "安排未来的学习" : "回顾过去的任务";
    }

    private String relativeDateLabel(long date) {
        long offset = dayDifferenceFromToday(date);
        if (offset == 0) {
            return "今日";
        }
        if (offset == 1) {
            return "明日";
        }
        if (offset == -1) {
            return "昨日";
        }
        return new SimpleDateFormat("M月d日", Locale.getDefault()).format(new Date(date));
    }

    private long dayDifferenceFromToday(long date) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(DateTimeUtils.startOfDayMillis(date));
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(DateTimeUtils.startOfDayMillis(System.currentTimeMillis()));
        long difference = 0;
        while (target.before(today)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
            difference--;
        }
        while (target.after(today)) {
            target.add(Calendar.DAY_OF_MONTH, -1);
            difference++;
        }
        return difference;
    }

    private String displayDate(long date) {
        return new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault()).format(new Date(date));
    }

    private void confirmDelete(TaskRecord task) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除任务")
                .setMessage("删除后不会影响历史统计，但任务将从今日计划中隐藏。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认删除", (dialog, which) -> {
                    TaskReminderScheduler.cancel(requireContext(), task.id);
                    provider.taskRepository.softDeleteTask(task.id);
                    render();
                })
                .show();
    }
}
