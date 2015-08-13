package com.example.opanjwani.heartzonetraining;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;

public class SetUpDialog extends DialogFragment {

    private static final String DIALOG_TITLE = "Title";
    private static final String DIALOG_DESCRIPTION = "Description";
    private static final String DIALOG_MIN_VALUE = "Min Value";
    private static final String DIALOG_MAX_VALUE = "Max Value";
    private static final String DIALOG_NAG_VALUES = "Nag Values";
    private static final String DIALOG_FIRST_DESCRIPTION_TEXT = "First Description Text";
    private static final String DIALOG_SECOND_DESCRIPTION_TEXT = "Second Description Text";
    private static final String DIALOG_SECOND_PICKER_VISIBILITY = "Second Picker Visibility";

    private Listener listener;
    private Context context;
    private AppCompatDialog dialog;
    private NumberPicker firstPicker;
    private NumberPicker secondPicker;
    private TextView firstPickerText;
    private TextView secondPickerText;

    public SetUpDialog() {
    }

    public static SetUpDialog newInstance(String title, String description, int minValue, int maxValue, String[] values, String first, String second, int option) {
        SetUpDialog setUpDialog = new SetUpDialog();
        Bundle args = new Bundle();
        if (title != null) {
            args.putString(DIALOG_TITLE, title);
        }
        if (description != null) {
            args.putString(DIALOG_DESCRIPTION, description);
        }
        if (values != null) {
            args.putStringArray(DIALOG_NAG_VALUES, values);
        }
        if (first != null) {
            args.putString(DIALOG_FIRST_DESCRIPTION_TEXT, first);
        }
        if (second != null) {
            args.putString(DIALOG_SECOND_DESCRIPTION_TEXT, second);
        }
        args.putInt(DIALOG_SECOND_PICKER_VISIBILITY, option);
        args.putInt(DIALOG_MIN_VALUE, minValue);
        args.putInt(DIALOG_MAX_VALUE, maxValue);
        setUpDialog.setArguments(args);
        return setUpDialog;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        context = new ContextThemeWrapper(getActivity(), R.style.Base_Theme_AppCompat_Dialog);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View content = inflater.inflate(R.layout.dialog_number_picker, null);

        dialog = new AppCompatDialog(context, R.style.Base_Theme_AppCompat_Dialog);
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(content);

        TextView title = (TextView) content.findViewById(R.id.dialog_title);
        TextView description = (TextView) content.findViewById(R.id.dialog_description);
        firstPicker = (NumberPicker) content.findViewById(R.id.first_picker);
        secondPicker = (NumberPicker) content.findViewById(R.id.second_picker);
        firstPickerText = (TextView) content.findViewById(R.id.first_picker_text);
        secondPickerText = (TextView) content.findViewById(R.id.second_picker_text);
        TextView submitButton = (TextView) content.findViewById(R.id.submit_values_button);
        TextView cancelButton = (TextView) content.findViewById(R.id.cancel_values_button);

        if (getArguments().getString(DIALOG_TITLE) != null) {
            title.setText(getArguments().getString(DIALOG_TITLE));
        }
        if (getArguments().getString(DIALOG_DESCRIPTION) != null) {
            description.setText(getArguments().getString(DIALOG_DESCRIPTION));
        }
        if (getArguments().getInt(DIALOG_MIN_VALUE) == Integer.MAX_VALUE) {
            firstPicker.setMinValue(1);
        } else {
            firstPicker.setMinValue(getArguments().getInt(DIALOG_MIN_VALUE));
        }
        if (getArguments().getInt(DIALOG_MAX_VALUE) == Integer.MAX_VALUE) {
            firstPicker.setMaxValue(59);
        } else {
            firstPicker.setMaxValue(getArguments().getInt(DIALOG_MAX_VALUE));
        }
        if (getArguments().getStringArray(DIALOG_NAG_VALUES) == null) {
            secondPicker.setMinValue(0);
            secondPicker.setMaxValue(59);
        } else {
            secondPicker.setMinValue(0);
            secondPicker.setMaxValue(10);
            secondPicker.setDisplayedValues(getArguments().getStringArray(DIALOG_NAG_VALUES));
        }

        if (getArguments().getInt(DIALOG_SECOND_PICKER_VISIBILITY) != Integer.MAX_VALUE) {
            secondPicker.setVisibility(View.GONE);
            secondPickerText.setVisibility(View.GONE);
        }
        if (getArguments().getString(DIALOG_FIRST_DESCRIPTION_TEXT) != null) {
            firstPickerText.setText(getArguments().getString(DIALOG_FIRST_DESCRIPTION_TEXT));
        }
        if (getArguments().getString(DIALOG_SECOND_DESCRIPTION_TEXT) != null) {
            secondPickerText.setText(getArguments().getString(DIALOG_SECOND_DESCRIPTION_TEXT));
        }

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (secondPicker != null || secondPicker.getDisplayedValues() == null) {
                    listener.setValue(firstPicker.getValue(), secondPicker.getValue());
                } else {
                    listener.setValue(firstPicker.getValue(), 0);
                }
                dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });


        return dialog;
    }

    public interface Listener {
        void setValue(int valOne, int valTwo);
    }

}