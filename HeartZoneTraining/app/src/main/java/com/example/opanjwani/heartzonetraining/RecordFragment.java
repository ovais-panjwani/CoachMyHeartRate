package com.example.opanjwani.heartzonetraining;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.ua.sdk.Ua;
import com.ua.sdk.datapoint.DataFrame;
import com.ua.sdk.datapoint.DataTypeRef;
import com.ua.sdk.datasourceidentifier.DataSourceIdentifier;
import com.ua.sdk.recorder.Recorder;
import com.ua.sdk.recorder.RecorderManager;
import com.ua.sdk.recorder.RecorderObserver;
import com.ua.sdk.recorder.data.DataFrameObserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;

import static com.ua.sdk.datapoint.BaseDataTypes.TYPE_HEART_RATE;
import static com.ua.sdk.datapoint.BaseDataTypes.TYPE_HEART_RATE_SUMMARY;

public class RecordFragment extends BaseFragment implements IntervalManager.Listener {

    public static final String SESSION_NAME = "RecordSession";

    private Recorder recorder;
    private RecorderManager recorderManager;
    private Context context;
    private Ua ua;
    private MyDataFrameObserver dataFrameObserver;
    private MyRecorderObserver recorderObserver;
    private Button finishButton;
    private Button pauseButton;
    private Listener listener;
    private boolean started;
    private boolean afterPause;
    private IntervalManager intervalManager;
    private LineGraphSeries<DataPoint> dataPoints;
    private GraphView graph;
    private TextView activityText;

    @Override
    protected int getTitleId() {
        return R.string.title_record_workout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (Listener) activity;
        context = activity.getApplicationContext();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_record, container, false);

        UaWrapper uaWrapper = UaWrapper.getInstance();
        ua = uaWrapper.getUa();
        recorderManager = ua.getRecorderManager();
        recorder = recorderManager.getRecorder(SetUpFragment.SESSION_NAME);
        intervalManager = IntervalManager.getInstance();
//        handler = new Handler();
        bindRecordSession();

        activityText = (TextView) view.findViewById(R.id.activity_text);
        graph = (GraphView) view.findViewById(R.id.graph);
        dataPoints = new LineGraphSeries<>();
        dataPoints.setColor(getResources().getColor(R.color.primaryColor));
        graph.addSeries(dataPoints);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(30);
        graph.getViewport().setMaxY(200);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(intervalManager.getPrepTime() + (intervalManager.getNumReps()
                * (intervalManager.getWorkTime() + intervalManager.getRestTime())));

        finishButton = (Button) view.findViewById(R.id.finish_button);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recorderManager.getRecorder(SESSION_NAME) != null) {
                    finishButton.setEnabled(false);
                    pauseButton.setEnabled(false);
                    recorder.destroy();
                    dataPoints = new LineGraphSeries<>();
                    dataPoints.setColor(getResources().getColor(R.color.primaryColor));
                    graph.removeAllSeries();
                    graph.addSeries(dataPoints);
                    graph.getViewport().setYAxisBoundsManual(true);
                    graph.getViewport().setMinY(30);
                    graph.getViewport().setMaxY(200);
                    context.stopService(new Intent(context, RecorderService.class));
                    listener.onFinishWorkout();
                }
            }
        });
        pauseButton = (Button) view.findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recorder.getDataFrame().isSegmentStarted()) {
                    recorder.stopSegment();
                    context.stopService(new Intent(context, RecorderService.class));
                    pauseButton.setText(getString(R.string.start_text));
                    finishButton.setEnabled(false);
                } else {
                    recorder.startSegment();
                    context.startService(new Intent(context, RecorderService.class));
                    pauseButton.setText(getString(R.string.pause_text));
                    finishButton.setEnabled(true);
                }
            }
        });
        afterPause = false;
        recorder.startSegment();
        context.startService(new Intent(context, RecorderService.class));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (recorderManager.getRecorder(SESSION_NAME) != null && recorder != null && afterPause) {
            if (recorder.getDataFrame().isSegmentStarted()) {
                activityText.setText(intervalManager.getState());
                dataPoints = new LineGraphSeries<>();
                dataPoints.setColor(getResources().getColor(R.color.primaryColor));
                graph.removeAllSeries();
                graph.addSeries(dataPoints);
                for(DataFrame dataFrame: recorder.getAllDataFrames()) {
                    if (!Double.isNaN(dataFrame.getActiveTime()) && !Double.isNaN(dataFrame.getHeartRateDataPoint().getHeartRate()))
                        dataPoints.appendData(new DataPoint(dataFrame.getActiveTime(), dataFrame.getHeartRateDataPoint().getHeartRate()),
                                true, 10000);
                }
                dataFrameObserver = new MyDataFrameObserver();
                recorder.addDataFrameObserver(dataFrameObserver, TYPE_HEART_RATE.getRef(), TYPE_HEART_RATE_SUMMARY.getRef());
                afterPause = false;
            }
        }
        intervalManager.addListener(this);

        if (recorderManager.getRecorder(SESSION_NAME) == null && recorder == null) {
            listener.onFinishWorkout();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        afterPause = true;
        intervalManager.removeListener(this);
        recorder.removeDataFrameObserver(dataFrameObserver);
    }

    @Override
    public void onIntervalFinished() {
        if (recorder != null) {
            View view = (View) graph;
            Bitmap image = getBitmapFromView(view);
            File filedir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            filedir.mkdirs();
            String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
            File file = new File(filedir, "Workout_Graph_" + timeStamp + ".jpg");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finishButton.setEnabled(false);
            pauseButton.setEnabled(false);
            recorder.destroy();
            dataPoints = new LineGraphSeries<>();
            graph.removeAllSeries();
            graph.addSeries(dataPoints);
            graph.getViewport().setYAxisBoundsManual(true);
            graph.getViewport().setMinY(30);
            graph.getViewport().setMaxY(200);
            context.stopService(new Intent(context, RecorderService.class));
            listener.onFinishWorkout();
        }
    }

    @Override
    public void onStateChanged(String state) {
        if (activityText != null) {
            activityText.setText(state);
        }
    }

    private void bindRecordSession() {
        dataFrameObserver = new MyDataFrameObserver();

        recorder.addDataFrameObserver(dataFrameObserver, TYPE_HEART_RATE.getRef(), TYPE_HEART_RATE_SUMMARY.getRef());
        Log.d("####", "got called yo" + dataFrameObserver.toString());

        recorderObserver = new MyRecorderObserver();
        recorder.addRecorderObserver(recorderObserver);
    }

    private class MyDataFrameObserver implements DataFrameObserver {
        @Override
        public void onDataFrameUpdated(DataSourceIdentifier dataSourceIdentifier, DataTypeRef dataTypeRef, DataFrame dataFrame) {
            double data = dataFrame.getHeartRateDataPoint().getHeartRate();
            if (dataFrame.isSegmentStarted()) {
                    dataPoints.appendData(new DataPoint(dataFrame.getActiveTime(), data), true, 10000);
            }
        }
    }

    private class MyRecorderObserver implements RecorderObserver {

        @Override
        public void onSegmentStateUpdated(DataFrame dataFrame) {
            started = dataFrame.isSegmentStarted();
        }

        @Override
        public void onTimeUpdated(double activeTime, double elapsedTime) {
            Log.d("######", "activeTime=" + activeTime + " elapsedTime=" + elapsedTime);
            if (started && !Double.isNaN(activeTime)) {
                intervalManager.onUpdate(activeTime);
            }
        }
    }

    public static Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable =view.getBackground();
        if (bgDrawable!=null)
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        else
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE);
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    public interface Listener {
        void onFinishWorkout();
    }
}
