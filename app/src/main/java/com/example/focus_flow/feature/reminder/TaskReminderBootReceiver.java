package com.example.focus_flow.feature.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TaskReminderBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        TaskReminderScheduler.restoreAll(context);
    }
}
