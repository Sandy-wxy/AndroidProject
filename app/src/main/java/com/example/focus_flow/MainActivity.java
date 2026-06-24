package com.example.focus_flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.fragment.app.Fragment;

import com.example.focus_flow.databinding.ActivityMainBinding;
import com.example.focus_flow.data.repository.RepositoryProvider;
import com.example.focus_flow.feature.widget.FocusQuickWidgetProvider;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_FOCUS = "open_focus";
    public static final String EXTRA_ADD_TASK = "add_task";

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment == null) {
            throw new IllegalStateException("Navigation host is missing");
        }
        navController = navHostFragment.getNavController();
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment,
                R.id.tasksFragment,
                R.id.focusFragment,
                R.id.forestFragment,
                R.id.noiseFragment,
                R.id.statsFragment,
                R.id.profileFragment
        ).build();
        bindBottomNav(navController);
        openFocusIfRequested(getIntent());
        openAddTaskIfRequested(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openFocusIfRequested(intent);
        openAddTaskIfRequested(intent);
    }

    private void openAddTaskIfRequested(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_ADD_TASK, false)) {
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.homeFragment) {
                navController.navigate(R.id.homeFragment);
            }
            binding.main.post(this::dispatchAddTaskRequest);
        }
    }

    private void dispatchAddTaskRequest() {
        if (!consumeAddTaskRequest()) {
            return;
        }
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        Fragment current = host == null ? null
                : host.getChildFragmentManager().getPrimaryNavigationFragment();
        if (current instanceof com.example.focus_flow.feature.home.HomeFragment) {
            ((com.example.focus_flow.feature.home.HomeFragment) current).openTaskCreator();
        }
    }

    public boolean consumeAddTaskRequest() {
        Intent intent = getIntent();
        if (intent == null || !intent.getBooleanExtra(EXTRA_ADD_TASK, false)) {
            return false;
        }
        intent.removeExtra(EXTRA_ADD_TASK);
        return true;
    }

    private void openFocusIfRequested(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_FOCUS, false)) {
            intent.removeExtra(EXTRA_OPEN_FOCUS);
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.focusFragment) {
                navController.navigate(R.id.focusFragment);
            }
        }
    }

    private void bindBottomNav(NavController navController) {
        bindNavItem(binding.navHome, navController, R.id.homeFragment);
        bindNavItem(binding.navTasks, navController, R.id.tasksFragment);
        bindNavItem(binding.navFocus, navController, R.id.focusFragment);
        bindNavItem(binding.navForest, navController, R.id.forestFragment);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> updateBottomNav(destination.getId()));
        updateBottomNav(navController.getCurrentDestination() == null
                ? R.id.homeFragment
                : navController.getCurrentDestination().getId());
    }

    private void bindNavItem(View item, NavController navController, int destinationId) {
        item.setOnClickListener(v -> {
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != destinationId) {
                navController.navigate(destinationId);
            }
        });
    }

    private void updateBottomNav(int destinationId) {
        binding.navHome.setSelected(destinationId == R.id.homeFragment);
        binding.navTasks.setSelected(destinationId == R.id.tasksFragment);
        binding.navFocus.setSelected(destinationId == R.id.focusFragment);
        binding.navForest.setSelected(destinationId == R.id.forestFragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null || navController == null) {
            return;
        }
        boolean forestEnabled = RepositoryProvider.get(this)
                .settingsRepository.isForestTabEnabled();
        binding.navForest.setVisibility(forestEnabled
                ? android.view.View.VISIBLE : android.view.View.GONE);
        if (!forestEnabled && navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.forestFragment) {
            navController.navigate(R.id.homeFragment);
        }
        FocusQuickWidgetProvider.refreshAll(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
