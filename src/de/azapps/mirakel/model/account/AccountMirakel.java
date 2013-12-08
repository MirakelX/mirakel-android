package de.azapps.mirakel.model.account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;

public class AccountMirakel extends AccountBase {
	public static final String ACCOUNT_TYPE_MIRAKEL = "de.azapps.mirakel";
	public static final String ACCOUNT_TYPE_DAVDROID = "bitfire.at.davdroid";

	public static final String TABLE = "account";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { DatabaseHelper.ID,
			DatabaseHelper.NAME, TYPE, ENABLED };
	private static final String TAG = "Account";
	private static Context context;

	public enum ACCOUNT_TYPES {
		MIRAKEL, TASKWARRIOR, CALDAV, LOCAL;
		public int toInt() {
			switch (this) {
			case CALDAV:
				return 1;
			case LOCAL:
				return -1;
			case MIRAKEL:
				Log.w(TAG, "do not use Mirakel-Accounts");
				return 3;
			case TASKWARRIOR:
				return 2;
			default:
				throw new RuntimeException();
			}
		}

		public static ACCOUNT_TYPES parseInt(int i) {
			switch (i) {
			case -1:
				return LOCAL;
			case 1:
				return CALDAV;
			case 2:
				return TASKWARRIOR;
			case 3:
				return MIRAKEL;
			default:
				throw new IllegalArgumentException();
			}

		}

		public static ACCOUNT_TYPES parseAccountType(String type) {
			if (type.equals(ACCOUNT_TYPE_DAVDROID)) {
				return CALDAV;
			} else if (type.equals(ACCOUNT_TYPE_MIRAKEL)) {
				return TASKWARRIOR;
			} else {
				return LOCAL;
			}
		}

		public static String toName(ACCOUNT_TYPES type) {
			switch (type) {
			case CALDAV:
				return ACCOUNT_TYPE_DAVDROID;
			case MIRAKEL:
			case TASKWARRIOR:
				return ACCOUNT_TYPE_MIRAKEL;
			case LOCAL:
			default:
				return null;
			}
		}

		public static ACCOUNT_TYPES getSyncType(String type) {
			if (type.equals("Mirakel")) {
				return MIRAKEL;
			} else if (type.equals("Taskwarrior")) {
				return TASKWARRIOR;
			} else if (type.equals("CalDav")) {
				return CALDAV;
			} else
				return LOCAL;
		}
	};

	public AccountMirakel(int id, String name, ACCOUNT_TYPES type,
			boolean enabled) {
		super(id, name, type, enabled);
	}

	public static AccountMirakel newAccount(String name, ACCOUNT_TYPES type,
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
				ACCOUNT_TYPES.parseInt(c.getInt(2)), c.getInt(3) == 1);
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
		ContentValues cv = new ContentValues();
		cv.put(ListMirakel.ACCOUNT_ID, getLocal().getId());
		database.update(ListMirakel.TABLE, cv, "account_id=" + getId(), null);
		Account a = getAndroidAccount();
		if (a == null) {
			Log.wtf(TAG, "account not found");
			return;
		}
		AccountManager.get(context).removeAccount(a, null, null);
	}

	private Account getAndroidAccount() {
		AccountManager am = AccountManager.get(context);
		Account[] accounts = am.getAccountsByType(ACCOUNT_TYPES
				.toName(getType()));
		for (Account a : accounts) {
			if (a.name.equals(getName()))
				return a;
		}
		return null;
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
		Cursor c = database.query(TABLE, allColumns, DatabaseHelper.NAME + "='"
				+ name + "'", null, null, null, null);
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
		int id = MirakelPreferences.getDefaultAccount();
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
				+ ACCOUNT_TYPES.LOCAL.toInt() + " AND " + ENABLED + "=1", null,
				null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		} else {
			c.close();
			return newAccount(context.getString(R.string.local_account),
					ACCOUNT_TYPES.LOCAL, true);
		}
	}

	/**
	 * Close the Database–Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	public static void update(Account[] accounts) {
		List<AccountMirakel> accountList = AccountMirakel.getAll();
		Map<String, AccountMirakel> map = new HashMap<String, AccountMirakel>();
		for (AccountMirakel a : accountList) {
			map.put(a.getName(), a);
		}
		for (Account a : accounts) {
			Log.d(TAG, "Accountname: " + a.name + " | TYPE: " + a.type);
			if (a.type.equals(AccountMirakel.ACCOUNT_TYPE_MIRAKEL)
					|| a.type.equals(AccountMirakel.ACCOUNT_TYPE_DAVDROID)) {
				Log.d(TAG, "is supportet Account");
				if (!map.containsKey(a.name)) {
					// Add new account here....
					AccountMirakel.newAccount(a.name,
							ACCOUNT_TYPES.parseAccountType(a.type), true);
				} else {
					// Account exists..
					map.remove(a.name);
				}

			}
		}
		for (Entry<String, AccountMirakel> el : map.entrySet()) {
			// Remove deleted accounts
			if (el.getValue().getType() != ACCOUNT_TYPES.LOCAL)
				el.getValue().destroy();
		}

	}

	public static AccountMirakel get(Account account) {
		Cursor c = database.query(TABLE, allColumns, DatabaseHelper.NAME + "='"
				+ account.name + "'", null, null, null, null);
		if (c.getCount() < 1) {
			c.close();
			return null;
		}
		c.moveToFirst();
		AccountMirakel a = cursorToAccount(c);
		c.close();
		return a;
	}

}
