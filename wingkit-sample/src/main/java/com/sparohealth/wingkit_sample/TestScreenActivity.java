package com.sparohealth.wingkit_sample;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sparohealth.wingkit.classes.AmbientNoiseMonitor;
import com.sparohealth.wingkit.classes.Client;
import com.sparohealth.wingkit.classes.ReachabilityMonitor;
import com.sparohealth.wingkit.classes.SensorMonitor;
import com.sparohealth.wingkit.classes.TestSession;
import com.sparohealth.wingkit.classes.lungfunctiontest.TestSessionManager;
import com.sparohealth.wingkit.classes.lungfunctiontest.TestSessionRecorder;
import com.sparohealth.wingkit_sample.shapes.CircleAnimation;
import com.sparohealth.wingkit_sample.shapes.CircleView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;

public class TestScreenActivity extends AppCompatActivity implements ReachabilityMonitor.ReachabilityMonitorDelegate, SensorMonitor.SensorMonitorDelegate,
        AmbientNoiseMonitor.AmbientNoiseMonitorDelegate, TestSessionManager.TestSessionManagerDelegate {
    private String TAG = "TestScreen";

    private App application;

    private ProgressDialog progressDialog;
    private Button startTest;
    private Button cancelTest;
    private TextView testMessage;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    TestSessionRecorder recorder;
    //    TestSession testSession = null;
    TestSessionManager sessionManager = null;

    CircleView circle;
    double currentStrength;
    AlertDialog alertDialog;
    boolean testCancelled = false;
    boolean testStarted = false;
    private TestScreenActivity activity = this;
    private String currentError = "";
    private double baselineAmplitude = 0;
    int baseRadius = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_test_screen);

        application = (App)getApplication();
        sessionManager = new TestSessionManager(application.client);
        sessionManager.setDelegate(this);
        baselineAmplitude = getIntent().getDoubleExtra("baseline", 0);

        recorder = new TestSessionRecorder(this, new TestSessionRecorder.TestRecorderDelegate() {
            @Override
            public void recorderStateChanged(TestSessionRecorder.TestRecorderState state){
                switch (state){
                    //we only care about recording or finished states at this point
                    case finished:
                        try {
                            Log.d(TAG, "recorderStateChanged - finished");
                            //check that the signal strength threshold has not passed
                            if (!recorder.isValidRecording() && !testCancelled){
                                Log.d(TAG, "recorderStateChanged - exceeded signal strength threshold");
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AlertDialog alert = new AlertDialog.Builder(activity).create();
                                        alert.setTitle("Bad Recording");
                                        alert.setMessage("We weren't able to get a good recording that time. Let's try that again.");
                                        alert.setButton(AlertDialog.BUTTON_NEUTRAL, "Try Again", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                startTest.setVisibility(View.VISIBLE);
                                                dialogInterface.dismiss();
                                            }
                                        });
                                        alert.show();
                                    }
                                });
                            }
                            // if the recording WAS cancelled during the test...
                            else if (recorder.isCancelled() || testCancelled) {
                                // do nothing
                            }
                            else {
                                uploadRecording();
                            }
                        }catch (JSONException je){
                            je.printStackTrace();
                        }
                        break;
                    case recording:
                        // TODO: 11/6/2017
                        Log.d("recorderStateChanged", "recording");
                        //signalStrengthChanged(recorder.getCurrentStrength());
                        break;
                }
            }

            @Override
            public void signalStrengthChanged(final Double newStrength) {
                int scale = (int)(0 - baselineAmplitude);

                if (Math.abs(baselineAmplitude) > Math.abs(newStrength) && newStrength != 0) {
                    int diff = (int)Math.abs(newStrength - baselineAmplitude);
                    double percentage = ((double)diff / (double)scale);

                    Log.d(TAG, "Calculated percentage growth of " + String.valueOf(percentage * 100));

                    final int newRadius = (int)(baseRadius + (baseRadius * percentage * 3));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CircleAnimation animation;
                            //update the circle based on strength
                            animation = new CircleAnimation(circle, newRadius);

                            animation.setDuration(100);
                            circle.startAnimation(animation);
                        }
                    });
                }
                else {
                    Log.d(TAG, "Resetting radius to base value " + String.valueOf(baseRadius));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CircleAnimation animation;
                            //update the circle based on strength
                            animation = new CircleAnimation(circle, baseRadius);

                            animation.setDuration(100);
                            circle.startAnimation(animation);
                        }
                    });

                }
            }
        });
        circle = new CircleView(this);
        baseRadius = circle.getRadius();

        initMonitors();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(circle,params);

        //request permission to use microphone
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        cancelTest = (Button)findViewById(R.id.cancel);
        cancelTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //start test button click handler
        startTest = (Button)findViewById(R.id.start);

        startTest.setVisibility(View.INVISIBLE);

        startTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Start test clicked");

                startTest.setVisibility(View.INVISIBLE);

                testMessage = (TextView)findViewById(R.id.testMessage);

                ValueAnimator valueAnimator = ValueAnimator.ofFloat(1f, 0f);
                valueAnimator.setDuration(500);
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float alpha = (float) animation.getAnimatedValue();
                        testMessage.setAlpha(alpha);
                    }
                });

                valueAnimator.start();

                Log.d(TAG, "Beginning to record the test");
                testStarted = true;
                testCancelled = false;
                recorder.startRecording();
            }
        });

        setupSession();
    }

    private void setupSession() {
        Log.d(TAG, "Setting up the test session");

        try {
            createTestSession();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initMonitors() {
        try{
            application.reachabilityMonitor.setDelegate(this);
            application.sensorMonitor.setDelegate(this);

            registerReceiver(application.reachabilityMonitor, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            registerReceiver(application.sensorMonitor, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        }catch (Exception ex){

        }

    }

    private void createTestSession() throws JSONException {
        Log.d(TAG, "Calling Client.createTestSession");
        application.client.createTestSession(application.patientData, "", 37.7858, -122.406, null, null, null, new Client.WingApiCallback() {
            @Override
            public void onSuccessResponse(JSONObject result) {
                try {
                    Log.d(TAG, "Client.createTestSession recieved - " + result.toString());

                    Gson gson = new Gson();
                    sessionManager.testSession = gson.fromJson(result.toString(),TestSession.class);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startTest.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Client.createTestSession error - " + Arrays.toString(e.getStackTrace()));
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "An error occurred creating the Test Session!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onErrorResponse(Exception error) {
                Log.e(TAG, "Client.createTestSession error - " + error.getMessage() + "\n" + Arrays.toString(error.getStackTrace()));

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "An error occurred creating the Test Session!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void uploadRecording() throws JSONException {
        Log.d(TAG, "Beginning the upload process");

        if (testCancelled && testStarted) {
            testCancelled = false;
            testStarted = false;
        }
        else {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    if (progressDialog != null) {
                        progressDialog.setTitle("Please wait");
                        progressDialog.setMessage("Uploading...");
                    } else {
                        progressDialog = new ProgressDialog(activity);
                        progressDialog.setCancelable(false);
                        progressDialog.setIndeterminate(true);
                        progressDialog.setTitle("Please wait");
                        progressDialog.setMessage("Uploading...");
                    }
                    if (!progressDialog.isShowing()) {
                        progressDialog.show();
                    }
                }
            });

            sessionManager.uploadRecording(recorder.getFilename());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    @Override
    public void sensorStateDidChange(SensorMonitor monitor) {
        //enable/disable the start test button and change the color based on sensor status
        if (!monitor.verifySensorIsAvailable()){
            currentError = "sensor";
            if (alertDialog != null) {
                alertDialog.dismiss();
                alertDialog = null;
            }
            alertDialog = new AlertDialog.Builder(activity).create();
            alertDialog.setTitle("Sensor Error");
            alertDialog.setMessage("Be sure Wing is plugged in and be careful not to pull on the cord when blowing into Wing!");
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel Test", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Retry", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertDialog.show();
                }
            });

            if (testStarted) {
                recorder.stopRecording();
                testCancelled = true;
            }
        }
        else {
            if (alertDialog != null) {
                if (currentError.equals("sensor")) {
                    alertDialog.dismiss();
                }
            }
        }

        startTest.setVisibility(monitor.verifySensorIsAvailable() ? View.VISIBLE : View.INVISIBLE);
        startTest.setTextColor(monitor.verifySensorIsAvailable()? Color.parseColor("#0099cc"):Color.GRAY);
    }


    @Override
    public void reachabilityMonitorDidChangeReachability(ReachabilityMonitor monitor) {

        //enable/disable the start test button and change the color based on sensor status
        if (!monitor.isConnected){
            currentError = "network";
            if (alertDialog != null) {
                alertDialog.dismiss();
                alertDialog = null;
            }
            alertDialog = new AlertDialog.Builder(activity).create();
            alertDialog.setTitle("Internet Error");
            alertDialog.setMessage("You must be connected to the internet in order to take a test. Please fix your connection and try again.");
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertDialog.show();
                }
            });

            if (testStarted) {
                recorder.stopRecording();
                testCancelled = true;
            }
        }
        else {
            if (alertDialog != null) {
                if (currentError.equals("network")) {
                    alertDialog.dismiss();
                    alertDialog = null;
                }
            }
        }

        startTest.setVisibility(monitor.isConnected ? View.VISIBLE : View.INVISIBLE);
        startTest.setTextColor(monitor.isConnected? Color.parseColor("#0099cc"):Color.GRAY);

//
//        if (recorder !=null){
//            switch (recorder.getRecorderState()){
//                //we only care about ready and recording states
//                case ready:
//                    Log.d("recorderStateChanged", "ready");
//                    if (monitor.isConnected){
//                        //get rid of the active alert
//                    }else{
//                        Toast.makeText(this,"Internet Disconnected",(int) 500.0).show();
//                    }
//
//                case recording:
//                    Log.d("recorderStateChanged", "recording");
//                    if (!monitor.isConnected){
//                        Toast.makeText(this,"Internet Disconnected",(int) 500.0).show();
//                    }
//            }
//        }
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(application.reachabilityMonitor);
        unregisterReceiver(application.sensorMonitor);
        application.reachabilityMonitor.setDelegate(null);
        application.sensorMonitor.setDelegate(null);
    }

    @Override public void onStop() {
        super.onStop();
        if (alertDialog != null) { alertDialog.dismiss(); alertDialog = null; }
    }

    @Override
    public void ambientNoiseMonitorDidChangeState(AmbientNoiseMonitor monitor) {
        if (!monitor.isBelowThreshold){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity,"AmbientNoiseMonitor: ",Toast.LENGTH_SHORT);
                }
            });
        }

    }

    @Override
    public void completed(TestSessionManager.TestSessionManagerError status) {
        Log.d(TAG, "Received a processing complete message");

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        final AlertDialog alert = new AlertDialog.Builder(activity).create();
        alert.setTitle("Test Complete");
        alert.setCancelable(false);

        if (status == null) {
            switch(sessionManager.state) {
                case noTest: {
                    break;
                }
                case goodTestFirst: {
                    alert.setMessage("Your test was processed successfully. Tap Next Test to continue.");
                    alert.setButton(AlertDialog.BUTTON_NEUTRAL, "Next Test", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startTest.setVisibility(View.VISIBLE);
                            dialogInterface.dismiss();
                        }
                    });
                    break;
                }
                case notProcessedTestFirst: {
                    alert.setMessage("An error occurred while processing this test. Tap Next Test to try it again.");
                    alert.setButton(AlertDialog.BUTTON_NEUTRAL, "Next Test", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startTest.setVisibility(View.VISIBLE);
                            dialogInterface.dismiss();
                        }
                    });
                    break;
                }
                case notReproducibleTestFirst: {
                    alert.setMessage("Your current tests' results aren't reproducible. Tap Next Test to take another test.");
                    alert.setButton(AlertDialog.BUTTON_NEUTRAL, "Next Test", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startTest.setVisibility(View.VISIBLE);
                            dialogInterface.dismiss();
                        }
                    });
                    break;
                }
                case notProcessedTestFinal: {
                    alert.setMessage("Another processing error occurred. Start a new test session in order to try again.");
                    alert.setButton(AlertDialog.BUTTON_NEUTRAL, "Dismiss", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            activity.startActivity(intent);
                            activity.finish();
                        }
                    });
                    break;
                }
                case notReproducibleTestFinal: {
                    alert.setMessage("The results from your tests aren't reproducible. Please begin a new test session to try again.");
                    alert.setButton(AlertDialog.BUTTON_NEUTRAL, "View Results", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            Gson gson = new Gson();
                            String testResults = gson.toJson(sessionManager.testSession, TestSession.class);
                            Intent intent = new Intent(getApplicationContext(), TestResultsActivity.class);
                            intent.putExtra("json", testResults);
                            activity.startActivity(intent);
                            activity.finish();
                        }
                    });
                    break;
                }
                case reproducibleTestFinal: {
                    alert.setMessage("You've completed the test session with reproducible results!");
                    alert.setButton(AlertDialog.BUTTON_NEUTRAL, "View Results", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            Gson gson = new Gson();
                            String testResults = gson.toJson(sessionManager.testSession, TestSession.class);
                            Intent intent = new Intent(getApplicationContext(), TestResultsActivity.class);
                            intent.putExtra("json", testResults);
                            activity.startActivity(intent);
                            activity.finish();
                        }
                    });
                    break;
                }
                default: {
                    break;
                }
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alert.show();
                }
            });
        }
        else {
            alert.setTitle("Upload Error");
            alert.setMessage("(" + status.toString() + ")");
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel Test", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                }
            });
            alert.setButton(AlertDialog.BUTTON_NEUTRAL, "Try Again", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startTest.setVisibility(View.VISIBLE);
                    dialogInterface.dismiss();
                }
            });

            switch (status) {
                case testUploadFailed: {
                    break;
                }
                case createUploadTargetFailed: {
                    break;
                }
                case processingTimeout: {
                    break;
                }
                case retrieveTestSessionFailed: {
                    break;
                }
                case uploadTargetCreationFailed: {
                    break;
                }
            }
        }


    }

    @Override
    public void processing() {
        Log.d(TAG, "Received a processing status");

        this.runOnUiThread(new Runnable() {
            public void run() {
                if (progressDialog != null) {
                    progressDialog.setTitle("Please wait");
                    progressDialog.setMessage("Processing...");
                }
                else {
                    progressDialog = new ProgressDialog(activity);
                    progressDialog.setCancelable(false);
                    progressDialog.setIndeterminate(true);
                    progressDialog.setTitle("Please wait");
                    progressDialog.setMessage("Processing...");
                }
                if (!progressDialog.isShowing()) {
                    progressDialog.show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
    }
}
