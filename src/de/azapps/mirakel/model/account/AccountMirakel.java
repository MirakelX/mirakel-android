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
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

public class AccountMirakel extends AccountBase {
	public enum ACCOUNT_TYPES {
		CALDAV, LOCAL, MIRAKEL, TASKWARRIOR;
		public static ACCOUNT_TYPES getSyncType(String type) {
			if (type.equals("Mirakel")) return MIRAKEL;
			else if (type.equals("Taskwarrior")) return TASKWARRIOR;
			else if (type.equals("CalDav")) return CALDAV;
			else
				return LOCAL;
		}

		public static ACCOUNT_TYPES parseAccountType(String type) {
			if (type.equals(ACCOUNT_TYPE_DAVDROID)) return CALDAV;
			else if (type.equals(ACCOUNT_TYPE_MIRAKEL)) return TASKWARRIOR;
			else return LOCAL;
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

		public String typeName(Context ctx) {
			switch (this) {
				case CALDAV:
					return ctx.getString(R.string.calDavName);
				case LOCAL:
					return ctx.getString(R.string.local_account);
				case MIRAKEL:
					return ctx.getString(R.string.app_name);
				case TASKWARRIOR:
					return ctx.getString(R.string.tw_account);
				default:
					return "Unkown account type";
			}
		}
	}
	public static final String ACCOUNT_TYPE_DAVDROID = "bitfire.at.davdroid";

	public static final String ACCOUNT_TYPE_MIRAKEL = "de.azapps.mirakel";
	private static final String[] allColumns = { DatabaseHelper.ID,
		DatabaseHelper.NAME, TYPE, ENABLED };
	private static Context context;
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	public static final String TABLE = "account";

	private static final String TAG = "Account";

	/**
	 * Close the Database–Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	private static AccountMirakel cursorToAccount(Cursor c) {
		return new AccountMirakel(c.getInt(0), c.getString(1),
				ACCOUNT_TYPES.parseInt(c.getInt(2)), c.getInt(3) == 1);
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

		}
		return null;
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

	public static AccountMirakel get(int id) {
		Cursor c = database.query(TABLE, allColumns, DatabaseHelper.ID + " = "
				+ id, null, null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		}
		c.close();
		return null;
	}

	public static List<AccountMirakel> getAll() {
		Cursor c = database.query(TABLE, allColumns, null, null, null, null,
				null);
		List<AccountMirakel> accounts = cursorToAccountList(c);
		c.close();
		return accounts;
	}

	public static AccountMirakel getByName(String name) {
		Cursor c = database.query(TABLE, allColumns, DatabaseHelper.NAME + "='"
				+ name + "'", null, null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		}
		c.close();
		return null;
	}

	public static List<AccountMirakel> getEnabled(boolean isEnabled) {
		Cursor c = database.query(TABLE, allColumns, ENABLED + "="
				+ (isEnabled ? 1 : 0), null, null, null, null);
		List<AccountMirakel> accounts = cursorToAccountList(c);
		c.close();
		return accounts;
	}

	public static AccountMirakel getLocal() {
		Cursor c = database.query(TABLE, allColumns, TYPE + "="
				+ ACCOUNT_TYPES.LOCAL.toInt() + " AND " + ENABLED + "=1", null,
				null, null, null);
		c.moveToFirst();
		if (c.getCount() > 0) {
			AccountMirakel a = cursorToAccount(c);
			c.close();
			return a;
		}
		c.close();
		return newAccount(context.getString(R.string.local_account),
				ACCOUNT_TYPES.LOCAL, true);
	}

	public static void init(Context ctx) {
		AccountMirakel.context = ctx;
		dbHelper = new DatabaseHelper(ctx);
		database = dbHelper.getWritableDatabase();
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

	public static void update(Account[] accounts) {
		List<AccountMirakel> accountList = AccountMirakel.getAll();
		int accountCountStart = accountList.size();
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
			if (el.getValue().getType() != ACCOUNT_TYPES.LOCAL) {
				el.getValue().destroy();
			}
		}
		accountList = AccountMirakel.getAll();
		int accountCountEnd = accountList.size();
		if (Math.abs(accountCountEnd - accountCountStart) == 1
				&& accountCountStart == 1) {
			MirakelPreferences.setShowAccountName(true);
		}

	}


	public AccountMirakel(int id, String name, ACCOUNT_TYPES type,
			boolean enabled) {
		super(id, name, type, enabled);
	}

	public void destroy() {
		if (getType() == ACCOUNT_TYPES.LOCAL) return;
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

	public Account getAndroidAccount() {
		AccountManager am = AccountManager.get(context);
		Account[] accounts = am.getAccountsByType(ACCOUNT_TYPES
				.toName(getType()));
		for (Account a : accounts) {
			if (a.name.equals(getName()))
				return a;
		}
		return null;
	}

	public void save() {
		database.update(TABLE, getContentValues(), DatabaseHelper.ID + "="
				+ getId(), null);
	}

}
