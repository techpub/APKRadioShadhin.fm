package de.domradio;

import android.app.Application;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.cast.CastMediaControlIntent;

import de.domradio.service.cast.DomradioMediaRouterCallback;


public class DomradioApplication extends Application {

    private final static String tag = DomradioApplication.class.getSimpleName();
    public Tracker appTracker;
    private MediaRouter mediaRouter;
    private MediaRouteSelector mediaRouteSelector;
    private DomradioMediaRouterCallback mediaRouterCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        createTracker();
        setupChromeCastEnvironment();
    }

    private void setupChromeCastEnvironment() {
        mediaRouter = MediaRouter.getInstance(this);
        String receiverAppId = getString(R.string.receiver_app_id);
        String category = CastMediaControlIntent.categoryForCast(receiverAppId);
        mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(category).build();
        mediaRouterCallback = new DomradioMediaRouterCallback(this);
    }

    public MediaRouteSelector getMediaRouteSelector() {
        return mediaRouteSelector;
    }

    private void createTracker() {
        if (appTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            appTracker = analytics.newTracker(R.xml.global_tracker);
            appTracker.enableAdvertisingIdCollection(true);
        }
    }

    public Tracker getAppTracker() {
        createTracker();
        return appTracker;
    }

    public void startDiscovery() {
        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        Log.d(tag, "starting media router discovery...");
    }

    public void stopDiscovery() {
        mediaRouter.removeCallback(mediaRouterCallback);
        Log.d(tag, "stopping media router discovery...");
    }
}
