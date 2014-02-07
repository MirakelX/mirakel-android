package de.azapps.mirakel.main_activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.azapps.mirakel.Mirakel;

public class MainActivityBroadcastReceiver extends BroadcastReceiver {
	private MainActivity	main;

	public MainActivityBroadcastReceiver(MainActivity main) {
		this.main = main;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Mirakel.SYNC_FINISHED)) {
			main.updateUI();
		}
	}

}
