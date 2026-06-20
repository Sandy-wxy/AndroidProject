package com.example.focus_flow.domain.rules;

import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.core.model.TaskDifficulty;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;
import com.example.focus_flow.domain.stats.RecentStats;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class EncouragementEngine {
    private final Map<EncouragementCategory, String[]> messages = new EnumMap<>(EncouragementCategory.class);

    public EncouragementEngine() {
        messages.put(EncouragementCategory.FIRST_COMPLETE_TODAY, new String[]{
                "今天的第一个专注块完成了，节奏已经被你启动。",
                "第一步最难，你已经跨过去了。",
                "今天的学习账户已经入账一笔。",
                "一个稳定的开始，比完美计划更重要。",
                "很好，今天不是空白的一天。"
        });
        messages.put(EncouragementCategory.SMOOTH_COMPLETE, new String[]{
                "这次专注很完整，状态保持得很漂亮。",
                "你刚刚完成了一段高质量的沉浸时间。",
                "专注没有被打断，这就是扎实的推进。",
                "这一轮很稳，继续沿着这个节奏走。",
                "时间被认真使用时，会留下很明确的进度。"
        });
        messages.put(EncouragementCategory.COMPLETED_WITH_PAUSE, new String[]{
                "中间有停顿，但你还是把它完成了。",
                "能重新回来继续，本身就是专注能力的一部分。",
                "暂停没有让你放弃，这次很值得记录。",
                "节奏不必完美，完成才是关键。",
                "你把注意力拉回来了，这比从不分心更真实。"
        });
        messages.put(EncouragementCategory.HARD_TASK_COMPLETE, new String[]{
                "困难任务已经推进了一大步，别低估这次完成。",
                "高难度任务能完成，说明你今天很能扛。",
                "这不是轻松的一轮，但你拿下来了。",
                "复杂任务最怕拖延，你已经把它切开了。",
                "越难的任务，越需要这样的稳定推进。"
        });
        messages.put(EncouragementCategory.STREAK_COMPLETE, new String[]{
                "连续完成的节奏正在形成，保持住。",
                "你的专注不是偶然，已经开始变成习惯。",
                "一轮又一轮的完成，会把目标慢慢推近。",
                "稳定比爆发更稀有，你正在做到。",
                "连续完成很不容易，这个记录值得保留。"
        });
        messages.put(EncouragementCategory.LOW_QUALITY_COMPLETE, new String[]{
                "虽然状态一般，但你依然完成了这一轮。",
                "这次质量不完美，不过完成记录是真实的。",
                "状态低的时候还能坚持，已经很不容易。",
                "下一轮可以短一点，把节奏慢慢找回来。",
                "今天不用苛责自己，先把注意力重新养起来。"
        });
        messages.put(EncouragementCategory.ABANDONED_NEAR_END, new String[]{
                "已经坚持到后半段了，这不是失败。",
                "差一点完成，说明你离稳定只差一点调整。",
                "这次虽然提前结束，但前面的时间仍然算数。",
                "下次可以把时长稍微调短，完成率会更高。",
                "你不是没开始，你已经推进了很久。"
        });
        messages.put(EncouragementCategory.ABANDONED_HALF, new String[]{
                "你已经完成了一部分，先把原因记录下来。",
                "中途停下也能提供信息，下一轮会更准。",
                "这次不是白费，它会帮助系统调整推荐。",
                "先别急着否定自己，找到中断原因更重要。",
                "完成一半说明你能进入状态，只是节奏还要再调。"
        });
        messages.put(EncouragementCategory.ABANDONED_EARLY, new String[]{
                "开始得不顺也没关系，下一轮从更短时间试起。",
                "这次先当作一次状态检测，不必过度责备。",
                "能意识到无法继续，也是调整计划的一部分。",
                "把任务拆小一点，下一次会更容易启动。",
                "今天可以慢一点，但不要让一次中断定义整天。"
        });
        messages.put(EncouragementCategory.EXTERNAL_INTERRUPTION, new String[]{
                "这次中断不完全由你决定，记录下来就好。",
                "环境打断了节奏，但进度不会被抹掉。",
                "先保存现场，等条件合适再继续。",
                "不是每一次专注都能避开意外，重要的是能回来。",
                "这次先收住，下一轮重新建立安静环境。"
        });
    }

    public EncouragementCategory determineCategory(FocusSessionRecord session, TaskRecord task,
                                                   RecentStats statsAfterSession,
                                                   boolean isFirstCompletedSessionOfDay) {
        if (session.status == FocusSessionStatus.INTERRUPTED) {
            return EncouragementCategory.EXTERNAL_INTERRUPTION;
        }
        if (session.status == FocusSessionStatus.ABANDONED) {
            if (session.progressRatio >= 0.70) {
                return EncouragementCategory.ABANDONED_NEAR_END;
            }
            if (session.progressRatio >= 0.40) {
                return EncouragementCategory.ABANDONED_HALF;
            }
            return EncouragementCategory.ABANDONED_EARLY;
        }
        if (session.status == FocusSessionStatus.COMPLETED) {
            if (session.qualityScore != null && session.qualityScore <= 2) {
                return EncouragementCategory.LOW_QUALITY_COMPLETE;
            }
            if (isFirstCompletedSessionOfDay) {
                return EncouragementCategory.FIRST_COMPLETE_TODAY;
            }
            if (statsAfterSession.consecutiveCompletedSessions >= 3) {
                return EncouragementCategory.STREAK_COMPLETE;
            }
            if (task.difficulty == TaskDifficulty.HARD || task.difficulty == TaskDifficulty.EXTREME) {
                return EncouragementCategory.HARD_TASK_COMPLETE;
            }
            if (session.pauseCount > 0) {
                return EncouragementCategory.COMPLETED_WITH_PAUSE;
            }
        }
        return EncouragementCategory.SMOOTH_COMPLETE;
    }

    public String chooseMessage(EncouragementCategory category, Random random) {
        String[] options = messages.get(category);
        if (options == null || options.length == 0) {
            options = messages.get(EncouragementCategory.SMOOTH_COMPLETE);
        }
        return options[random.nextInt(options.length)];
    }
}
