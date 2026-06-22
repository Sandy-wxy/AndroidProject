package com.example.focus_flow.feature.reminder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class TaskReminderBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.getPackageManager().setComponentEnabledSetting(
                new ComponentName(context, "com.example.focus_flow.DefaultIconAlias"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        TaskReminderScheduler.restoreAll(context);
    }
}
