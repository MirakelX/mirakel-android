/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.SparseIntArray;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialListsWhereDeserializer;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList;
import de.azapps.mirakel.model.list.meta.SpecialListsContentProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.query_builder.Cursor2List;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.CursorWrapper;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 58;

    private static final String TAG = "DatabaseHelper";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String SYNC_STATE_FIELD = "sync_state";

    private static void createAccountTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE account (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, name"
                   + " TEXT NOT NULL, content TEXT, "
                   + "enabled INTEGER NOT NULL DEFAULT 0, "
                   + "type INTEGER NOT NULL DEFAULT "
                   + ACCOUNT_TYPES.LOCAL.toInt() + ')');
    }

    protected static void createTasksTableOLD(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, list_id"
                   + " INTEGER REFERENCES lists (_id"
                   + ") ON DELETE CASCADE ON UPDATE CASCADE, name"
                   + " TEXT NOT NULL, content TEXT, done"
                   + " INTEGER NOT NULL DEFAULT 0, priority"
                   + " INTEGER NOT NULL DEFAULT 0, due STRING, "
                   + "created_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + "updated_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + "sync_state INTEGER DEFAULT 1)");
    }

    private final Context context;

    protected static void createTasksTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, list_id"
                   + " INTEGER REFERENCES lists (_id"
                   + ") ON DELETE CASCADE ON UPDATE CASCADE, name"
                   + " TEXT NOT NULL, content TEXT, done"
                   + " INTEGER NOT NULL DEFAULT 0, priority"
                   + " INTEGER NOT NULL DEFAULT 0, due STRING, "
                   + "created_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + "updated_at INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + "sync_state INTEGER DEFAULT 1,"
                   + "reminder INTEGER,uuid"
                   + " TEXT NOT NULL DEFAULT '',additional_entries"
                   + " TEXT NOT NULL DEFAULT '',recurring"
                   + " INTEGER DEFAULT '-1',recurring_reminder"
                   + " INTEGER DEFAULT '-1',progress"
                   + " INTEGER NOT NULL default 0)");
    }

    private DatabaseHelper(final Context ctx, final String name) {
        super(ctx, name, null, DATABASE_VERSION);
        this.context = ctx;
    }

    private static DatabaseHelper databaseHelperSingleton;


    public static DatabaseHelper getDatabaseHelper(final Context context) {
        if (databaseHelperSingleton == null) {
            databaseHelperSingleton = new DatabaseHelper(context, getDBName(context));
        }
        return databaseHelperSingleton;
    }

    @VisibleForTesting
    public static void resetDB() {
        synchronized (databaseHelperSingleton) {
            if (databaseHelperSingleton != null) {
                String path = databaseHelperSingleton.getWritableDatabase().getPath();
                databaseHelperSingleton.close();
                new File(path).delete();
            }
            databaseHelperSingleton = null;
        }
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
        db.execSQL("CREATE TABLE special_lists (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, name"
                   + " TEXT NOT NULL, active"
                   + " INTEGER NOT NULL DEFAULT 0, whereQuery"
                   + " STRING NOT NULL DEFAULT '', sort_by"
                   + " INTEGER NOT NULL DEFAULT " + ListMirakel.SORT_BY.OPT.getShort() + ", "
                   + "sync_state INTEGER DEFAULT 1"
                   + ", def_list INTEGER, "
                   + "def_date INTEGER,color"
                   + " INTEGER, lft INTEGER ,"
                   + "rgt INTEGER)");
        db.execSQL("INSERT INTO special_lists (name" +  ','
                   + "active" + ',' + "whereQuery" + ','
                   + "lft, rgt) VALUES (" + '\''
                   + this.context.getString(R.string.list_all) + "',1,'"
                   + "done=0',1,2)");
        db.execSQL("INSERT INTO special_lists (name" +  ','
                   + "active" + ',' + "whereQuery" + ','
                   + "lft, rgt" + ','
                   + "def_date) VALUES (" + '\''
                   + this.context.getString(R.string.list_today) + "',1,'"
                   + "due not null and done=0 and date("
                   + "due)<=date(\"now\",\"localtime\")',3,4,0)");
        db.execSQL("INSERT INTO special_lists (name" +  ','
                   + "active" + ',' + "whereQuery" + ','
                   + "lft, rgt" + ','
                   + "def_date) VALUES (" + '\''
                   + this.context.getString(R.string.list_week) + "',1,'"
                   + "due not null and done=0 and date("
                   + "due"
                   + ")<=date(\"now\",\"+7 day\",\"localtime\")',5,6,7)");
        db.execSQL("INSERT INTO special_lists (name" + ','
                   + "active,whereQuery" + ','
                   + "lft, rgt" + ','
                   + "def_date) VALUES (" + '\''
                   + this.context.getString(R.string.list_overdue) + "',1,'"
                   + "due not null and done=0 and date("
                   + "due"
                   + ")<=date(\"now\",\"-1 day\",\"localtime\")',7,8,-1)");
    }

    private void createRecurringTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "
                   + "recurring"
                   + " (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
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
        db.execSQL("INSERT INTO recurring"
                   + "(days,label,for_due) VALUES (1,'"
                   + this.context.getString(R.string.daily) + "',1);");
        db.execSQL("INSERT INTO recurring"
                   + "(days,label,for_due) VALUES (2,'"
                   + this.context.getString(R.string.second_day) + "',1);");
        db.execSQL("INSERT INTO recurring"
                   + "(days,label,for_due) VALUES (7,'"
                   + this.context.getString(R.string.weekly) + "',1);");
        db.execSQL("INSERT INTO recurring"
                   + "(days,label,for_due) VALUES (14,'"
                   + this.context.getString(R.string.two_weekly) + "',1);");
        db.execSQL("INSERT INTO recurring"
                   + "(months,label,for_due) VALUES (1,'"
                   + this.context.getString(R.string.monthly) + "',1);");
        db.execSQL("INSERT INTO recurring"
                   + "(years,label,for_due) VALUES (1,'"
                   + this.context.getString(R.string.yearly) + "',1);");
        db.execSQL("INSERT INTO recurring"
                   + "(hours,label,for_due) VALUES (1,'"
                   + this.context.getString(R.string.hourly) + "',0);");
        db.execSQL("INSERT INTO recurring"
                   + "(minutes,label,for_due) VALUES (1,'"
                   + this.context.getString(R.string.minutly) + "',0);");
    }

    private void createSemanticTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE semantic_conditions (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                   + "condition TEXT NOT NULL, due INTEGER, "
                   + "priority INTEGER, list INTEGER,default_list_id"
                   + " INTEGER, weekday INTEGER);");
        db.execSQL("INSERT INTO semantic_conditions (condition,due) VALUES "
                   + "(\""
                   + this.context.getString(R.string.today).toLowerCase(
                       Helpers.getLocale(this.context))
                   + "\",0);"
                   + "INSERT INTO semantic_conditions (condition,due) VALUES (\""
                   + this.context.getString(R.string.tomorrow).toLowerCase(
                       Helpers.getLocale(this.context)) + "\",1);");
        final String[] weekdays = this.context.getResources().getStringArray(
                                      R.array.weekdays);
        for (int i = 1; i < weekdays.length; i++) { // Ignore first element
            db.execSQL("INSERT INTO semantic_conditions ("
                       + "condition" + ',' + "weekday"
                       + ") VALUES (?, " + i + ')', new String[] { weekdays[i] });
        }
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        DefinitionsHelper.freshInstall = true;
        createRecurringTable(db);
        createSemanticTable(db);
        createAccountTable(db);
        final String accountname = this.context
                                   .getString(R.string.local_account);
        ContentValues cv = new ContentValues();
        cv.put("name", accountname);
        cv.put("type", -1);
        cv.put("enabled", true);
        final long accountId = db.insert("account", null, cv);
        createListsTable(db, accountId);
        createTasksTable(db);
        createSubtaskTable(db);
        createFileTable(db);
        createCalDavExtraTable(db);
        // Add defaults
        db.execSQL("INSERT INTO lists (name" +  ','
                   + "lft" + ',' + "rgt) VALUES ('"
                   + this.context.getString(R.string.inbox) + "',0,1)");
        db.execSQL("INSERT INTO tasks (list_id" + ','
                   + "name) VALUES (1,'"
                   + this.context.getString(R.string.first_task) + "')");
        createSpecialListsTable(db);
        final String[] lists = this.context.getResources().getStringArray(
                                   R.array.demo_lists);
        for (int i = 0; i < lists.length; i++) {
            db.execSQL("INSERT INTO lists (name" +  ','
                       + "lft" + ',' + "rgt) VALUES ('"
                       + lists[i] + "'," + (i + 2) + ',' + (i + 3) + ')');
        }
        MirakelInternalContentProvider.init(db);
        onUpgrade(db, 32, DATABASE_VERSION);
        if (MirakelCommonPreferences.isDemoMode()) {
            Semantic.init(context);
            final String[] tasks = this.context.getResources().getStringArray(
                                       R.array.demo_tasks);
            final String[] task_lists = { lists[1], lists[1], lists[1],
                                          lists[0], lists[2], lists[2]
                                        };
            final DateTime[] dues = new DateTime[6];
            dues[0] = new DateTime();
            dues[1] = null;
            dues[2] = new DateTime().withDayOfWeek(DateTimeConstants.MONDAY);
            dues[3] = new DateTime();
            dues[4] = null;
            dues[5] = null;
            final int[] priorities = { 2, -1, 1, 2, 0, 0 };
            for (int i = 0; i < tasks.length; i++) {
                final Task t = new Task(tasks[i], ListMirakel.findByName(task_lists[i]).get());
                t.setDue(fromNullable(dues[i]));
                t.setPriority(priorities[i]);
                t.setSyncState(SYNC_STATE.ADD);
                try {
                    cv = t.getContentValues();
                } catch (DefinitionsHelper.NoSuchListException e) {
                    Log.wtf(TAG, "missing list", e);
                }
                cv.remove("_id");
                db.insert("tasks", null, cv);
            }
        }
        MirakelInternalContentProvider.init(db);
    }

    private static void createListsTable(final SQLiteDatabase db,
                                         final long accountId) {
        db.execSQL("CREATE TABLE lists (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, name"
                   + " TEXT NOT NULL, sort_by"
                   + " INTEGER NOT NULL DEFAULT 0, created_at"
                   + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at"
                   + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + "sync_state INTEGER DEFAULT 1"
                   + ", lft INTEGER, rgt"
                   + " INTEGER , color INTEGER,"
                   + "account_id REFERENCES "
                   + "account (_id"
                   + ") ON DELETE CASCADE ON UPDATE CASCADE DEFAULT " + accountId
                   + ')');
    }

    @Override
    public void onDowngrade(final SQLiteDatabase db, final int oldVersion,
                            final int newVersion) {
        Log.e(TAG, "You are downgrading the Database!");
        // This is only for developers… There shouldn't happen bad things if you
        // use a database with a higher version.
    }

    @SuppressWarnings("fallthrough")
    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                          final int newVersion) {
        Log.e(DatabaseHelper.class.getName(),
              "Upgrading database from version " + oldVersion + " to "
              + newVersion);
        try {
            ExportImport.exportDB(this.context);
        } catch (final RuntimeException e) {
            Log.w(TAG, "Cannot backup database", e);
        }
        switch (oldVersion) {
        case 1:// Nothing, Startversion
        case 2:
            // Add sync-state
            db.execSQL("Alter Table tasks add column "
                       + "sync_state INTEGER DEFAULT 1"
                       + ';');
            db.execSQL("Alter Table lists add column "
                       + "sync_state INTEGER DEFAULT 1"
                       + ';');
            db.execSQL("CREATE TABLE settings (_id"
                       + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                       + "server TEXT NOT NULL,user TEXT NOT NULL,"
                       + "password TEXT NOT NULL" + ')');
            db.execSQL("INSERT INTO settings (_id"
                       + ",server,user,password)VALUES ('0','localhost','','')");
        case 3:
            // Add lft,rgt to lists
            // Set due to null, instate of 1970 in Tasks
            // Manage fromate of updated_at created_at in Tasks/Lists
            // drop settingssettings
            db.execSQL("UPDATE tasks set due"
                       + "='null' where due='1970-01-01'");
            final String newDate = new SimpleDateFormat(
                this.context.getString(R.string.dateTimeFormat), Locale.US)
            .format(new Date());
            db.execSQL("UPDATE tasks set created_at='"
                       + newDate + '\'');
            db.execSQL("UPDATE tasks set updated_at='"
                       + newDate + '\'');
            db.execSQL("UPDATE lists set created_at"
                       + "='" + newDate + '\'');
            db.execSQL("UPDATE lists set updated_at"
                       + "='" + newDate + '\'');
            db.execSQL("Drop TABLE IF EXISTS settings");
        case 4:
            /*
             * Remove NOT NULL from Task-Table
             */
            db.execSQL("ALTER TABLE tasks RENAME TO tmp_tasks;");
            createTasksTableOLD(db);
            String cols = "_id, list_id, name, "
                          + "done" + ',' + "priority" + ',' + "due" + ','
                          + "created_at" + ',' + "updated_at" + ',' + "sync_state";
            db.execSQL("INSERT INTO tasks (" + cols + ") " + cols
                       + "FROM tmp_tasks;");
            db.execSQL("DROP TABLE tmp_tasks");
            db.execSQL("UPDATE tasks set due"
                       + "=null where due='' OR due"
                       + "='null'");
            /*
             * Update Task-Table
             */
            db.execSQL("Alter Table lists add column "
                       + "lft INTEGER;");
            db.execSQL("Alter Table lists add column "
                       + "rgt INTEGER;");
        case 5:
            createSpecialListsTable(db);
            db.execSQL("update lists set "
                       + "lft"
                       + "=(select count(*) from (select * from "
                       + "lists) as a where a._id" + '<'
                       + "lists" + '.' + "_id)*2 +1;");
            db.execSQL("update lists set "
                       + "rgt" + '=' + "lft+1;");
        case 6:
            /*
             * Remove NOT NULL
             */
            db.execSQL("ALTER TABLE tasks RENAME TO tmp_tasks;");
            createTasksTableOLD(db);
            cols = "_id, list_id, name, done"
                   + ',' + "priority" + ',' + "due" + ',' + "created_at"
                   + ',' + "updated_at" + ',' + "sync_state";
            db.execSQL("INSERT INTO tasks (" + cols + ") "
                       + "SELECT " + cols + "FROM tmp_tasks;");
            db.execSQL("DROP TABLE tmp_tasks");
            db.execSQL("UPDATE tasks set due"
                       + "=null where due=''");
        case 7:
            /*
             * Add default list and default date for SpecialLists
             */
            db.execSQL("Alter Table special_lists add column "
                       + "def_list INTEGER;");
            db.execSQL("Alter Table special_lists add column "
                       + "def_date INTEGER;");
        case 8:
            /*
             * Add reminders for Tasks
             */
            db.execSQL("Alter Table tasks add column "
                       + "reminder INTEGER;");
        case 9:
            /*
             * Update Special Lists Table
             */
            db.execSQL("UPDATE special_lists SET def_date"
                       + "=0 where _id=2 and def_date"
                       + "=null");
            db.execSQL("UPDATE special_lists SET def_date"
                       + "=7 where _id=3 and def_date"
                       + "=null");
            db.execSQL("UPDATE special_lists SET def_date"
                       + "=-1, active=0 where _id"
                       + "=4 and def_date=null");
        case 10:
            /*
             * Add UUID to Task
             */
            db.execSQL("Alter Table tasks add column uuid"
                       + " TEXT NOT NULL DEFAULT '';");
        // MainActivity.updateTasksUUID = true; TODO do we need this
        // anymore?
        // Don't remove this version-gap
        case 13:
            db.execSQL("Alter Table tasks add column "
                       + "additional_entries TEXT NOT NULL DEFAULT '';");
        case 14:// Add Sematic
            db.execSQL("CREATE TABLE semantic_conditions (_id"
                       + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                       + "condition TEXT NOT NULL, due INTEGER, "
                       + "priority INTEGER, list INTEGER);");
            db.execSQL("INSERT INTO semantic_conditions (condition,due) VALUES "
                       + "(\""
                       + this.context.getString(R.string.today).toLowerCase(
                           Helpers.getLocale(this.context))
                       + "\",0);"
                       + "INSERT INTO semantic_conditions (condition,due) VALUES (\""
                       + this.context.getString(R.string.tomorrow).toLowerCase(
                           Helpers.getLocale(this.context)) + "\",1);");
        case 15:// Add Color
            db.execSQL("Alter Table lists add column "
                       + "color INTEGER;");
            db.execSQL("Alter Table special_lists add column "
                       + "color INTEGER;");
        case 16:// Add File
            createFileTable(db);
        case 17:// Add Subtask
            createSubtaskTable(db);
        case 18:// Modify Semantic
            db.execSQL("ALTER TABLE semantic_conditions"
                       + " add column default_list_id INTEGER");
            db.execSQL("update semantic_conditions SET condition=LOWER(condition);");
        case 19:// Make Specialist sortable
            db.execSQL("ALTER TABLE special_lists add column  "
                       + "lft INTEGER;");
            db.execSQL("ALTER TABLE special_lists add column  "
                       + "rgt INTEGER ;");
            db.execSQL("update special_lists set "
                       + "lft"
                       + "=(select count(*) from (select * from "
                       + "special_lists) as a where a._id" + '<'
                       + "special_lists" + '.' + "_id)*2 +1;");
            db.execSQL("update special_lists set "
                       + "rgt" + '=' + "lft+1;");
        case 20:// Add Recurring
            db.execSQL("CREATE TABLE recurring (_id"
                       + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                       + "years INTEGER DEFAULT 0,months INTEGER DEFAULT 0,"
                       + "days INTEGER DEFAULT 0,hours INTEGER DEFAULT 0,"
                       + "minutes INTEGER DEFAULT 0,"
                       + "for_due INTEGER DEFAULT 0,label STRING);");
            db.execSQL("ALTER TABLE tasks add column "
                       + "recurring INTEGER DEFAULT '-1';");
            db.execSQL("INSERT INTO recurring"
                       + "(days,label,for_due) VALUES (1,'"
                       + this.context.getString(R.string.daily) + "',1);");
            db.execSQL("INSERT INTO recurring"
                       + "(days,label,for_due) VALUES (2,'"
                       + this.context.getString(R.string.second_day) + "',1);");
            db.execSQL("INSERT INTO recurring"
                       + "(days,label,for_due) VALUES (7,'"
                       + this.context.getString(R.string.weekly) + "',1);");
            db.execSQL("INSERT INTO recurring"
                       + "(days,label,for_due) VALUES (14,'"
                       + this.context.getString(R.string.two_weekly) + "',1);");
            db.execSQL("INSERT INTO recurring"
                       + "(months,label,for_due) VALUES (1,'"
                       + this.context.getString(R.string.monthly) + "',1);");
            db.execSQL("INSERT INTO recurring"
                       + "(years,label,for_due) VALUES (1,'"
                       + this.context.getString(R.string.yearly) + "',1);");
            db.execSQL("INSERT INTO recurring"
                       + "(hours,label,for_due) VALUES (1,'"
                       + this.context.getString(R.string.hourly) + "',0);");
            db.execSQL("INSERT INTO recurring"
                       + "(minutes,label,for_due) VALUES (1,'"
                       + this.context.getString(R.string.minutly) + "',0);");
        case 21:
            db.execSQL("ALTER TABLE tasks add column "
                       + "recurring_reminder INTEGER DEFAULT '-1';");
        case 22:
            db.execSQL("ALTER TABLE recurring"
                       + " add column start_date String;");
            db.execSQL("ALTER TABLE recurring"
                       + " add column end_date String;");
        case 23:
            db.execSQL("ALTER TABLE recurring"
                       + " add column temporary int NOT NULL default 0;");
        // Add Accountmanagment
        case 24:
            createAccountTable(db);
            int type = -1;
            AccountManager am = AccountManager.get(this.context);
            String accountname = this.context.getString(R.string.local_account);
            if (am.getAccountsByType("de.azapps.mirakel").length > 0) {
                final Account a = am
                                  .getAccountsByType("de.azapps.mirakel")[0];
                final String t = AccountManager.get(this.context).getUserData(
                                     a, DefinitionsHelper.BUNDLE_SERVER_TYPE);
                if (t.equals("TaskWarrior")) {
                    type = 2;
                    accountname = a.name;
                }
            }
            ContentValues cv = new ContentValues();
            cv.put("name", accountname);
            cv.put("type", type);
            cv.put("enabled", true);
            final long accountId = db.insert("account", null, cv);
            db.execSQL("ALTER TABLE lists add column "
                       + "account_id REFERENCES "
                       + "account (_id"
                       + ") ON DELETE CASCADE ON UPDATE CASCADE DEFAULT "
                       + accountId + "; ");
        // add progress
        case 25:
            db.execSQL("ALTER TABLE tasks"
                       + " add column progress int NOT NULL default 0;");
        // Add some columns for caldavsync
        case 26:
            createCalDavExtraTable(db);
        case 27:
            db.execSQL("UPDATE tasks SET progress"
                       + "=100 WHERE done= 1 AND recurring"
                       + "=-1");
        case 28:
            db.execSQL("ALTER TABLE semantic_conditions"
                       + " add column weekday int;");
            final String[] weekdays = this.context.getResources()
                                      .getStringArray(R.array.weekdays);
            for (int i = 1; i < weekdays.length; i++) { // Ignore first element
                db.execSQL("INSERT INTO semantic_conditions ("
                           + "condition" + ',' + "weekday"
                           + ") VALUES (?, " + i + ')',
                           new String[] { weekdays[i] });
            }
        // add some options to reccuring
        case 29:
            db.execSQL("ALTER TABLE recurring"
                       + " add column isExact INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE recurring"
                       + " add column monday INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE recurring"
                       + " add column tuesday INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE recurring"
                       + " add column wednesday INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE recurring"
                       + " add column thursday INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE recurring"
                       + " add column friday INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE recurring"
                       + " add column saturday INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE recurring"
                       + " add column sunnday INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE recurring"
                       + " add column derived_from INTEGER DEFAULT NULL");
        // also save the time of a due-date
        case 30:
            db.execSQL("UPDATE tasks set due" + '='
                       + "due||' 00:00:00'");
        // save all times in tasktable as utc-unix-seconds
        case 31:
            updateTimesToUTC(db);
        // move tw-sync-key to db
        // move tw-certs into accountmanager
        case 32:
            db.execSQL("ALTER TABLE account add column "
                       + "sync_key STRING DEFAULT '';");
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
            List<AccountMirakel> accounts = new CursorWrapper(db.query("account",
                    AccountMirakel.allColumns, null, null, null, null,
                    null)).doWithCursor(new Cursor2List<>(AccountMirakel.class));
            for (final AccountMirakel a : accounts) {
                if (a.getType() == ACCOUNT_TYPES.TASKWARRIOR) {
                    final Account account = a.getAndroidAccount(this.context);
                    if (account == null) {
                        db.delete("account", "_id=?",
                                  new String[] {String.valueOf(a.getId())});
                        continue;
                    }
                    a.setSyncKey(fromNullable(accountManager.getPassword(account)));
                    db.update("account", a.getContentValues(), "_id"
                              + "=?", new String[] {String.valueOf(a.getId())});
                    if ((ca != null) && (client != null) && (clientKey != null)) {
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
            db.execSQL("UPDATE special_lists SET "
                       + "whereQuery=replace("
                       + "whereQuery"
                       + ",'date(due',\"date(due,'unixepoch'\")");
        case 34:
            new CursorWrapper(db.query("special_lists", new String[] { "_id",
                                       "whereQuery"
                                                                     }, null, null, null, null, null))
            .doWithCursor(new SpecialListsConverter(db));
        case 35:
            am = AccountManager.get(this.context);
            for (final Account a : am
                 .getAccountsByType("de.azapps.mirakel")) {
                clientKey = am.getUserData(a,
                                           DefinitionsHelper.BUNDLE_KEY_CLIENT);
                if ((clientKey != null) && !clientKey.trim().isEmpty()) {
                    am.setPassword(a, clientKey
                                   + "\n:" + am.getPassword(a));
                }
            }
        case 36:
            new CursorWrapper(db.query("files",
                                       new String[] { "_id", "path" }, null, null, null, null,
            null)).doWithCursor(new CursorWrapper.WithCursor() {
                @Override
                public void withOpenCursor(@NonNull CursorGetter getter) {
                    if (getter.getCount() > 0) {
                        getter.moveToFirst();
                        do {
                            final File f = new File(getter.getString("path"));
                            String[] id = new String[] {getter.getString("_id")};
                            if (f.exists()) {
                                final ContentValues cv = new ContentValues();
                                cv.put("path", Uri.fromFile(f).toString());
                                db.update("files", cv, "_id=?",
                                          id);
                            } else {
                                db.delete("files", "_id=?",
                                          id);
                            }
                        } while (getter.moveToNext());
                    }
                }
            });
        case 37:
            // Introduce tags
            db.execSQL("CREATE TABLE tag (_id"
                       + " INTEGER PRIMARY KEY AUTOINCREMENT, name"
                       + " TEXT NOT NULL, dark_text"
                       + " INTEGER NOT NULL DEFAULT 0, color_a"
                       + " INTEGER NOT NULL DEFAULT 0, color_b"
                       + " INTEGER NOT NULL DEFAULT 0, color_g"
                       + " INTEGER NOT NULL DEFAULT 0, color_r"
                       + " INTEGER NOT NULL DEFAULT 0);");
            db.execSQL("CREATE TABLE "
                       + "task_tag"
                       + " ("
                       + "_id"
                       + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                       + " task_id INTEGER REFERENCES "
                       + "tasks"
                       + " ("
                       + "_id"
                       + ") "
                       + "ON DELETE CASCADE ON UPDATE CASCADE,tag_id INTEGER REFERENCES "
                       + "tag (_id"
                       + ") ON DELETE CASCADE ON UPDATE CASCADE);");
            new CursorWrapper(db.query("tasks", new String[] { "_id",
                                       "additional_entries"
                                                             },
                                       "additional_entries LIKE '%\"tags\":[\"%'", null, null,
                                       null, null))
            .doWithCursor(new ConvertTags(db));
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
            db.execSQL("ALTER TABLE tasks add column "
                       + "is_shown_recurring INTEGER DEFAULT 1;");
            final Map<Task, List<Task>> recurring = new HashMap<>();
            new CursorWrapper(db.query("tasks", Task.allColumns,
                                       "additional_entries LIKE ?",
                                       new String[] { "%\"status\":\"recurring\"%" }, null, null,
            null)).doWithCursor(new CursorWrapper.WithCursor() {
                @Override
                public void withOpenCursor(@NonNull final CursorGetter getter) {
                    for (getter.moveToFirst(); getter.moveToNext();) {
                        final Task t = new Task(getter);
                        final String recurString = t.getAdditionalString("recur");
                        if (recurString == null) {
                            continue;
                        }
                        // check if is childtask
                        if (t.existAdditional("parent")) {
                            final Optional<Task> masterOptional = Task.getByUUID(t
                                                                  .getAdditionalString("parent"));

                            if (masterOptional.isPresent()) {
                                final Task master = masterOptional.get();
                                final List<Task> list;
                                if (recurring.containsKey(master)) {
                                    list = recurring.get(master);
                                } else {
                                    list = new ArrayList<>(1);
                                }
                                list.add(t);
                                recurring.put(master, list);
                            }
                        } else if (!recurring.containsKey(t)) {// its recurring master
                            recurring.put(t, new ArrayList<Task>(0));
                        }
                        t.setRecurrence(of(CompatibilityHelper.parseTaskWarriorRecurrence(
                                               recurString)));
                        t.save();
                    }
                }
            });

            final StringBuilder idsToHide = new StringBuilder();
            boolean first = true;
            for (final Entry<Task, List<Task>> rec : recurring.entrySet()) {
                if (rec.getValue().isEmpty()) {
                    continue;
                }
                Task newest = rec.getValue().get(0);
                for (final Task t : rec.getValue()) {
                    cv = new ContentValues();
                    cv.put("parent", rec.getKey().getId());
                    cv.put("child", t.getId());
                    final int counter = t.getAdditionalInt("imask");
                    cv.put("offsetCount", counter);
                    final Optional<Recurring> recurringOptional = t.getRecurrence();
                    if (recurringOptional.isPresent()) {
                        cv.put("offset", counter * recurringOptional.get().getIntervalMs());
                    } else {
                        continue;
                    }
                    db.insert("recurring_tw_mask", null, cv);
                    final int newestOffset = newest.getAdditionalInt("imask");
                    final int currentOffset = t.getAdditionalInt("imask");
                    if (newestOffset < currentOffset) {
                        if (first) {
                            first = false;
                        } else {
                            idsToHide.append(',');
                        }
                        idsToHide.append(newest.getId());
                        newest = t;
                    }
                }
            }
            if (!idsToHide.toString().isEmpty()) {
                cv = new ContentValues();
                cv.put("is_shown_recurring", false);
                db.update("tasks", cv, "_id IN (?)",
                          new String[] { idsToHide.toString() });
            }
        case 40:
            // Update settings
            updateSettings();
            // Alter tag table
            db.execSQL("ALTER TABLE tag RENAME to tmp_tags;");
            db.execSQL("CREATE TABLE tag ("
                       + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                       + "name TEXT NOT NULL, "
                       + "dark_text INTEGER NOT NULL DEFAULT 0, "
                       + "color INTEGER NOT NULL DEFAULT 0);");
            db.execSQL("INSERT INTO tag" +
                       " (_id,name,dark_text) SELECT _id,name,dark_text FROM tmp_tags;");
            final String[] tagColumns = new String[] {"_id", "color_a", "color_r", "color_g", "color_b"};
            new CursorWrapper(db.query("tmp_tags", tagColumns, null, null, null, null,
            null)).doWithCursor(new CursorWrapper.WithCursor() {
                @Override
                public void withOpenCursor(@NonNull CursorGetter getter) {
                    if (getter.moveToFirst()) {
                        do {
                            int i = 0;
                            final int id = getter.getInt(i++);
                            final int rgba = getter.getInt(i++);
                            final int rgbr = getter.getInt(i++);
                            final int rgbg = getter.getInt(i++);
                            final int rgbb = getter.getInt(i);
                            final int newColor = Color.argb(rgba, rgbr, rgbg, rgbb);
                            new CursorWrapper(db.query("tag", Tag.allColumns, "_id"
                                                       + "=?", new String[] {String.valueOf(id)}, null, null,
                            null)).doWithCursor(new CursorWrapper.WithCursor() {
                                @Override
                                public void withOpenCursor(@NonNull CursorGetter getter) {
                                    if (getter.moveToFirst()) {
                                        final Tag newTag = new Tag(getter);
                                        newTag.setBackgroundColor(newColor);
                                        db.update("tag", newTag.getContentValues(), "_id=?", new String[] {String.valueOf(newTag.getId())});
                                    }
                                }
                            });

                        } while (getter.moveToNext());
                    }
                }
            });
            db.execSQL("DROP TABLE tmp_tags;");
            db.execSQL("create unique index tag_unique ON tag (name);");
        case 41:
            updateCaldavExtra(db);
        case 42:
            updateListTable(db);
        case 43:
            db.execSQL("DROP VIEW caldav_lists;");
            db.execSQL("DROP VIEW caldav_tasks;");
            createCaldavListsView(db);
            createCaldavTasksView(db);
        case 44:
            createCaldavPropertyView(db);
        case 45:
            updateSpecialLists(db);
        case 46:
            db.execSQL("UPDATE tasks SET updated_at =strftime('%s','now') WHERE " +
                       "updated_at>strftime('%s','now');");
        case 47:
            db.execSQL("CREATE VIEW tasks_view AS select tasks.*, lists" +
                       ".account_id AS account_id, lists.name AS list_name FROM tasks INNER JOIN " +
                       "lists ON tasks.list_id = lists._id;");
        case 48:
            db.execSQL("ALTER TABLE lists add column "
                       + "icon_path TEXT;");
            db.execSQL("ALTER TABLE special_lists add column "
                       + "icon_path TEXT;");
            db.execSQL("UPDATE lists SET icon_path" +
                       "='file:///android_asset/list_icons/inbox.png' WHERE name= '" + context.getString(
                           R.string.inbox) + "';");
            db.execSQL("UPDATE special_lists SET icon_path" +
                       "='file:///android_asset/list_icons/today.png' WHERE name= '" + context.getString(
                           R.string.list_today) + "';");
            db.execSQL("UPDATE special_lists SET icon_path" +
                       "='file:///android_asset/list_icons/week.png' WHERE name= '" + context.getString(
                           R.string.list_week) + "';");
            db.execSQL("UPDATE special_lists SET icon_path" +
                       "='file:///android_asset/list_icons/overdue.png' WHERE name= '" +
                       context.getString(R.string.list_overdue) + "';");
        case 49:
            normaliseLfts(db);
        case 50:
            db.execSQL("CREATE VIEW autocomplete_helper AS " +
                       "SELECT 'tag' || tag._id AS _id, tag._id AS obj_id, name AS name, 3 AS score, 'tag' AS type, color AS color, 0 as done FROM tag INNER JOIN task_tag ON task_tag.tag_id = tag._id "
                       +
                       "UNION " +
                       "SELECT 'task' || _id AS _id, _id AS obj_id, name AS name, - done * 5 AS score, 'task' AS type, 0 AS color, done as done FROM tasks WHERE sync_state!=-1 AND is_shown_recurring = 1;");
        case 51:
            db.execSQL("CREATE VIEW lists_with_special AS " +
                       "SELECT lists._id AS _id, lists.name AS name, sort_by, lists.created_at AS created_at, lists.updated_at updated_at, lists.sync_state AS sync_state, lft, rgt,color, account_id, icon_path, 1 AS isNormal, COUNT(tasks._id) AS task_count "
                       +
                       "FROM lists LEFT JOIN tasks ON tasks.list_id = lists._id AND tasks.sync_state != -1 AND tasks.is_shown_recurring = 1 AND tasks.done = 0"
                       +
                       " GROUP BY lists._id " +
                       " UNION " +
                       "SELECT -special_lists._id AS _id, special_lists.name, sort_by, DATE(\"now\") AS created_at, DATE(\"now\") AS updated_at, 0 AS sync_state, lft, rgt, color, account._id AS account_id, icon_path, 0 AS isNormal, -1 AS task_count from special_lists, (select  _id from account union select null) as account where active = 1 ORDER BY lft ASC;");
        case 52:
            db.execSQL("UPDATE special_lists SET icon_path" +
                       "='file:///android_asset/list_icons/all_tasks.png' WHERE name= '" +
                       context.getString(R.string.list_all) + "';");
        case 53:
            MirakelPreferences.init(context);
            SharedPreferences.Editor e = MirakelPreferences.getEditor();
            for (int i = 0; i < 100; i++) {
                e = e.remove("OLD" + i);
            }
            e.commit();
        case 54:
            db.execSQL("DROP VIEW caldav_lists;");
            db.execSQL("DROP VIEW caldav_tasks;");
            createCaldavListsView(db);
            createCaldavTasksView(db);
        case 55:
            db.execSQL("DROP VIEW caldav_tasks;");
            createCaldavTasksView(db);
        case 56:
            mergeListsSpecialLists(db);
        case 57:
            db.execSQL("UPDATE tasks SET due = due * 1000 WHERE due is not null;");
            db.execSQL("UPDATE tasks SET created_at=created_at*1000, updated_at=updated_at*1000;");
            db.execSQL("UPDATE tasks SET reminder=reminder * 1000 WHERE reminder is not null;");
            db.execSQL("DROP VIEW caldav_tasks;");
            createCaldavTasksView(db);

            db.execSQL("UPDATE recurring SET start_date=start_date*1000 WHERE start_date is not null");
            db.execSQL("UPDATE recurring SET end_date=end_date*1000 WHERE end_date is not null;");

        default:
            break;
        }
    }

    private void mergeListsSpecialLists(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE lists RENAME TO old_lists;");
        db.execSQL("CREATE TABLE lists (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                   + " name TEXT NOT NULL,"
                   + " sort_by INTEGER NOT NULL DEFAULT 0,"
                   + " created_at INTEGER NOT NULL DEFAULT (strftime('%s','now')* 1000), "
                   + " updated_at INTEGER NOT NULL DEFAULT (strftime('%s','now')* 1000), "
                   + " sync_state INTEGER DEFAULT 1,"
                   + " lft INTEGER, "
                   + " rgt INTEGER, "
                   + " color INTEGER, "
                   + " account_id INTEGER REFERENCES account(_id) ON DELETE CASCADE ON UPDATE CASCADE, "
                   + " icon_path TEXT, "
                   + " whereQuery TEXT, "
                   + " def_list INTEGER, "
                   + " def_date INTEGER, "
                   + " active INTEGER NOT NULL DEFAULT 1, "
                   + " is_special INTEGER NOT NULL DEFAULT 0 "
                   + ')');
        db.execSQL("INSERT INTO lists(_id, name, sort_by, created_at, updated_at, sync_state, lft, rgt, color, account_id, icon_path, is_special) "
                   + "SELECT _id,name,sort_by, " +
                   "strftime('%s',CASE "
                   +
                   "WHEN (created_at LIKE \"%:%\") THEN substr(created_at,0,11)||' '||substr(created_at,12,2)||':'||substr(created_at,15,2)||':'||substr(created_at,18,2) "
                   +
                   "WHEN (created_at LIKE \"%Z\") THEN substr(created_at,0,11)||' '||substr(created_at,12,2)||':'||substr(created_at,14,2)||':'||substr(created_at,16,2)"
                   +
                   "ELSE datetime()"
                   +
                   " END) * 1000, "
                   +
                   " strftime('%s',CASE WHEN (updated_at LIKE \"%:%\") " +
                   "THEN substr(updated_at,0,11)||' '||substr(updated_at,12,2)||':'||substr(updated_at,15,2)||':'||substr(updated_at,18,2) "
                   +
                   "ELSE substr(updated_at,0,11)||' '||substr(updated_at,12,2)||':'||substr(updated_at,14,2)||':'||substr(updated_at,16,2) END) * 1000, "
                   +
                   " sync_state, lft, rgt, color, account_id, icon_path,0 from old_lists");
        db.execSQL("INSERT INTO lists (name, sort_by, sync_state, lft, rgt, color, account_id, icon_path, whereQuery, def_list, def_date, active, is_special) "
                   +
                   "SELECT name, sort_by, sync_state, lft, rgt, color, 0, icon_path, whereQuery, def_list, def_date, active, 1 FROM special_lists;");
        Cursor specialListsUpdate = db.query("special_lists", new String[] {"_id", "whereQuery"}, null,
                                             null, null, null, null);
        SparseIntArray idMapping = new SparseIntArray();
        Map<Integer, SpecialListsBaseProperty> whereMapping = new HashMap<>();
        if (specialListsUpdate.moveToFirst()) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            long notificationsList = Integer.parseInt(settings.getString("notificationsList", "-1"));
            long startupList = Integer.parseInt(settings.getString("startupList", "-1"));
            SharedPreferences.Editor editor = settings.edit();

            do {
                String oldWhere = specialListsUpdate.getString(1);
                Integer oldId = specialListsUpdate.getInt(0) * -1;
                Cursor newSpecial = db.query("lists", new String[] {"_id"}, "whereQuery = ?", new String[] {oldWhere},
                                             null, null, null);
                Integer newId;
                if (newSpecial.moveToFirst()) {
                    newId = newSpecial.getInt(0);
                    newSpecial.close();
                } else {
                    Log.wtf(TAG, "list not found in new lists");
                    newSpecial.close();
                    continue;
                }
                if (notificationsList == oldId) {
                    editor.putString("notificationsList", String.valueOf(newId));
                }
                if (startupList == oldId) {
                    editor.putString("startupList", String.valueOf(newId));
                }
                idMapping.put(oldId, newId);
                if (oldWhere.contains("list_id")) {
                    final Optional<SpecialListsBaseProperty> where = SpecialListsWhereDeserializer.deserializeWhere(
                                oldWhere, "db update");
                    if (where.isPresent()) {
                        whereMapping.put(newId, where.get());
                    }
                }
            } while (specialListsUpdate.moveToNext());
            editor.apply();
            // update widget preferences
            try {
                Class widget = Class.forName(DefinitionsHelper.MAINWIDGET_CLASS);
                Method update = widget.getMethod("update", idMapping.getClass());
                update.invoke(null, idMapping);
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                //eat it
            } catch (InvocationTargetException ignored) {
                //eat it
            } catch (IllegalAccessException ignored) {
                //eat it
            }

        }
        specialListsUpdate.close();
        for (final Entry<Integer, SpecialListsBaseProperty> list : whereMapping.entrySet()) {
            transformWhere(list.getValue(), idMapping);
            final ContentValues updateLists = new ContentValues();
            updateLists.put("whereQuery", list.getValue().serialize());
            db.update("lists", updateLists, "_id = ?", new String[] {String.valueOf(list.getKey())});
        }


        db.execSQL("CREATE VIEW list_with_count AS " +
                   "SELECT lists._id AS _id, lists.name AS name, sort_by, " +
                   "lists.created_at AS created_at, lists.updated_at updated_at, " +
                   "lists.sync_state AS sync_state, lft, rgt,color, account_id, " +
                   "icon_path, is_special,whereQuery,def_list,def_date,active, COUNT(tasks._id) AS task_count " +
                   "FROM lists " +
                   "LEFT JOIN tasks ON tasks.list_id = lists._id AND " +
                   "tasks.sync_state != -1 AND tasks.is_shown_recurring = 1 " +
                   "AND tasks.done = 0 GROUP BY lists._id  ORDER BY lft ASC;");

        db.execSQL("DROP TABLE special_lists");
        db.execSQL("DROP TABLE old_lists");
        db.execSQL("DROP VIEW lists_with_special");
    }

    private void transformWhere(final SpecialListsBaseProperty where, final SparseIntArray idMapping) {
        if (where instanceof SpecialListsListProperty) {
            ((SpecialListsListProperty) where).setContent(new ArrayList<>(Collections2.transform(((
                        SpecialListsListProperty) where).getContent(),
            new Function<Integer, Integer>() {
                @Override
                public Integer apply(final Integer input) {
                    return idMapping.get(input, input);
                }
            })));
        } else if (where instanceof SpecialListsConjunctionList) {
            for (final SpecialListsBaseProperty child : ((SpecialListsConjunctionList) where).getChilds()) {
                transformWhere(child, idMapping);
            }
        }
    }


    /**
     * We want to be able to mix normal and special lists. Therefore and because we had a
     * bug which led to incorrect enumeration of lft's we normalise the values here.
     * @param db
     */
    private void normaliseLfts(final SQLiteDatabase db) {
        new CursorWrapper(db.rawQuery(
                              "select _id, 1 as isNormal, lft from lists WHERE sync_state !=  -1 "
                              + " UNION "
                              + " select _id, 0 as isNormal, lft from special_lists WHERE sync_state !=  -1 ORDER BY isNormal ASC, lft ASC;",
        null)).doWithCursor(new CursorWrapper.WithCursor() {
            @Override
            public void withOpenCursor(@NonNull CursorGetter getter) {
                int lft = 1;
                while (getter.moveToNext()) {
                    final long id = getter.getLong("_id");
                    final boolean isNormal = getter.getInt("isNormal") == 1;
                    final String table;
                    if (isNormal) {
                        table = "lists";
                    } else {
                        table = "special_lists";
                    }
                    final ContentValues contentValues = new ContentValues(2);
                    contentValues.put("lft", lft);
                    contentValues.put("rgt", lft + 1);
                    db.update(table, contentValues, "_id = ?", new String[] {String.valueOf(id)});
                    lft += 2;
                }
            }
        });
    }

    private void updateSpecialLists(final SQLiteDatabase db) {
        new CursorWrapper(db.query("special_lists", new String[] {"whereQuery", "_id"},
                                   null,
        null, null, null, null)).doWithCursor(new CursorWrapper.WithCursor() {
            @Override
            public void withOpenCursor(@NonNull CursorGetter getter) {
                while (getter.moveToNext()) {
                    final String query = getter.getString("whereQuery");
                    StringBuilder newQuery = new StringBuilder();
                    int counter = 0;
                    boolean isMultiPart = false;
                    boolean isArgument = false;
                    boolean lastIsBracket = false;
                    for (int i = 0; i < query.length(); i++) {
                        char p = query.charAt(i);
                        switch (p) {
                        case '"':
                            lastIsBracket = false;
                            isArgument = !isArgument;
                            newQuery.append(p);
                            break;
                        case '{':
                            if (!lastIsBracket) {
                                newQuery.append(p);
                            }
                            if (!isArgument) {
                                if (!lastIsBracket) {
                                    counter++;
                                }
                                lastIsBracket = true;
                            }
                            break;
                        case '}':
                            lastIsBracket = false;
                            if (counter > 0) {
                                newQuery.append(p);
                                if (!isArgument) {
                                    counter--;
                                }
                            }
                            break;
                        case ',':
                            lastIsBracket = false;
                            if (DefinitionsHelper.freshInstall && (counter == 0)) {
                                isMultiPart = true;
                                newQuery.append(p);
                                break;
                            } else if (!DefinitionsHelper.freshInstall && (counter == 1)) {
                                isMultiPart = true;
                                newQuery.append('}').append(p).append('{');
                                break;
                            }
                        default:
                            lastIsBracket = false;
                            newQuery.append(p);
                        }
                    }
                    final ContentValues newWhere = new ContentValues();
                    if (isMultiPart) {
                        newQuery = new StringBuilder("[" + newQuery + ']');
                    }
                    newWhere.put("whereQuery", newQuery.toString());
                    db.update("special_lists", newWhere, "_id=?", new String[] {getter.getString("_id")});
                }
            }
        });
    }



    private static void createCaldavListsView(final SQLiteDatabase db) {
        db.execSQL("CREATE VIEW caldav_lists AS SELECT _sync_id, sync_version, CASE WHEN l.sync_state IN (-1,0) THEN 0 ELSE 1 END AS _dirty, sync1, sync2, sync3, sync4, sync5, sync6, sync7, sync8, a.name AS account_name, account_type, l._id, l.name AS list_name, l.color AS list_color, access_level as list_access_level, visible, "
                   +
                   "a.enabled AS sync_enabled, owner AS list_owner\n"
                   +
                   "FROM lists as l\n" +
                   "LEFT JOIN caldav_lists_extra ON l._id=list_id\n" +
                   "LEFT JOIN account AS a ON a._id = account_id;");
        // Create trigger for lists
        // Insert trigger
        db.execSQL("CREATE TRIGGER caldav_lists_insert_trigger INSTEAD OF INSERT ON caldav_lists\n" +
                   "BEGIN\n"
                   + "INSERT INTO account(name,type) SELECT new.account_name, " + 2 +
                   " WHERE NOT EXISTS(SELECT 1 FROM account WHERE name=new.account_name);"
                   + "INSERT INTO lists (sync_state, name, color, account_id,lft,rgt) VALUES (0, new.list_name, new.list_color, (SELECT DISTINCT _id FROM account WHERE name = new.account_name),(SELECT MAX(lft) from lists)+2,(SELECT MAX(rgt) from lists)+2);"
                   + "UPDATE account SET enabled=new.sync_enabled WHERE name = new.account_name;"
                   + "INSERT INTO caldav_lists_extra VALUES\n" +
                   "((SELECT last_insert_rowid() FROM lists),new._sync_id, new.sync_version, new.sync1, new.sync2, new.sync3, new.sync4, new.sync5, new.sync6, new.sync7, new.sync8, new.account_type , new.list_access_level, new.visible, new.sync_enabled, new.list_owner);\n"
                   +
                   "END;");
        db.execSQL("CREATE TRIGGER caldav_lists_update_trigger INSTEAD OF UPDATE on caldav_lists\n" +
                   "BEGIN\n" +
                   "UPDATE lists SET sync_state=0, name = new.list_name, color = new.list_color WHERE _id = old._id;\n"
                   + "UPDATE account SET enabled=new.sync_enabled WHERE name = new.account_name;"
                   + "INSERT OR REPLACE INTO caldav_lists_extra VALUES (new._id, new._sync_id, new.sync_version, new.sync1, new.sync2, new.sync3, new.sync4, new.sync5, new.sync6, new.sync7, new.sync8, new.account_type , new.list_access_level, new.visible, new.sync_enabled, new.list_owner);\n"
                   +
                   "END;");
        db.execSQL("CREATE TRIGGER caldav_lists_delete_trigger INSTEAD OF DELETE on caldav_lists\n" +
                   "BEGIN\n" +
                   "    DELETE FROM lists WHERE _id = old._id;\n" +
                   "END;\n");
    }

    private void updateListTable(final SQLiteDatabase db) {
        db.execSQL("ALTER TABLE lists RENAME TO tmp_lists;");
        db.execSQL("CREATE TABLE lists (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, name"
                   + " TEXT NOT NULL, sort_by"
                   + " INTEGER NOT NULL DEFAULT 0, created_at"
                   + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at"
                   + " INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                   + "sync_state INTEGER DEFAULT 1"
                   + ", lft INTEGER, rgt"
                   + " INTEGER , color INTEGER,"
                   + "account_id INTEGER REFERENCES "
                   + "account (_id"
                   + ") ON DELETE CASCADE ON UPDATE CASCADE "
                   + ')');
        db.execSQL("INSERT INTO lists SELECT * FROM tmp_lists");
        db.execSQL("DROP TABLE tmp_lists;");
    }


    private void updateSettings() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settingsIntToLong(settings, "defaultAccountID");
        settingsIntToLong(settings, "subtaskAddToList");
    }
    private void settingsIntToLong(final SharedPreferences settings, final String key) {
        try {
            final int i = settings.getInt(key, -1);
            if (i != -1) {
                final SharedPreferences.Editor editor = settings.edit();
                editor.putLong(key, (long) i);
                editor.commit();
            }
        } catch (final ClassCastException e) {
            Log.i(TAG, "The setting was already a long", e);
        }
    }



    private static void createCaldavLists(final SQLiteDatabase db) {
        // Create table for extras for lists
        db.execSQL("CREATE TABLE caldav_lists_extra (\n" +
                   "list_id INTEGER PRIMARY KEY REFERENCES lists(_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                   "_sync_id TEXT,\n" +
                   "sync_version TEXT,\n" +
                   "sync1 TEXT,\n" +
                   "sync2 TEXT,\n" +
                   "sync3 TEXT,\n" +
                   "sync4 TEXT,\n" +
                   "sync5 TEXT,\n" +
                   "sync6 TEXT,\n" +
                   "sync7 TEXT,\n" +
                   "sync8 TEXT,\n" +
                   "account_type TEXT,\n" +
                   "access_level INTEGER,\n" +
                   "visible INTEGER,\n" +
                   "sync_enabled INTEGER,\n" +
                   "owner TEXT);");
        createCaldavListsView(db);
    }


    private static void createCaldavTasks(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE caldav_tasks_extra (\n" +
                   "_sync_id TEXT,\n" +
                   "sync_version TEXT,\n" +
                   "sync1 TEXT,\n" +
                   "sync2 TEXT,\n" +
                   "sync3 TEXT,\n" +
                   "sync4 TEXT,\n" +
                   "sync5 TEXT,\n" +
                   "sync6 TEXT,\n" +
                   "sync7 TEXT,\n" +
                   "sync8 TEXT,\n" +
                   "_uid TEXT,\n" +
                   "task_id INTEGER PRIMARY KEY REFERENCES tasks(_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                   "location TEXT,\n" +
                   "geo TEXT,\n" +
                   "url TEXT,\n" +
                   "organizer TEXT,\n" +
                   "priority INTEGER,\n" +
                   "classification INTEGER,\n" +
                   "completed_is_allday DEFAULT 0,\n" +
                   "status INTEGER,\n" +
                   "task_color INTEGER,\n" +
                   "dtstart INTEGER,\n" +
                   "is_allday INTEGER,\n" +
                   "tz TEXT,\n" +
                   "duration TEXT,\n" +
                   "rdate TEXT,\n" +
                   "exdate TEXT,\n" +
                   "rrule TEXT,\n" +
                   "original_instance_sync_id INTEGER,\n" +
                   "original_instance_id INTEGER,\n" +
                   "original_instance_time INTEGER,\n" +
                   "original_instance_allday INTEGER,\n" +
                   "parent_id INTEGER,\n" +
                   "sorting TEXT,\n" +
                   "has_alarms INTEGER);");
        createCaldavTasksView(db);
    }

    private static void createCaldavTasksView(final SQLiteDatabase db) {
        // View
        db.execSQL("CREATE VIEW caldav_tasks AS SELECT \n" +
                   "e._sync_id,\n" +
                   "e.sync_version,\n" +
                   "CASE WHEN t.sync_state IN (-1,0) THEN 0 ELSE 1 END _dirty,\n" +
                   "e.sync1,\n" +
                   "e.sync2,\n" +
                   "e.sync3,\n" +
                   "e.sync4,\n" +
                   "e.sync5,\n" +
                   "e.sync6,\n" +
                   "e.sync7,\n" +
                   "e.sync8,\n" +
                   "e._uid,\n" +
                   "CASE WHEN t.sync_state = -1 THEN 1 ELSE 0 END _deleted,\n" +
                   "t._id,\n" +
                   "t.list_id,\n" +
                   "t.name as title,\n" +
                   "e.location,\n" +
                   "e.geo,\n" +
                   "t.content as description,\n" +
                   "e.url,\n" +
                   "e.organizer,\n" +
                   "CASE \n" +
                   "     WHEN t.priority<0 THEN\n" +
                   "         CASE WHEN e.priority BETWEEN 7 AND 9 THEN e.priority ELSE 9 END\n"
                   +
                   "     WHEN t.priority=1 THEN\n" +
                   "         CASE WHEN e.priority BETWEEN 4 AND 6 THEN e.priority ELSE 5 END\n"
                   +
                   "     WHEN t.priority=2 THEN\n" +
                   "         CASE WHEN e.priority BETWEEN 1 AND 3 THEN e.priority ELSE 1 END\n"
                   +
                   "     ELSE 0\n" +
                   "END AS priority,\n" +
                   "e.classification as class,\n" +
                   "CASE WHEN t.done=1 THEN t.updated_at ELSE null END AS completed,\n" +
                   "e.completed_is_allday,\n" +
                   "t.progress AS percent_complete,\n" +
                   "CASE\n" +
                   "     WHEN t.done = 1 THEN \n" +
                   "         CASE WHEN e.status IN (2,3) THEN e.status ELSE 2 END\n" +
                   "     WHEN t.progress>0 AND NOT t.done=1 THEN 1\n" +
                   "     ELSE \n" +
                   "         CASE WHEN e.status IN(0,1) THEN e.status ELSE 0 END\n" +
                   "END AS status,\n" +
                   "CASE \n" +
                   "     WHEN t.done = 0 AND t.progress=0 THEN 1 \n" +
                   "     ELSE CASE WHEN status = 0 AND NOT t.done=1 THEN 1 ELSE 0 END\n" +
                   "END AS is_new,\n" +
                   "CASE \n" +
                   "     WHEN done=1 THEN 1\n" +
                   "     ELSE CASE WHEN (status=3 OR status=2) AND NOT t.done=0 THEN 1 ELSE 0 END\n " +
                   "END AS is_closed,\n" +
                   "task_color,\n" +
                   "e.dtstart,\n" +
                   "e.is_allday,\n" +
                   "t.created_at AS created,\n" +
                   "t.updated_at AS last_modified,\n" +
                   "e.tz,\n" +
                   "t.due AS due,\n" +
                   "e.duration,\n" +
                   "e.rdate,\n" +
                   "e.exdate,\n" +
                   "e.rrule,\n" +
                   "e.original_instance_sync_id,\n" +
                   "e.original_instance_id,\n" +
                   "e.original_instance_time,\n" +
                   "e.original_instance_allday,\n" +
                   "e.parent_id,\n" +
                   "e.sorting,\n" +
                   "e.has_alarms,\n" +
                   "l.account_name,\n" +
                   "l.account_type,\n" +
                   "l.list_name,\n" +
                   "l.list_color,\n" +
                   "l.list_owner AS list_owner,\n" +
                   "l.list_access_level,\n" +
                   "l.visible\n" +
                   "FROM\n" +
                   "tasks AS t\n" +
                   "LEFT JOIN caldav_tasks_extra as e ON task_id = t._id\n" +
                   "INNER JOIN caldav_lists as l ON l._id = t.list_id;");
        // Insert trigger
        db.execSQL("CREATE TRIGGER caldav_tasks_insert_trigger INSTEAD OF INSERT ON caldav_tasks\n" +
                   "BEGIN\n" +
                   "    INSERT INTO tasks (sync_state, list_id, name, content, progress, done, due, priority, created_at, updated_at) VALUES (\n"
                   +
                   "    0,\n" +
                   "    new.list_id,\n" +
                   "    new.title,\n" +
                   "    new.description,\n" +
                   "    CASE WHEN new.percent_complete IS NULL THEN 0 ELSE new.percent_complete END,\n" +
                   "    CASE WHEN new.status IN(2,3) THEN 1 ELSE 0 END,  \n" +
                   "    new.due,\n" +
                   "    CASE WHEN new.priority=0 THEN 0\n" +
                   "         WHEN new.priority < 4 THEN 2 \n" +
                   "         WHEN new.priority < 7 THEN 1 \n" +
                   "         WHEN new.priority <= 9 THEN -1 \n" +
                   "    ELSE 0\n" +
                   "    END,\n" +
                   "    CASE WHEN new.created IS NULL THEN strftime('%s','now')*1000 ELSE new.created  END,\n" +
                   "    CASE WHEN new.last_modified IS NULL THEN strftime('%s','now')*1000 ELSE new.last_modified END);\n"
                   +
                   "    INSERT INTO caldav_tasks_extra (task_id,_sync_id,location,geo,url,organizer,priority,classification, completed_is_allday,"
                   +
                   "    status, task_color, dtstart, is_allday, tz, duration, rdate, exdate, rrule, original_instance_sync_id, "
                   +
                   "    original_instance_id, original_instance_time, original_instance_allday, parent_id, sorting, has_alarms,"
                   +
                   "    sync1, sync2, sync3, sync4, sync5, sync6, sync7, sync8)\n"
                   +
                   "    VALUES\n" +
                   "    ((SELECT last_insert_rowid() FROM tasks),new._sync_id, new.location, new.geo, new.url, new.organizer, "
                   +
                   "    new.priority, new.class, new.completed_is_allday, new.status, new.task_color, new.dtstart, new.is_allday, "
                   +
                   "    new.tz, new.duration, new.rdate, new.exdate, new.rrule, new.original_instance_sync_id, new.original_instance_id, "
                   +
                   "    new.original_instance_time, new.original_instance_allday, new.parent_id, new.sorting, new.has_alarms,"
                   +
                   "    new.sync1, new.sync2, new.sync3, new.sync4, new.sync5, new.sync6, new.sync7, new.sync8);\n"
                   +
                   "END;");
        // Update trigger
        db.execSQL("CREATE TRIGGER caldav_tasks_update_trigger INSTEAD OF UPDATE ON caldav_tasks\n" +
                   "BEGIN\n" +
                   "UPDATE tasks SET\n" +
                   "sync_state = 0,\n" +
                   "list_id = new.list_id,\n" +
                   "name = new.title,\n" +
                   "content = new.description,\n" +
                   "progress = CASE WHEN new.percent_complete IS NULL THEN 0 ELSE new.percent_complete END,\n" +
                   "done = CASE WHEN new.status IN(2,3) THEN 1 ELSE 0 END,    \n" +
                   "due = new.due ,\n" +
                   "priority = CASE WHEN new.priority=0 THEN 0\n" +
                   "                WHEN new.priority < 4 THEN 2 \n" +
                   "                WHEN new.priority < 7 THEN 1 \n" +
                   "                WHEN new.priority <= 9 THEN -1 \n" +
                   "                ELSE 0\n" +
                   "END,\n" +
                   "updated_at = CASE WHEN new.last_modified IS NULL THEN strftime('%s','now') * 1000 ELSE new.last_modified END\n"
                   +
                   "WHERE _id = old._id;\n" +
                   "INSERT OR REPLACE INTO caldav_tasks_extra VALUES (\n" +
                   "new._sync_id,\n" +
                   "new.sync_version,\n" +
                   "new.sync1,\n" +
                   "new.sync2,\n" +
                   "new.sync3,\n" +
                   "new.sync4,\n" +
                   "new.sync5,\n" +
                   "new.sync6,\n" +
                   "new.sync7,\n" +
                   "new.sync8,\n" +
                   "new._uid,\n" +
                   "new._id,\n" +
                   "new.location,\n" +
                   "new.geo,\n" +
                   "new.url,\n" +
                   "new.organizer,\n" +
                   "new.priority,\n" +
                   "new.class,\n" +
                   "new.completed_is_allday,\n" +
                   "new.status,\n" +
                   "new.task_color,\n" +
                   "new.dtstart,\n" +
                   "new.is_allday,\n" +
                   "new.tz,\n" +
                   "new.duration,\n" +
                   "new.rdate,\n" +
                   "new.exdate,\n" +
                   "new.rrule,\n" +
                   "new.original_instance_sync_id,\n" +
                   "new.original_instance_id,\n" +
                   "new.original_instance_time,\n" +
                   "new.original_instance_allday,\n" +
                   "new.parent_id,\n" +
                   "new.sorting,\n" +
                   "new.has_alarms);\n" +
                   "END;");
        // Delete Trigger
        db.execSQL("CREATE TRIGGER caldav_tasks_delete_trigger INSTEAD OF DELETE ON caldav_tasks\n" +
                   "BEGIN\n" +
                   "    DELETE FROM tasks WHERE _id=old._id;\n" +
                   "    DELETE FROM caldav_tasks_extra WHERE task_id=old._id;\n" +
                   "END;");
    }

    private static void createCaldavProperties(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE caldav_properties (\n" +
                   "property_id INTEGER,\n" +
                   "task_id INTEGER REFERENCES tasks(_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                   "mimetype TEXT,\n" +
                   "prop_version TEXT,\n" +
                   "prop_sync1 TEXT,\n" +
                   "prop_sync2 TEXT,\n" +
                   "prop_sync3 TEXT,\n" +
                   "prop_sync4 TEXT,\n" +
                   "prop_sync5 TEXT,\n" +
                   "prop_sync6 TEXT,\n" +
                   "prop_sync7 TEXT,\n" +
                   "prop_sync8 TEXT,\n" +
                   "data0  TEXT,\n" +
                   "data1  TEXT,\n" +
                   "data2  TEXT,\n" +
                   "data3  TEXT,\n" +
                   "data4  TEXT,\n" +
                   "data5  TEXT,\n" +
                   "data6  TEXT,\n" +
                   "data7  TEXT,\n" +
                   "data8  TEXT,\n" +
                   "data9  TEXT,\n" +
                   "data10 TEXT,\n" +
                   "data11 TEXT,\n" +
                   "data12 TEXT,\n" +
                   "data13 TEXT,\n" +
                   "data14 TEXT,\n" +
                   "data15 TEXT,\n" +
                   "PRIMARY KEY (property_id, task_id)\n" +
                   ");");
    }

    private static void createCaldavPropertyView(final SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS caldav_property_view;");
        db.execSQL("CREATE VIEW caldav_property_view AS\n" +
                   "SELECT\n" +
                   "property_id,\n" +
                   "task_id,\n" +
                   "mimetype,\n" +
                   "prop_version,\n" +
                   "prop_sync1,\n" +
                   "prop_sync2,\n" +
                   "prop_sync3,\n" +
                   "prop_sync4,\n" +
                   "prop_sync5,\n" +
                   "prop_sync6,\n" +
                   "prop_sync7,\n" +
                   "prop_sync8,\n" +
                   "data0,\n" +
                   "data1,\n" +
                   "data2,\n" +
                   "data3,\n" +
                   "data4,\n" +
                   "data5,\n" +
                   "data6,\n" +
                   "data7,\n" +
                   "data8,\n" +
                   "data9,\n" +
                   "data10,\n" +
                   "data11,\n" +
                   "data12,\n" +
                   "data13,\n" +
                   "data14,\n" +
                   "data15\n" +
                   "FROM caldav_properties\n" +
                   "UNION\n" +
                   "SELECT \n" +
                   "(SELECT MAX(property_id) FROM caldav_properties)+tag._id AS property_id,\n" +
                   "task.task_id AS task_id,\n" +
                   "'vnd.android.cursor.item/category' AS mimetype,\n" +
                   "0 AS prop_version,\n" +
                   "null AS prop_sync1,\n" +
                   "null AS prop_sync2,\n" +
                   "null AS prop_sync3,\n" +
                   "null AS prop_sync4,\n" +
                   "null AS prop_sync5,\n" +
                   "null AS prop_sync6,\n" +
                   "null AS prop_sync7,\n" +
                   "null AS prop_sync8,\n" +
                   "tag._id AS data0,\n" +
                   "tag.name AS data1,\n" +
                   "tag.color AS data2,\n" +
                   "null AS data3,\n" +
                   "null AS data4,\n" +
                   "null AS data5,\n" +
                   "null AS data6,\n" +
                   "null AS data7,\n" +
                   "null AS data8,\n" +
                   "null AS data9,\n" +
                   "null AS data10,\n" +
                   "null AS data11,\n" +
                   "null AS data12,\n" +
                   "null AS data13,\n" +
                   "null AS data14,\n" +
                   "null AS data15\n" +
                   "FROM tag AS TAG\n" +
                   "INNER JOIN task_tag as task ON tag._id=task.tag_id\n" +
                   ';');
        db.execSQL("Create TRIGGER caldav_property_insert_tag_trigger INSTEAD OF INSERT ON caldav_property_view\n"
                   +
                   "WHEN new.mimetype = 'vnd.android.cursor.item/category'\n" +
                   "BEGIN\n" +
                   "\tINSERT OR REPLACE INTO tag (name,color) VALUES (new.data1, new.data2);\n" +
                   "\tINSERT OR REPLACE INTO task_tag(task_id,tag_id) VALUES(new.task_id,(SELECT _id FROM tag WHERE name=new.data1 AND color=new.data2));\n"
                   +
                   "END;");
        db.execSQL("Create TRIGGER caldav_property_insert_other_trigger INSTEAD OF INSERT ON caldav_property_view\n"
                   +
                   "WHEN NOT new.mimetype = 'vnd.android.cursor.item/category'\n" +
                   "BEGIN\n" +
                   "\tINSERT OR REPLACE INTO caldav_properties (property_id, task_id, mimetype, prop_version, prop_sync1, prop_sync2, prop_sync3, prop_sync4, prop_sync5, prop_sync6, prop_sync7, prop_sync8, data0, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14, data15) VALUES (new.property_id, new.task_id, new.mimetype, new.prop_version, new.prop_sync1, new.prop_sync2, new.prop_sync3, new.prop_sync4, new.prop_sync5, new.prop_sync6, new.prop_sync7, new.prop_sync8, new.data0, new.data1, new.data2, new.data3, new.data4, new.data5, new.data6, new.data7, new.data8, new.data9, new.data10, new.data11, new.data12, new.data13, new.data14, new.data15);\n"
                   +
                   "END;");
        db.execSQL("Create TRIGGER caldav_property_update_tag_trigger INSTEAD OF UPDATE ON caldav_property_view\n"
                   +
                   "WHEN new.mimetype = 'vnd.android.cursor.item/category'\n" +
                   "BEGIN\n" +
                   "\tUPDATE tag SET name=new.data1, color=new.data2 WHERE _id=new.data0;\n" +
                   "\tINSERT INTO task_tag(tag_id,task_id) SELECT new.data0, new.task_id WHERE NOT EXISTS(SELECT 1 FROM task_tag WHERE task_tag.tag_id=new.data0 AND task_tag.task_id =new.task_id);\n"
                   +
                   "END;");
        db.execSQL("Create TRIGGER caldav_property_delete_tag_trigger INSTEAD OF DELETE ON caldav_property_view\n"
                   +
                   "WHEN new.mimetype = 'vnd.android.cursor.item/category'\n" +
                   "BEGIN\n" +
                   "\tDELETE FROM tag WHERE _id=old.data0;\n" +
                   "END;");
    }

    private static void createCaldavCategories(final SQLiteDatabase db) {
        // View
        // This is just a cross product of the tag table and all possible accounts.
        // I think we need this because the caldav task adapter could do something like
        // SELECT * FROM caldav_categories WHERE account_name="…";
        // And we have one list of tags for all accounts
        db.execSQL("CREATE VIEW caldav_categories AS\n" +
                   "SELECT _id, account_name, account_type, name, color FROM tag,\n" +
                   "(SELECT DISTINCT(account_name) account_name, account_type FROM caldav_lists) as account_info;");
        // INSERT Trigger
        db.execSQL("Create TRIGGER caldav_categories_insert_trigger INSTEAD OF INSERT ON caldav_categories\n"
                   +
                   "BEGIN\n" +
                   "    INSERT OR REPLACE INTO tag (name,color) VALUES (new.name, new.color);\n" +
                   "END;");
        // UPDATE Trigger
        db.execSQL("CREATE TRIGGER caldav_categories_update_trigger INSTEAD OF UPDATE ON caldav_categories\n"
                   +
                   "BEGIN\n" +
                   "    UPDATE tag SET name=new.name, color=new.color WHERE _id=new._id;\n" +
                   "END;");
        // DELETE Trigger
        // Do nothing! We care about tags not caldav!
        db.execSQL("CREATE TRIGGER caldav_categories_delete_trigger INSTEAD OF DELETE ON caldav_categories\n"
                   +
                   "BEGIN\n" +
                   "SELECT _id from tag;\n" +
                   "END;");
    }

    private static void createCaldavAlarms(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE caldav_alarms (\n" +
                   "alarm_id INTEGER PRIMARY KEY,\n" +
                   "last_trigger TEXT,\n" +
                   "next_trigger TEXT);");
    }
    private static void updateCaldavExtra(final SQLiteDatabase db) {
        createCaldavLists(db);
        createCaldavTasks(db);
        createCaldavProperties(db);
        createCaldavCategories(db);
        createCaldavAlarms(db);
        db.execSQL("DROP TABLE caldav_extra;");
    }

    private static void createTableRecurrenceTW(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "
                   + "recurring_tw_mask"
                   + '('
                   + "_id"
                   + " INTEGER PRIMARY KEY,"
                   + "parent INTEGER REFERENCES "
                   + "tasks"
                   + " ("
                   + "_id"
                   + ") "
                   + "ON DELETE CASCADE ON UPDATE CASCADE,child INTEGER REFERENCES "
                   + "tasks"
                   + " ("
                   + "_id"
                   + ") "
                   + "ON DELETE CASCADE ON UPDATE CASCADE ,offset INTEGER,offsetCount INTEGER)");
    }

    private static void createCalDavExtraTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE caldav_extra(_id INTEGER PRIMARY KEY,"
                   + "ETAG TEXT,SYNC_ID TEXT DEFAULT NULL, "
                   + "REMOTE_NAME TEXT)");
    }

    private static void createFileTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE files (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT, task_id"
                   + " INTEGER NOT NULL DEFAULT 0, name TEXT, path TEXT"
                   + ')');
    }

    private static void createSubtaskTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE subtasks (_id"
                   + " INTEGER PRIMARY KEY AUTOINCREMENT,parent_id"
                   + " INTEGER REFERENCES tasks (_id"
                   + ") ON DELETE CASCADE ON UPDATE CASCADE,child_id"
                   + " INTEGER REFERENCES tasks (_id"
                   + ") ON DELETE CASCADE ON UPDATE CASCADE);");
    }

    private static void updateTimesToUTC(final SQLiteDatabase db) {
        db.execSQL("ALTER TABLE tasks RENAME TO tmp_tasks;");
        createTasksTable(db);
        final int offset = DateTimeHelper.getTimeZoneOffset(false,
                           new GregorianCalendar());
        db.execSQL("Insert INTO tasks (_id, uuid, list_id, name, "
                   + "content, done, due, reminder, priority, created_at, "
                   + "updated_at, sync_state, additional_entries, recurring, "
                   + "recurring_reminder, progress) "
                   + "Select _id, uuid, list_id, name, content, done, "
                   + "strftime('%s',"
                   + "due"
                   + ") - ("
                   + offset
                   + "), "
                   + getStrFtime("reminder", offset)
                   + ", priority, "
                   + getStrFtime("created_at", offset)
                   + ", "
                   + getStrFtime("updated_at", offset)
                   + ", "
                   + "sync_state, additional_entries, recurring, recurring_reminder, progress FROM tmp_tasks;");
        db.execSQL("DROP TABLE tmp_tasks");
    }

    private static String getStrFtime(final String col, final int offset) {
        String ret = "strftime('%s',substr(" + col + ",0,11)||' '||substr("
                     + col + ",12,2)||':'||substr(" + col + ",14,2)||':'||substr("
                     + col + ",16,2)) - (" + offset + ")";
        if (col.equals("created_at") || col.equals("updated_at")) {
            ret = "CASE WHEN (" + ret
                  + ") IS NULL THEN strftime('%s','now') ELSE (" + ret
                  + ") END";
        }
        return ret;
    }



    private static class SpecialListsConverter implements CursorWrapper.WithCursor {
        private final SQLiteDatabase db;

        public SpecialListsConverter(SQLiteDatabase db) {
            this.db = db;
        }

        @Override
        public void withOpenCursor(@NonNull CursorGetter getter) {
            for (getter.moveToFirst(); !getter.isAfterLast(); getter
                 .moveToNext()) {
                final int id = getter.getInt("_id");
                final ContentValues contentValues = new ContentValues();
                final String[] where = getter.getString("whereQuery").toLowerCase()
                                       .split("and");
                final Map<String, SpecialListsBaseProperty> whereMap = new HashMap<>(where.length);
                for (final String p : where) {
                    try {
                        if (p.contains("list_id")) {
                            whereMap.put("list_id", CompatibilityHelper
                                         .getSetProperty(p,
                                                         SpecialListsListProperty.class,
                                                         "list_id"));
                        } else if (p.contains("name")) {
                            whereMap.put("name",
                                         CompatibilityHelper.getStringProperty(p,
                                                 SpecialListsNameProperty.class,
                                                 "name"));
                        } else if (p.contains("priority")) {
                            whereMap.put("priority", CompatibilityHelper
                                         .getSetProperty(p,
                                                         SpecialListsPriorityProperty.class,
                                                         "priority"));
                        } else if (p.contains("done")) {
                            whereMap.put("done",
                                         CompatibilityHelper.getDoneProperty(p));
                        } else if (p.contains("due")) {
                            whereMap.put("due",
                                         CompatibilityHelper.getDueProperty(p));
                        } else if (p.contains("content")) {
                            whereMap.put("content", CompatibilityHelper
                                         .getStringProperty(p,
                                                            SpecialListsContentProperty.class,
                                                            "content"));
                        } else if (p.contains("reminder")) {
                            whereMap.put("reminder",
                                         CompatibilityHelper.getReminderProperty(p));
                        }
                    } catch (final TransformerException e) {
                        Log.w(TAG, "due cannot be transformed", e);
                    }
                }
                contentValues.put("whereQuery",
                                  CompatibilityHelper.serializeWhereSpecialLists(whereMap));
                db.update("special_lists", contentValues, "_id=?",
                          new String[] {String.valueOf(id)});
            }
        }
    }

    private class ConvertTags implements CursorWrapper.WithCursor {
        private final SQLiteDatabase db;
        private int count = 0;

        public ConvertTags(final SQLiteDatabase db) {
            this.db = db;
        }

        @Override
        public void withOpenCursor(@NonNull final CursorGetter getter) {
            if (getter.getCount() > 0) {
                getter.moveToFirst();
                do {
                    final int taskId = getter.getInt("_id");
                    final Map<String, String> entryMap = Task
                                                         .parseAdditionalEntries(getter.getString("additional_entries"));
                    String entries = entryMap.get("tags").trim();
                    entries = entries.replace("[", "");
                    entries = entries.replace("]", "");
                    entries = entries.replace("\"", "");
                    final String[] tags = entries.split(",");
                    for (final String tag : tags) {
                        final Long tagId = new CursorWrapper(db.query("tag", new String[] {"_id"}, "name"
                                                             + "=?", new String[] {tag}, null, null, null))
                        .doWithCursor(new CursorWrapper.CursorConverter<Long>() {
                            @Override
                            public Long convert(@NonNull CursorGetter getter) {
                                if (getter.getCount() > 0) {
                                    getter.moveToFirst();
                                    return getter.getLong("_id");
                                } else {
                                    // create tag;
                                    final int color = Tag.getNextColor(count++, context);
                                    ContentValues cv = new ContentValues();
                                    cv.put("name", tag);
                                    cv.put("color_r", Color.red(color));
                                    cv.put("color_g", Color.green(color));
                                    cv.put("color_b", Color.blue(color));
                                    cv.put("color_a", Color.alpha(color));
                                    return db.insert("tag", null, cv);
                                }
                            }
                        });
                        ContentValues cv = new ContentValues();
                        cv.put("tag_id", tagId);
                        cv.put("task_id", taskId);
                        db.insert("task_tag", null, cv);
                        entryMap.remove("tags");
                        cv = new ContentValues();
                        cv.put("additional_entries",
                               Task.serializeAdditionalEntries(entryMap));
                        db.update("tasks", cv, "_id=?",
                                  new String[] {String.valueOf(taskId)});
                    }
                } while (getter.moveToNext());
            }
        }
    }
}
