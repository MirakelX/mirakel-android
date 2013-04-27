package de.azapps.mirakel.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.azapps.mirakel.services.NotificationService;

public class BootUpReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent arg1) {
		NotificationService.updateNotificationAndWidget(context);
	}

}
