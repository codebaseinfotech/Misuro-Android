package com.CB.MisureFinestre;

import android.app.Application;
import com.bugfender.sdk.Bugfender;

public class MisureFinestreApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Bugfender
        Bugfender.init(this, "8yvm6FHNkJ7kBvwlF6uuKA0Lbpe6eAqN", true);
        Bugfender.enableCrashReporting();
        Bugfender.enableUIEventLogging(this);
        Bugfender.enableLogcatLogging();

        Bugfender.d("APP_LIFECYCLE", "Application started - Bugfender initialized");
    }
}
