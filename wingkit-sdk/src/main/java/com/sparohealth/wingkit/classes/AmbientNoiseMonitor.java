package com.sparohealth.wingkit.classes;

import android.content.Context;
import android.media.MediaRecorder;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Monitor ambient noise levels to determine if there is too much background noise for a successful lung function test
 */
public class AmbientNoiseMonitor {
    private double mAmplitudeSample = 0;
    private int mAmplitudeSampleCount = 0;
    private MediaRecorder recorder = null;
    // The object that acts as the delegate of the monitor.
    private AmbientNoiseMonitorDelegate delegate = null;
    /// Indicates whether the ambient noise level is below or above the allowed threshold.
    public boolean isBelowThreshold = true;
    private double lastDecibelVal = 0;
    private double noiseThreshold = -10.0;
    Context currentContext = null;
    /// Indicates whether the monitor is active or not.
    public boolean isActive = false;
    final long noiseCheckInterval = 250;
    Timer timer = new Timer();

    /**
     * Get the baseline noise level recorded by AmbientNoiseMonitor
     * @return Base noise level (dB)
     */
    public double baselineAmplitude() {
        return this.mAmplitudeSample / this.mAmplitudeSampleCount;
    }

    /**
     * Initialize the Ambient Noise Monitor
     * @param callback Callback delegate for status update events
     */
    public AmbientNoiseMonitor (AmbientNoiseMonitorDelegate callback){
        delegate = callback;
        recorder = new MediaRecorder();
    }

    /**
     * Configures a session for the audio recorder
     */
    private void configureSession() {
        String outputFile = "";

        File file = new File(currentContext.getExternalFilesDir(null),"ambientNoise.amr");
        outputFile = file.getAbsolutePath();

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        recorder.setOutputFile(outputFile);
    }

    /**
    * Stops an audio session and stops the timer
    * */
    public void stop(){
        try {
            timer.cancel();
            recorder.stop();
            recorder.reset();
            recorder.release();
        }
        catch (RuntimeException e) {

        }
    }

    /**
    * Starts measuring the amount of Ambient Noise
    * @throws IOException
    */
    public void start(Context newCtx) throws IOException {
        currentContext = newCtx;

        try {
            configureSession();
            recorder.prepare();
            recorder.start();
            startTimer();
        }
        catch (IOException ex){
            throw ex;
        }

    }

    /**
    * Starts a timer with the task of checking the ambient noise every interval -- done on a seperate thread
    */
    public void startTimer(){
        //create new timer that checks for noise every x milliseconds
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                checkAmbientNoise();
            }
        };

        //schedule task
        timer.scheduleAtFixedRate(task,0,noiseCheckInterval);

    }

    /**
    * Checks whether the ambient noise is above a threshold
    */
    private void checkAmbientNoise(){
        double currentAmpVal = recorder.getMaxAmplitude();
        double currentDBval = 20 * Math.log10( currentAmpVal/ 32767.0);

        // if the current noise level is NOT the max negative value...
        if (currentDBval != Double.NEGATIVE_INFINITY) {
            // update the amplitude sample total and count of samples (used for calculating the baseline)
            mAmplitudeSample += currentDBval;
            mAmplitudeSampleCount++;
        }

        //check that environment has a noise level within range
        if (currentDBval != lastDecibelVal && currentAmpVal > 0){
            isBelowThreshold = currentDBval < noiseThreshold;
            lastDecibelVal = currentDBval;

            if (delegate != null)
                delegate.ambientNoiseMonitorDidChangeState(this);
        }

    }

    /**
     * Return the noise threshold value
     * @return Noise threshold
     */
    public double getNoiseThreshold() {
        return noiseThreshold;
    }

    /**
     * Return the noise check interval
     * @return Noise check interval
     */
    public long getNoiseCheckInterval() {
        return noiseCheckInterval;
    }

    /**
     * Callback interface for status update events from AmbientNoiseMonitor
     */
    public interface AmbientNoiseMonitorDelegate {
        /**
         * Send the updated monitor state to the callback object
         * @param monitor
         */
        void ambientNoiseMonitorDidChangeState(AmbientNoiseMonitor monitor);
    }
}
