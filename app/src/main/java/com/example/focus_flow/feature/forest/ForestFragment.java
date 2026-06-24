package com.example.focus_flow.feature.forest;

import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.domain.forest.ForestEvolutionEngine;
import com.example.focus_flow.domain.forest.ForestState;
import com.example.focus_flow.feature.tasks.TaskCards;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ForestFragment extends Fragment {
    private static final int TREE_SECONDS = 25 * 60;

    private RepositoryProvider provider;
    private LinearLayout content;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        provider = RepositoryProvider.get(requireContext());
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        content = TaskUi.vertical(requireContext(), 22);
        scroll.addView(content);
        return scroll;
    }

    @Override
    public void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        content.removeAllViews();
        long now = System.currentTimeMillis();
        List<FocusSessionRecord> all = provider.focusSessionRepository
                .getSessionsBetween(0, now);
        List<FocusSessionRecord> trees = qualifiedTrees(all);
        ForestState forestState = new ForestEvolutionEngine().evaluate(all, now);

        content.addView(TaskUi.text(requireContext(), "我的专注森林", 30,
                requireContext().getColor(R.color.text_primary),
                android.graphics.Typeface.BOLD));
        content.addView(TaskUi.text(requireContext(),
                "专注时光会在这里长成一片属于你的森林。",
                14, requireContext().getColor(R.color.text_secondary),
                android.graphics.Typeface.NORMAL));
        content.addView(TaskUi.spacer(requireContext(), 14));

        MaterialCardView forestCard = TaskUi.glassCard(requireContext());
        FocusForestView forest = new FocusForestView(requireContext());
        forest.setForestState(forestState);
        forestCard.addView(forest, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(requireContext(), 330)));
        content.addView(forestCard);

        TaskCards cards = new TaskCards(requireContext());
        LinearLayout metrics = TaskUi.horizontal(requireContext());
        metrics.addView(cards.metricCard("已种树木", trees.size() + " 棵"), metricParams());
        metrics.addView(cards.metricCard("森林专注", DateTimeUtils.formatDurationShort(totalSeconds(trees))),
                metricParams());
        content.addView(metrics);
        addEvolutionCard(forestState);

        MaterialCardView ruleCard = TaskUi.glassCard(requireContext());
        LinearLayout ruleBody = TaskUi.vertical(requireContext(), 18);
        ruleCard.addView(ruleBody);
        ruleBody.addView(TaskUi.text(requireContext(), "成长规则", 20,
                requireContext().getColor(R.color.text_primary),
                android.graphics.Typeface.BOLD));
        ruleBody.addView(TaskUi.text(requireContext(),
                "🌱 完成 25–44 分钟：种下一棵阔叶树\n"
                        + "🌲 完成 45–89 分钟：种下一棵松树\n"
                        + "🌳 完成 90 分钟及以上：种下一棵金色大树",
                14, requireContext().getColor(R.color.text_secondary),
                android.graphics.Typeface.NORMAL));
        ruleBody.addView(TaskUi.text(requireContext(),
                "提前放弃或不足 25 分钟的专注不会生成树木。",
                12, requireContext().getColor(R.color.text_weak),
                android.graphics.Typeface.NORMAL));
        MaterialButton start = TaskUi.button(requireContext(),
                trees.isEmpty() ? "开始专注，种下第一棵树" : "继续专注，让森林生长", true);
        start.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(R.id.focusFragment));
        ruleBody.addView(start);
        content.addView(ruleCard);

        int todayTrees = 0;
        long todayStart = DateTimeUtils.startOfDayMillis(System.currentTimeMillis());
        for (FocusSessionRecord tree : trees) {
            if (tree.startedAt >= todayStart) {
                todayTrees++;
            }
        }
        content.addView(TaskUi.text(requireContext(),
                todayTrees == 0 ? "今天还没有新树，完成一次长专注试试看。"
                        : "今天新增 " + todayTrees + " 棵树，森林正在变得更茂盛。",
                13, requireContext().getColor(R.color.focus_green),
                android.graphics.Typeface.BOLD));
    }

    private void addEvolutionCard(ForestState state) {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 14);
        card.addView(body);
        android.widget.TextView level = TaskUi.text(requireContext(),
                "Lv." + state.level.levelNumber + "  " + state.level.sceneName,
                18, requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD);
        level.setId(R.id.forest_evolution_level);
        body.addView(level);
        int remaining = Math.max(0, state.nextLevelTreeTarget - state.trees.size());
        body.addView(TaskUi.text(requireContext(),
                "连续学习 " + state.streakDays + " 天 · 距离下一场景还需要 " + remaining + " 棵树",
                13, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        body.addView(TaskUi.text(requireContext(),
                "数学、语言、编程等科目会长出不同树种；45 分钟和 90 分钟以上会成长为更高大的树。",
                12, requireContext().getColor(R.color.text_weak), android.graphics.Typeface.NORMAL));
        content.addView(card);
    }

    private List<FocusSessionRecord> qualifiedTrees(List<FocusSessionRecord> sessions) {
        List<FocusSessionRecord> result = new ArrayList<>();
        for (FocusSessionRecord session : sessions) {
            if (session.status == FocusSessionStatus.COMPLETED
                    && session.actualFocusSeconds >= TREE_SECONDS) {
                result.add(session);
            }
        }
        return result;
    }

    private int totalSeconds(List<FocusSessionRecord> sessions) {
        int total = 0;
        for (FocusSessionRecord session : sessions) {
            total += Math.max(0, session.actualFocusSeconds);
        }
        return total;
    }

    private LinearLayout.LayoutParams metricParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(TaskUi.dp(requireContext(), 4), TaskUi.dp(requireContext(), 4),
                TaskUi.dp(requireContext(), 4), TaskUi.dp(requireContext(), 4));
        return params;
    }
}
