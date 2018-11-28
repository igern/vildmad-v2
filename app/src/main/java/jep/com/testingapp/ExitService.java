package jep.com.testingapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class ExitService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        System.out.println("onTaskRemoved called");
        super.onTaskRemoved(rootIntent);
        getSharedPreferences("login", MODE_PRIVATE).edit().putBoolean("logged", false).apply();
        getSharedPreferences("login", MODE_PRIVATE).edit().remove("user").apply();
        this.stopSelf();
    }
}