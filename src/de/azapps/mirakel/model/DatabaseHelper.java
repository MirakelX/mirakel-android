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
package de.azapps.mirakel.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import de.azapps.mirakel.helper.ExportImport;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakelandroid.R;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = "DatabaseHelper";
	private Context context;
	public static final int DATABASE_VERSION = 23;

	public DatabaseHelper(Context ctx) {
		super(ctx, "mirakel.db", null, DATABASE_VERSION);
		context = ctx;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate");

		db.execSQL("CREATE TABLE " + ListMirakel.TABLE + " ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "name TEXT NOT NULL, "
				+ "sort_by INTEGER NOT NULL DEFAULT 0, "
				+ "created_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "updated_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "sync_state INTEGER DEFAULT " + SYNC_STATE.ADD + ", "
				+ "lft INTEGER, " + "rgt INTEGER " + ")");
		createTasksTableString(db);
		db.execSQL("INSERT INTO lists (name,lft,rgt) VALUES ('"
				+ context.getString(R.string.inbox) + "',0,1)");
		db.execSQL("INSERT INTO tasks (list_id,name) VALUES (1,'"
				+ context.getString(R.string.first_task) + "')");
		createSpecialListsTable(db);
		onUpgrade(db, 7, DATABASE_VERSION);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.e(DatabaseHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion);
		ExportImport.exportDB(context);
		switch (oldVersion) {
		case 1:// Nothing, Startversion
		case 2:
			// Add sync-state
			;
			db.execSQL("Alter Table " + Task.TABLE
					+ " add column sync_state INTEGER DEFAULT "
					+ SYNC_STATE.ADD + ";");
			db.execSQL("Alter Table " + ListMirakel.TABLE
					+ " add column sync_state INTEGER DEFAULT "
					+ SYNC_STATE.ADD + ";");
			db.execSQL("CREATE TABLE settings ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "server TEXT NOT NULL," + "user TEXT NOT NULL,"
					+ "password TEXT NOT NULL" + ")");

			db.execSQL("INSERT INTO settings (_id,server,user,password)VALUES ('0','localhost','','')");
		case 3:
			// Add lft,rgt to lists
			// Set due to null, instate of 1970 in Tasks
			// Manage fromate of updated_at created_at in Tasks/Lists
			// drop settingssettings

			db.execSQL("UPDATE " + Task.TABLE
					+ " set due='null' where due='1970-01-01'");
			String newDate = new SimpleDateFormat(
					context.getString(R.string.dateTimeFormat), Locale.US)
					.format(new Date());
			db.execSQL("UPDATE " + Task.TABLE + " set created_at='" + newDate
					+ "'");
			db.execSQL("UPDATE " + Task.TABLE + " set updated_at='" + newDate
					+ "'");
			db.execSQL("UPDATE " + ListMirakel.TABLE + " set created_at='"
					+ newDate + "'");
			db.execSQL("UPDATE " + ListMirakel.TABLE + " set updated_at='"
					+ newDate + "'");
			db.execSQL("Drop TABLE IF EXISTS settings");
		case 4:
			/*
			 * Remove NOT NULL from Task-Table
			 */

			db.execSQL("ALTER TABLE tasks RENAME TO tmp_tasks;");
			createTasksTableString(db);
			db.execSQL("INSERT INTO tasks (_id, list_id, name,done,priority,due,created_at,updated_at,sync_state) "
					+ "SELECT _id, list_id, name,done,priority,due,created_at,updated_at,sync_state "
					+ "FROM tmp_tasks;");
			db.execSQL("DROP TABLE tmp_tasks");
			db.execSQL("UPDATE tasks set due=null where due='' OR due='null'");
			/*
			 * Update Task-Table
			 */
			db.execSQL("Alter Table " + ListMirakel.TABLE
					+ " add column lft INTEGER;");
			db.execSQL("Alter Table " + ListMirakel.TABLE
					+ " add column rgt INTEGER;");
		case 5:
			createSpecialListsTable(db);
			db.execSQL("update lists set lft=(select count(*) from (select * from lists) as a where a._id<lists._id)*2 +1;");
			db.execSQL("update lists set rgt=lft+1;");
		case 6:
			/*
			 * Remove NOT NULL
			 */
			db.execSQL("ALTER TABLE tasks RENAME TO tmp_tasks;");
			createTasksTableString(db);
			db.execSQL("INSERT INTO tasks (_id, list_id, name,done,priority,due,created_at,updated_at,sync_state) "
					+ "SELECT _id, list_id, name,done,priority,due,created_at,updated_at,sync_state "
					+ "FROM tmp_tasks;");
			db.execSQL("DROP TABLE tmp_tasks");
			db.execSQL("UPDATE tasks set due=null where due=''");
		case 7:
			/*
			 * Add default list and default date for SpecialLists
			 */
			db.execSQL("Alter Table " + SpecialList.TABLE
					+ " add column def_list INTEGER;");
			db.execSQL("Alter Table " + SpecialList.TABLE
					+ " add column def_date INTEGER;");
		case 8:
			/*
			 * Add reminders for Tasks
			 */
			db.execSQL("Alter Table " + Task.TABLE
					+ " add column reminder INTEGER;");
		case 9:
			/*
			 * Update Special Lists Table
			 */
			db.execSQL("UPDATE special_lists SET def_date=0 where _id=2 and def_date=null");
			db.execSQL("UPDATE special_lists SET def_date=7 where _id=3 and def_date=null");
			db.execSQL("UPDATE special_lists SET def_date=-1, active=0 where _id=4 and def_date=null");
		case 10:
			/*
			 * Add UUID to Task
			 */
			db.execSQL("Alter Table " + Task.TABLE
					+ " add column uuid TEXT NOT NULL DEFAULT '';");
			MainActivity.updateTasksUUID = true;
			// Don't remove this version-gap
		case 13:
			db.execSQL("Alter Table "
					+ Task.TABLE
					+ " add column additional_entries TEXT NOT NULL DEFAULT '';");
		case 14:// Add Sematic
			db.execSQL("CREATE TABLE " + Semantic.TABLE + " ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "condition TEXT NOT NULL, " + "due INTEGER, "
					+ "priority INTEGER, " + "list INTEGER);");
			db.execSQL("INSERT INTO semantic_conditions (condition,due) VALUES "
					+ "(\""
					+ context.getString(R.string.today).toLowerCase()
					+ "\",0);"
					+ "INSERT INTO semantic_conditions (condition,due) VALUES (\""
					+ context.getString(R.string.tomorrow).toLowerCase()
					+ "\",1);");
		case 15:// Add Color
			db.execSQL("Alter Table " + ListMirakel.TABLE
					+ " add column color INTEGER;");
			db.execSQL("Alter Table " + SpecialList.TABLE
					+ " add column color INTEGER;");
		case 16:// Add File
			db.execSQL("CREATE TABLE " + FileMirakel.TABLE + " ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "task_id INTEGER NOT NULL DEFAULT 0, " + "name TEXT, "
					+ "path TEXT" + ")");
		case 17:// Add Subtask
			db.execSQL("CREATE TABLE " + Task.SUBTASK_TABLE + " ("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "parent_id INTEGER REFERENCES " + Task.TABLE
					+ " (_id) ON DELETE CASCADE ON UPDATE CASCADE,"
					+ "child_id INTEGER REFERENCES " + Task.TABLE
					+ " (_id) ON DELETE CASCADE ON UPDATE CASCADE);");
		case 18:// Modify Semantic
			db.execSQL("ALTER TABLE " + Semantic.TABLE
					+ " add column default_list_id INTEGER");
			db.execSQL("update semantic_conditions SET condition=LOWER(condition);");
		case 19:// Make Specialist sortable
			db.execSQL("ALTER TABLE " + SpecialList.TABLE
					+ " add column  lft INTEGER;");
			db.execSQL("ALTER TABLE " + SpecialList.TABLE
					+ " add column  rgt INTEGER ;");
			db.execSQL("update " + SpecialList.TABLE
					+ " set lft=(select count(*) from (select * from "
					+ SpecialList.TABLE + ") as a where a._id<"
					+ SpecialList.TABLE + "._id)*2 +1;");
			db.execSQL("update " + SpecialList.TABLE + " set rgt=lft+1;");
		case 20:// Add Recurring
			db.execSQL("CREATE TABLE " + Recurring.TABLE
					+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "years INTEGER DEFAULT 0," + "months INTEGER DEFAULT 0,"
					+ "days INTEGER DEFAULT 0," + "hours INTEGER DEFAULT 0,"
					+ "minutes INTEGER DEFAULT 0,"
					+ "for_due INTEGER DEFAULT 0," + "label STRING);");
			db.execSQL("ALTER TABLE " + Task.TABLE
					+ " add column recurring INTEGER DEFAULT '-1';");
			db.execSQL("INSERT INTO " + Recurring.TABLE
					+ "(days,label,for_due) VALUES (1,'"
					+ context.getString(R.string.daily) + "',1);");
			db.execSQL("INSERT INTO " + Recurring.TABLE
					+ "(days,label,for_due) VALUES (2,'"
					+ context.getString(R.string.second_day) + "',1);");
			db.execSQL("INSERT INTO " + Recurring.TABLE
					+ "(days,label,for_due) VALUES (7,'"
					+ context.getString(R.string.weekly) + "',1);");
			db.execSQL("INSERT INTO " + Recurring.TABLE
					+ "(days,label,for_due) VALUES (14,'"
					+ context.getString(R.string.two_weekly) + "',1);");
			db.execSQL("INSERT INTO " + Recurring.TABLE
					+ "(months,label,for_due) VALUES (1,'"
					+ context.getString(R.string.monthly) + "',1);");
			db.execSQL("INSERT INTO " + Recurring.TABLE
					+ "(years,label,for_due) VALUES (1,'"
					+ context.getString(R.string.yearly) + "',1);");
			db.execSQL("INSERT INTO " + Recurring.TABLE
					+ "(hours,label,for_due) VALUES (1,'"
					+ context.getString(R.string.hourly) + "',0);");
			db.execSQL("INSERT INTO " + Recurring.TABLE
					+ "(minutes,label,for_due) VALUES (1,'"
					+ context.getString(R.string.minutly) + "',0);");
		case 21:
			db.execSQL("ALTER TABLE " + Task.TABLE
					+ " add column recurring_reminder INTEGER DEFAULT '-1';");
		case 22:
			db.execSQL("ALTER TABLE " + Recurring.TABLE
					+ " add column start_date String;");
			db.execSQL("ALTER TABLE " + Recurring.TABLE
					+ " add column end_date String;");
		}
	}

	private void createTasksTableString(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE "
				+ Task.TABLE
				+ " ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "list_id INTEGER REFERENCES lists (_id) ON DELETE CASCADE ON UPDATE CASCADE, "
				+ "name TEXT NOT NULL, " + "content TEXT, "
				+ "done INTEGER NOT NULL DEFAULT 0, "
				+ "priority INTEGER NOT NULL DEFAULT 0, " + "due STRING, "
				+ "created_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "updated_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ "sync_state INTEGER DEFAULT " + SYNC_STATE.ADD + ")");
	}

	private void createSpecialListsTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + SpecialList.TABLE + " ("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "name TEXT NOT NULL, "
				+ "active INTEGER NOT NULL DEFAULT 0, "
				+ "whereQuery STRING NOT NULL DEFAULT '', "
				+ "sort_by INTEGER NOT NULL DEFAULT " + ListMirakel.SORT_BY_OPT
				+ ", " + "sync_state INTEGER DEFAULT " + SYNC_STATE.ADD + ")");
		db.execSQL("INSERT INTO " + SpecialList.TABLE
				+ " (name,active,whereQuery) VALUES (" + "'"
				+ context.getString(R.string.list_all) + "',1,'')");
		db.execSQL("INSERT INTO "
				+ SpecialList.TABLE
				+ " (name,active,whereQuery) VALUES ("
				+ "'"
				+ context.getString(R.string.list_today)
				+ "',1,'due not null and done=0 and date(due)<=date(\"now\",\"localtime\")')");
		db.execSQL("INSERT INTO "
				+ SpecialList.TABLE
				+ " (name,active,whereQuery) VALUES ("
				+ "'"
				+ context.getString(R.string.list_week)
				+ "',1,'due not null and done=0 and date(due)<=date(\"now\",\"+7 day\",\"localtime\")')");
		db.execSQL("INSERT INTO "
				+ SpecialList.TABLE
				+ " (name,active,whereQuery) VALUES ("
				+ "'"
				+ context.getString(R.string.list_overdue)
				+ "',1,'due not null and done=0 and date(due)<=date(\"now\",\"-1 day\",\"localtime\")')");
	}

}
