package de.azapps.mirakel.model.account;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_TYPES;
import de.azapps.mirakelandroid.R;

public class AccountMirakel extends AccountBase {
	public static final String TABLE = "account";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { DatabaseHelper.ID,
			DatabaseHelper.NAME, TYPE, ENABLED };
	@SuppressWarnings("unused")
	private static final String TAG = "Account";
	private static Context context;

	public AccountMirakel(int id, String name, SYNC_TYPES type, boolean enabled) {
		super(id, name, type, enabled);
	}

	public static AccountMirakel newAccount(String name, SYNC_TYPES type,
			boolean enabled) {
		ContentValues cv = new ContentValues();
		cv.put(DatabaseHelper.NAME, name);
		cv.put(TYPE, type.toInt());
		cv.put(ENABLED, enabled);
		long id = database.insert(TABLE, null, cv);
		Cursor cursor = database.query(TABLE, allColumns, DatabaseHelper.ID
				+ " = " + id, null, null, null, null);
		cursor.moveToFirst();
		AccountMirakel newAccount = cursorToAccount(cursor);
		cursor.close();
		return newAccount;
	}

	private static AccountMirakel cursorToAccount(Cursor c) {
		return new AccountMirakel(c.getInt(0), c.getString(1),
				SYNC_TYPES.parseInt(c.getInt(2)), c.getInt(3) == 1);
	}

	public static AccountMirakel get(int id) {
		Cursor c = database.query(TABLE, allColumns, DatabaseHelper.ID + " = "
				+ id, null, null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		} else {
			c.close();
			return null;
		}
	}

	public void save() {
		database.update(TABLE, getContentValues(), DatabaseHelper.ID + "="
				+ getId(), null);
	}

	public void destroy() {
		database.delete(TABLE, DatabaseHelper.ID + "=" + getId(), null);
	}

	public static List<AccountMirakel> getAll() {
		Cursor c = database.query(TABLE, allColumns, null, null, null, null,
				null);
		List<AccountMirakel> accounts = cursorToAccountList(c);
		c.close();
		return accounts;
	}

	public static List<AccountMirakel> getEnabled(boolean isEnabled) {
		Cursor c = database.query(TABLE, allColumns, ENABLED + "="
				+ (isEnabled ? 1 : 0), null, null, null, null);
		List<AccountMirakel> accounts = cursorToAccountList(c);
		c.close();
		return accounts;
	}

	private static List<AccountMirakel> cursorToAccountList(Cursor c) {
		if (c.getCount() > 0) {
			List<AccountMirakel> accounts = new ArrayList<AccountMirakel>();
			c.moveToFirst();
			while (!c.isAfterLast()) {
				accounts.add(cursorToAccount(c));
				c.moveToNext();
			}
			return accounts;

		} else {
			return null;
		}
	}

	public static void init(Context context) {
		AccountMirakel.context = context;
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	public static AccountMirakel getByName(String name) {
		Cursor c = database.query(TABLE, allColumns, DatabaseHelper.NAME + "="
				+ name, null, null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		} else {
			c.close();
			return null;
		}
	}

	// Get the default account
	public static AccountMirakel getDefault() {
		// TODO set this somewhere
		int id = PreferenceManager.getDefaultSharedPreferences(context).getInt(
				"defaultAccount", -1);
		AccountMirakel a = null;
		if (id != -1) {
			a = get(id);
		}
		if (a == null) {
			a = getLocal();
		}
		return a;
	}

	public static AccountMirakel getLocal() {
		Cursor c = database.query(TABLE, allColumns, TYPE + "="
				+ SYNC_TYPES.LOCAL.toInt() + " AND " + ENABLED + "=1", null,
				null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		} else {
			c.close();
			return newAccount(context.getString(R.string.local_account),
					SYNC_TYPES.LOCAL, true);
		}
	}

	/**
	 * Close the Database–Connection
	 */
	public static void close() {
		dbHelper.close();
	}


}
