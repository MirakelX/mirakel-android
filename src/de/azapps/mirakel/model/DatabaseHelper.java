/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.model.account.AccountBase;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.semantic.SemanticBase;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.R;
import de.azapps.tools.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	public static final String	CREATED_AT			= "created_at";
	public static final int		DATABASE_VERSION	= 31;
	public static final String	ID					= "_id";

	public static final String	NAME				= "name";
	private static final String	TAG					= "DatabaseHelper";
	public static final String	UPDATED_AT			= "updated_at";
	public static final String	SYNC_STATE_FIELD			= "sync_state";

	private static void createAccountTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + AccountMirakel.TABLE + " (" + ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + NAME
				+ " TEXT NOT NULL, " + "content TEXT, "
				+ AccountBase.ENABLED + " INTEGER NOT NULL DEFAULT 0, "
				+ AccountBase.TYPE + " INTEGER NOT NULL DEFAULT "
				+ ACCOUNT_TYPES.LOCAL.toInt() + ")");

	}

	private static void createTasksTableString(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + Task.TABLE + " (" + ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + Task.LIST_ID
				+ " INTEGER REFERENCES " + ListMirakel.TABLE + " (" + ID
				+ ") ON DELETE CASCADE ON UPDATE CASCADE, " + NAME
				+ " TEXT NOT NULL, " + "content TEXT, " + Task.DONE
				+ " INTEGER NOT NULL DEFAULT 0, " + Task.PRIORITY
				+ " INTEGER NOT NULL DEFAULT 0, " + Task.DUE + " STRING, "
				+ CREATED_AT + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ UPDATED_AT + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ SYNC_STATE_FIELD + " INTEGER DEFAULT " + SYNC_STATE.ADD
				+ ")");
	}

	private final Context	context;

	public DatabaseHelper(Context ctx) {
		super(ctx, getDBName(), null, DATABASE_VERSION);
		MirakelPreferences.init(ctx);
		this.context = ctx;
	}

	/**
	 * Returns the database name depending if Mirakel is in demo mode or not.
	 * 
	 * If Mirakel is in demo mode, it creates for the current language a fresh
	 * new database if it does not exist.
	 * 
	 * @return
	 */
	private static String getDBName() {
		String db_name = "mirakel.db";
		try {
			if (MirakelCommonPreferences.isDemoMode()) {
				db_name = "demo_" + MirakelCommonPreferences.getLanguage()
						+ ".db";
			}
		} catch (NullPointerException e) {
			// Then the settings are not initialized and we should not do anything
		}
		return db_name;
	}

	private void createSpecialListsTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + SpecialList.TABLE + " (" + ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + NAME
				+ " TEXT NOT NULL, " + SpecialList.ACTIVE
				+ " INTEGER NOT NULL DEFAULT 0, " + SpecialList.WHERE_QUERY
				+ " STRING NOT NULL DEFAULT '', " + ListMirakel.SORT_BY
				+ " INTEGER NOT NULL DEFAULT " + ListMirakel.SORT_BY_OPT + ", "
				+ SYNC_STATE_FIELD + " INTEGER DEFAULT " + SYNC_STATE.ADD
				+ ")");
		db.execSQL("INSERT INTO " + SpecialList.TABLE + " (" + NAME + ","
				+ SpecialList.ACTIVE + "," + SpecialList.WHERE_QUERY
				+ ") VALUES (" + "'"
				+ this.context.getString(R.string.list_all) + "',1,'')");
		db.execSQL("INSERT INTO " + SpecialList.TABLE + " (" + NAME + ","
				+ SpecialList.ACTIVE + "," + SpecialList.WHERE_QUERY
				+ ") VALUES (" + "'"
				+ this.context.getString(R.string.list_today) + "',1,'"
				+ Task.DUE + " not null and " + Task.DONE + "=0 and date("
				+ Task.DUE + ")<=date(\"now\",\"localtime\")')");
		db.execSQL("INSERT INTO " + SpecialList.TABLE + " (" + NAME + ","
				+ SpecialList.ACTIVE + "," + SpecialList.WHERE_QUERY
				+ ") VALUES (" + "'"
				+ this.context.getString(R.string.list_week) + "',1,'"
				+ Task.DUE + " not null and " + Task.DONE + "=0 and date("
				+ Task.DUE + ")<=date(\"now\",\"+7 day\",\"localtime\")')");
		db.execSQL("INSERT INTO " + SpecialList.TABLE + " (" + NAME + ","
				+ SpecialList.ACTIVE + "," + SpecialList.WHERE_QUERY
				+ ") VALUES (" + "'"
				+ this.context.getString(R.string.list_overdue) + "',1,'"
				+ Task.DUE + " not null and " + Task.DONE + "=0 and date("
				+ Task.DUE + ")<=date(\"now\",\"-1 day\",\"localtime\")')");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate");
		DefinitionsHelper.freshInstall=true;

		db.execSQL("CREATE TABLE " + ListMirakel.TABLE + " (" + ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + NAME
				+ " TEXT NOT NULL, " + ListMirakel.SORT_BY
				+ " INTEGER NOT NULL DEFAULT 0, " + CREATED_AT
				+ " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, " + UPDATED_AT
				+ " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
				+ SYNC_STATE_FIELD + " INTEGER DEFAULT " + SYNC_STATE.ADD
				+ ", " + ListMirakel.LFT + " INTEGER, " + ListMirakel.RGT
				+ " INTEGER " + ")");
		createTasksTableString(db);
		db.execSQL("INSERT INTO " + ListMirakel.TABLE + " (" + NAME + ","
				+ ListMirakel.LFT + "," + ListMirakel.RGT + ") VALUES ('"
				+ this.context.getString(R.string.inbox) + "',0,1)");
		db.execSQL("INSERT INTO " + Task.TABLE + " (" + Task.LIST_ID + ","
				+ DatabaseHelper.NAME + ") VALUES (1,'"
				+ this.context.getString(R.string.first_task) + "')");
		createSpecialListsTable(db);
		onUpgrade(db, 7, DATABASE_VERSION);
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.e(TAG, "You are downgrading the Database!");
		// This is only for developers… There shouldn't happen bad things if you
		// use a database with a higher version.
	}

	@SuppressWarnings({ "fallthrough" })
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.e(DatabaseHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion);
		try {
			ExportImport.exportDB(this.context);
		} catch (Exception e) {
			Log.w(TAG, "Cannot backup database");
		}
		switch (oldVersion) {
			case 1:// Nothing, Startversion
			case 2:
				// Add sync-state
				db.execSQL("Alter Table " + Task.TABLE + " add column "
						+ SYNC_STATE_FIELD + " INTEGER DEFAULT "
						+ SYNC_STATE.ADD + ";");
				db.execSQL("Alter Table " + ListMirakel.TABLE + " add column "
						+ SYNC_STATE_FIELD + " INTEGER DEFAULT "
						+ SYNC_STATE.ADD + ";");
				db.execSQL("CREATE TABLE settings (" + ID
						+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "server TEXT NOT NULL," + "user TEXT NOT NULL,"
						+ "password TEXT NOT NULL" + ")");

				db.execSQL("INSERT INTO settings ("
						+ ID
						+ ",server,user,password)VALUES ('0','localhost','','')");
			case 3:
				// Add lft,rgt to lists
				// Set due to null, instate of 1970 in Tasks
				// Manage fromate of updated_at created_at in Tasks/Lists
				// drop settingssettings

				db.execSQL("UPDATE " + Task.TABLE + " set " + Task.DUE
						+ "='null' where " + Task.DUE + "='1970-01-01'");
				String newDate = new SimpleDateFormat(
						this.context.getString(R.string.dateTimeFormat),
						Locale.US).format(new Date());
				db.execSQL("UPDATE " + Task.TABLE + " set " + CREATED_AT + "='"
						+ newDate + "'");
				db.execSQL("UPDATE " + Task.TABLE + " set " + UPDATED_AT + "='"
						+ newDate + "'");
				db.execSQL("UPDATE " + ListMirakel.TABLE + " set " + CREATED_AT
						+ "='" + newDate + "'");
				db.execSQL("UPDATE " + ListMirakel.TABLE + " set " + UPDATED_AT
						+ "='" + newDate + "'");
				db.execSQL("Drop TABLE IF EXISTS settings");
			case 4:
				/*
				 * Remove NOT NULL from Task-Table
				 */

				db.execSQL("ALTER TABLE " + Task.TABLE
						+ " RENAME TO tmp_tasks;");
				createTasksTableString(db);
				String cols = ID + ", " + Task.LIST_ID + ", " + NAME + ", "
						+ Task.DONE + "," + Task.PRIORITY + "," + Task.DUE
						+ "," + CREATED_AT + "," + UPDATED_AT + ","
						+ SYNC_STATE_FIELD;
				db.execSQL("INSERT INTO " + Task.TABLE + " (" + cols + ") "
						+ cols + "FROM tmp_tasks;");
				db.execSQL("DROP TABLE tmp_tasks");
				db.execSQL("UPDATE " + Task.TABLE + " set " + Task.DUE
						+ "=null where " + Task.DUE + "='' OR " + Task.DUE
						+ "='null'");
				/*
				 * Update Task-Table
				 */
				db.execSQL("Alter Table " + ListMirakel.TABLE + " add column "
						+ ListMirakel.LFT + " INTEGER;");
				db.execSQL("Alter Table " + ListMirakel.TABLE + " add column "
						+ ListMirakel.RGT + " INTEGER;");
			case 5:
				createSpecialListsTable(db);
				db.execSQL("update " + ListMirakel.TABLE + " set "
						+ ListMirakel.LFT
						+ "=(select count(*) from (select * from "
						+ ListMirakel.TABLE + ") as a where a." + ID + "<"
						+ ListMirakel.TABLE + "." + ID + ")*2 +1;");
				db.execSQL("update " + ListMirakel.TABLE + " set "
						+ ListMirakel.RGT + "=" + ListMirakel.LFT + "+1;");
			case 6:
				/*
				 * Remove NOT NULL
				 */
				db.execSQL("ALTER TABLE " + Task.TABLE
						+ " RENAME TO tmp_tasks;");
				createTasksTableString(db);
				cols = ID + ", " + Task.LIST_ID + ", " + NAME + ", "
						+ Task.DONE + "," + Task.PRIORITY + "," + Task.DUE
						+ "," + CREATED_AT + "," + UPDATED_AT + ","
						+ SYNC_STATE_FIELD;
				db.execSQL("INSERT INTO " + Task.TABLE + " (" + cols + ") "
						+ "SELECT " + cols + "FROM tmp_tasks;");
				db.execSQL("DROP TABLE tmp_tasks");
				db.execSQL("UPDATE " + Task.TABLE + " set " + Task.DUE
						+ "=null where " + Task.DUE + "=''");
			case 7:
				/*
				 * Add default list and default date for SpecialLists
				 */
				db.execSQL("Alter Table " + SpecialList.TABLE + " add column "
						+ SpecialList.DEFAULT_LIST + " INTEGER;");
				db.execSQL("Alter Table " + SpecialList.TABLE + " add column "
						+ SpecialList.DEFAULT_DUE + " INTEGER;");
			case 8:
				/*
				 * Add reminders for Tasks
				 */
				db.execSQL("Alter Table " + Task.TABLE + " add column "
						+ Task.REMINDER + " INTEGER;");
			case 9:
				/*
				 * Update Special Lists Table
				 */
				db.execSQL("UPDATE special_lists SET "
						+ SpecialList.DEFAULT_DUE + "=0 where " + ID
						+ "=2 and " + SpecialList.DEFAULT_DUE + "=null");
				db.execSQL("UPDATE special_lists SET "
						+ SpecialList.DEFAULT_DUE + "=7 where " + ID
						+ "=3 and " + SpecialList.DEFAULT_DUE + "=null");
				db.execSQL("UPDATE special_lists SET "
						+ SpecialList.DEFAULT_DUE + "=-1, "
						+ SpecialList.ACTIVE + "=0 where " + ID + "=4 and "
						+ SpecialList.DEFAULT_DUE + "=null");
			case 10:
				/*
				 * Add UUID to Task
				 */
				db.execSQL("Alter Table " + Task.TABLE + " add column "
						+ Task.UUID + " TEXT NOT NULL DEFAULT '';");
//				MainActivity.updateTasksUUID = true; TODO do we need this anymore?
				// Don't remove this version-gap
			case 13:
				db.execSQL("Alter Table " + Task.TABLE + " add column "
						+ Task.ADDITIONAL_ENTRIES
						+ " TEXT NOT NULL DEFAULT '';");
			case 14:// Add Sematic
				db.execSQL("CREATE TABLE " + Semantic.TABLE + " (" + ID
						+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "condition TEXT NOT NULL, " + "due INTEGER, "
						+ "priority INTEGER, " + "list INTEGER);");
				db.execSQL("INSERT INTO semantic_conditions (condition,due) VALUES "
						+ "(\""
						+ this.context.getString(R.string.today).toLowerCase(
								Helpers.getLocal(this.context))
								+ "\",0);"
								+ "INSERT INTO semantic_conditions (condition,due) VALUES (\""
								+ this.context.getString(R.string.tomorrow)
								.toLowerCase(Helpers.getLocal(this.context))
						+ "\",1);");
			case 15:// Add Color
				db.execSQL("Alter Table " + ListMirakel.TABLE + " add column "
						+ ListMirakel.COLOR + " INTEGER;");
				db.execSQL("Alter Table " + SpecialList.TABLE + " add column "
						+ ListMirakel.COLOR + " INTEGER;");
			case 16:// Add File
				db.execSQL("CREATE TABLE " + FileMirakel.TABLE + " (" + ID
						+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + "task" + ID
						+ " INTEGER NOT NULL DEFAULT 0, " + "name TEXT, "
						+ "path TEXT" + ")");
			case 17:// Add Subtask
				db.execSQL("CREATE TABLE " + Task.SUBTASK_TABLE + " (" + ID
						+ " INTEGER PRIMARY KEY AUTOINCREMENT," + "parent" + ID
						+ " INTEGER REFERENCES " + Task.TABLE + " (" + ID
						+ ") ON DELETE CASCADE ON UPDATE CASCADE," + "child"
						+ ID + " INTEGER REFERENCES " + Task.TABLE + " (" + ID
						+ ") ON DELETE CASCADE ON UPDATE CASCADE);");
			case 18:// Modify Semantic
				db.execSQL("ALTER TABLE " + Semantic.TABLE
						+ " add column default_list" + ID + " INTEGER");
				db.execSQL("update semantic_conditions SET condition=LOWER(condition);");
			case 19:// Make Specialist sortable
				db.execSQL("ALTER TABLE " + SpecialList.TABLE + " add column  "
						+ ListMirakel.LFT + " INTEGER;");
				db.execSQL("ALTER TABLE " + SpecialList.TABLE + " add column  "
						+ ListMirakel.RGT + " INTEGER ;");
				db.execSQL("update " + SpecialList.TABLE + " set "
						+ ListMirakel.LFT
						+ "=(select count(*) from (select * from "
						+ SpecialList.TABLE + ") as a where a." + ID + "<"
						+ SpecialList.TABLE + "." + ID + ")*2 +1;");
				db.execSQL("update " + SpecialList.TABLE + " set "
						+ ListMirakel.RGT + "=" + ListMirakel.LFT + "+1;");
			case 20:// Add Recurring
				db.execSQL("CREATE TABLE " + Recurring.TABLE + " (" + ID
						+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ "years INTEGER DEFAULT 0,"
						+ "months INTEGER DEFAULT 0,"
						+ "days INTEGER DEFAULT 0,"
						+ "hours INTEGER DEFAULT 0,"
						+ "minutes INTEGER DEFAULT 0,"
						+ "for_due INTEGER DEFAULT 0," + "label STRING);");
				db.execSQL("ALTER TABLE " + Task.TABLE + " add column "
						+ Task.RECURRING + " INTEGER DEFAULT '-1';");
				db.execSQL("INSERT INTO " + Recurring.TABLE
						+ "(days,label,for_due) VALUES (1,'"
						+ this.context.getString(R.string.daily) + "',1);");
				db.execSQL("INSERT INTO " + Recurring.TABLE
						+ "(days,label,for_due) VALUES (2,'"
						+ this.context.getString(R.string.second_day) + "',1);");
				db.execSQL("INSERT INTO " + Recurring.TABLE
						+ "(days,label,for_due) VALUES (7,'"
						+ this.context.getString(R.string.weekly) + "',1);");
				db.execSQL("INSERT INTO " + Recurring.TABLE
						+ "(days,label,for_due) VALUES (14,'"
						+ this.context.getString(R.string.two_weekly) + "',1);");
				db.execSQL("INSERT INTO " + Recurring.TABLE
						+ "(months,label,for_due) VALUES (1,'"
						+ this.context.getString(R.string.monthly) + "',1);");
				db.execSQL("INSERT INTO " + Recurring.TABLE
						+ "(years,label,for_due) VALUES (1,'"
						+ this.context.getString(R.string.yearly) + "',1);");
				db.execSQL("INSERT INTO " + Recurring.TABLE
						+ "(hours,label,for_due) VALUES (1,'"
						+ this.context.getString(R.string.hourly) + "',0);");
				db.execSQL("INSERT INTO " + Recurring.TABLE
						+ "(minutes,label,for_due) VALUES (1,'"
						+ this.context.getString(R.string.minutly) + "',0);");
			case 21:
				db.execSQL("ALTER TABLE " + Task.TABLE + " add column "
						+ Task.RECURRING_REMINDER + " INTEGER DEFAULT '-1';");
			case 22:
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column start_date String;");
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column end_date String;");
			case 23:
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column temporary int NOT NULL default 0;");

				// Add Accountmanagment
			case 24:
				createAccountTable(db);
				ACCOUNT_TYPES type = ACCOUNT_TYPES.LOCAL;
				AccountManager am = AccountManager.get(this.context);
				String accountname = this.context
						.getString(R.string.local_account);
				if (am.getAccountsByType(AccountMirakel.ACCOUNT_TYPE_MIRAKEL).length > 0) {
					Account a = am
							.getAccountsByType(AccountMirakel.ACCOUNT_TYPE_MIRAKEL)[0];
					String t = AccountManager.get(this.context).getUserData(a,
							DefinitionsHelper.BUNDLE_SERVER_TYPE);
					if (t.equals(DefinitionsHelper.TYPE_TW_SYNC)) {
						type = ACCOUNT_TYPES.TASKWARRIOR;
						accountname = a.name;
					}
				}
				ContentValues cv = new ContentValues();
				cv.put(DatabaseHelper.NAME, accountname);
				cv.put(AccountBase.TYPE, type.toInt());
				cv.put(AccountBase.ENABLED, true);
				long accountId = db.insert(AccountMirakel.TABLE, null, cv);
				db.execSQL("ALTER TABLE " + ListMirakel.TABLE + " add column "
						+ ListMirakel.ACCOUNT_ID + " REFERENCES "
						+ AccountMirakel.TABLE + " (" + ID
						+ ") ON DELETE CASCADE ON UPDATE CASCADE DEFAULT "
						+ accountId + "; ");
				// add progress
			case 25:
				db.execSQL("ALTER TABLE " + Task.TABLE
						+ " add column progress int NOT NULL default 0;");
				// Add some columns for caldavsync
			case 26:
				db.execSQL("CREATE TABLE caldav_extra(" + ID
						+ " INTEGER PRIMARY KEY," + "ETAG TEXT,"
						+ "SYNC_ID TEXT DEFAULT NULL, " + "REMOTE_NAME TEXT)");
			case 27:
				db.execSQL("UPDATE " + Task.TABLE + " SET " + Task.PROGRESS
						+ "=100 WHERE " + Task.DONE + "= 1 AND "
						+ Task.RECURRING + "=-1");
			case 28:
				db.execSQL("ALTER TABLE " + Semantic.TABLE
						+ " add column weekday int;");
				String[] weekdays = this.context.getResources().getStringArray(
						R.array.weekdays);
				for (int i = 1; i < weekdays.length; i++) { // Ignore first element
					db.execSQL("INSERT INTO " + Semantic.TABLE + " ("
							+ SemanticBase.CONDITION + "," + SemanticBase.WEEKDAY
							+ ") VALUES (?, " + i + ")",
							new String[] { weekdays[i] });
				}
				// add some options to reccuring
			case 29:
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column isExact INTEGER DEFAULT 0;");

				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column monday INTEGER DEFAULT 0;");
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column tuesday INTEGER DEFAULT 0;");
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column wednesday INTEGER DEFAULT 0;");
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column thursday INTEGER DEFAULT 0;");
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column friday INTEGER DEFAULT 0;");
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column saturday INTEGER DEFAULT 0;");
				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column sunnday INTEGER DEFAULT 0;");

				db.execSQL("ALTER TABLE " + Recurring.TABLE
						+ " add column derived_from INTEGER DEFAULT NULL");
			case 30:
				db.execSQL("UPDATE " + Task.TABLE + " set " + Task.DUE
						+ "=" + Task.DUE + "||' 00:00:00'");
			default:
				break;

		}
	}

}
