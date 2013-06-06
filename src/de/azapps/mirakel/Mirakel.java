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

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.util.Log;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;

public class Mirakel extends Application {
	public static final int[] PRIO_COLOR = { Color.parseColor("#008000"),
			Color.parseColor("#00c400"), Color.parseColor("#3377FF"),
			Color.parseColor("#FF7700"), Color.parseColor("#FF3333") };
	public static final int NOTIF_DEFAULT = 123, NOTIF_REMINDER = 124;

	public static final String ACCOUNT_TYP = "de.azapps.mirakel";
	public static final String AUTHORITY_TYP = "de.azapps.mirakel.provider";
	public static final String BUNDLE_SERVER_URL = "url";

	public static int widgets[] = {};

	private static final String TAG = "Mirakel";

	public static final boolean DEBUG = false;// Set to false/true to
												// disable/enable debuginglog

	private static SQLiteOpenHelper openHelper;

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		super.onCreate();
		openHelper = new DatabaseHelper(this);
		Mirakel.getWritableDatabase().execSQL("PRAGMA foreign_keys=ON;");
		ListMirakel.init(getApplicationContext());
		Task.init(getApplicationContext());
		SpecialList.init(getApplicationContext());
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

}
