package com.example.focus_flow.feature.focus;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.focus_flow.R;
import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.navigation.FocusStartStore;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.domain.rules.EncouragementCategory;
import com.example.focus_flow.domain.rules.EncouragementEngine;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.example.focus_flow.service.focus.FocusTimerService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class FocusFragment extends Fragment {
    private LinearLayout content;
    private FocusTimerController controller;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FocusSessionRecord summarySession;
    private boolean finishConfirmVisible;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            FocusSessionRecord finished = controller.completeIfTimeReached();
            if (finished != null) {
                summarySession = controller.completeWithoutReview(finished.id);
            }
            render();
            handler.postDelayed(this, 1000L);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        controller = new FocusTimerController(requireContext());
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        content = TaskUi.vertical(requireContext(), 22);
        scrollView.addView(content);
        return scrollView;
    }

    @Override
    public void onResume() {
        super.onResume();
        consumePendingStartRequest();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(tick);
        super.onDestroyView();
    }

    private void consumePendingStartRequest() {
        if (controller.getSnapshot() != null) {
            FocusTimerService.start(requireContext());
            return;
        }
        FocusStartStore startStore = new FocusStartStore(requireContext());
        long taskId = startStore.consumePendingTaskId();
        int requestedMinutes = startStore.consumePendingMinutes();
        if (taskId > 0 && controller.startForTask(taskId, requestedMinutes) != null) {
            summarySession = null;
            FocusTimerService.start(requireContext());
        }
    }

    private void render() {
        if (content == null || controller == null) {
            return;
        }
        content.removeAllViews();
        if (summarySession != null) {
            renderSummary(summarySession);
            return;
        }
        FocusTimerSnapshot snapshot = controller.getSnapshot();
        if (snapshot != null) {
            renderRunning(snapshot);
            return;
        }
        FocusSessionRecord unrated = controller.getLatestUnratedTerminalSession();
        if (unrated != null) {
            summarySession = controller.completeWithoutReview(unrated.id);
            renderSummary(summarySession);
            return;
        }
        renderEmpty();
    }

    private void renderRunning(FocusTimerSnapshot snapshot) {
        content.addView(TaskUi.text(requireContext(), "专注舱", 30,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        content.addView(TaskUi.text(requireContext(), snapshot.session.subjectSnapshot + " · " + snapshot.session.taskTitleSnapshot,
                14, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        content.addView(TaskUi.spacer(requireContext(), 18));

        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 20);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(body);
        FocusCountdownView countdownView = new FocusCountdownView(requireContext());
        countdownView.setSnapshot(snapshot);
        body.addView(countdownView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(requireContext(), 300)));
        body.addView(TaskUi.text(requireContext(),
                "计划 " + snapshot.session.plannedFocusMinutes + " 分钟 · 休息 " + snapshot.session.plannedBreakMinutes + " 分钟",
                14, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        body.addView(TaskUi.text(requireContext(),
                "已专注 " + DateTimeUtils.formatDurationShort(snapshot.activeSeconds)
                        + " · 暂停 " + snapshot.session.pauseCount + " 次",
                14, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        body.addView(TaskUi.spacer(requireContext(), 16));
        LinearLayout actions = TaskUi.horizontal(requireContext());
        MaterialButton pauseResume = TaskUi.button(requireContext(), snapshot.paused ? "继续" : "暂停", true);
        pauseResume.setId(R.id.focus_button_pause_resume);
        MaterialButton finish = TaskUi.button(requireContext(), "提前结束", false);
        finish.setId(R.id.focus_button_finish_early);
        actions.addView(pauseResume, weighted());
        actions.addView(finish, weightedWithMargin());
        body.addView(actions);
        if (finishConfirmVisible) {
            body.addView(TaskUi.spacer(requireContext(), 12));
            body.addView(confirmFinishPanel());
        }
        content.addView(card);
        addEnvironmentPresets();

        pauseResume.setOnClickListener(v -> {
            if (snapshot.paused) {
                controller.resume();
                FocusTimerService.start(requireContext());
            } else {
                controller.pause();
            }
            render();
        });
        finish.setOnClickListener(v -> confirmFinishEarly());
    }

    private View confirmFinishPanel() {
        LinearLayout panel = TaskUi.vertical(requireContext(), 14);
        panel.setBackgroundColor(requireContext().getColor(R.color.focus_cyan_soft));
        panel.setPadding(TaskUi.dp(requireContext(), 16), TaskUi.dp(requireContext(), 14),
                TaskUi.dp(requireContext(), 16), TaskUi.dp(requireContext(), 14));
        panel.addView(TaskUi.text(requireContext(), "确认提前结束？", 16,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        panel.addView(TaskUi.text(requireContext(), "当前进度会自动保存，并直接生成本次专注总结。", 13,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        LinearLayout row = TaskUi.horizontal(requireContext());
        MaterialButton cancel = TaskUi.button(requireContext(), "继续专注", false);
        cancel.setId(android.R.id.button2);
        MaterialButton confirm = TaskUi.button(requireContext(), "提前结束", true);
        confirm.setId(android.R.id.button1);
        row.addView(cancel, weighted());
        row.addView(confirm, weightedWithMargin());
        panel.addView(row);
        cancel.setOnClickListener(v -> {
            finishConfirmVisible = false;
            render();
        });
        confirm.setOnClickListener(v -> {
            finishConfirmVisible = false;
            FocusSessionRecord session = controller.finishEarly();
            if (session != null) {
                summarySession = controller.completeWithoutReview(session.id);
            }
            render();
        });
        return panel;
    }

    private void renderSummary(FocusSessionRecord session) {
        content.addView(TaskUi.text(requireContext(), "专注总结", 30,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        content.addView(TaskUi.spacer(requireContext(), 18));
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 20);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), session.taskTitleSnapshot, 22,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), "实际专注 " + DateTimeUtils.formatDurationShort(session.actualFocusSeconds)
                        + " · 有效进度 " + Math.round(session.effectiveProgressMinutes) + " 分钟",
                15, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        body.addView(TaskUi.text(requireContext(), encouragementMessage(session),
                15, requireContext().getColor(R.color.focus_cyan), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.spacer(requireContext(), 12));
        LinearLayout actions = TaskUi.horizontal(requireContext());
        MaterialButton next = TaskUi.button(requireContext(), "继续下一个", true);
        next.setId(R.id.focus_button_continue_next);
        MaterialButton home = TaskUi.button(requireContext(), "返回首页", false);
        home.setId(R.id.focus_button_back_home);
        actions.addView(next, weighted());
        actions.addView(home, weightedWithMargin());
        body.addView(actions);
        content.addView(card);

        next.setOnClickListener(v -> {
            if (session.taskId != null && controller.startForTask(session.taskId) != null) {
                summarySession = null;
                FocusTimerService.start(requireContext());
                render();
            }
        });
        home.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.homeFragment));
    }

    private void renderEmpty() {
        content.addView(TaskUi.text(requireContext(), "专注舱", 30,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        content.addView(TaskUi.text(requireContext(), "选择一个任务，开始经典番茄钟节奏。", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        content.addView(TaskUi.spacer(requireContext(), 18));
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 20);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "专注 25 分钟  ·  短休息 5 分钟", 14,
                requireContext().getColor(R.color.focus_cyan), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.spacer(requireContext(), 8));
        FocusCountdownView countdownView = new FocusCountdownView(requireContext());
        body.addView(countdownView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(requireContext(), 300)));
        body.addView(TaskUi.text(requireContext(), "暂无正在进行的专注", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        MaterialButton tasks = TaskUi.button(requireContext(), "去首页选择任务", true);
        tasks.setId(R.id.focus_button_go_tasks);
        body.addView(tasks);
        tasks.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.homeFragment));
        content.addView(card);
        addEnvironmentPresets();
    }

    private void addEnvironmentPresets() {
        content.addView(TaskUi.spacer(requireContext(), 12));
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "专注环境", 20,
                requireContext().getColor(R.color.text_primary),
                android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(),
                "快速进入适合当前状态的声音场景。",
                13, requireContext().getColor(R.color.text_secondary),
                android.graphics.Typeface.NORMAL));
        LinearLayout row = TaskUi.horizontal(requireContext());
        String[] names = {"雨夜窗边", "森林晨雾", "图书馆自习"};
        for (String name : names) {
            MaterialButton button = TaskUi.button(requireContext(), name, false);
            button.setOnClickListener(v -> applyEnvironment(name));
            row.addView(button, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        body.addView(row);
        MaterialButton noiseConsole = TaskUi.button(requireContext(), "白噪音控制台", true);
        noiseConsole.setId(R.id.focus_button_noise_console);
        noiseConsole.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.noiseFragment));
        body.addView(noiseConsole);
        MaterialButton quiet = TaskUi.button(requireContext(), "静音专注", false);
        quiet.setOnClickListener(v -> {
            com.example.focus_flow.audio.NoisePlaybackController.get().stopAll();
            android.widget.Toast.makeText(requireContext(),
                    "已切换为静音专注", android.widget.Toast.LENGTH_SHORT).show();
        });
        body.addView(quiet);
        content.addView(card);
    }

    private void applyEnvironment(String name) {
        com.example.focus_flow.data.repository.RepositoryProvider provider =
                com.example.focus_flow.data.repository.RepositoryProvider.get(requireContext());
        for (com.example.focus_flow.data.local.model.NoiseMixRecord mix
                : provider.noiseMixRepository.getAllMixes()) {
            if (name.equals(mix.name)) {
                provider.noiseMixRepository.applyMix(mix.id);
                com.example.focus_flow.audio.NoisePlaybackController.get().applyMix(
                        requireContext(), mix, provider.settingsRepository.getMasterVolume());
                android.widget.Toast.makeText(requireContext(),
                        "已开启 " + name, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    private void confirmFinishEarly() {
        new AlertDialog.Builder(requireContext())
                .setTitle("提前结束")
                .setMessage("当前进度会自动保存，并直接生成本次专注总结。")
                .setNegativeButton("继续专注", null)
                .setPositiveButton("提前结束", (dialog, which) -> {
                    FocusSessionRecord session = controller.finishEarly();
                    if (session != null) {
                        summarySession = controller.completeWithoutReview(session.id);
                    }
                    render();
                })
                .show();
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout.LayoutParams weightedWithMargin() {
        LinearLayout.LayoutParams params = weighted();
        params.setMarginStart(TaskUi.dp(requireContext(), 10));
        return params;
    }

    private String encouragementMessage(FocusSessionRecord session) {
        EncouragementCategory category;
        if (session.status == FocusSessionStatus.ABANDONED) {
            if (session.progressRatio >= 0.70) {
                category = EncouragementCategory.ABANDONED_NEAR_END;
            } else if (session.progressRatio >= 0.40) {
                category = EncouragementCategory.ABANDONED_HALF;
            } else {
                category = EncouragementCategory.ABANDONED_EARLY;
            }
        } else if (session.pauseCount > 0) {
            category = EncouragementCategory.COMPLETED_WITH_PAUSE;
        } else if (session.qualityScore != null && session.qualityScore <= 2) {
            category = EncouragementCategory.LOW_QUALITY_COMPLETE;
        } else {
            category = EncouragementCategory.SMOOTH_COMPLETE;
        }
        return new EncouragementEngine().chooseMessage(
                category,
                new java.util.Random(session.id + (session.qualityScore == null ? 0 : session.qualityScore)));
    }
}
