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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.TransformerException;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.CompatibilityHelper;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsContentProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskDeserializer;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String CREATED_AT = "created_at";
    public static final int DATABASE_VERSION = 41;

    private static final String TAG = "DatabaseHelper";
    public static final String UPDATED_AT = "updated_at";
    public static final String SYNC_STATE_FIELD = "sync_state";

    private static void createAccountTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + AccountMirakel.TABLE + " (" + ModelBase.ID
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ModelBase.NAME
                   + " TEXT NOT NULL, " + "content TEXT, "
                   + AccountMirakel.ENABLED + " INTEGER NOT NULL DEFAULT 0, "
                   + AccountMirakel.TYPE + " INTEGER NOT NULL DEFAULT "
                   + ACCOUNT_TYPES.LOCAL.toInt() + ")");
    }

    protected static void createTasksTableOLD(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Task.TABLE + " (" + ModelBase.ID
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Task.LIST_ID
                   + " INTEGER REFERENCES " + ListMirakel.TABLE + " (" + ModelBase.ID
                   + ") ON DELETE CASCADE ON UPDATE CASCADE, " + ModelBase.NAME
                   + " TEXT NOT NULL, " + "content TEXT, " + Task.DONE
                   + " INTEGER NOT NULL DEFAULT 0, " + Task.PRIORITY
                   + " INTEGER NOT NULL DEFAULT 0, " + Task.DUE + " STRING, "
                   + CREATED_AT + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + UPDATED_AT + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + SYNC_STATE_FIELD + " INTEGER DEFAULT " + SYNC_STATE.ADD + ")");
    }

    private final Context context;

    protected static void createTasksTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Task.TABLE + " (" + ModelBase.ID
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Task.LIST_ID
                   + " INTEGER REFERENCES " + ListMirakel.TABLE + " (" + ModelBase.ID
                   + ") ON DELETE CASCADE ON UPDATE CASCADE, " + ModelBase.NAME
                   + " TEXT NOT NULL, " + "content TEXT, " + Task.DONE
                   + " INTEGER NOT NULL DEFAULT 0, " + Task.PRIORITY
                   + " INTEGER NOT NULL DEFAULT 0, " + Task.DUE + " STRING, "
                   + CREATED_AT + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + UPDATED_AT + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + SYNC_STATE_FIELD + " INTEGER DEFAULT " + SYNC_STATE.ADD + ","
                   + Task.REMINDER + " INTEGER," + Task.UUID
                   + " TEXT NOT NULL DEFAULT ''," + Task.ADDITIONAL_ENTRIES
                   + " TEXT NOT NULL DEFAULT ''," + Task.RECURRING
                   + " INTEGER DEFAULT '-1'," + Task.RECURRING_REMINDER
                   + " INTEGER DEFAULT '-1'," + Task.PROGRESS
                   + " INTEGER NOT NULL default 0)");
    }

    private DatabaseHelper(final Context ctx) {
        super(ctx, getDBName(ctx), null, DATABASE_VERSION);
        this.context = ctx;
    }

    private static DatabaseHelper databaseHelperSingleton;

    public static DatabaseHelper getDatabaseHelper(Context context) {
        if (databaseHelperSingleton == null) {
            databaseHelperSingleton = new DatabaseHelper(context);
        }
        return databaseHelperSingleton;
    }


    /**
     * Returns the database name depending if Mirakel is in demo mode or not.
     *
     * If Mirakel is in demo mode, it creates for the current language a fresh
     * new database if it does not exist.
     *
     * @return
     */
    public static String getDBName(final Context ctx) {
        MirakelPreferences.init(ctx);
        return MirakelModelPreferences.getDBName();
    }

    private void createSpecialListsTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + SpecialList.TABLE + " (" + ModelBase.ID
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ModelBase. NAME
                   + " TEXT NOT NULL, " + SpecialList.ACTIVE
                   + " INTEGER NOT NULL DEFAULT 0, " + SpecialList.WHERE_QUERY
                   + " STRING NOT NULL DEFAULT '', " + ListMirakel.SORT_BY_FIELD
                   + " INTEGER NOT NULL DEFAULT " + ListMirakel.SORT_BY.OPT.getShort() + ", "
                   + SYNC_STATE_FIELD + " INTEGER DEFAULT " + SYNC_STATE.ADD
                   + ", " + SpecialList.DEFAULT_LIST + " INTEGER, "
                   + SpecialList.DEFAULT_DUE + " INTEGER," + ListMirakel.COLOR
                   + " INTEGER, " + ListMirakel.LFT + " INTEGER ,"
                   + ListMirakel.RGT + " INTEGER)");
        db.execSQL("INSERT INTO " + SpecialList.TABLE + " (" + ModelBase.NAME + ","
                   + SpecialList.ACTIVE + "," + SpecialList.WHERE_QUERY + ","
                   + ListMirakel.LFT + ", " + ListMirakel.RGT + ") VALUES (" + "'"
                   + this.context.getString(R.string.list_all) + "',1,'"
                   + Task.DONE + "=0',1,2)");
        db.execSQL("INSERT INTO " + SpecialList.TABLE + " (" + ModelBase.NAME + ","
                   + SpecialList.ACTIVE + "," + SpecialList.WHERE_QUERY + ","
                   + ListMirakel.LFT + ", " + ListMirakel.RGT + ","
                   + SpecialList.DEFAULT_DUE + ") VALUES (" + "'"
                   + this.context.getString(R.string.list_today) + "',1,'"
                   + Task.DUE + " not null and " + Task.DONE + "=0 and date("
                   + Task.DUE + ")<=date(\"now\",\"localtime\")',3,4,0)");
        db.execSQL("INSERT INTO " + SpecialList.TABLE + " (" + ModelBase.NAME + ","
                   + SpecialList.ACTIVE + "," + SpecialList.WHERE_QUERY + ","
                   + ListMirakel.LFT + ", " + ListMirakel.RGT + ","
                   + SpecialList.DEFAULT_DUE + ") VALUES (" + "'"
                   + this.context.getString(R.string.list_week) + "',1,'"
                   + Task.DUE + " not null and " + Task.DONE + "=0 and date("
                   + Task.DUE
                   + ")<=date(\"now\",\"+7 day\",\"localtime\")',5,6,7)");
        db.execSQL("INSERT INTO " + SpecialList.TABLE + " (" + ModelBase.NAME + ","
                   + SpecialList.ACTIVE + "," + SpecialList.WHERE_QUERY + ","
                   + ListMirakel.LFT + ", " + ListMirakel.RGT + ","
                   + SpecialList.DEFAULT_DUE + ") VALUES (" + "'"
                   + this.context.getString(R.string.list_overdue) + "',1,'"
                   + Task.DUE + " not null and " + Task.DONE + "=0 and date("
                   + Task.DUE
                   + ")<=date(\"now\",\"-1 day\",\"localtime\")',7,8,-1)");
    }

    private void createRecurringTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "
                   + Recurring.TABLE
                   + " (" + ModelBase.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                   + "years INTEGER DEFAULT 0,"
                   + "months INTEGER DEFAULT 0,"
                   + "days INTEGER DEFAULT 0,"
                   + "hours INTEGER DEFAULT 0,"
                   + "minutes INTEGER DEFAULT 0,"
                   + "for_due INTEGER DEFAULT 0,"
                   + "label STRING, start_date String,"
                   + " end_date String, "
                   + "temporary int NOT NULL default 0,"
                   + " isExact INTEGER DEFAULT 0, "
                   + "monday INTEGER DEFAULT 0,"
                   + "tuesday INTEGER DEFAULT 0, "
                   + "wednesday INTEGER DEFAULT 0,"
                   + "thursday INTEGER DEFAULT 0, "
                   + "friday INTEGER DEFAULT 0,"
                   + "saturday INTEGER DEFAULT 0,"
                   + "sunnday INTEGER DEFAULT 0, "
                   + "derived_from INTEGER DEFAULT NULL);");
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
    }

    private void createSemanticTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Semantic.TABLE + " (" + ModelBase.ID
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                   + "condition TEXT NOT NULL, " + "due INTEGER, "
                   + "priority INTEGER, " + "list INTEGER," + "default_list" + ModelBase.ID
                   + " INTEGER, weekday INTEGER);");
        db.execSQL("INSERT INTO semantic_conditions (condition,due) VALUES "
                   + "(\""
                   + this.context.getString(R.string.today).toLowerCase(
                       Helpers.getLocal(this.context))
                   + "\",0);"
                   + "INSERT INTO semantic_conditions (condition,due) VALUES (\""
                   + this.context.getString(R.string.tomorrow).toLowerCase(
                       Helpers.getLocal(this.context)) + "\",1);");
        final String[] weekdays = this.context.getResources().getStringArray(
                                      R.array.weekdays);
        for (int i = 1; i < weekdays.length; i++) { // Ignore first element
            db.execSQL("INSERT INTO " + Semantic.TABLE + " ("
                       + Semantic.CONDITION + "," + Semantic.WEEKDAY
                       + ") VALUES (?, " + i + ")", new String[] { weekdays[i] });
        }
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        DefinitionsHelper.freshInstall = true;
        createRecurringTable(db);
        createSemanticTable(db);
        createAccountTable(db);
        final ACCOUNT_TYPES type = ACCOUNT_TYPES.LOCAL;
        final String accountname = this.context
                                   .getString(R.string.local_account);
        ContentValues cv = new ContentValues();
        cv.put(ModelBase.NAME, accountname);
        cv.put(AccountMirakel.TYPE, type.toInt());
        cv.put(AccountMirakel.ENABLED, true);
        final long accountId = db.insert(AccountMirakel.TABLE, null, cv);
        createListsTable(db, accountId);
        createTasksTable(db);
        createSubtaskTable(db);
        createFileTable(db);
        createCalDavExtraTable(db);
        // Add defaults
        db.execSQL("INSERT INTO " + ListMirakel.TABLE + " (" + ModelBase.NAME + ","
                   + ListMirakel.LFT + "," + ListMirakel.RGT + ") VALUES ('"
                   + this.context.getString(R.string.inbox) + "',0,1)");
        db.execSQL("INSERT INTO " + Task.TABLE + " (" + Task.LIST_ID + ","
                   + ModelBase.NAME + ") VALUES (1,'"
                   + this.context.getString(R.string.first_task) + "')");
        createSpecialListsTable(db);
        final String[] lists = this.context.getResources().getStringArray(
                                   R.array.demo_lists);
        for (int i = 0; i < lists.length; i++) {
            db.execSQL("INSERT INTO " + ListMirakel.TABLE + " (" + ModelBase.NAME + ","
                       + ListMirakel.LFT + "," + ListMirakel.RGT + ") VALUES ('"
                       + lists[i] + "'," + (i + 2) + "," + (i + 3) + ")");
        }
        MirakelInternalContentProvider.init(db, context);
        onUpgrade(db, 32, DATABASE_VERSION);
        if (MirakelCommonPreferences.isDemoMode()) {
            Semantic.init(context);
            final String[] tasks = this.context.getResources().getStringArray(
                                       R.array.demo_tasks);
            final String[] task_lists = { lists[1], lists[1], lists[1],
                                          lists[0], lists[2], lists[2]
                                        };
            Calendar[] dues = new Calendar[6];
            dues[0] = new GregorianCalendar();
            dues[1] = null;
            dues[2] = new GregorianCalendar();
            while (dues[2].get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                dues[2].add(Calendar.DAY_OF_MONTH, 1);
            }
            dues[3] = new GregorianCalendar();
            dues[4] = null;
            dues[5] = null;
            final int[] priorities = { 2, -1, 1, 2, 0, 0 };
            for (int i = 0; i < tasks.length; i++) {
                final Task t = new Task(tasks[i]);
                t.setDue(dues[i]);
                t.setPriority(priorities[i]);
                t.setList(ListMirakel.findByName(task_lists[i]));
                t.setSyncState(SYNC_STATE.ADD);
                try {
                    cv = t.getContentValues();
                } catch (DefinitionsHelper.NoSuchListException e) {
                    Log.wtf(TAG, "missing list", e);
                }
                cv.remove(ModelBase.ID);
                db.insert(Task.TABLE, null, cv);
            }
        }
        MirakelInternalContentProvider.init(db, context);
    }

    private static void createListsTable(final SQLiteDatabase db,
                                         final long accountId) {
        db.execSQL("CREATE TABLE " + ListMirakel.TABLE + " (" + ModelBase.ID
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ModelBase.NAME
                   + " TEXT NOT NULL, " + ListMirakel.SORT_BY_FIELD
                   + " INTEGER NOT NULL DEFAULT 0, " + CREATED_AT
                   + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, " + UPDATED_AT
                   + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + SYNC_STATE_FIELD + " INTEGER DEFAULT " + SYNC_STATE.ADD
                   + ", " + ListMirakel.LFT + " INTEGER, " + ListMirakel.RGT
                   + " INTEGER " + ", " + ListMirakel.COLOR + " INTEGER,"
                   + ListMirakel.ACCOUNT_ID + " REFERENCES "
                   + AccountMirakel.TABLE + " (" + ModelBase.ID
                   + ") ON DELETE CASCADE ON UPDATE CASCADE DEFAULT " + accountId
                   + ")");
    }

    @Override
    public void onDowngrade(final SQLiteDatabase db, final int oldVersion,
                            final int newVersion) {
        Log.e(TAG, "You are downgrading the Database!");
        // This is only for developers… There shouldn't happen bad things if you
        // use a database with a higher version.
    }

    @SuppressWarnings({ "fallthrough" })
    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                          final int newVersion) {
        Log.e(DatabaseHelper.class.getName(),
              "Upgrading database from version " + oldVersion + " to "
              + newVersion);
        try {
            ExportImport.exportDB(this.context);
        } catch (final Exception e) {
            Log.w(TAG, "Cannot backup database", e);
        }
        switch (oldVersion) {
        case 1:// Nothing, Startversion
        case 2:
            // Add sync-state
            db.execSQL("Alter Table " + Task.TABLE + " add column "
                       + SYNC_STATE_FIELD + " INTEGER DEFAULT " + SYNC_STATE.ADD
                       + ";");
            db.execSQL("Alter Table " + ListMirakel.TABLE + " add column "
                       + SYNC_STATE_FIELD + " INTEGER DEFAULT " + SYNC_STATE.ADD
                       + ";");
            db.execSQL("CREATE TABLE settings (" + ModelBase.ID
                       + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                       + "server TEXT NOT NULL," + "user TEXT NOT NULL,"
                       + "password TEXT NOT NULL" + ")");
            db.execSQL("INSERT INTO settings (" + ModelBase.ID
                       + ",server,user,password)VALUES ('0','localhost','','')");
        case 3:
            // Add lft,rgt to lists
            // Set due to null, instate of 1970 in Tasks
            // Manage fromate of updated_at created_at in Tasks/Lists
            // drop settingssettings
            db.execSQL("UPDATE " + Task.TABLE + " set " + Task.DUE
                       + "='null' where " + Task.DUE + "='1970-01-01'");
            final String newDate = new SimpleDateFormat(
                this.context.getString(R.string.dateTimeFormat), Locale.US)
            .format(new Date());
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
            db.execSQL("ALTER TABLE " + Task.TABLE + " RENAME TO tmp_tasks;");
            createTasksTableOLD(db);
            String cols = ModelBase.ID + ", " + Task.LIST_ID + ", " + ModelBase.NAME + ", "
                          + Task.DONE + "," + Task.PRIORITY + "," + Task.DUE + ","
                          + CREATED_AT + "," + UPDATED_AT + "," + SYNC_STATE_FIELD;
            db.execSQL("INSERT INTO " + Task.TABLE + " (" + cols + ") " + cols
                       + "FROM tmp_tasks;");
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
                       + ListMirakel.TABLE + ") as a where a." + ModelBase.ID + "<"
                       + ListMirakel.TABLE + "." + ModelBase.ID + ")*2 +1;");
            db.execSQL("update " + ListMirakel.TABLE + " set "
                       + ListMirakel.RGT + "=" + ListMirakel.LFT + "+1;");
        case 6:
            /*
             * Remove NOT NULL
             */
            db.execSQL("ALTER TABLE " + Task.TABLE + " RENAME TO tmp_tasks;");
            createTasksTableOLD(db);
            cols = ModelBase.ID + ", " + Task.LIST_ID + ", " + ModelBase.NAME + ", " + Task.DONE
                   + "," + Task.PRIORITY + "," + Task.DUE + "," + CREATED_AT
                   + "," + UPDATED_AT + "," + SYNC_STATE_FIELD;
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
            db.execSQL("UPDATE special_lists SET " + SpecialList.DEFAULT_DUE
                       + "=0 where " + ModelBase.ID + "=2 and " + SpecialList.DEFAULT_DUE
                       + "=null");
            db.execSQL("UPDATE special_lists SET " + SpecialList.DEFAULT_DUE
                       + "=7 where " + ModelBase.ID + "=3 and " + SpecialList.DEFAULT_DUE
                       + "=null");
            db.execSQL("UPDATE special_lists SET " + SpecialList.DEFAULT_DUE
                       + "=-1, " + SpecialList.ACTIVE + "=0 where " + ModelBase.ID
                       + "=4 and " + SpecialList.DEFAULT_DUE + "=null");
        case 10:
            /*
             * Add UUID to Task
             */
            db.execSQL("Alter Table " + Task.TABLE + " add column " + Task.UUID
                       + " TEXT NOT NULL DEFAULT '';");
            // MainActivity.updateTasksUUID = true; TODO do we need this
            // anymore?
            // Don't remove this version-gap
        case 13:
            db.execSQL("Alter Table " + Task.TABLE + " add column "
                       + Task.ADDITIONAL_ENTRIES + " TEXT NOT NULL DEFAULT '';");
        case 14:// Add Sematic
            db.execSQL("CREATE TABLE " + Semantic.TABLE + " (" + ModelBase.ID
                       + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                       + "condition TEXT NOT NULL, " + "due INTEGER, "
                       + "priority INTEGER, " + "list INTEGER);");
            db.execSQL("INSERT INTO semantic_conditions (condition,due) VALUES "
                       + "(\""
                       + this.context.getString(R.string.today).toLowerCase(
                           Helpers.getLocal(this.context))
                       + "\",0);"
                       + "INSERT INTO semantic_conditions (condition,due) VALUES (\""
                       + this.context.getString(R.string.tomorrow).toLowerCase(
                           Helpers.getLocal(this.context)) + "\",1);");
        case 15:// Add Color
            db.execSQL("Alter Table " + ListMirakel.TABLE + " add column "
                       + ListMirakel.COLOR + " INTEGER;");
            db.execSQL("Alter Table " + SpecialList.TABLE + " add column "
                       + ListMirakel.COLOR + " INTEGER;");
        case 16:// Add File
            createFileTable(db);
        case 17:// Add Subtask
            createSubtaskTable(db);
        case 18:// Modify Semantic
            db.execSQL("ALTER TABLE " + Semantic.TABLE
                       + " add column default_list" + ModelBase.ID + " INTEGER");
            db.execSQL("update semantic_conditions SET condition=LOWER(condition);");
        case 19:// Make Specialist sortable
            db.execSQL("ALTER TABLE " + SpecialList.TABLE + " add column  "
                       + ListMirakel.LFT + " INTEGER;");
            db.execSQL("ALTER TABLE " + SpecialList.TABLE + " add column  "
                       + ListMirakel.RGT + " INTEGER ;");
            db.execSQL("update " + SpecialList.TABLE + " set "
                       + ListMirakel.LFT
                       + "=(select count(*) from (select * from "
                       + SpecialList.TABLE + ") as a where a." + ModelBase.ID + "<"
                       + SpecialList.TABLE + "." + ModelBase.ID + ")*2 +1;");
            db.execSQL("update " + SpecialList.TABLE + " set "
                       + ListMirakel.RGT + "=" + ListMirakel.LFT + "+1;");
        case 20:// Add Recurring
            db.execSQL("CREATE TABLE " + Recurring.TABLE + " (" + ModelBase.ID
                       + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                       + "years INTEGER DEFAULT 0," + "months INTEGER DEFAULT 0,"
                       + "days INTEGER DEFAULT 0," + "hours INTEGER DEFAULT 0,"
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
            String accountname = this.context.getString(R.string.local_account);
            if (am.getAccountsByType(AccountMirakel.ACCOUNT_TYPE_MIRAKEL).length > 0) {
                final Account a = am
                                  .getAccountsByType(AccountMirakel.ACCOUNT_TYPE_MIRAKEL)[0];
                final String t = AccountManager.get(this.context).getUserData(
                                     a, DefinitionsHelper.BUNDLE_SERVER_TYPE);
                if (t.equals(DefinitionsHelper.TYPE_TW_SYNC)) {
                    type = ACCOUNT_TYPES.TASKWARRIOR;
                    accountname = a.name;
                }
            }
            ContentValues cv = new ContentValues();
            cv.put(ModelBase.NAME, accountname);
            cv.put(AccountMirakel.TYPE, type.toInt());
            cv.put(AccountMirakel.ENABLED, true);
            final long accountId = db.insert(AccountMirakel.TABLE, null, cv);
            db.execSQL("ALTER TABLE " + ListMirakel.TABLE + " add column "
                       + ListMirakel.ACCOUNT_ID + " REFERENCES "
                       + AccountMirakel.TABLE + " (" + ModelBase.ID
                       + ") ON DELETE CASCADE ON UPDATE CASCADE DEFAULT "
                       + accountId + "; ");
            // add progress
        case 25:
            db.execSQL("ALTER TABLE " + Task.TABLE
                       + " add column progress int NOT NULL default 0;");
            // Add some columns for caldavsync
        case 26:
            createCalDavExtraTable(db);
        case 27:
            db.execSQL("UPDATE " + Task.TABLE + " SET " + Task.PROGRESS
                       + "=100 WHERE " + Task.DONE + "= 1 AND " + Task.RECURRING
                       + "=-1");
        case 28:
            db.execSQL("ALTER TABLE " + Semantic.TABLE
                       + " add column weekday int;");
            final String[] weekdays = this.context.getResources()
                                      .getStringArray(R.array.weekdays);
            for (int i = 1; i < weekdays.length; i++) { // Ignore first element
                db.execSQL("INSERT INTO " + Semantic.TABLE + " ("
                           + Semantic.CONDITION + "," + Semantic.WEEKDAY
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
            // also save the time of a due-date
        case 30:
            db.execSQL("UPDATE " + Task.TABLE + " set " + Task.DUE + "="
                       + Task.DUE + "||' 00:00:00'");
            // save all times in tasktable as utc-unix-seconds
        case 31:
            updateTimesToUTC(db);
            // move tw-sync-key to db
            // move tw-certs into accountmanager
        case 32:
            db.execSQL("ALTER TABLE " + AccountMirakel.TABLE + " add column "
                       + AccountMirakel.SYNC_KEY + " STRING DEFAULT '';");
            String ca = null,
                   client = null,
                   clientKey = null;
            final File caCert = new File(FileUtils.getMirakelDir()
                                         + "ca.cert.pem");
            final File userCert = new File(FileUtils.getMirakelDir()
                                           + "client.cert.pem");
            final File userKey = new File(FileUtils.getMirakelDir()
                                          + "client.key.pem");
            try {
                ca = FileUtils.readFile(caCert);
                client = FileUtils.readFile(userCert);
                clientKey = FileUtils.readFile(userKey);
                caCert.delete();
                userCert.delete();
                userKey.delete();
            } catch (final IOException e) {
                Log.wtf(TAG, "ca-files not found", e);
            }
            final AccountManager accountManager = AccountManager
                                                  .get(this.context);
            Cursor c = db.query(AccountMirakel.TABLE,
                                AccountMirakel.allColumns, null, null, null, null, null);
            final List<AccountMirakel> accounts = AccountMirakel.cursorToAccountList(c);
            c.close();
            for (final AccountMirakel a : accounts) {
                if (a.getType() == ACCOUNT_TYPES.TASKWARRIOR) {
                    final Account account = a.getAndroidAccount(this.context);
                    if (account == null) {
                        db.delete(AccountMirakel.TABLE, ModelBase.ID + "=?",
                                  new String[] { a.getId() + "" });
                        continue;
                    }
                    a.setSyncKey(accountManager.getPassword(account));
                    db.update(AccountMirakel.TABLE, a.getContentValues(), ModelBase.ID
                              + "=?", new String[] { a.getId() + "" });
                    if (ca != null && client != null && clientKey != null) {
                        accountManager.setUserData(account,
                                                   DefinitionsHelper.BUNDLE_CERT, ca);
                        accountManager.setUserData(account,
                                                   DefinitionsHelper.BUNDLE_CERT_CLIENT, client);
                        accountManager.setUserData(account,
                                                   DefinitionsHelper.BUNDLE_KEY_CLIENT, clientKey);
                    }
                }
            }
        case 33:
            db.execSQL("UPDATE " + SpecialList.TABLE + " SET "
                       + SpecialList.WHERE_QUERY + "=replace("
                       + SpecialList.WHERE_QUERY
                       + ",'date(due',\"date(due,'unixepoch'\")");
        case 34:
            Cursor cursor = db.query(SpecialList.TABLE, new String[] { ModelBase.ID,
                                     SpecialList.WHERE_QUERY
                                                                     }, null, null, null, null, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
                 .moveToNext()) {
                final int id = cursor.getInt(0);
                final ContentValues contentValues = new ContentValues();
                final String[] where = cursor.getString(1).toLowerCase()
                                       .split("and");
                final Map<String, SpecialListsBaseProperty> whereMap = new HashMap<>();
                for (final String p : where) {
                    try {
                        if (p.contains(Task.LIST_ID)) {
                            whereMap.put(Task.LIST_ID, CompatibilityHelper
                                         .getSetProperty(p,
                                                         SpecialListsListProperty.class,
                                                         Task.LIST_ID));
                        } else if (p.contains(ModelBase.NAME)) {
                            whereMap.put(ModelBase.NAME,
                                         CompatibilityHelper.getStringProperty(p,
                                                 SpecialListsNameProperty.class,
                                                 ModelBase.NAME));
                        } else if (p.contains(Task.PRIORITY)) {
                            whereMap.put(Task.PRIORITY, CompatibilityHelper
                                         .getSetProperty(p,
                                                         SpecialListsPriorityProperty.class,
                                                         Task.PRIORITY));
                        } else if (p.contains(Task.DONE)) {
                            whereMap.put(Task.DONE,
                                         CompatibilityHelper.getDoneProperty(p));
                        } else if (p.contains(Task.DUE)) {
                            whereMap.put(Task.DUE,
                                         CompatibilityHelper.getDueProperty(p));
                        } else if (p.contains(Task.CONTENT)) {
                            whereMap.put(Task.CONTENT, CompatibilityHelper
                                         .getStringProperty(p,
                                                            SpecialListsContentProperty.class,
                                                            Task.CONTENT));
                        } else if (p.contains(Task.REMINDER)) {
                            whereMap.put(Task.REMINDER,
                                         CompatibilityHelper.getReminderProperty(p));
                        }
                    } catch (final TransformerException e) {
                        Log.w(TAG, "due cannot be transformed", e);
                    }
                }
                contentValues.put(SpecialList.WHERE_QUERY,
                                  SpecialList.serializeWhere(whereMap));
                db.update(SpecialList.TABLE, contentValues, ModelBase.ID + "=?",
                          new String[] { id + "" });
            }
            cursor.close();
        case 35:
            am = AccountManager.get(this.context);
            for (final Account a : am
                 .getAccountsByType(AccountMirakel.ACCOUNT_TYPE_MIRAKEL)) {
                clientKey = am.getUserData(a,
                                           DefinitionsHelper.BUNDLE_KEY_CLIENT);
                if (clientKey != null && clientKey.trim().length() != 0) {
                    am.setPassword(a, clientKey
                                   + "\n:" + am.getPassword(a));
                }
            }
        case 36:
            cursor = db.query(FileMirakel.TABLE,
                              new String[] { "_id", "path" }, null, null, null, null,
                              null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    final File f = new File(cursor.getString(1));
                    if (f.exists()) {
                        cv = new ContentValues();
                        cv.put("path", Uri.fromFile(f).toString());
                        db.update(FileMirakel.TABLE, cv, "_id=?",
                                  new String[] { cursor.getString(0) });
                    } else {
                        db.delete(FileMirakel.TABLE, "_id=?",
                                  new String[] { cursor.getString(0) });
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        case 37:
            // Introduce tags
            db.execSQL("CREATE TABLE " + Tag.TABLE + " (" + ModelBase.ID
                       + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ModelBase.NAME
                       + " TEXT NOT NULL, " + Tag.DARK_TEXT
                       + " INTEGER NOT NULL DEFAULT 0, " + "color_a"
                       + " INTEGER NOT NULL DEFAULT 0, " + "color_b"
                       + " INTEGER NOT NULL DEFAULT 0, " + "color_g"
                       + " INTEGER NOT NULL DEFAULT 0, " + "color_r"
                       + " INTEGER NOT NULL DEFAULT 0);");
            db.execSQL("CREATE TABLE "
                       + Tag.TAG_CONNECTION_TABLE
                       + " ("
                       + ModelBase.ID
                       + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                       + " task_id INTEGER REFERENCES "
                       + Task.TABLE
                       + " ("
                       + ModelBase.ID
                       + ") "
                       + "ON DELETE CASCADE ON UPDATE CASCADE,tag_id INTEGER REFERENCES "
                       + Tag.TABLE + " (" + ModelBase.ID
                       + ") ON DELETE CASCADE ON UPDATE CASCADE);");
            cursor = db.query(Task.TABLE, new String[] { "_id",
                              "additional_entries"
                                                       },
                              "additional_entries LIKE '%\"tags\":[\"%'", null, null,
                              null, null);
            if (cursor.getCount() > 0) {
                int count = 0;
                cursor.moveToFirst();
                do {
                    final int taskId = cursor.getInt(0);
                    final Map<String, String> entryMap = Task
                                                         .parseAdditionalEntries(cursor.getString(1));
                    String entries = entryMap.get("tags").trim();
                    entries = entries.replace("[", "");
                    entries = entries.replace("]", "");
                    entries = entries.replace("\"", "");
                    final String[] tags = entries.split(",");
                    for (final String tag : tags) {
                        c = db.query(Tag.TABLE, new String[] { ModelBase.ID }, ModelBase.NAME
                                     + "=?", new String[] { tag }, null, null, null);
                        int tagId;
                        if (c.getCount() > 0) {
                            c.moveToFirst();
                            tagId = c.getInt(0);
                        } else {
                            // create tag;
                            final int color = Tag.getNextColor(count++,
                                                               this.context);
                            cv = new ContentValues();
                            cv.put(ModelBase.NAME, tag);
                            cv.put("color_r", Color.red(color));
                            cv.put("color_g", Color.green(color));
                            cv.put("color_b", Color.blue(color));
                            cv.put("color_a", Color.alpha(color));
                            tagId = (int) db.insert(Tag.TABLE, null, cv);
                        }
                        cv = new ContentValues();
                        cv.put("tag_id", tagId);
                        cv.put("task_id", taskId);
                        db.insert(Tag.TAG_CONNECTION_TABLE, null, cv);
                        entryMap.remove("tags");
                        cv = new ContentValues();
                        cv.put(Task.ADDITIONAL_ENTRIES,
                               Task.serializeAdditionalEntries(entryMap));
                        db.update(Task.TABLE, cv, ModelBase.ID + "=?",
                                  new String[] { taskId + "" });
                    }
                } while (cursor.moveToNext());
            }
            if (!DefinitionsHelper.freshInstall) {
                final List<Integer> parts = MirakelCommonPreferences
                                            .loadIntArray("task_fragment_adapter_settings");
                parts.add(8);// hardcode tags, because of dependencies
                MirakelCommonPreferences.saveIntArray(
                    "task_fragment_adapter_settings", parts);
            }
            // refactor recurrence to follow the taskwarrior method
        case 38:
            createTableRecurrenceTW(db);
        case 39:
            db.execSQL("ALTER TABLE " + Task.TABLE + " add column "
                       + Task.RECURRING_SHOWN + " INTEGER DEFAULT 1;");
            c = db.query(Task.TABLE, Task.allColumns,
                         "additional_entries LIKE ?",
                         new String[] { "%\"status\":\"recurring\"%" }, null, null,
                         null);
            final Map<Task, List<Task>> recurring = new HashMap<>();
            for (c.moveToFirst(); c.moveToNext();) {
                final Task t = Task.cursorToTask(c);
                final String recurString = t.getAdditionalString("recur");
                if (recurString == null) {
                    continue;
                }
                // check if is childtask
                if (t.existAdditional("parent")) {
                    final Task master = Task.getByUUID(t
                                                       .getAdditionalString("parent"));
                    List<Task> list;
                    if (recurring.containsKey(master)) {
                        list = recurring.get(master);
                    } else {
                        list = new ArrayList<>();
                    }
                    list.add(t);
                    recurring.put(master, list);
                } else if (!recurring.containsKey(t)) {// its recurring master
                    recurring.put(t, new ArrayList<Task>());
                }
                t.setRecurrence(TaskDeserializer.parseTaskWarriorRecurrence(
                                    recurString).getId());
                t.save();
            }
            String idsToHidde = "";
            boolean first = true;
            for (final Entry<Task, List<Task>> rec : recurring.entrySet()) {
                if (rec.getValue().size() == 0) {
                    continue;
                }
                Task newest = rec.getValue().get(0);
                for (final Task t : rec.getValue()) {
                    cv = new ContentValues();
                    cv.put("parent", rec.getKey().getId());
                    cv.put("child", t.getId());
                    final int counter = t.getAdditionalInt("imask");
                    cv.put("offsetCount", counter);
                    cv.put("offset", counter * t.getRecurring().getInterval());
                    db.insert(Recurring.TW_TABLE, null, cv);
                    final int newestOffset = newest.getAdditionalInt("imask");
                    final int currentOffset = t.getAdditionalInt("imask");
                    if (newestOffset < currentOffset) {
                        if (first) {
                            first = false;
                        } else {
                            idsToHidde += ",";
                        }
                        idsToHidde += newest.getId();
                        newest = t;
                    }
                }
            }
            if (!idsToHidde.equals("")) {
                cv = new ContentValues();
                cv.put(Task.RECURRING_SHOWN, false);
                db.update(Task.TABLE, cv, "_id IN (?)",
                          new String[] { idsToHidde });
            }
            c.close();
        case 40:
            // Update settings
            updateSettings();
            // Alter tag table
            db.execSQL("ALTER TABLE tag RENAME to tmp_tags;");
            db.execSQL("CREATE TABLE " + Tag.TABLE + " ("
                       + ModelBase.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                       + ModelBase.NAME + " TEXT NOT NULL, "
                       + Tag.DARK_TEXT + " INTEGER NOT NULL DEFAULT 0, "
                       + Tag.BACKGROUND_COLOR + " INTEGER NOT NULL DEFAULT 0);");
            db.execSQL("INSERT INTO " + Tag.TABLE +
                       " (_id,name,dark_text) SELECT _id,name,dark_text FROM tmp_tags;");
            String[] tagColumns = new String[] {"_id", "color_a", "color_r", "color_g", "color_b"};
            Cursor tagCursor = db.query("tmp_tags", tagColumns, null, null, null, null, null);
            if (tagCursor.moveToFirst()) {
                do {
                    int i = 0;
                    int id = tagCursor.getInt(i++);
                    int rgba = tagCursor.getInt(i++);
                    int rgbr = tagCursor.getInt(i++);
                    int rgbg = tagCursor.getInt(i++);
                    int rgbb = tagCursor.getInt(i);
                    int newColor = Color.argb(rgba, rgbr, rgbg, rgbb);
                    Cursor tagC = db.query(Tag.TABLE, Tag.allColumns, ModelBase.ID
                                           + "=?", new String[] {id + ""}, null, null, null);
                    if (tagC.moveToFirst()) {
                        Tag newTag = new Tag(tagC);
                        tagC.close();
                        newTag.setBackgroundColor(newColor);
                        db.update(Tag.TABLE, newTag.getContentValues(), ModelBase.ID + "=?", new String[] {newTag.getId() + ""});
                    }
                } while (tagCursor.moveToNext());
            }
            db.execSQL("DROP TABLE tmp_tags;");
            db.execSQL("create unique index tag_unique ON tag (name);");
        default:
            break;
        }
    }


    private void updateSettings() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settingsIntToLong(settings, "defaultAccountID");
        settingsIntToLong(settings, "subtaskAddToList");
    }
    private void settingsIntToLong(SharedPreferences settings, String key) {
        final int i = settings.getInt(key, -1);
        if (i != -1) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(key, Long.valueOf(i));
            editor.commit();
        }
    }


    private static void createTableRecurrenceTW(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "
                   + Recurring.TW_TABLE
                   + "("
                   + ModelBase.ID
                   + " INTEGER PRIMARY KEY,"
                   + "parent INTEGER REFERENCES "
                   + Task.TABLE
                   + " ("
                   + ModelBase.ID
                   + ") "
                   + "ON DELETE CASCADE ON UPDATE CASCADE,child INTEGER REFERENCES "
                   + Task.TABLE
                   + " ("
                   + ModelBase.ID
                   + ") "
                   + "ON DELETE CASCADE ON UPDATE CASCADE ,offset INTEGER,offsetCount INTEGER)");
    }

    private static void createCalDavExtraTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE caldav_extra(" + ModelBase.ID + " INTEGER PRIMARY KEY,"
                   + "ETAG TEXT," + "SYNC_ID TEXT DEFAULT NULL, "
                   + "REMOTE_NAME TEXT)");
    }

    private static void createFileTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + FileMirakel.TABLE + " (" + ModelBase.ID
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, " + "task" + ModelBase.ID
                   + " INTEGER NOT NULL DEFAULT 0, " + "name TEXT, " + "path TEXT"
                   + ")");
    }

    private static void createSubtaskTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Task.SUBTASK_TABLE + " (" + ModelBase.ID
                   + " INTEGER PRIMARY KEY AUTOINCREMENT," + "parent" + ModelBase.ID
                   + " INTEGER REFERENCES " + Task.TABLE + " (" + ModelBase.ID
                   + ") ON DELETE CASCADE ON UPDATE CASCADE," + "child" + ModelBase.ID
                   + " INTEGER REFERENCES " + Task.TABLE + " (" + ModelBase.ID
                   + ") ON DELETE CASCADE ON UPDATE CASCADE);");
    }

    private static void updateTimesToUTC(final SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Task.TABLE + " RENAME TO tmp_tasks;");
        createTasksTable(db);
        final int offset = DateTimeHelper.getTimeZoneOffset(false,
                           new GregorianCalendar());
        db.execSQL("Insert INTO tasks (_id, uuid, list_id, name, "
                   + "content, done, due, reminder, priority, created_at, "
                   + "updated_at, sync_state, additional_entries, recurring, "
                   + "recurring_reminder, progress) "
                   + "Select _id, uuid, list_id, name, content, done, "
                   + "strftime('%s',"
                   + Task.DUE
                   + ") - ("
                   + offset
                   + "), "
                   + getStrFtime(Task.REMINDER, offset)
                   + ", priority, "
                   + getStrFtime(CREATED_AT, offset)
                   + ", "
                   + getStrFtime(UPDATED_AT, offset)
                   + ", "
                   + "sync_state, additional_entries, recurring, recurring_reminder, progress FROM tmp_tasks;");
        db.execSQL("DROP TABLE tmp_tasks");
    }

    private static String getStrFtime(final String col, final int offset) {
        String ret = "strftime('%s',substr(" + col + ",0,11)||' '||substr("
                     + col + ",12,2)||':'||substr(" + col + ",14,2)||':'||substr("
                     + col + ",16,2)) - (" + offset + ")";
        if (col.equals(CREATED_AT) || col.equals(UPDATED_AT)) {
            ret = "CASE WHEN (" + ret
                  + ") IS NULL THEN strftime('%s','now') ELSE (" + ret
                  + ") END";
        }
        return ret;
    }
}
