package de.domradio.service.cast;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;


public class DomradioConnectionCallback implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String tag = DomradioConnectionCallback.class.getSimpleName();
    private boolean waitingForReconnect;
    private DomradioMediaRouterCallback domradioMediaRouterCallback;

    public DomradioConnectionCallback(DomradioMediaRouterCallback domradioMediaRouterCallback) {
        this.domradioMediaRouterCallback = domradioMediaRouterCallback;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(tag, "connection established ...");
        if (waitingForReconnect) {
            waitingForReconnect = false;
            domradioMediaRouterCallback.reconnectChannels();
        } else {
            domradioMediaRouterCallback.launchApplication();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(tag, "connection suspended ...");
        waitingForReconnect = true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(tag, "connection failed ...");
        domradioMediaRouterCallback.tearDown();
    }
}
