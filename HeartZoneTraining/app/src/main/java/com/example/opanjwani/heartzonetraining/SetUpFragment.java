package com.example.opanjwani.heartzonetraining;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.ua.sdk.recorder.data.DataFrameObserver;
import com.ua.sdk.user.User;
import com.ua.sdk.user.UserManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.ua.sdk.datapoint.BaseDataTypes.TYPE_HEART_RATE;
import static com.ua.sdk.datapoint.BaseDataTypes.TYPE_HEART_RATE_SUMMARY;

public class SetUpFragment extends BaseFragment implements RestingHeartRateDialog.Listener{

    public static final String SESSION_NAME = "RecordSession";
    public static final String DATA_SOURCE_HEART_RATE = "heart_rate_data_source";
    public static final String FIRST_RUN_DID_NOT_HAPPEN = "FirstRunDidNotHappen";
    public static final String NOT_FIRST_RUN_PREF_NAME = "NotFirstRun";
    public static final String REST_HEART_RATE_PREF_NAME = "RestingHeartRate";
    public static final String REST_HEART_RATE_VALUE = "RestingHeartRateValue";

    private Listener listener;
    private Context context;
    private Ua ua;
    private UserManager userManager;
    private User user;
    private IntervalManager intervalManager;
    private HeartRateZoneManager heartRateZoneManager;
    private Recorder recorder;
    private RecorderManager recorderManager;
    private int age;
    private DataSourceIdentifier heartRateDataSourceIdentifier;
    private DataSourceIdentifier heartRateSummaryDataSourceIdentifier;
    private RestingHeartRateDialog restingHeartRateDialog;
    private MyCheckFirstRunTask checkFirstRunTask;
    private boolean notFirstRun;
    private int restingHeartRate;
    private ArrayList<HeartRateZone> heartRateZones;
    private String[] values;
    private MyDataFrameObserver dataFrameObserver;
    private Button scanner;
    private TextView connected;
    private Button startButton;
    private int warmUpTime = 300;
    private int highIntensityTime = 300;
    private int lowIntensityTime = 300;
    private int nagDuringTime = 300;
    private int nagBetweenTime = 300;

    @Override
    protected int getTitleId() {
        return R.string.title_set_up;
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
        final View view = inflater.inflate(R.layout.fragment_set_up, container, false);

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
        heartRateZoneManager = HeartRateZoneManager.getInstance();

        heartRateDataSourceIdentifier = recorderManager.getDataSourceIdentifierBuilder().setName(DATA_SOURCE_HEART_RATE)
                .setDevice(recorderManager.getDeviceBuilder().setName("Heart Rate").setManufacturer("none").setModel("none").build())
                .build();
        heartRateSummaryDataSourceIdentifier = recorderManager.getDataSourceIdentifierBuilder().setName("heart_rate_summary_derived")
                .setDevice(recorderManager.getDeviceBuilder().setName("Heart Rate Summary").setManufacturer("none").setModel("none").build())
                .build();

        scanner = (Button) view.findViewById(R.id.heart_rate_scanner);
        connected = (TextView) view.findViewById(R.id.connected_scanner);
        final TextView warmUp = (TextView) view.findViewById(R.id.warm_up);
        final TextView highIntensity = (TextView) view.findViewById(R.id.high_intensity);
        final TextView lowIntensity = (TextView) view.findViewById(R.id.low_intensity);
        final TextView numOfReps = (TextView) view.findViewById(R.id.num_of_reps);
        final TextView nagDuring = (TextView) view.findViewById(R.id.nagging_during);
        final TextView nagBetween = (TextView) view.findViewById(R.id.nagging_between);
        final TextView highIntensityHR = (TextView) view.findViewById(R.id.high_intensity_hr);
        final TextView lowIntensityHR = (TextView) view.findViewById(R.id.low_intensity_hr);
        startButton = (Button) view.findViewById(R.id.start_button);
        values = new String[]{"5", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55"};

        scanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onBleScan();
            }
        });
        warmUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetUpDialog setUpDialog = SetUpDialog.newInstance("Warm Up", getString(R.string.warm_up_interval_description), Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, null,
                        Integer.MAX_VALUE);
                setUpDialog.setListener(new SetUpDialog.Listener() {
                    @Override
                    public void setValue(int min, int sec) {
                        warmUp.setText(min + ":" + sec);
                        warmUpTime = (min * 60) + sec;
                    }
                });
                setUpDialog.show(getActivity().getFragmentManager(), "Warm Up Dialog");
            }
        });

        highIntensity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetUpDialog setUpDialog = SetUpDialog.newInstance("High Intensity Interval", getString(R.string.high_intensity_interval_description), Integer.MAX_VALUE, Integer.MAX_VALUE, null,
                        null, null, Integer.MAX_VALUE);
                setUpDialog.setListener(new SetUpDialog.Listener() {
                    @Override
                    public void setValue(int min, int sec) {
                        highIntensity.setText(min + ":" + sec);
                        highIntensityTime = (min * 60) + sec;
                    }
                });
                setUpDialog.show(getActivity().getFragmentManager(), "High Intensity Interval Dialog");
            }
        });

        lowIntensity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetUpDialog setUpDialog = SetUpDialog.newInstance("Low Intensity Interval", getString(R.string.low_intensity_interval_description), Integer.MAX_VALUE, Integer.MAX_VALUE, null,
                        null, null, Integer.MAX_VALUE);
                setUpDialog.setListener(new SetUpDialog.Listener() {
                    @Override
                    public void setValue(int min, int sec) {
                        lowIntensity.setText(min + ":" + sec);
                        lowIntensityTime = (min * 60) + sec;
                    }
                });
                setUpDialog.show(getActivity().getFragmentManager(), "Low Intensity Interval Dialog");
            }
        });

        numOfReps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetUpDialog setUpDialog = SetUpDialog.newInstance("Number of Reps", getString(R.string.num_of_reps_description), Integer.MAX_VALUE, 15, null, "Reps", null, View.GONE);
                setUpDialog.setListener(new SetUpDialog.Listener() {
                    @Override
                    public void setValue(int reps, int nullVal) {
                        numOfReps.setText("" + reps);
                    }
                });
                setUpDialog.show(getActivity().getFragmentManager(), "Number of Reps Dialog");
            }
        });

        nagDuring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetUpDialog setUpDialog = SetUpDialog.newInstance("Nag Time During Zone", getString(R.string.nagging_during_interval_description), 0, Integer.MAX_VALUE, values, null, null,
                        Integer.MAX_VALUE);
                setUpDialog.setListener(new SetUpDialog.Listener() {
                    @Override
                    public void setValue(int min, int sec) {
                        nagDuring.setText(min + ":" + values[sec]);
                        nagDuringTime = (min * 60) + Integer.parseInt(values[sec]);
                    }
                });
                setUpDialog.show(getActivity().getFragmentManager(), "Nag Time During Zone Dialog");
            }
        });

        nagBetween.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetUpDialog setUpDialog = SetUpDialog.newInstance("Nag Time Between Zones", getString(R.string.nagging_between_interval_description), 0, Integer.MAX_VALUE, values, null, null,
                        Integer.MAX_VALUE);
                setUpDialog.setListener(new SetUpDialog.Listener() {
                    @Override
                    public void setValue(int min, int sec) {
                        nagBetween.setText(min + ":" + values[sec]);
                        nagBetweenTime = (min * 60) + Integer.parseInt(values[sec]);
                    }
                });
                setUpDialog.show(getActivity().getFragmentManager(), "Nag Time Between Zones Dialog");
            }
        });

        highIntensityHR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetUpDialog setUpDialog = SetUpDialog.newInstance("High Intensity HR Zone", getString(R.string.high_intensity_hr_description), Integer.MAX_VALUE, 5, null, "HR Zone", null,
                        View.GONE);
                setUpDialog.setListener(new SetUpDialog.Listener() {
                    @Override
                    public void setValue(int zone, int nullVal) {
                        highIntensityHR.setText("" + zone);
                    }
                });
                setUpDialog.show(getActivity().getFragmentManager(), "High Intensity HR Zone Dialog");
            }
        });

        lowIntensityHR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetUpDialog setUpDialog = SetUpDialog.newInstance("Low Intensity HR Zone", getString(R.string.low_intensity_hr_description), Integer.MAX_VALUE, 5, null, "HR Zone", null,
                        View.GONE);
                setUpDialog.setListener(new SetUpDialog.Listener() {
                    @Override
                    public void setValue(int zone, int nullVal) {
                        lowIntensityHR.setText("" + zone);
                    }
                });
                setUpDialog.show(getActivity().getFragmentManager(), "Low Intensity HR Zone Dialog");
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (heartRateZones == null) {
                    getHeartRateZones();
                }
                if (!recorder.getDataFrame().isSegmentStarted()) {
                    intervalManager.init(warmUpTime, highIntensityTime,lowIntensityTime, Integer.valueOf(numOfReps.getText().toString()),
                            nagDuringTime, nagBetweenTime);
                    heartRateZoneManager.init(heartRateZones, heartRateZones.get(Integer.valueOf(lowIntensityHR.getText().toString()) - 1),
                            heartRateZones.get(Integer.valueOf(highIntensityHR.getText().toString()) - 1));
                    listener.onStartClicked();
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkFirstRun();
        restingHeartRateDialog = new RestingHeartRateDialog();
        restingHeartRateDialog.setListener(this);
        try {
            user = userManager.getCurrentUser();
        } catch (UaException e) {
            Log.e("RecordFragment", "Error fetching current user", e);
            throw new NullPointerException("No User Bail Out");
        }

        recorder = recorderManager.getRecorder(SESSION_NAME);
        if (recorder == null) {
            setUpRecorder();
        } else {
            bindRecordSession();
        }
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

    private class CreateRecorderCallback implements RecorderManager.CreateCallback {
        @Override
        public void onCreated(Recorder newRecorder, UaException ex) {
            if (ex == null) {
                recorder = newRecorder;
                bindRecordSession();
                startButton.setEnabled(true);
            } else {
                UaLog.error("Failed to initialize recording.", ex);
                Toast.makeText(context, "Failed to initialize recording.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setUpRecorder() {
        RecorderConfiguration config = recorderManager.createRecorderConfiguration();
        config.setName(SESSION_NAME);
        config.setUserRef(user.getRef());
        config.setActivityTypeRef(ActivityTypeRef.getBuilder().setActivityTypeId("16").build());

        config.addDataSource(recorderManager.createDerivedDataSourceConfiguration()
                .setDataSource(DerivedDataSourceConfiguration.DataSourceType.HEART_RATE_SUMMARY)
                .setDataSourceIdentifier(heartRateSummaryDataSourceIdentifier)
                .setPriority(0));
        recorderManager.createRecorder(config, new CreateRecorderCallback());
    }

    private void bindRecordSession() {
        dataFrameObserver = new MyDataFrameObserver();

        recorder.addDataFrameObserver(dataFrameObserver, TYPE_HEART_RATE.getRef(), TYPE_HEART_RATE_SUMMARY.getRef());
        Log.d("####", "got called yo" + dataFrameObserver.toString());

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

    private class MyCheckFirstRunTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            notFirstRun = getSharedPreferences(NOT_FIRST_RUN_PREF_NAME).getBoolean(FIRST_RUN_DID_NOT_HAPPEN, false);
            return notFirstRun;
        }

        @Override
        protected void onPostExecute(Boolean notFirstRun) {
            if (!notFirstRun) {
                restingHeartRateDialog.show(getActivity().getFragmentManager(), "Heart Rate Zone Dialog");
            }
        }
    }

    private class MyDataFrameObserver implements DataFrameObserver {
        @Override
        public void onDataFrameUpdated(DataSourceIdentifier dataSourceIdentifier, DataTypeRef dataTypeRef, DataFrame dataFrame) {
            double data = dataFrame.getHeartRateDataPoint().getHeartRate();
            Log.d("###### frag", formatHeartRate(dataFrame.getHeartRateDataPoint().getHeartRate()));
            if (!Double.isNaN(data)) {
                scanner.setVisibility(View.GONE);
                connected.setVisibility(View.VISIBLE);
                connected.setText("Connected: " + formatHeartRate(dataFrame.getHeartRateDataPoint().getHeartRate()));
            }
        }
    }

    private SharedPreferences getSharedPreferences(String key) {
        return context.getSharedPreferences(key, Context.MODE_PRIVATE);
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

    private String formatHeartRate(double heartRate) {
        if (Double.isNaN(heartRate)) {
            return "--- Bpm";
        } else {
            return String.format("%.0f Bpm", heartRate);
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
        void onStartClicked();
    }
}
