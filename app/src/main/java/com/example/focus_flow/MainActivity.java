package com.example.focus_flow;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.focus_flow.databinding.ActivityMainBinding;
import com.google.android.material.textview.MaterialTextView;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_FOCUS = "open_focus";

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
                R.id.focusFragment,
                R.id.profileFragment
        ).build();
        bindBottomNav(navController);
        openFocusIfRequested(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openFocusIfRequested(intent);
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
        bindNavItem(binding.navFocus, navController, R.id.focusFragment);
        bindNavItem(binding.navProfile, navController, R.id.profileFragment);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> updateBottomNav(destination.getId()));
        updateBottomNav(navController.getCurrentDestination() == null
                ? R.id.homeFragment
                : navController.getCurrentDestination().getId());
    }

    private void bindNavItem(MaterialTextView item, NavController navController, int destinationId) {
        item.setOnClickListener(v -> {
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != destinationId) {
                navController.navigate(destinationId);
            }
        });
    }

    private void updateBottomNav(int destinationId) {
        binding.navHome.setSelected(destinationId == R.id.homeFragment);
        binding.navFocus.setSelected(destinationId == R.id.focusFragment);
        binding.navProfile.setSelected(destinationId == R.id.profileFragment);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
