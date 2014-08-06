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

package de.azapps.mirakel.model.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.tools.Log;

public class AccountMirakel extends AccountBase {
    public enum ACCOUNT_TYPES {
        CALDAV, LOCAL, TASKWARRIOR;
        public static ACCOUNT_TYPES getSyncType(final String type) {
            if (type.equals("Taskwarrior")) {
                return TASKWARRIOR;
            } else if (type.equals("CalDav")) {
                return CALDAV;
            } else {
                return LOCAL;
            }
        }

        public static ACCOUNT_TYPES parseAccountType(final String type) {
            if (ACCOUNT_TYPE_DAVDROID.equals(type) || ACCOUNT_TYPE_DAVDROID_MIRAKEL.equals(type)) {
                return CALDAV;
            } else if (ACCOUNT_TYPE_MIRAKEL.equals(type)) {
                return TASKWARRIOR;
            } else {
                return LOCAL;
            }
        }

        public static ACCOUNT_TYPES parseInt(final int i) {
            switch (i) {
            case -1:
                return LOCAL;
            case 1:
                return CALDAV;
            case 2:
                return TASKWARRIOR;
            default:
                throw new IllegalArgumentException();
            }
        }

        public static String toName(final ACCOUNT_TYPES type) {
            switch (type) {
            case CALDAV:
                return ACCOUNT_TYPE_DAVDROID;
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
            case TASKWARRIOR:
                return 2;
            default:
                throw new RuntimeException();
            }
        }

        public String typeName(final Context ctx) {
            switch (this) {
            case CALDAV:
                return ctx.getString(R.string.calDavName);
            case LOCAL:
                return ctx.getString(R.string.local_account);
            case TASKWARRIOR:
                return ctx.getString(R.string.tw_account);
            default:
                return "Unkown account type";
            }
        }
    }

    public static final String ACCOUNT_TYPE_DAVDROID = "bitfire.at.davdroid";
    public static final String ACCOUNT_TYPE_DAVDROID_MIRAKEL = "bitfire.at.davdroid.mirakel";

    public static final String ACCOUNT_TYPE_MIRAKEL = "de.azapps.mirakel";

    private static final List<String> allowedAccounts = Arrays.asList(new String[] {ACCOUNT_TYPE_DAVDROID_MIRAKEL, ACCOUNT_TYPE_DAVDROID, ACCOUNT_TYPE_MIRAKEL});

    public static final String[] allColumns = { ModelBase.ID,
                                                ModelBase.NAME, TYPE, ENABLED, SYNC_KEY
                                              };
    public static final Uri URI = MirakelInternalContentProvider.ACCOUNT_URI;

    public static final String TABLE = "account";

    private static final String TAG = "Account";

    protected Uri getUri() {
        return URI;
    }

    public static List<AccountMirakel> cursorToAccountList(final Cursor c) {
        List<AccountMirakel> l = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                l.add(new AccountMirakel(c));
            } while (c.moveToNext());
        }
        return l;
    }


    public AccountMirakel(final Cursor c) {
        super(c.getInt(c.getColumnIndex(ID)), c.getString(c.getColumnIndex(NAME)),
              ACCOUNT_TYPES.parseInt(c.getInt(c.getColumnIndex(TYPE))), c.getInt(c.getColumnIndex(ENABLED)) == 1,
              c.getString(c.getColumnIndex(SYNC_KEY)));
    }

    public static AccountMirakel get(final Account account) {
        return new MirakelQueryBuilder(context).and(NAME, Operation.EQ,
                account.name).get(AccountMirakel.class);
    }

    public static AccountMirakel get(final long id) {
        return new MirakelQueryBuilder(context).get(AccountMirakel.class, id);
    }

    public static long countRemoteAccounts() {
        return new MirakelQueryBuilder(context).
               and (TYPE, Operation.NOT_EQ, ACCOUNT_TYPES.LOCAL.toInt()).and(ENABLED,
                       Operation.EQ, true).count(URI);
    }

    public static List<AccountMirakel> all() {
        return new MirakelQueryBuilder(context).getList(AccountMirakel.class);
    }

    public static List<AccountMirakel> getEnabled(final boolean isEnabled) {
        return new MirakelQueryBuilder(context).and(ENABLED, Operation.EQ,
                isEnabled).getList(AccountMirakel.class);
    }

    public static AccountMirakel getLocal() {
        AccountMirakel a = new MirakelQueryBuilder(context).and(TYPE, Operation.EQ,
                ACCOUNT_TYPES.LOCAL.toInt()).and(ENABLED, Operation.EQ,
                        true).get(AccountMirakel.class);
        if (a != null) {
            return a;
        }
        return newAccount(context.getString(R.string.local_account),
                          ACCOUNT_TYPES.LOCAL, true);
    }

    public static List<AccountMirakel> getRemote() {
        return new MirakelQueryBuilder(context).and(TYPE, Operation.NOT_EQ,
                ACCOUNT_TYPES.LOCAL.toInt()).and(ENABLED, Operation.EQ,
                        true).getList(AccountMirakel.class);
    }


    public static AccountMirakel newAccount(final String name,
                                            final ACCOUNT_TYPES type, final boolean enabled) {
        final ContentValues cv = new ContentValues();
        cv.put(ModelBase.NAME, name);
        cv.put(TYPE, type.toInt());
        cv.put(ENABLED, enabled);
        final long id = insert(URI, cv);
        return get(id);
    }

    public static void update(final Account[] accounts) {
        final List<AccountMirakel> accountList = AccountMirakel.all();
        final long countRemotes = AccountMirakel.countRemoteAccounts();
        final Map<String, AccountMirakel> map = new HashMap<>();
        for (final AccountMirakel a : accountList) {
            map.put(a.getName(), a);
        }
        for (final Account a : accounts) {
            if (allowedAccounts.contains(a.type)) {
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
        for (final Entry<String, AccountMirakel> el : map.entrySet()) {
            // Remove deleted accounts
            if (el.getValue().getType() != ACCOUNT_TYPES.LOCAL) {
                el.getValue().destroy();
            }
        }
        final long countRemotesNow = AccountMirakel.countRemoteAccounts();
        if (countRemotes == 0 && countRemotesNow == 1) {
            // If we just added our first remote account we want to set it as
            // the default one.
            final List<AccountMirakel> remotes = AccountMirakel.getRemote();
            // This could happen, the operations are not atomar
            if (remotes.size() != 0 && remotes.get(0).getType() != ACCOUNT_TYPES.CALDAV) {
                final AccountMirakel account = remotes.get(0);
                MirakelModelPreferences.setDefaultAccount(account);
                ListMirakel.setDefaultAccount(account);
            }
        } else if (countRemotes == 1 && countRemotesNow > 1) {
            // If we have now more than one remote account we want to show the
            // account name in the listfragment
            MirakelCommonPreferences.setShowAccountName(true);
        }
    }

    public AccountMirakel(final int id, final String name,
                          final ACCOUNT_TYPES type, final boolean enabled,
                          final String syncKey) {
        super(id, name, type, enabled, syncKey);
    }

    @Override
    public void destroy() {
        if (getType() == ACCOUNT_TYPES.LOCAL) {
            return;
        }
        MirakelInternalContentProvider.withTransaction(new MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                AccountMirakel.super.destroy();
                final ContentValues cv = new ContentValues();
                cv.put(ListMirakel.ACCOUNT_ID, getLocal().getId());
                update(MirakelInternalContentProvider.LIST_URI, cv, "account_id=" + getId(), null);
                final Account a = getAndroidAccount();
                if (a == null) {
                    Log.wtf(TAG, "account not found");
                    return;
                }
                AccountManager.get(context).removeAccount(a, null, null);
            }
        });
    }

    public Account getAndroidAccount(final Context ctx) {
        AccountMirakel.context = ctx;
        return getAndroidAccount();
    }

    public Account getAndroidAccount() {
        final AccountManager am = AccountManager.get(context);
        final Account[] accounts = am.getAccountsByType(ACCOUNT_TYPES
                                   .toName(getType()));
        for (final Account a : accounts) {
            if (a.name.equals(getName())) {
                return a;
            }
        }
        return null;
    }


}
