/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.widget.Toast;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakelandroid.R;

@SuppressWarnings("unused")
@ReportsCrashes(
		formKey = "", // This is required for backward compatibility but not used 
		reportType = org.acra.sender.HttpSender.Type.JSON, 
		httpMethod = org.acra.sender.HttpSender.Method.PUT,
	    formUri = "https://mirakel.iriscouch.com/acra-mirakel/_design/acra-storage/_update/report",
	    formUriBasicAuthLogin = "android",
	    formUriBasicAuthPassword = "Kd4PBcVi2lwAbi763qaS" ,
	    disableSSLCertValidation = true,
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
        resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.

)

public class Mirakel extends Application {
	public static final int[] PRIO_COLOR = { Color.parseColor("#008000"),
			Color.parseColor("#00c400"), Color.parseColor("#3377FF"),
			Color.parseColor("#FF7700"), Color.parseColor("#FF3333") };
	public static final int NOTIF_DEFAULT = 123, NOTIF_REMINDER = 124;

	public static final String ACCOUNT_TYP = "de.azapps.mirakel";
	public static final String AUTHORITY_TYP = "de.azapps.mirakel.provider";
	public static final String BUNDLE_SERVER_URL = "url";
	public static  String APK_NAME;

	public static int widgets[] = {};

	private static final String TAG = "Mirakel";


	private static SQLiteOpenHelper openHelper;

	@Override
	public void onCreate() {
		super.onCreate();
		ACRA.init(this);
		APK_NAME=getPackageName();
		openHelper = new DatabaseHelper(this);
		Mirakel.getWritableDatabase().execSQL("PRAGMA foreign_keys=ON;");
		ListMirakel.init(getApplicationContext());
		Task.init(getApplicationContext());
		SpecialList.init(getApplicationContext());
		//Kill Notification Service if Notification disabled
		if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notificationsUse", false)&&startService(new Intent(this, NotificationService.class)) != null) { 
		    stopService(new Intent(Mirakel.this,NotificationService.class));
		}
		// Set Alarms
		ReminderAlarm.updateAlarms(getApplicationContext());
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		ListMirakel.close();
		Task.close();
		SpecialList.close();
	}

	public static SQLiteDatabase getWritableDatabase() {
		return openHelper.getWritableDatabase();
	}

	public static SQLiteDatabase getReadableDatabase() {
		return openHelper.getReadableDatabase();
	}

	public static class NoSuchListException extends Exception {
		static final long serialVersionUID = 1374828057;
	}
	public static class NoSuchTaskException extends Exception {
		static final long serialVersionUID = 1374828058;
	}
}
