package com.sparohealth.wingkit_sample;

import android.Manifest;
import android.app.ListActivity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sparohealth.wingkit.classes.AmbientNoiseMonitor;
import com.sparohealth.wingkit.classes.ReachabilityMonitor;
import com.sparohealth.wingkit.classes.SensorMonitor;
import com.sparohealth.wingkit_sample.adapters.PretestChecksAdapter;

import java.util.ArrayList;

public class PretestChecksActivity extends ListActivity implements SensorMonitor.SensorMonitorDelegate, ReachabilityMonitor.ReachabilityMonitorDelegate, AmbientNoiseMonitor.AmbientNoiseMonitorDelegate {
    private ArrayList<ChecklistItem> itemsArray = new ArrayList<>();
    private PretestChecksAdapter adapter;

    private App application;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private Button btnStartTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_pretest_checks);

            application = (App)getApplication();

            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }catch (Exception ex){
            Log.d("", ex.getMessage());
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(application.reachabilityMonitor);
        unregisterReceiver(application.sensorMonitor);
        application.ambientNoiseMonitor.stop();
        application.ambientNoiseMonitor = null;
        adapter = null;
    }
    private void createItemsArray(){
        //link this with live data
        itemsArray.add(new ChecklistItem("Internet Connection", application.reachabilityMonitor.isConnected));
        itemsArray.add(new ChecklistItem("Quiet Environment", application.ambientNoiseMonitor.isBelowThreshold));
        itemsArray.add(new ChecklistItem("Sensor Connection", application.sensorMonitor.verifySensorIsAvailable()));
    }

    @Override
    public void sensorStateDidChange(SensorMonitor monitor) {
        if (itemsArray.size() > 0){
            ChecklistItem wingSensor = itemsArray.get(2);

            //when sensor changes, update the checklist item for sensor connection
            wingSensor.setChecked(monitor.verifySensorIsAvailable());

            //tell the table to refresh its data
            if (adapter != null)
                adapter.notifyDataSetChanged();

            //enable/disable the start test button and change the color based on sensor status
            if (!monitor.verifySensorIsAvailable()){
                Toast.makeText(this,"WING sensor unplugged.",Toast.LENGTH_LONG).show();
                btnStartTest.setEnabled(false);
                btnStartTest.setTextColor(Color.GRAY);

            }else if (!application.ambientNoiseMonitor.isBelowThreshold || !application.reachabilityMonitor.isConnected) {
                btnStartTest.setEnabled(false);
                btnStartTest.setTextColor(Color.GRAY);
            }else{
                btnStartTest.setTextColor(Color.parseColor("#0099cc"));
                btnStartTest.setEnabled(true);
            }
        }
    }

    @Override
    public void ambientNoiseMonitorDidChangeState(AmbientNoiseMonitor monitor) {
        if (itemsArray.size()>0){
            try {
                //this method executes when the noise level is already beyond the threshold
                ChecklistItem noiseSensor = itemsArray.get(1);

                //update the list item for Quiet environment
                noiseSensor.setChecked(monitor.isBelowThreshold);

                Handler mainHandler = new Handler(this.getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        //tell the table to refresh its data
                        if (adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                };
                mainHandler.post(myRunnable);

                //enable/disable the start test button and change the color based on sensor status
                if (!monitor.isBelowThreshold){
                    Toast.makeText(this,"Ambient Noise error, please find a quiet environment ",Toast.LENGTH_LONG).show();
                    btnStartTest.setEnabled(false);
                    btnStartTest.setTextColor(Color.GRAY);

                }else if (!application.sensorMonitor.verifySensorIsAvailable() || !application.reachabilityMonitor.isConnected) {
                    btnStartTest.setEnabled(false);
                    btnStartTest.setTextColor(Color.GRAY);
                }else{
                    btnStartTest.setTextColor(Color.parseColor("#0099cc"));
                    btnStartTest.setEnabled(true);
                }

            }catch (Exception ex){
                Log.d("Debug", ex.getMessage());
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            switch (requestCode){
                case REQUEST_RECORD_AUDIO_PERMISSION:
                    permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    break;
            }

            if (permissionToRecordAccepted){
                if (application.reachabilityMonitor == null) {
                    application.reachabilityMonitor = new ReachabilityMonitor(this);
                }
                application.reachabilityMonitor.setDelegate(this);

                if (application.sensorMonitor == null) {
                    application.sensorMonitor = new SensorMonitor(this);
                }
                else {
                    application.sensorMonitor.setDelegate(this);
                }

                application.ambientNoiseMonitor = new AmbientNoiseMonitor(this);

                registerReceiver(application.reachabilityMonitor, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                registerReceiver(application.sensorMonitor,new IntentFilter(Intent.ACTION_HEADSET_PLUG));

                if (!application.ambientNoiseMonitor.isActive)
                    application.ambientNoiseMonitor.start(this);


                createItemsArray();

                btnStartTest = findViewById(R.id.start);
                btnStartTest.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (application.reachabilityMonitor.isConnected && application.ambientNoiseMonitor.isBelowThreshold && application.sensorMonitor.verifySensorIsAvailable()) {
                            application.ambientNoiseMonitor.stop();
                            double baseline = application.ambientNoiseMonitor.baselineAmplitude();
                            application.ambientNoiseMonitor = null;
                            Intent i = new Intent(getApplicationContext(), TestScreenActivity.class);
                            i.putExtra("baseline", baseline);
                            startActivity(i);
                        }
                    }
                });

                adapter = new PretestChecksAdapter(getListView().getContext(),itemsArray);
                ListView itemsListView  = getListView();

                itemsListView.setAdapter(adapter);
            }else {
                setContentView(R.layout.activity_pretest_checks);

                TextView txtLbl = findViewById(R.id.textView3);
                txtLbl.setText("Please enable audio recording in your Settings.");
                txtLbl.setGravity(Gravity.CENTER);
                Button btn= findViewById(R.id.start);
                btn.setVisibility(View.INVISIBLE);
            }
        }catch (Exception ex){

        }

        return;

    }

    @Override
    public void reachabilityMonitorDidChangeReachability(ReachabilityMonitor monitor) {
        // TODO: 10/27/2017

        if (itemsArray.size()>0){
            ChecklistItem internetSensor  = itemsArray.get(0);

            //update the list item for internet connection
            internetSensor.setChecked(monitor.isConnected);

            //tell the table to refresh its data
            if (adapter != null){
                adapter.notifyDataSetChanged();
            }

            //enable/disable the start test button and change the color based on sensor status
            if (!monitor.isConnected){
                Toast.makeText(this,"Internet Connection error", Toast.LENGTH_LONG).show();
                btnStartTest.setEnabled(false);
                btnStartTest.setTextColor(Color.GRAY);

            }else if (!application.sensorMonitor.verifySensorIsAvailable() || !application.ambientNoiseMonitor.isBelowThreshold) {
                btnStartTest.setEnabled(false);
                btnStartTest.setTextColor(Color.GRAY);
            }else {
                btnStartTest.setEnabled(true);
                btnStartTest.setTextColor(Color.parseColor("#0099cc"));
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(getApplicationContext(), DemographicsActivity.class);
        startActivity(i);
        finish();
    }
}
