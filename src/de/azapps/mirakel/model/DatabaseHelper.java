package de.azapps.mirakel.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = "DatabaseHelper";
	private Context context;

	public DatabaseHelper(Context ctx) {
		super(ctx, "mirakel.db", null, Mirakel.DATABASE_VERSION);
		context=ctx;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate");

		db.execSQL("CREATE TABLE "+Mirakel.TABLE_LISTS+" ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "name TEXT NOT NULL, "
				+ "sort_by INTEGER NOT NULL DEFAULT 0, "
				+ "created_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "updated_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "sync_state INTEGER DEFAULT "+Mirakel.SYNC_STATE_ADD//+", "
				//+ "lft INTEGER, "
				//+ "rgt INTEGER "
				+ ")");
		db.execSQL("CREATE TABLE "+Mirakel.TABLE_TASKS+" ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "list_id INTEGER REFERENCES lists (_id) ON DELETE CASCADE ON UPDATE CASCADE, "
				+ "name TEXT NOT NULL, " 
				+ "content TEXT NOT NULL DEFAULT '', "
				+ "done INTEGER NOT NULL DEFAULT 0, "
				+ "priority INTEGER NOT NULL DEFAULT 0, " 
				+ "due STRING NOT NULL DEFAULT '', "
				+ "created_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "updated_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "sync_state INTEGER DEFAULT "+Mirakel.SYNC_STATE_ADD
				+ ")");
		db.execSQL("INSERT INTO lists (name) VALUES ('" + context.getString(R.string.inbox) +"')");
		db.execSQL("INSERT INTO tasks (list_id,name) VALUES (1,'" + context.getString(R.string.first_task) +"')");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.e(DatabaseHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion);
		switch(oldVersion){
			case 1://Nothing, Startversion
			case 2:
				//Add sync-state
				String update="Alter Table "+Mirakel.TABLE_TASKS+" add column sync_state INTEGER DEFAULT "+Mirakel.SYNC_STATE_ADD+";";
				db.execSQL(update);
				update="Alter Table "+Mirakel.TABLE_LISTS+" add column sync_state INTEGER DEFAULT "+Mirakel.SYNC_STATE_ADD+";";
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
			case 3:
				//Add lft,rgt to lists
				//Set due to null, instate of 1970 in Tasks
				//Manage fromate of updated_at created_at in Tasks/Lists
				//drop settingssettings
				//update="Alter Table "+Mirakel.TABLE_LISTS+" add column lft INTEGER;";
				//db.execSQL(update);
				//update="Alter Table "+Mirakel.TABLE_LISTS+" add column rgt INTEGER;";
				//db.execSQL(update);
				
				update="UPDATE "+Mirakel.TABLE_TASKS+" set due='null' where due='1970-01-01'";
				db.execSQL(update);
				String newDate=new SimpleDateFormat(context.getString(R.string.dateTimeFormat),Locale.US).format(new Date());
				update="UPDATE "+Mirakel.TABLE_TASKS+" set created_at='"+newDate+"'";
				db.execSQL(update);
				update="UPDATE "+Mirakel.TABLE_TASKS+" set updated_at='"+newDate+"'";
				db.execSQL(update);
				update="UPDATE "+Mirakel.TABLE_LISTS+" set created_at='"+newDate+"'";
				db.execSQL(update);
				update="UPDATE "+Mirakel.TABLE_LISTS+" set updated_at='"+newDate+"'";
				db.execSQL(update);
				update="Drop TABLE IF EXISTS settings";
				db.execSQL(update);
				db.execSQL("PRAGMA writable_schema=ON;");
				String c="CREATE TABLE tasks (_id INTEGER PRIMARY KEY AUTOINCREMENT, list_id INTEGER REFERENCES lists (_id) ON DELETE CASCADE ON UPDATE CASCADE, name TEXT NOT NULL, content TEXT, done INTEGER NOT NULL DEFAULT 0, priority INTEGER NOT NULL DEFAULT 0, due INTEGER, created_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, sync_state INTEGER DEFAULT 1)";
				db.execSQL("Update sqlite_master set sql='"+c+"' where name='tasks'");
				db.execSQL("PRAGMA writable_schema=OFF;");
			//Next DB-Versions
			//case 4:
			//case 5:
				
		}
		//db.execSQL("DROP TABLE IF EXISTS lists");
		//db.execSQL("DROP TABLE IF EXISTS tasks");
		//onCreate(db);

	}

}
