package com.example.opanjwani.heartzonetraining;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ua.sdk.Ua;
import com.ua.sdk.UaException;
import com.ua.sdk.UaLog;
import com.ua.sdk.activitytype.ActivityTypeRef;
import com.ua.sdk.datapoint.DataFrame;
import com.ua.sdk.datapoint.DataTypeRef;
import com.ua.sdk.datasourceidentifier.DataSourceIdentifier;
import com.ua.sdk.recorder.DerivedDataSourceConfiguration;
import com.ua.sdk.recorder.Recorder;
import com.ua.sdk.recorder.RecorderConfiguration;
import com.ua.sdk.recorder.RecorderManager;
import com.ua.sdk.recorder.RecorderObserver;
import com.ua.sdk.recorder.data.DataFrameObserver;
import com.ua.sdk.user.User;
import com.ua.sdk.user.UserManager;

import java.util.concurrent.RecursiveTask;

import static com.ua.sdk.datapoint.BaseDataTypes.TYPE_HEART_RATE;

public class RecordFragment extends BaseFragment {

    public static final String SESSION_NAME = "RecordSession";
    public static final String DATA_SOURCE_HEART_RATE = "heart_rate_data_source";

    private Recorder recorder;
    private RecorderManager recorderManager;
    private DataSourceIdentifier heartRateDataSourceIdentifier;
    private DataSourceIdentifier heartRateSummaryDataSourceIdentifier;
    private Context context;
    private Ua ua;
    private UserManager userManager;
    private MyDataFrameObserver dataFrameObserver;
    private MyRecorderObserver recorderObserver;
    private TextView heartRateValue;
    private TextView activityText;
    private EditText prep;
    private EditText work;
    private EditText rest;
    private EditText reps;
    private Button startButton;
    private Button finishButton;
    private Listener listener;
    private boolean started;
    private IntervalManager intervalManager;

    @Override
    protected int getTitleId() {
        return R.string.record_workout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (Listener) activity;
        context = activity.getApplicationContext();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_record, container, false);

        UaWrapper uaWrapper = UaWrapper.getInstance();
        ua = uaWrapper.getUa();
        recorderManager = ua.getRecorderManager();
        userManager = ua.getUserManager();
        intervalManager = IntervalManager.getInstance();

        heartRateDataSourceIdentifier = recorderManager.getDataSourceIdentifierBuilder().setName(DATA_SOURCE_HEART_RATE)
                .setDevice(recorderManager.getDeviceBuilder().setName("Heart Rate").setManufacturer("none").setModel("none").build())
                .build();
        heartRateSummaryDataSourceIdentifier = recorderManager.getDataSourceIdentifierBuilder().setName("heart_rate_summary_derived")
                .setDevice(recorderManager.getDeviceBuilder().setName("Heart Rate Summary").setManufacturer("none").setModel("none").build())
                .build();

        TextView heartRateScanner = (TextView) view.findViewById(R.id.heart_rate_scanner);
        heartRateScanner.setOnClickListener(new MyScannerOnClickListener());
        activityText = (TextView) view.findViewById(R.id.activity_text);
        prep = (EditText) view.findViewById(R.id.prep_time);
        work = (EditText) view.findViewById(R.id.work_time);
        rest = (EditText) view.findViewById(R.id.rest_time);
        reps = (EditText) view.findViewById(R.id.num_of_reps);
        heartRateValue = (TextView) view.findViewById(R.id.high);
        startButton = (Button)view.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onActionButton();
            }
        });
        finishButton = (Button)view.findViewById(R.id.finish_button);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.destroy();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        User user;

        try {
            user = userManager.getCurrentUser();
        } catch (UaException e) {
            Log.e("RecordFragment", "Error fetching current user", e);
            throw new NullPointerException("No User Bail Out");
        }

        recorder = recorderManager.getRecorder(SESSION_NAME);
        if (recorder == null) {
            final RecorderConfiguration config = recorderManager.createRecorderConfiguration();
            config.setName(SESSION_NAME);
            config.setUserRef(user.getRef());
            config.setActivityTypeRef(ActivityTypeRef.getBuilder().setActivityTypeId("16").build());

            config.addDataSource(recorderManager.createDerivedDataSourceConfiguration()
                    .setDataSource(DerivedDataSourceConfiguration.DataSourceType.HEART_RATE_SUMMARY)
                    .setDataSourceIdentifier(heartRateSummaryDataSourceIdentifier)
                    .setPriority(0));

            recorderManager.createRecorder(config, new RecorderManager.CreateCallback() {
                @Override
                public void onCreated(Recorder newRecorder, UaException ex) {
                    if (ex == null) {
                        recorder = newRecorder;
                        bindRecordSession();
                    } else {
                        UaLog.error("Failed to initialize recording.", ex);
                        Toast.makeText(context, "Failed to initialize recording.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            bindRecordSession();
        }
    }

    private class MyScannerOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            listener.onBleScan();
        }
    }

    private void bindRecordSession() {
        dataFrameObserver = new MyDataFrameObserver();

        recorder.addDataFrameObserver(dataFrameObserver, TYPE_HEART_RATE.getRef());

        recorderObserver = new MyRecorderObserver();
        recorder.addRecorderObserver(recorderObserver);
    }

    public class MyDataFrameObserver implements DataFrameObserver {
        @Override
        public void onDataFrameUpdated(DataSourceIdentifier dataSourceIdentifier, DataTypeRef dataTypeRef, DataFrame dataFrame) {
            if (dataSourceIdentifier.equals(heartRateDataSourceIdentifier)) {
                heartRateValue.setText(formatHeartRate(dataFrame.getHeartRateDataPoint(heartRateDataSourceIdentifier).getHeartRate()));
            }
        }
    }

    private String formatHeartRate(double heartRate) {
        if (Double.isNaN(heartRate)) {
            return "--- Bpm";
        } else {
            return String.format("%.0f Bpm", heartRate);
        }
    }

    private String formatTime(double duration) {
        int durationSeconds = (int)duration;
        int seconds = durationSeconds % 60;
        int minutes = (durationSeconds / 60) % 60;
        int hours = (durationSeconds / 3600) % 24;

        String secondsStr = String.format("%02d", seconds);
        String minutesStr = String.format("%02d", minutes);
        String hoursStr = String.format("%02d", hours);
        return String.format("%1$s:%2$s:%3$s", hoursStr, minutesStr, secondsStr);
    }

    private class MyRecorderObserver implements RecorderObserver {

        @Override
        public void onSegmentStateUpdated(DataFrame dataFrame) {
           // updateActionButton(dataFrame);
            started = dataFrame.isSegmentStarted();
        }

        @Override
        public void onTimeUpdated(double activeTime, double elapsedTime) {
            //timeValue.setText(formatTime(activeTime));
            if (started) {
//                int currentRepTime = (Integer.valueOf(reps.getText().toString()) - numReps) * cycle;
//                Log.d("######## currentRepTime",String.valueOf(currentRepTime));
//                Log.d("############### numReps",String.valueOf(numReps));
//                Log.d("########### elapsedTime",String.valueOf(elapsedTime));
//                Log.d("############## prepTime",String.valueOf(prepTime));
//                Log.d("############## workTime",String.valueOf(workTime));
//                Log.d("############## restTime",String.valueOf(restTime));
//                if ((prepTime + currentRepTime) >= elapsedTime) {
//                    activityText.setText("PREP");
//                } else if ((workTime + prepTime + currentRepTime) >= elapsedTime && elapsedTime >= (prepTime + currentRepTime)) {
//                    activityText.setText("WORK");
//                } else if ((restTime + workTime + prepTime + currentRepTime) >= elapsedTime && elapsedTime >= (workTime + prepTime + currentRepTime)) {
//                    activityText.setText("REST");
//                }
//
//                if (numReps > 0 && elapsedTime >= (restTime + workTime + prepTime + currentRepTime)) {
//                    numReps--;
//                    if (numReps == 0) {
//                        recorder.stopSegment();
//                        started = false;
//                        Log.d("###### recorderFinished",String.valueOf(started));
//                    }
//                }
            }
        }
    }

    private void onActionButton() {
        if (recorder.getDataFrame().isSegmentStarted()) {
            recorder.stopSegment();
            started = false;
        } else {
            started = true;
            intervalManager.init(Integer.valueOf(prep.getText().toString()), Integer.valueOf(work.getText().toString()),
                                Integer.valueOf(rest.getText().toString()), Integer.valueOf(reps.getText().toString()));
            recorder.startSegment();
        }

    }

    public interface Listener {
        void onBleScan();
    }
}
