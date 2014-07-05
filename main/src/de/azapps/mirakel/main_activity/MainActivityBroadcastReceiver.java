package de.azapps.mirakel.main_activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.azapps.mirakel.DefinitionsHelper;

public class MainActivityBroadcastReceiver extends BroadcastReceiver {
    private final MainActivity main;

    public MainActivityBroadcastReceiver (final MainActivity main) {
        this.main = main;
    }

    @Override
    public void onReceive (final Context context, final Intent intent) {
        if (intent.getAction ().equals (DefinitionsHelper.SYNC_FINISHED)) {
            this.main.updateUI ();
        }
    }

}
