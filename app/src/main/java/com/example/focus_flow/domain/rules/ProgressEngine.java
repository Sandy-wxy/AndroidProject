package com.example.focus_flow.domain.rules;

import com.example.focus_flow.core.model.FocusSessionStatus;
import com.example.focus_flow.data.local.model.FocusSessionRecord;
import com.example.focus_flow.data.local.model.TaskRecord;

import java.util.List;

public class ProgressEngine {
    public double effectiveMinutesForSession(FocusSessionRecord session) {
        int actualMinutes = Math.max(0, session.actualFocusSeconds / 60);
        if (session.status == FocusSessionStatus.COMPLETED) {
            double multiplier;
            if (session.qualityScore == null || session.qualityScore >= 4) {
                multiplier = 1.0;
            } else if (session.qualityScore == 3) {
                multiplier = 0.85;
            } else {
                multiplier = 0.65;
            }
            return actualMinutes * multiplier;
        }
        if (session.status == FocusSessionStatus.ABANDONED) {
            if (session.progressRatio >= 0.70 && session.qualityScore != null && session.qualityScore >= 3) {
                return actualMinutes * 0.60;
            }
            if (session.progressRatio >= 0.40) {
                return actualMinutes * 0.40;
            }
            return actualMinutes * 0.20;
        }
        if (session.status == FocusSessionStatus.INTERRUPTED) {
            return actualMinutes * 0.50;
        }
        return 0;
    }

    public double totalEffectiveMinutes(List<FocusSessionRecord> sessions) {
        double total = 0;
        for (FocusSessionRecord session : sessions) {
            if (session.effectiveProgressMinutes > 0) {
                total += session.effectiveProgressMinutes;
            } else {
                total += effectiveMinutesForSession(session);
            }
        }
        return total;
    }

    public int progressPercent(TaskRecord task, List<FocusSessionRecord> sessions) {
        if (task.estimatedTotalMinutes <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(totalEffectiveMinutes(sessions) / task.estimatedTotalMinutes * 100.0));
    }
}
