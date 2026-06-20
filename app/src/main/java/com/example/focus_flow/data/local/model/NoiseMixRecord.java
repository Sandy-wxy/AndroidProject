package com.example.focus_flow.data.local.model;

import java.util.ArrayList;
import java.util.List;

public class NoiseMixRecord {
    public long id;
    public String name;
    public boolean isPreset;
    public long createdAt;
    public long updatedAt;
    public final List<NoiseMixItemRecord> items = new ArrayList<>();
}
