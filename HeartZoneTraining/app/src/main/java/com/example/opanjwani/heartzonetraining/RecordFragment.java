package com.example.opanjwani.heartzonetraining;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ua.sdk.LocalDate;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.ua.sdk.datapoint.BaseDataTypes.TYPE_HEART_RATE;

public class RecordFragment extends BaseFragment implements IntervalManager.Listener, HeartRateZoneDialog.Listener {

    public static final String SESSION_NAME = "RecordSession";
    public static final String DATA_SOURCE_HEART_RATE = "heart_rate_data_source";
    public static final String FIRST_RUN_DID_NOT_HAPPEN = "FirstRunDidNotHappen";
    public static final String NOT_FIRST_RUN_PREF_NAME = "NotFirstRun";
    public static final String REST_HEART_RATE_PREF_NAME = "RestingHeartRate";
    public static final String REST_HEART_RATE_VALUE = "RestingHeartRateValue";

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
    private boolean notFirstRun;
    private int age;
    private MyCheckFirstRunTask checkFirstRunTask;
    private IntervalManager intervalManager;
    private int restingHeartRate;
    private ArrayList<HeartRateZone> heartRateZones;
    private RecorderConfiguration config;

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

        checkFirstRun();
        UaWrapper uaWrapper = UaWrapper.getInstance();
        ua = uaWrapper.getUa();
        recorderManager = ua.getRecorderManager();
        userManager = ua.getUserManager();
        try {
            age = extractAge(userManager.getCurrentUser().getBirthdate());
        } catch (UaException e) {
            UaLog.error("User was not fetched in time.");
        }
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
        startButton = (Button) view.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onActionButton();
            }
        });
        finishButton = (Button) view.findViewById(R.id.finish_button);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.destroy();
                context.stopService(new Intent(context, RecorderService.class));
            }
        });

        if (!notFirstRun) {
            HeartRateZoneDialog heartRateZoneDialog = HeartRateZoneDialog.getInstance();
            heartRateZoneDialog.setListener(this);
            heartRateZoneDialog.show(getActivity().getFragmentManager(), "Heart Rate Zone Dialog");
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        User user;

        intervalManager.addListener(this);
        try {
            user = userManager.getCurrentUser();
        } catch (UaException e) {
            Log.e("RecordFragment", "Error fetching current user", e);
            throw new NullPointerException("No User Bail Out");
        }

        recorder = recorderManager.getRecorder(SESSION_NAME);
        if (recorder == null) {
            config = recorderManager.createRecorderConfiguration();
            config.setName(SESSION_NAME);
            config.setUserRef(user.getRef());
            config.setActivityTypeRef(ActivityTypeRef.getBuilder().setActivityTypeId("16").build());

            config.addDataSource(recorderManager.createDerivedDataSourceConfiguration()
                    .setDataSource(DerivedDataSourceConfiguration.DataSourceType.HEART_RATE_SUMMARY)
                    .setDataSourceIdentifier(heartRateSummaryDataSourceIdentifier)
                    .setPriority(0));
            recorderManager.createRecorder(config, new createRecorderCallback());
        } else {
            bindRecordSession();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        intervalManager.removeListener(this);
    }

    private class createRecorderCallback implements RecorderManager.CreateCallback {
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
    }

    @Override
    public void onIntervalFinished() {
        recorder.destroy();
        context.stopService(new Intent(context, RecorderService.class));
    }

    @Override
    public void onStateChanged(String state) {
        if (activityText != null) {
            activityText.setText(state);
        }

        String data = state + "\n";
    }

    @Override
    public void retrieveRestingHeartRate(int restingHeartRate) {
        this.restingHeartRate = getSharedPreferences(REST_HEART_RATE_PREF_NAME).getInt(REST_HEART_RATE_VALUE, restingHeartRate);
        SharedPreferences.Editor editor = getSharedPreferences(REST_HEART_RATE_PREF_NAME).edit();
        editor.putInt(REST_HEART_RATE_VALUE, restingHeartRate);
        editor.apply();
        getHeartRateZones();
        editor = getSharedPreferences(NOT_FIRST_RUN_PREF_NAME).edit();
        editor.putBoolean(FIRST_RUN_DID_NOT_HAPPEN, true);
        editor.apply();
    }

    private SharedPreferences getSharedPreferences(String key) {
        return context.getSharedPreferences(key, Context.MODE_PRIVATE);
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

        if (notFirstRun) {
            restingHeartRate = getSharedPreferences(REST_HEART_RATE_PREF_NAME).getInt(REST_HEART_RATE_VALUE, 0);
            getHeartRateZones();
        }
    }

    private void checkFirstRun() {
        if (checkFirstRunTask != null) {
            checkFirstRunTask.cancel(false);
            checkFirstRunTask = null;
        }
        checkFirstRunTask = new MyCheckFirstRunTask();
        checkFirstRunTask.execute();
    }

    private class MyCheckFirstRunTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            notFirstRun = getSharedPreferences(NOT_FIRST_RUN_PREF_NAME).getBoolean(FIRST_RUN_DID_NOT_HAPPEN, false);
            return null;
        }
    }

    public class MyDataFrameObserver implements DataFrameObserver {
        @Override
        public void onDataFrameUpdated(DataSourceIdentifier dataSourceIdentifier, DataTypeRef dataTypeRef, DataFrame dataFrame) {
            Log.d("###### frag", formatHeartRate(dataFrame.getHeartRateDataPoint().getHeartRate()));
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
        int durationSeconds = (int) duration;
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

            Log.d("######", "actiivetime=" + activeTime + " elapsedTime=" + elapsedTime);
            if (started && !Double.isNaN(activeTime)) {
                intervalManager.onUpdate(activeTime);
            }
        }
    }

    private void onActionButton() {
        if (recorderManager.getRecorder(SESSION_NAME) == null) {
            recorderManager.createRecorder(config, new createRecorderCallback());
            bindRecordSession();
        }
        if (recorder.getDataFrame().isSegmentStarted()) {
            recorder.stopSegment();
            context.stopService(new Intent(context, RecorderService.class));
        } else {
            intervalManager.init(Integer.valueOf(prep.getText().toString()), Integer.valueOf(work.getText().toString()),
                    Integer.valueOf(rest.getText().toString()), Integer.valueOf(reps.getText().toString()));
            recorder.startSegment();
            context.startService(new Intent(context, RecorderService.class));
        }
    }

    private void getHeartRateZones() {
        heartRateZones = new ArrayList<>();
        int maxHR = (int) (207 - (0.7 * age));
        int reserveHR = maxHR - restingHeartRate;
        for (int i = 1; i <= 5; i++) {
            HeartRateZone heartRateZone = new HeartRateZone(i, (int) (maxHR - (((6 - i) * 0.1) * reserveHR)),
                    (int) (maxHR - (((5 - i) * 0.1) * reserveHR)));
            Log.d("######### " + heartRateZone.getName(), "" + heartRateZone.getStart() + " " + heartRateZone.getEnd());
            heartRateZones.add(heartRateZone);
        }
    }

    private static int extractAge(LocalDate userBirthdate) {
        Calendar a = new GregorianCalendar(Locale.US);
        a.set(Calendar.YEAR, userBirthdate.getYear());
        a.set(Calendar.MONTH, userBirthdate.getMonth());
        a.set(Calendar.DAY_OF_MONTH, userBirthdate.getDayOfMonth());
        Calendar b = getCalendar(new Date());
        int diff = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
        if (a.get(Calendar.MONTH) > b.get(Calendar.MONTH) ||
                (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) && a.get(Calendar.DATE) > b.get(Calendar.DATE))) {
            diff--;
        }
        return diff;
    }

    private static Calendar getCalendar(Date date) {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(date);
        return cal;
    }

    public interface Listener {
        void onBleScan();
    }
}
