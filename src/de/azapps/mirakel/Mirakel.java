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

import java.util.Locale;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakelandroid.R;

@ReportsCrashes(
// This is required for backward compatibility but not used
formKey = "",
// optional, displayed as soon as the crash occurs, before collecting data which
// can take a few seconds
reportType = org.acra.sender.HttpSender.Type.JSON, httpMethod = org.acra.sender.HttpSender.Method.PUT, formUri = "https://mirakel.iriscouch.com/acra-mirakel/_design/acra-storage/_update/report", formUriBasicAuthLogin = "android", formUriBasicAuthPassword = "Kd4PBcVi2lwAbi763qaS", disableSSLCertValidation = true, mode = ReportingInteractionMode.DIALOG, resToastText = R.string.crash_toast_text,
// optional. default is a warning sign
resDialogText = R.string.crash_dialog_text, resDialogIcon = android.R.drawable.ic_dialog_info,
// optional. default is your application name
resDialogTitle = R.string.crash_dialog_title,
// optional. when defined, adds a user text field input with this text resource
// as a label
resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, resDialogOkToast = R.string.crash_dialog_ok_toast
// optional. displays a Toast message when the user accepts to send a report.

)
public class Mirakel extends Application {
	public static final int NOTIF_DEFAULT = 123, NOTIF_REMINDER = 124;

	public static final String ACCOUNT_TYPE = "de.azapps.mirakel";
	public static final String AUTHORITY_TYP = "de.azapps.mirakel.provider";
	public static String APK_NAME;
	public static String VERSIONS_NAME;

	public static int widgets[] = {};

	private static final String TAG = "Mirakel";

	private static SQLiteOpenHelper openHelper;
	private static String MIRAKEL_DIR;
	public static int GRAVITY_LEFT, GRAVITY_RIGHT;

	public static String getMirakelDir() {
		if (MIRAKEL_DIR == null)
			MIRAKEL_DIR = Environment.getDataDirectory() + "/data/"
					+ Mirakel.APK_NAME + "/";
		return MIRAKEL_DIR;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// Some initialization
		APK_NAME = getPackageName();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			GRAVITY_LEFT = Gravity.START;
			GRAVITY_RIGHT = Gravity.END;
		} else {
			GRAVITY_LEFT = Gravity.LEFT;
			GRAVITY_RIGHT = Gravity.RIGHT;
		}
		Locale locale = Helpers.getLocal(this);
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config,
				getBaseContext().getResources().getDisplayMetrics());
		ACRA.init(this);
		try {
			VERSIONS_NAME = getPackageManager().getPackageInfo(
					getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.wtf(TAG, "App not found");
			VERSIONS_NAME = "";
		}
		openHelper = new DatabaseHelper(this);
		Mirakel.getWritableDatabase().execSQL("PRAGMA foreign_keys=ON;");
		Context ctx = getApplicationContext();

		// Initialize Models

		ListMirakel.init(ctx);
		Task.init(ctx);
		SpecialList.init(ctx);
		FileMirakel.init(ctx);
		Semantic.init(ctx);
		Recurring.init(ctx);
		Helpers.init(ctx);
		AccountMirakel.init(ctx);

		// Kill Notification Service if Notification disabled
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (!settings.getBoolean("notificationsUse", false)
				&& startService(new Intent(this, NotificationService.class)) != null) {
			stopService(new Intent(Mirakel.this, NotificationService.class));
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
		FileMirakel.close();
		Semantic.close();
		Recurring.close();
		AccountMirakel.close();
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
