package de.azapps.mirakel;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Mirakel extends Application {
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
