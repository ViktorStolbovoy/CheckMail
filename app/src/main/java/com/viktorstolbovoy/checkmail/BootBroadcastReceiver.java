package com.viktorstolbovoy.checkmail;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootBroadcastReceiver extends BroadcastReceiver {
    public BootBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences(SetupActivity.PREFS_NAME,  Context.MODE_PRIVATE);
        int color =  settings.getInt(SetupActivity.COLOR_SETTING, 0);

        if (color == 0) {
            SetupActivity.CancelSchedule(context);
        }
        else {
            SetupActivity.SetSchedule(context);
        }
    }
}
