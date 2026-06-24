package com.example.focus_flow.feature.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.focus_flow.R;
import com.example.focus_flow.SettingsActivity;
import com.example.focus_flow.core.common.DateTimeUtils;
import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.TaskStatus;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.domain.stats.StatsCalculator;
import com.example.focus_flow.domain.stats.SummaryStats;
import com.example.focus_flow.feature.stats.StatsLineChartView;
import com.example.focus_flow.feature.stats.StatsRingChartView;
import com.example.focus_flow.feature.tasks.TaskCards;
import com.example.focus_flow.feature.tasks.TaskUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProfileFragment extends Fragment {
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private LinearLayout content;
    private LinearLayout selectedDayContainer;
    private RepositoryProvider provider;
    private AccountPreferences account;
    private final StatsCalculator calculator = new StatsCalculator();
    private long selectedDate = System.currentTimeMillis();
    private List<FocusSessionRecord> yearSessions = Collections.emptyList();
    private Map<Long, ContributionHeatmapView.DayContribution> contributions = Collections.emptyMap();
    private long heatmapStart;
    private long heatmapEnd;
    private ActivityResultLauncher<String[]> avatarPicker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        avatarPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            try {
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
            account.setAvatarUri(uri.toString());
            render();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        provider = RepositoryProvider.get(requireContext());
        account = new AccountPreferences(requireContext());
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
        provider.focusSessionRepository.refresh();
        content.removeAllViews();
        addBackHomeButton();
        if (!account.isLoggedIn()) {
            renderLogin();
            return;
        }
        prepareContributionData();
        renderProfile();
    }

    private void addBackHomeButton() {
        content.addView(TaskUi.backHeader(requireContext(), R.id.profile_button_back_home,
                "我的", "个人数据、年度贡献和资料管理", v -> navigateBack()));
        content.addView(TaskUi.spacer(requireContext(), 16));
    }

    private void navigateBack() {
        androidx.navigation.NavController navController = NavHostFragment.findNavController(this);
        if (!navController.navigateUp()) {
            navController.navigate(R.id.homeFragment);
        }
    }
    private void renderLogin() {
        content.addView(TaskUi.text(requireContext(), "欢迎回来", 30,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        content.addView(TaskUi.text(requireContext(), "登录后查看个人数据、专注统计和年度贡献。",
                14, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        content.addView(TaskUi.spacer(requireContext(), 20));

        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 20);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "登录 / 创建本地账户", 20,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));

        EditText name = input("昵称", InputType.TYPE_CLASS_TEXT);
        name.setId(R.id.profile_input_name);
        EditText email = input("邮箱", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        email.setId(R.id.profile_input_email);
        EditText password = input("密码（至少 6 位）",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setId(R.id.profile_input_password);
        body.addView(name);
        body.addView(email);
        body.addView(password);
        body.addView(TaskUi.spacer(requireContext(), 10));
        MaterialButton login = TaskUi.button(requireContext(), "进入 Focus Flow", true);
        login.setId(R.id.profile_button_login);
        body.addView(login);
        body.addView(TaskUi.text(requireContext(), "账户信息仅保存在本机；密码只用于本次校验，不会被保存。",
                12, requireContext().getColor(R.color.text_weak), android.graphics.Typeface.NORMAL));
        content.addView(card);

        login.setOnClickListener(v -> {
            String nameText = name.getText().toString().trim();
            String emailText = email.getText().toString().trim();
            String passwordText = password.getText().toString();
            boolean valid = true;
            if (nameText.isEmpty()) {
                name.setError("请输入昵称");
                valid = false;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                email.setError("请输入有效邮箱");
                valid = false;
            }
            if (passwordText.length() < 6) {
                password.setError("密码至少 6 位");
                valid = false;
            }
            if (!valid) {
                return;
            }
            account.login(nameText, emailText);
            Toast.makeText(requireContext(), "登录成功", Toast.LENGTH_SHORT).show();
            render();
        });
    }

    private void renderProfile() {
        addProfileHeader();
        addOverviewMetrics();
        addContributionCalendar();
        addSelectedDayDetails();
        addTrendAndSubjectStats();
        addAccountActions();
    }

    private void addProfileHeader() {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        card.setId(R.id.profile_card_identity);
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout body = TaskUi.vertical(requireContext(), 20);
        card.addView(body);

        LinearLayout header = TaskUi.horizontal(requireContext());
        FrameLayout avatarBox = avatarView();
        avatarBox.setOnClickListener(v -> avatarPicker.launch(new String[]{"image/*"}));
        header.addView(avatarBox, new LinearLayout.LayoutParams(
                TaskUi.dp(requireContext(), 72), TaskUi.dp(requireContext(), 72)));

        LinearLayout identity = TaskUi.vertical(requireContext(), 0);
        identity.addView(TaskUi.text(requireContext(), account.name(), 27,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        identity.addView(TaskUi.text(requireContext(), account.email(), 13,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        identity.addView(TaskUi.text(requireContext(), profileRank(), 13,
                requireContext().getColor(R.color.focus_purple), android.graphics.Typeface.BOLD));
        LinearLayout.LayoutParams identityParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        identityParams.setMarginStart(TaskUi.dp(requireContext(), 14));
        header.addView(identity, identityParams);

        MaterialButton edit = TaskUi.button(requireContext(), "编辑资料", false);
        edit.setId(R.id.profile_button_edit);
        edit.setOnClickListener(v -> showProfileEditor());
        header.addView(edit);
        body.addView(header);
        body.addView(TaskUi.spacer(requireContext(), 12));
        body.addView(TaskUi.text(requireContext(),
                "过去一年 " + activeDayCount() + " 个活跃日 · 当前连续 " + currentStreak() + " 天",
                14, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.BOLD));
        content.addView(card);
        card.setOnClickListener(v -> showProfileEditor());
        content.addView(TaskUi.spacer(requireContext(), 10));
    }

    private FrameLayout avatarView() {
        FrameLayout box = new FrameLayout(requireContext());
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        background.setColor(requireContext().getColor(R.color.focus_cyan));
        String avatarUri = account.avatarUri();
        if (avatarUri != null && !avatarUri.isEmpty()) {
            ImageView image = new ImageView(requireContext());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(background);
            image.setClipToOutline(true);
            try {
                image.setImageURI(Uri.parse(avatarUri));
            } catch (RuntimeException ignored) {
                account.setAvatarUri("");
            }
            box.addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            TextView initials = TaskUi.text(requireContext(), avatarText(), 25,
                    requireContext().getColor(android.R.color.white), android.graphics.Typeface.BOLD);
            initials.setGravity(android.view.Gravity.CENTER);
            initials.setBackground(background);
            box.addView(initials, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        TextView camera = TaskUi.text(requireContext(), "✎", 12,
                requireContext().getColor(android.R.color.white), android.graphics.Typeface.BOLD);
        camera.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable badge = new android.graphics.drawable.GradientDrawable();
        badge.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        badge.setColor(requireContext().getColor(R.color.focus_purple));
        camera.setBackground(badge);
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                TaskUi.dp(requireContext(), 25), TaskUi.dp(requireContext(), 25),
                android.view.Gravity.END | android.view.Gravity.BOTTOM);
        box.addView(camera, badgeParams);
        return box;
    }

    private void showProfileEditor() {
        LinearLayout form = TaskUi.vertical(requireContext(), 18);
        form.addView(TaskUi.text(requireContext(), "编辑个人资料", 22,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        EditText name = input("昵称", InputType.TYPE_CLASS_TEXT);
        name.setText(account.name());
        name.setId(R.id.profile_edit_name);
        EditText email = input("邮箱",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        email.setText(account.email());
        email.setId(R.id.profile_edit_email);
        MaterialButton avatar = TaskUi.button(requireContext(), "更换头像", false);
        avatar.setId(R.id.profile_button_change_avatar);
        avatar.setOnClickListener(v -> avatarPicker.launch(new String[]{"image/*"}));
        form.addView(name);
        form.addView(email);
        form.addView(avatar);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String nameText = name.getText().toString().trim();
                    String emailText = email.getText().toString().trim();
                    if (nameText.isEmpty()) {
                        name.setError("昵称不能为空");
                        return;
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                        email.setError("请输入有效邮箱");
                        return;
                    }
                    account.updateProfile(nameText, emailText);
                    Toast.makeText(requireContext(), "个人资料已更新", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    render();
                }));
        dialog.show();
    }

    private void addOverviewMetrics() {
        SummaryStats summary = calculator.calculateSummary(terminalSessions(yearSessions));
        TaskCards cards = new TaskCards(requireContext());
        LinearLayout rowOne = TaskUi.horizontal(requireContext());
        rowOne.addView(cards.metricCard("年度专注", formatDuration(summary.totalFocusSeconds)), metricParams());
        rowOne.addView(cards.metricCard("完成任务",
                String.valueOf(provider.taskRepository.getCompletedTasks().size())), metricParams());
        content.addView(rowOne);

        LinearLayout rowTwo = TaskUi.horizontal(requireContext());
        rowTwo.addView(cards.metricCard("活跃天数", String.valueOf(activeDayCount())), metricParams());
        rowTwo.addView(cards.metricCard("完成率", percent(summary.completionRate)), metricParams());
        content.addView(rowTwo);
        content.addView(TaskUi.spacer(requireContext(), 10));
    }

    private void addContributionCalendar() {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        LinearLayout body = TaskUi.vertical(requireContext(), 16);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), "年度贡献", 20,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(),
                "每个方格代表一天；完成任务越多、专注越久，颜色越深。左右滑动查看全年。",
                13, requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));

        HorizontalScrollView horizontal = new HorizontalScrollView(requireContext());
        horizontal.setHorizontalScrollBarEnabled(false);
        ContributionHeatmapView heatmap = new ContributionHeatmapView(requireContext());
        heatmap.setId(R.id.profile_contribution_heatmap);
        heatmap.setData(heatmapStart, heatmapEnd, contributions, selectedDate);
        heatmap.setOnDaySelectedListener(day -> {
            selectedDate = day;
            renderSelectedDayDetails();
        });
        horizontal.addView(heatmap);
        body.addView(horizontal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(requireContext(), 150)));
        horizontal.post(() -> horizontal.fullScroll(View.FOCUS_RIGHT));

        LinearLayout legend = TaskUi.horizontal(requireContext());
        legend.setGravity(android.view.Gravity.END);
        legend.addView(TaskUi.text(requireContext(), "少  □  ▣  ▣  ■  多", 11,
                requireContext().getColor(R.color.text_weak), android.graphics.Typeface.NORMAL));
        body.addView(legend);
        content.addView(card);
    }

    private void addSelectedDayDetails() {
        selectedDayContainer = TaskUi.vertical(requireContext(), 0);
        selectedDayContainer.setId(R.id.profile_selected_day_tasks);
        content.addView(selectedDayContainer);
        renderSelectedDayDetails();
    }

    private void renderSelectedDayDetails() {
        if (selectedDayContainer == null) {
            return;
        }
        selectedDayContainer.removeAllViews();
        selectedDayContainer.addView(TaskUi.spacer(requireContext(), 12));

        String dayLabel = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
                .format(new Date(selectedDate));
        ContributionHeatmapView.DayContribution contribution =
                contributions.get(DateTimeUtils.startOfDayMillis(selectedDate));
        int focusSeconds = contribution == null ? 0 : contribution.focusSeconds;
        int completedCount = contribution == null ? 0 : contribution.completedTasks;
        selectedDayContainer.addView(TaskUi.text(requireContext(), dayLabel, 20,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        selectedDayContainer.addView(TaskUi.text(requireContext(),
                "专注 " + formatDuration(focusSeconds) + " · 完成 " + completedCount + " 个任务",
                13, requireContext().getColor(R.color.focus_cyan), android.graphics.Typeface.BOLD));
        selectedDayContainer.addView(TaskUi.spacer(requireContext(), 8));

        List<TaskRecord> tasks = tasksForSelectedDate();
        if (tasks.isEmpty()) {
            selectedDayContainer.addView(infoCard("暂无任务记录",
                    "这一天没有计划或完成的任务，贡献值来自专注时长时仍会显示颜色。", R.color.text_weak));
            return;
        }
        for (TaskRecord task : tasks) {
            boolean completedOnDay = task.completedAt != null
                    && task.completedAt >= DateTimeUtils.startOfDayMillis(selectedDate)
                    && task.completedAt <= DateTimeUtils.endOfDayMillis(selectedDate);
            String status = completedOnDay ? "已完成" : statusText(task.status);
            String detail = task.subject + " · 预计 " + task.estimatedTotalMinutes + " 分钟 · " + status;
            if (completedOnDay) {
                detail += " " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(new Date(task.completedAt));
            }
            selectedDayContainer.addView(infoCard(task.title, detail,
                    completedOnDay ? R.color.focus_green : R.color.focus_orange));
        }
    }

    private void addTrendAndSubjectStats() {
        content.addView(TaskUi.spacer(requireContext(), 10));
        MaterialCardView trendCard = TaskUi.glassCard(requireContext());
        LinearLayout trendBody = TaskUi.vertical(requireContext(), 16);
        trendCard.addView(trendBody);
        trendBody.addView(TaskUi.text(requireContext(), "最近 7 天专注趋势", 20,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        StatsLineChartView line = new StatsLineChartView(requireContext());
        line.setData(lastSevenDaySeconds(), lastSevenDayLabels());
        trendBody.addView(line, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(requireContext(), 220)));
        content.addView(trendCard);

        MaterialCardView subjectCard = TaskUi.glassCard(requireContext());
        LinearLayout subjectBody = TaskUi.vertical(requireContext(), 16);
        subjectCard.addView(subjectBody);
        subjectBody.addView(TaskUi.text(requireContext(), "年度科目分布", 20,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        List<StatsRingChartView.Segment> segments = subjectSegments();
        StatsRingChartView ring = new StatsRingChartView(requireContext());
        ring.setSegments(segments);
        subjectBody.addView(ring, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, TaskUi.dp(requireContext(), 220)));
        for (StatsRingChartView.Segment segment : segments) {
            TextView legend = TaskUi.text(requireContext(),
                    "● " + segment.label + "  " + formatDuration(segment.value),
                    13, segment.color, android.graphics.Typeface.BOLD);
            subjectBody.addView(legend);
        }
        content.addView(subjectCard);
    }

    private void addAccountActions() {
        MaterialCardView appearanceCard = TaskUi.glassCard(requireContext());
        LinearLayout appearanceBody = TaskUi.vertical(requireContext(), 16);
        appearanceCard.addView(appearanceBody);
        appearanceBody.addView(TaskUi.text(requireContext(), "个性化", 20,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        appearanceBody.addView(TaskUi.text(requireContext(),
                "选择喜欢的桌面图标，让 Focus Flow 更符合你的风格。",
                13, requireContext().getColor(R.color.text_secondary),
                android.graphics.Typeface.NORMAL));
        MaterialButton iconButton = TaskUi.button(requireContext(),
                "应用图标 · " + selectedIconTitle(), false);
        iconButton.setId(R.id.profile_button_app_icon);
        iconButton.setOnClickListener(v -> showIconPicker());
        appearanceBody.addView(iconButton);
        content.addView(appearanceCard);

        MaterialButton logout = TaskUi.button(requireContext(), "退出登录", false);
        logout.setId(R.id.profile_button_logout);
        logout.setOnClickListener(v -> {
            account.logout();
            render();
        });
        content.addView(TaskUi.spacer(requireContext(), 12));
        content.addView(logout);
    }

    private String selectedIconTitle() {
        String selected = LauncherIconManager.selectedStyle(requireContext());
        for (LauncherIconManager.IconStyle style : LauncherIconManager.STYLES) {
            if (style.key.equals(selected)) {
                return style.title;
            }
        }
        return LauncherIconManager.STYLES[0].title;
    }

    private void showIconPicker() {
        LinearLayout list = TaskUi.vertical(requireContext(), 14);
        list.addView(TaskUi.text(requireContext(), "选择应用图标", 22,
                requireContext().getColor(R.color.text_primary), android.graphics.Typeface.BOLD));
        list.addView(TaskUi.text(requireContext(),
                "桌面图标通常会在几秒内更新，具体时间取决于系统启动器。",
                13, requireContext().getColor(R.color.text_secondary),
                android.graphics.Typeface.NORMAL));

        String current = LauncherIconManager.selectedStyle(requireContext());
        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(list)
                        .setNegativeButton("取消", null)
                        .create();

        for (LauncherIconManager.IconStyle style : LauncherIconManager.STYLES) {
            MaterialCardView card = TaskUi.glassCard(requireContext());
            card.setClickable(true);
            card.setFocusable(true);
            card.setStrokeWidth(TaskUi.dp(requireContext(),
                    style.key.equals(current) ? 2 : 1));
            card.setStrokeColor(requireContext().getColor(
                    style.key.equals(current) ? R.color.focus_purple : R.color.glass_stroke));

            LinearLayout row = TaskUi.horizontal(requireContext());
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            ImageView preview = new ImageView(requireContext());
            preview.setImageResource(style.iconRes);
            row.addView(preview, new LinearLayout.LayoutParams(
                    TaskUi.dp(requireContext(), 58), TaskUi.dp(requireContext(), 58)));

            LinearLayout labels = TaskUi.vertical(requireContext(), 0);
            labels.addView(TaskUi.text(requireContext(), style.title, 17,
                    requireContext().getColor(R.color.text_primary),
                    android.graphics.Typeface.BOLD));
            labels.addView(TaskUi.text(requireContext(), style.description, 12,
                    requireContext().getColor(R.color.text_secondary),
                    android.graphics.Typeface.NORMAL));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            labelParams.setMarginStart(TaskUi.dp(requireContext(), 14));
            row.addView(labels, labelParams);

            TextView selectedMark = TaskUi.text(requireContext(),
                    style.key.equals(current) ? "✓" : "›", 20,
                    requireContext().getColor(style.key.equals(current)
                            ? R.color.focus_purple : R.color.text_weak),
                    android.graphics.Typeface.BOLD);
            row.addView(selectedMark);
            card.addView(row);
            card.setOnClickListener(v -> {
                LauncherIconManager.apply(requireContext(), style);
                Toast.makeText(requireContext(),
                        "已切换为“" + style.title + "”", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                render();
            });
            list.addView(card);
        }
        dialog.show();
    }

    private void prepareContributionData() {
        heatmapEnd = DateTimeUtils.startOfDayMillis(System.currentTimeMillis());
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(heatmapEnd);
        start.add(Calendar.DAY_OF_YEAR, -364);
        while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }
        heatmapStart = DateTimeUtils.startOfDayMillis(start.getTimeInMillis());
        yearSessions = provider.focusSessionRepository.getSessionsBetween(
                heatmapStart, DateTimeUtils.endOfDayMillis(heatmapEnd));

        Map<Long, ContributionHeatmapView.DayContribution> values = new HashMap<>();
        for (FocusSessionRecord session : yearSessions) {
            if (session.status == FocusSessionStatus.RUNNING) {
                continue;
            }
            long day = DateTimeUtils.startOfDayMillis(session.startedAt);
            contribution(values, day).focusSeconds += Math.max(0, session.actualFocusSeconds);
        }
        for (TaskRecord task : provider.taskRepository.getCompletedTasks()) {
            if (task.completedAt == null || task.completedAt < heatmapStart
                    || task.completedAt > DateTimeUtils.endOfDayMillis(heatmapEnd)) {
                continue;
            }
            long day = DateTimeUtils.startOfDayMillis(task.completedAt);
            contribution(values, day).completedTasks++;
        }
        contributions = values;
        if (selectedDate < heatmapStart || selectedDate > DateTimeUtils.endOfDayMillis(heatmapEnd)) {
            selectedDate = heatmapEnd;
        }
    }

    private ContributionHeatmapView.DayContribution contribution(
            Map<Long, ContributionHeatmapView.DayContribution> values, long day) {
        ContributionHeatmapView.DayContribution result = values.get(day);
        if (result == null) {
            result = new ContributionHeatmapView.DayContribution();
            values.put(day, result);
        }
        return result;
    }

    private List<TaskRecord> tasksForSelectedDate() {
        List<TaskRecord> tasks = new ArrayList<>(provider.taskRepository.getTasksForDate(
                DateTimeUtils.formatDate(selectedDate)));
        Set<Long> ids = new HashSet<>();
        for (TaskRecord task : tasks) {
            ids.add(task.id);
        }
        for (TaskRecord completed : provider.taskRepository.getCompletedTasksBetween(
                DateTimeUtils.startOfDayMillis(selectedDate),
                DateTimeUtils.endOfDayMillis(selectedDate))) {
            if (ids.add(completed.id)) {
                tasks.add(completed);
            }
        }
        tasks.sort((left, right) -> {
            boolean leftDone = left.completedAt != null;
            boolean rightDone = right.completedAt != null;
            if (leftDone != rightDone) {
                return leftDone ? -1 : 1;
            }
            return Long.compare(right.updatedAt, left.updatedAt);
        });
        return tasks;
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

    private int activeDayCount() {
        int count = 0;
        for (ContributionHeatmapView.DayContribution contribution : contributions.values()) {
            if (contribution.score() > 0) {
                count++;
            }
        }
        return count;
    }

    private int currentStreak() {
        int streak = 0;
        long day = heatmapEnd;
        while (day >= heatmapStart) {
            ContributionHeatmapView.DayContribution contribution = contributions.get(day);
            if (contribution == null || contribution.score() == 0) {
                if (day == heatmapEnd) {
                    day -= DAY_MILLIS;
                    continue;
                }
                break;
            }
            streak++;
            day -= DAY_MILLIS;
        }
        return streak;
    }

    private String profileRank() {
        int active = activeDayCount();
        if (active >= 180) {
            return "Grandmaster · 专注宗师";
        }
        if (active >= 90) {
            return "Expert · 专注专家";
        }
        if (active >= 30) {
            return "Specialist · 稳定学习者";
        }
        if (active >= 7) {
            return "Pupil · 成长学习者";
        }
        return "Newbie · 新晋学习者";
    }

    private String avatarText() {
        String name = account.name().trim();
        return name.isEmpty() ? "F" : name.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private int[] lastSevenDaySeconds() {
        int[] values = new int[7];
        long first = DateTimeUtils.startOfDayMillis(heatmapEnd - 6L * DAY_MILLIS);
        for (FocusSessionRecord session : yearSessions) {
            if (session.status == FocusSessionStatus.RUNNING) {
                continue;
            }
            int index = (int) ((DateTimeUtils.startOfDayMillis(session.startedAt) - first) / DAY_MILLIS);
            if (index >= 0 && index < 7) {
                values[index] += Math.max(0, session.actualFocusSeconds);
            }
        }
        return values;
    }

    private String[] lastSevenDayLabels() {
        String[] labels = new String[7];
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(heatmapEnd - 6L * DAY_MILLIS);
        for (int i = 0; i < labels.length; i++) {
            labels[i] = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return labels;
    }

    private List<StatsRingChartView.Segment> subjectSegments() {
        Map<String, Integer> raw = calculator.subjectSeconds(terminalSessions(yearSessions));
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(raw.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue()).reversed());
        int[] colors = {
                requireContext().getColor(R.color.focus_cyan),
                requireContext().getColor(R.color.focus_purple),
                requireContext().getColor(R.color.focus_green),
                requireContext().getColor(R.color.focus_orange),
                requireContext().getColor(R.color.focus_pink)
        };
        List<StatsRingChartView.Segment> segments = new ArrayList<>();
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            if (entry.getValue() <= 0) {
                continue;
            }
            String subject = entry.getKey() == null || entry.getKey().trim().isEmpty()
                    ? "未分类" : entry.getKey();
            segments.add(new StatsRingChartView.Segment(subject, entry.getValue(), colors[i]));
        }
        return segments;
    }

    private MaterialCardView infoCard(String title, String bodyText, int accentColor) {
        MaterialCardView card = TaskUi.glassCard(requireContext());
        card.setStrokeColor(requireContext().getColor(accentColor));
        LinearLayout body = TaskUi.vertical(requireContext(), 16);
        card.addView(body);
        body.addView(TaskUi.text(requireContext(), title, 16,
                requireContext().getColor(accentColor), android.graphics.Typeface.BOLD));
        body.addView(TaskUi.text(requireContext(), bodyText, 13,
                requireContext().getColor(R.color.text_secondary), android.graphics.Typeface.NORMAL));
        return card;
    }

    private EditText input(String hint, int inputType) {
        EditText input = new EditText(requireContext());
        input.setHint(hint);
        input.setInputType(inputType);
        input.setTextColor(requireContext().getColor(R.color.text_primary));
        input.setHintTextColor(requireContext().getColor(R.color.text_weak));
        return input;
    }

    private String statusText(TaskStatus status) {
        if (status == TaskStatus.COMPLETED) {
            return "已完成";
        }
        if (status == TaskStatus.IN_PROGRESS) {
            return "进行中";
        }
        if (status == TaskStatus.ARCHIVED) {
            return "已归档";
        }
        return "待开始";
    }

    private String formatDuration(int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        if (minutes >= 60) {
            return (minutes / 60) + "小时" + (minutes % 60) + "分";
        }
        return minutes + "分钟";
    }

    private String percent(double value) {
        return Math.round(Math.max(0, Math.min(1, value)) * 100) + "%";
    }

    private LinearLayout.LayoutParams metricParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMarginEnd(TaskUi.dp(requireContext(), 6));
        return params;
    }
}
