package de.azapps.mirakel;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.util.Log;

public class Mirakel extends Application {
	public static final int LIST_ALL=0;
	public static final int LIST_DAILY=-1;
	public static final int LIST_WEEKLY=-2;
	public static final int[] PRIO_COLOR = { Color.parseColor("#006400"),
		Color.GREEN, Color.YELLOW, Color.parseColor("#FF8C00"), Color.RED };
	private static final String TAG="Mirakel";
	
	private static SQLiteOpenHelper openHelper;
	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		
		super.onCreate();
		openHelper=new DatabaseHelper(this);
		Mirakel.getWritableDatabase().execSQL("PRAGMA foreign_keys=ON;");
		
	}
	public static SQLiteDatabase getWritableDatabase() {
		return openHelper.getWritableDatabase();
	}

	public static SQLiteDatabase getReadableDatabase() {
		return openHelper.getReadableDatabase();
	}

}
