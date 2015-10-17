package de.domradio.service.cast;


import android.content.Context;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

import de.domradio.R;
import de.domradio.service.RadioService;

public class DomradioMediaRouterCallback extends MediaRouter.Callback implements ResultCallback<Cast.ApplicationConnectionResult>, RemoteMediaPlayer.OnStatusUpdatedListener {

    private static final String tag = DomradioMediaRouterCallback.class.getSimpleName();
    private CastDevice selectedDevice;
    private Context applicationContext;
    private GoogleApiClient apiClient;
    private RemoteMediaPlayer remoteMediaPlayer;

    public DomradioMediaRouterCallback(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
        super.onRouteSelected(router, route);
        String routeId = route.getId();
        Log.d(tag, "device selected ... " + routeId);

        selectedDevice = CastDevice.getFromBundle(route.getExtras());
        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder(selectedDevice, new DomradioCastListener(this));
        DomradioConnectionCallback connectionCallback = new DomradioConnectionCallback(this);
        apiClient = new GoogleApiClient.Builder(applicationContext)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(connectionCallback)
                .addOnConnectionFailedListener(connectionCallback)
                .build();
        apiClient.connect();
    }

    @Override
    public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
        super.onRouteUnselected(router, route);
        String routeId = route.getId();
        Log.d(tag, "device unselected ... " + routeId);
        apiClient.disconnect();
        selectedDevice = null;
    }

    public void reconnectChannels() {

    }

    public void launchApplication() {
        try {
            String receiverAppId = applicationContext.getString(R.string.receiver_app_id);
            Cast.CastApi.launchApplication(apiClient, receiverAppId, false).setResultCallback(this);
        } catch (Exception e) {
            Log.e(tag, "Failed to launch application!", e);
        }
    }

    private void playRadio() {
        remoteMediaPlayer = new RemoteMediaPlayer();
        remoteMediaPlayer.setOnStatusUpdatedListener(this);
        remoteMediaPlayer.setOnMetadataUpdatedListener(
                new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                    @Override
                    public void onMetadataUpdated() {
                        MediaInfo mediaInfo = remoteMediaPlayer.getMediaInfo();
                        MediaMetadata metadata = mediaInfo.getMetadata();
                        Log.d(tag, "mediaplayer metadata " + metadata.toString());
                    }
                });
        try {
            Cast.CastApi.setMessageReceivedCallbacks(apiClient, remoteMediaPlayer.getNamespace(), remoteMediaPlayer);
        } catch (IOException e) {
            Log.e(tag, "Exception while creating media channel", e);
        }
        remoteMediaPlayer.requestStatus(apiClient).setResultCallback(
                new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                    @Override
                    public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e(tag, "Failed to request status.");
                        }
                    }
                });

        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, "domradio.de");
        MediaInfo mediaInfo = new MediaInfo.Builder(RadioService.RADIO_URL_LOW)
                .setContentType("audio/mpeg")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
        try {
            remoteMediaPlayer.load(apiClient, mediaInfo, true)
                    .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                            if (result.getStatus().isSuccess()) {
                                Log.d(tag, "Media loaded successfully");
                            }
                        }
                    });
        } catch (IllegalStateException e) {
            Log.e(tag, "Problem occurred with media during loading", e);
        } catch (Exception e) {
            Log.e(tag, "Problem opening media during loading", e);
        }
    }

    public void tearDown() {

    }

    public GoogleApiClient getApiClient() {
        return apiClient;
    }

    @Override
    public void onResult(Cast.ApplicationConnectionResult result) {
        Status status = result.getStatus();
        if (status.isSuccess()) {
            playRadio();
        } else {
            tearDown();
        }
    }

    @Override
    public void onStatusUpdated() {
        MediaStatus mediaStatus = remoteMediaPlayer.getMediaStatus();
        if (mediaStatus != null) {
            switch (mediaStatus.getPlayerState()) {
                case MediaStatus.PLAYER_STATE_PLAYING:
                    Log.d(tag, "remote player is playing ...");
                    break;
                case MediaStatus.PLAYER_STATE_BUFFERING:
                    Log.d(tag, "remote player is loading ...");
                    break;
                default:
                    Log.d(tag, "remote player is not playing ...");
                    break;
            }
        }
    }
}
