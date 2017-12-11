package com.sparohealth.wingkit_sample;
import android.app.Application;

import com.sparohealth.wingkit.classes.AmbientNoiseMonitor;
import com.sparohealth.wingkit.classes.Client;
import com.sparohealth.wingkit.classes.PatientData;
import com.sparohealth.wingkit.classes.ReachabilityMonitor;
import com.sparohealth.wingkit.classes.SensorMonitor;

/**
 * Created by darien.sandifer on 10/24/2017.
 */
public class App extends Application {
    public Client client = null;
    public PatientData patientData = null;
    public ReachabilityMonitor reachabilityMonitor;
    public SensorMonitor sensorMonitor;
    public AmbientNoiseMonitor ambientNoiseMonitor;

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
