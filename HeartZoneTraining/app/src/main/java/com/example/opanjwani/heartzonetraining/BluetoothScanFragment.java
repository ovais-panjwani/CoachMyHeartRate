package com.example.opanjwani.heartzonetraining;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ua.sdk.UaLog;
import com.ua.sdk.datasourceidentifier.DataSourceIdentifier;
import com.ua.sdk.recorder.Recorder;
import com.ua.sdk.recorder.RecorderManager;
import com.ua.sdk.recorder.data.BluetoothServiceType;

import java.util.ArrayList;
import java.util.List;

public class BluetoothScanFragment extends BaseFragment {

    private static final int MSG_DEVICE = 1;

    private ListView listView;
    private Listener listener;
    private MyAdapter adapter;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private MyLeScanCallbacks myLeScanCallbacks;
    private MyHandler myHandler;
    private RecorderManager recorderManager;

    @Override
    protected int getTitleId() {
        return R.string.title_scanner;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (Listener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        recorderManager = UaWrapper.getInstance().getUa().getRecorderManager();
        myLeScanCallbacks = new MyLeScanCallbacks();
        myHandler = new MyHandler();
        adapter = new MyAdapter();
        bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth_scan, container, false);

        listView = (ListView) view.findViewById(R.id.list_bluetooth_devices);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new MyOnItemClickListener());

        startScan();

        return view;
    }

    private void startScan() {
        bluetoothAdapter.startLeScan(myLeScanCallbacks);
    }

    @Override
    public void onPause() {
        super.onPause();
        bluetoothAdapter.stopLeScan(myLeScanCallbacks);
    }

    public class MyLeScanCallbacks implements BluetoothAdapter.LeScanCallback {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            UaLog.debug("found BLE device " + device.getAddress());

            myHandler.sendMessage(Message.obtain(myHandler, MSG_DEVICE, device));
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DEVICE:
                    adapter.addDevice((BluetoothDevice) msg.obj);
            }
        }
    }

    private class MyOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = (BluetoothDevice) parent.getAdapter().getItem(position);
            bluetoothAdapter.stopLeScan(myLeScanCallbacks);

            Recorder recorder = recorderManager.getRecorder(RecordFragment.SESSION_NAME);
            if (recorder != null) {
                DataSourceIdentifier dataSourceIdentifier = recorderManager.getDataSourceIdentifierBuilder()
                        .setName(SetUpFragment.DATA_SOURCE_HEART_RATE)
                        .setDevice(recorderManager.getDeviceBuilder().setName("Heart Rate").setManufacturer("none").setModel("none").build())
                        .build();
                recorder.addDataSource(recorderManager.createBluetoothDataSourceConfiguration().setDeviceAddress(device.getAddress()).setDataSourceIdentifier(dataSourceIdentifier).addProfileTypes(BluetoothServiceType.HEART_RATE));
            }

            listener.onBluetoothSelected();
        }
    }

    private class MyAdapter extends BaseAdapter {

        List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        public void addDevice(BluetoothDevice device) {
            if (!devices.contains(device)) {
                devices.add(device);
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
                Holder holder = new Holder();
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
                convertView.setTag(holder);
            }

            final Holder holder = (Holder) convertView.getTag();

            BluetoothDevice device = getItem(position);

            holder.text1.setText(device.getName());
            holder.text2.setText(device.getAddress());

            return convertView;
        }

        private class Holder {
            TextView text1;
            TextView text2;
        }
    }

    public interface Listener {
        void onBluetoothSelected();
    }
}
