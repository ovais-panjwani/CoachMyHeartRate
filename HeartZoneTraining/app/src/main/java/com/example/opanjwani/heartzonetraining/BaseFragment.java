package com.example.opanjwani.heartzonetraining;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;

public abstract class BaseFragment extends Fragment {

    protected abstract int getTitleId();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();

        int id = getTitleId();
        if (id == 0) {
            throw new RuntimeException("please provide a valid string resource id.");
        }

        String title = getString(id);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(title);
    }
}
