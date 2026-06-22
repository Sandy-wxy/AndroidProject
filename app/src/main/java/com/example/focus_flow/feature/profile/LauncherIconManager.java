package com.example.focus_flow.feature.profile;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import com.example.focus_flow.R;

public final class LauncherIconManager {
    private static final String PREFS = "launcher_icon_preferences";
    private static final String KEY_STYLE = "selected_style";

    public static final IconStyle[] STYLES = {
            new IconStyle("classic", "经典青", "清爽明亮的默认样式",
                    R.mipmap.ic_launcher_classic, ".ClassicIconAlias"),
            new IconStyle("ocean", "深海蓝", "沉静专注的蓝色时钟",
                    R.mipmap.ic_launcher_ocean, ".OceanIconAlias"),
            new IconStyle("aurora", "极光紫", "柔和灵动的紫色书本",
                    R.mipmap.ic_launcher_aurora, ".AuroraIconAlias"),
            new IconStyle("forest", "森林绿", "自然舒缓的绿色嫩芽",
                    R.mipmap.ic_launcher_forest, ".ForestIconAlias"),
            new IconStyle("tomato_girl", "番茄少女", "活力可爱的番茄专注伙伴",
                    R.mipmap.ic_launcher_tomato_girl, ".TomatoGirlIconAlias"),
            new IconStyle("tomato_clock", "番茄时钟", "简洁立体的番茄计时器",
                    R.mipmap.ic_launcher_tomato_clock, ".TomatoClockIconAlias")
    };

    private LauncherIconManager() {
    }

    public static String selectedStyle(Context context) {
        return preferences(context).getString(KEY_STYLE, STYLES[0].key);
    }

    public static void apply(Context context, IconStyle selected) {
        PackageManager manager = context.getPackageManager();
        String packageName = context.getPackageName();

        manager.setComponentEnabledSetting(
                new ComponentName(packageName, packageName + selected.aliasSuffix),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        for (IconStyle style : STYLES) {
            if (style.key.equals(selected.key)) {
                continue;
            }
            manager.setComponentEnabledSetting(
                    new ComponentName(packageName, packageName + style.aliasSuffix),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
        preferences(context).edit().putString(KEY_STYLE, selected.key).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class IconStyle {
        public final String key;
        public final String title;
        public final String description;
        public final int iconRes;
        private final String aliasSuffix;

        private IconStyle(String key, String title, String description,
                          int iconRes, String aliasSuffix) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
            this.aliasSuffix = aliasSuffix;
        }
    }
}
