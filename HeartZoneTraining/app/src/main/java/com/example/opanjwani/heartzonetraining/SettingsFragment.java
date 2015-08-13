package com.example.opanjwani.heartzonetraining;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by opanjwani on 7/10/15.
 */
public class SettingsFragment extends BaseFragment {
    @Override
    protected int getTitleId() {
        return R.string.title_settings;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_settings, container, false);
        return view;
    }
}
