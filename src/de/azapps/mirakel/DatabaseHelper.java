package de.azapps.mirakel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = "DatabaseHelper";

	public DatabaseHelper(Context ctx) {
		super(ctx, "mirakel.db", null, Mirakel.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate");

		db.execSQL("CREATE TABLE lists ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "name TEXT NOT NULL, "
				+ "sort_by INTEGER NOT NULL DEFAULT 0, "
				+ "created_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "updated_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "sync_state INTEGER DEFAULT "+Mirakel.SYNC_STATE_ADD
				+ ")");
		db.execSQL("CREATE TABLE tasks ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "list_id INTEGER REFERENCES lists (_id) ON DELETE CASCADE, "
				+ "name TEXT NOT NULL, " + "content TEXT, "
				+ "done INTEGER NOT NULL DEFAULT 0, "
				+ "priority INTEGER NOT NULL DEFAULT 0, " + "due INTEGER, "
				+ "created_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "updated_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "sync_state INTEGER DEFAULT "+Mirakel.SYNC_STATE_ADD
				+ ")");
		db.execSQL("CREATE TABLE settings ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "server TEXT NOT NULL,"
				+ "user TEXT NOT NULL,"
				+ "passwort TEXT NOT NULL"
				+ ")");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// @TODO implement user-friendly
		Log.e(DatabaseHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		switch(oldVersion){
			case 2://Nothing, Startversion
			case 3:
				//Add sync-state
				String update="Alter Table tasks add column sync_state INTEGER DEFAULT "+Mirakel.SYNC_STATE_ADD+";";
				db.execSQL(update);
				update="Alter Table lists add column sync_state INTEGER DEFAULT "+Mirakel.SYNC_STATE_ADD+";";
				db.execSQL(update);
				update="CREATE TABLE settings ("
						+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "server TEXT NOT NULL,"
						+ "user TEXT NOT NULL,"
						+ "password TEXT NOT NULL"
						+ ")";
				db.execSQL(update);
				
				update="INSERT INTO settings (_id,server,user,password)VALUES ('0','localhost','','')";
				db.execSQL(update);
			//Next DB-Versions
			//case 4:
			//case 5:
				
		
		}
		//db.execSQL("DROP TABLE IF EXISTS lists");
		//db.execSQL("DROP TABLE IF EXISTS tasks");
		//onCreate(db);

	}

}
