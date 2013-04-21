package de.azapps.mirakel.receivers;

import de.azapps.mirakel.services.NotificationService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootUpReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent arg1) {
		NotificationService.updateNotificationAndWidget(context);
	}

}
