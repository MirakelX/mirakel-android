package de.azapps.mirakel.main_activity;

import de.azapps.mirakel.DefinitionsHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MainActivityBroadcastReceiver extends BroadcastReceiver {
	private MainActivity	main;

	public MainActivityBroadcastReceiver(MainActivity main) {
		this.main = main;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(DefinitionsHelper.SYNC_FINISHED)) {
			this.main.updateUI();
		}
	}

}
