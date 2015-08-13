package com.example.opanjwani.heartzonetraining;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.ua.sdk.Ua;
import com.ua.sdk.UaException;
import com.ua.sdk.UaLog;
import com.ua.sdk.user.Gender;
import com.ua.sdk.user.User;

public class RestingHeartRateDialog extends DialogFragment {

    private Context context;
    private AppCompatDialog dialog;
    private Ua ua;
    private User user;
    private Gender gender;
    private Listener listener;

    public RestingHeartRateDialog() {
    }

    public void setListener(Listener listener) { this.listener = listener; }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        context = new ContextThemeWrapper(getActivity(), R.style.Base_Theme_AppCompat_Dialog);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View content = inflater.inflate(R.layout.dialog_heart_rate, null);

        UaWrapper uaWrapper = UaWrapper.getInstance();
        ua = uaWrapper.getUa();
        try {
            user = ua.getUserManager().getCurrentUser();
        } catch (UaException e) {
            UaLog.error("Failed to retrieve user", e);
            Toast.makeText(context, "Failed to retrieve user.", Toast.LENGTH_SHORT).show();
        }
        gender = user.getGender();

        dialog = new AppCompatDialog(context, R.style.Base_Theme_AppCompat_Dialog);
        dialog.setContentView(content);
        dialog.setTitle("Heart Rate Zone");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        Spinner healthSpinner = (Spinner) content.findViewById(R.id.health_spinner);
        final String[] spinnerText = {"Poor", "Below Average", "Average", "Above Average", "Good", "Excellent", "Athlete"};
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(context, R.layout.custom_spinner_item, spinnerText);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        healthSpinner.setAdapter(spinnerArrayAdapter);
        healthSpinner.setSelection(2);

        final EditText editText = (EditText) content.findViewById(R.id.resting_heart_rate);

        healthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editText.setText(String.valueOf(findRestingHeartRate(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button submit = (Button) content.findViewById(R.id.submit_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText().toString().equals("")) {
                    Toast.makeText(context, "Must input a resting heart rate.", Toast.LENGTH_SHORT).show();
                } else {
                    listener.retrieveRestingHeartRate(Integer.valueOf(editText.getText().toString()));
                    dialog.dismiss();
                }
            }
        });

        return dialog;
    }


    public int findRestingHeartRate(int position) {
        int restingHeartRate;
        if (gender == Gender.MALE) {
            restingHeartRate = 84 - (5 * position);
        } else {
            restingHeartRate = 85 - (5 * position);
        }
        return restingHeartRate;
    }

    public interface Listener {
        void retrieveRestingHeartRate(int restingHeartRate);
    }
}
