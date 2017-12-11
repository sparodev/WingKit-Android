package com.sparohealth.wingkit.classes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

/**
 *
 * Monitors the plugged in state of the sensor. Notifies its delegate whenever the state of the changes
 */

public class SensorMonitor extends BroadcastReceiver{

    public SensorMonitorDelegate delegate = null;
    private boolean isActive = false;
    private boolean isPluggedIn = false;
    private boolean isMicrophone = false;

    /**
     * Init method
     * @param newDelegate
     */
    public SensorMonitor(SensorMonitorDelegate newDelegate){
        delegate = newDelegate;
//        refreshState();
    }

    public void setDelegate(SensorMonitorDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Updates the state of the Recording device
     */
    private void refreshState() {
        isPluggedIn = verifySensorIsAvailable();
    }

    /**
     * Verifies that the WING device is plugged in
     * @return
     */
    public boolean verifySensorIsAvailable() {

        return isPluggedIn && isMicrophone;
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        if (delegate != null && AudioManager.ACTION_HEADSET_PLUG == intent.getAction()){
            //update state
            int state = intent.getIntExtra("state", -1);
            int microphone = intent.getIntExtra("microphone", -1);

            switch (state) {
                case 0:
                    isPluggedIn = false;
                    break;
                case 1:
                    isPluggedIn = true;
                    break;
                default:
                    isPluggedIn = false;
                    break;
            }

            switch (microphone) {
                case 0:
                    isMicrophone = false;
                    break;
                case 1:
                    isMicrophone = true;
                    break;
                default:
                    isMicrophone = false;
                    break;
            }

            //send a notification of state changing
            delegate.sensorStateDidChange(this);
        }
    }

    /**
     * The delegate of a SensorMonitor object must adopt the SensorMonitorDelegate protocol.
     * Methods of the protocol allow the delegate to observe sensor plugged in state changes.
     */
    public interface SensorMonitorDelegate {

        /**
         * Tells the delegate when the state of the sensor has changed.
         * @param monitor
         */
        void sensorStateDidChange(SensorMonitor monitor);
    }
}
