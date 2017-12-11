package com.sparohealth.wingkit.classes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Monitors network availability to determine if uploading a test recording is possible
 */
public class ReachabilityMonitor extends BroadcastReceiver {
    public ReachabilityMonitorDelegate delegate = null;
    private boolean isActive = false;
    ConnectivityManager connManager;
    public boolean isConnected = false;
    private Context appContext = null;

    /**
     * Set the callback delegate for the {@link ReachabilityMonitor}
     * @param delegate The callback delegate object
     */
    public void setDelegate(ReachabilityMonitorDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Initialize the {@link ReachabilityMonitor}
     * @param context The application context
     */
    public ReachabilityMonitor(Context context){
        this.appContext = context;
        connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        updateConnectionStatus();
    }

    private void updateConnectionStatus(){

        try {

            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

            if (networkInfo != null){
                isConnected = networkInfo.isConnectedOrConnecting();
                Log.d("Network", "Internet connected");
            }
            else
            {
                Log.d("Network", "Internet Dis-connected");
                isConnected = false;
            }

        }catch (Exception ex){
            Log.d("updateConnectionStatus",ex.getMessage());
        }

    }

    /**
     * Receives updates on the devices network connectivity status and sends changes to the delegate class
     * @param context The context from the broadcast
     * @param intent The intent from the broadcast
     */
    @Override
    public void onReceive(Context context, Intent intent){
        Log.d("INTERNET","Onreceive called");

        //send update
        try {
            if (delegate != null){
                updateConnectionStatus();
                delegate.reachabilityMonitorDidChangeReachability(this);
            }
        }catch (Exception ex){
            Log.d("OnReceive", ex.getMessage());
        }

    }

    /**
     * Callback interface for status updates from {@link ReachabilityMonitor}
     */
    public interface ReachabilityMonitorDelegate {

        /**
         * Tells the delegate when the network reachability state has changed.
         * @param monitor The {@link ReachabilityMonitor} object
         */
        void reachabilityMonitorDidChangeReachability(ReachabilityMonitor monitor);
    }
}
