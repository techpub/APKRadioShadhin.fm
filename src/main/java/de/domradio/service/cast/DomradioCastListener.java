package de.domradio.service.cast;

import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.common.api.GoogleApiClient;


public class DomradioCastListener extends Cast.Listener {
    private static final String tag = DomradioCastListener.class.getSimpleName();

    private DomradioMediaRouterCallback domradioMediaRouterCallback;

    public DomradioCastListener(DomradioMediaRouterCallback domradioMediaRouterCallback) {
        this.domradioMediaRouterCallback = domradioMediaRouterCallback;
    }

    @Override
    public void onApplicationStatusChanged() {
        GoogleApiClient apiClient = domradioMediaRouterCallback.getApiClient();
        if (apiClient != null) {
            Log.d(tag, "onApplicationStatusChanged: " + Cast.CastApi.getApplicationStatus(apiClient));
        }
    }

    @Override
    public void onVolumeChanged() {
        GoogleApiClient apiClient = domradioMediaRouterCallback.getApiClient();
        if (apiClient != null) {
            Log.d(tag, "onVolumeChanged: " + Cast.CastApi.getVolume(apiClient));
        }
    }

    @Override
    public void onApplicationDisconnected(int errorCode) {
        domradioMediaRouterCallback.tearDown();
    }
}
