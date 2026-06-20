package com.example.focus_flow.feature.shell;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.focus_flow.R;

public class SimpleSectionFragment extends Fragment {
    private final String title;
    private final String subtitle;

    public SimpleSectionFragment(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simple_section, container, false);
        TextView titleView = view.findViewById(R.id.sectionTitle);
        TextView subtitleView = view.findViewById(R.id.sectionSubtitle);
        titleView.setText(title);
        subtitleView.setText(subtitle);
        return view;
    }
}
