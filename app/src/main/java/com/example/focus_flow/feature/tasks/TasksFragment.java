package com.example.focus_flow.feature.tasks;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.focus_flow.R;
import com.example.focus_flow.core.navigation.FocusStartStore;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.feature.reminder.TaskReminderScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class TasksFragment extends Fragment {
    private LinearLayout content;
    private RepositoryProvider provider;

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
        provider.taskRepository.refresh();
        content.removeAllViews();
        content.addView(TaskUi.text(requireContext(), "任务", 30,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        content.addView(TaskUi.text(requireContext(), "查看全部学习任务、进度和计划番茄钟。", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        content.addView(TaskUi.spacer(requireContext(), 14));
        MaterialButton add = TaskUi.button(requireContext(), "+ 新增任务", true);
        add.setId(R.id.tasks_button_add_task);
        add.setOnClickListener(v -> showTaskForm(null));
        content.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(TaskUi.spacer(requireContext(), 14));

        List<TaskRecord> tasks = provider.taskRepository.observeAllVisibleTasks().getValue();
        if (tasks == null || tasks.isEmpty()) {
            content.addView(emptyCard());
            return;
        }
        TaskCards cards = new TaskCards(requireContext());
        TaskCards.Actions actions = actions();
        for (TaskRecord task : tasks) {
            content.addView(cards.taskCard(task, actions));
        }
    }

    private MaterialCardView emptyCard() {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 18);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "还没有任务记录", 18,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), "创建任务后，专注记录会自动关联到这里。", 14,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        return card;
    }

    private TaskCards.Actions actions() {
        return new TaskCards.Actions() {
            @Override
            public void onStart(TaskRecord task) {
                if (provider.focusSessionRepository.getRunningSession() != null) {
                    NavHostFragment.findNavController(TasksFragment.this).navigate(R.id.focusFragment);
                    return;
                }
                new FocusStartStore(requireContext()).requestStart(task.id);
                Toast.makeText(requireContext(), "已进入专注舱", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(TasksFragment.this).navigate(R.id.focusFragment);
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
        new TaskFormBottomSheet(task, (startNow, taskId) -> {
            render();
            if (startNow) {
                new FocusStartStore(requireContext()).requestStart(taskId);
                NavHostFragment.findNavController(this).navigate(R.id.focusFragment);
            }
        }).show(getParentFragmentManager(), "task_form");
    }

    private void confirmDelete(TaskRecord task) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除任务")
                .setMessage("删除后不会影响历史统计，但任务将从任务列表中隐藏。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认删除", (dialog, which) -> {
                    TaskReminderScheduler.cancel(requireContext(), task.id);
                    provider.taskRepository.softDeleteTask(task.id);
                    render();
                })
                .show();
    }
}
