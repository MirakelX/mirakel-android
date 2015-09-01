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

package de.azapps.mirakel.model.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.Cursor2List;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.CursorWrapper;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.fromNullable;

public class AccountMirakel extends AccountBase {
    private static final List<Integer> MOVABLE_TO_ACCOUNT_TYPES = new ArrayList<>(Arrays.asList(
                ACCOUNT_TYPES.LOCAL.toInt(), ACCOUNT_TYPES.TASKWARRIOR.toInt()));
    private static final CursorWrapper.CursorConverter<List<AccountMirakel>> LIST_FROM_CURSOR = new
    Cursor2List<>(AccountMirakel.class);

    public enum ACCOUNT_TYPES {
        ALL, CALDAV, LOCAL, TASKWARRIOR;

        public static ACCOUNT_TYPES parseAccountType(final String type) {
            switch (type) {
            case ACCOUNT_TYPE_DAVDROID:
            case ACCOUNT_TYPE_DAVDROID_MIRAKEL:
                return CALDAV;
            case ACCOUNT_TYPE_MIRAKEL:
                return TASKWARRIOR;
            case ACCOUNT_TYPE_ALL:
                return ALL;
            default:
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
            case 3:
                return ALL;
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
            case ALL:
                return ACCOUNT_TYPE_ALL;
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
            case ALL:
                return 3;
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
            case ALL:
                return ctx.getString(R.string.accounts_all);
            default:
                return "Unknown account type";
            }
        }

        public boolean isListEditable() {
            switch (this) {
            case CALDAV:
                return false;
            case LOCAL:
                return true;
            case TASKWARRIOR:
                return true;
            case ALL:
            default:
                throw new RuntimeException("Unknown account type");
            }
        }

        public boolean isListDeletable() {
            return isListEditable();
        }
    }

    public static final String ACCOUNT_TYPE_ALL = "universe.all";
    public static final String ACCOUNT_TYPE_DAVDROID = "bitfire.at.davdroid";
    public static final String ACCOUNT_TYPE_DAVDROID_MIRAKEL = "bitfire.at.davdroid.mirakel";
    public static final String ACCOUNT_TYPE_DMFS = "org.dmfs.caldav.account";

    public static final String ACCOUNT_TYPE_MIRAKEL = "de.azapps.mirakel";

    private static final List<String> allowedAccounts = Arrays.asList(ACCOUNT_TYPE_DAVDROID_MIRAKEL,
            ACCOUNT_TYPE_DAVDROID, ACCOUNT_TYPE_MIRAKEL, ACCOUNT_TYPE_DMFS);

    public static final String[] allColumns = {ModelBase.ID,
                                               ModelBase.NAME, "content", TYPE, ENABLED, SYNC_KEY
                                              };
    public static final Uri URI = MirakelInternalContentProvider.ACCOUNT_URI;

    public static final String TABLE = "account";

    private static final String TAG = "Account";

    @Override
    protected Uri getUri() {
        return URI;
    }



    public AccountMirakel(final CursorGetter c) {
        super(c.getInt(ID), c.getString(NAME),
              ACCOUNT_TYPES.parseInt(c.getInt(TYPE)), c.getInt(ENABLED) == 1,
              fromNullable(c.getString(SYNC_KEY)));
    }

    public static Optional<AccountMirakel> get(final Account account) {
        return new MirakelQueryBuilder(context).and(NAME, Operation.EQ,
                account.name).get(AccountMirakel.class);
    }

    public static Optional<AccountMirakel> get(final long id) {
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

    public static long countMovableTo() {
        return allMovableToMQB().count(AccountMirakel.URI);
    }

    private static MirakelQueryBuilder allMovableToMQB() {
        return new MirakelQueryBuilder(context).and(TYPE, Operation.IN, MOVABLE_TO_ACCOUNT_TYPES);
    }

    /**
     * This is a hack to add the "All Accounts" item in the Spinner
     *
     * @return
     */
    public static Cursor allCursorWithAllAccounts() {
        final MatrixCursor extras = new MatrixCursor(allColumns);
        extras.addRow(new String[] {"-1", ACCOUNT_TYPES.ALL.typeName(context), null, String.valueOf(ACCOUNT_TYPES.ALL.toInt()), "1", null});
        final CursorWrapper allCursor = allCursor();
        return new MergeCursor(new Cursor[] {extras, allCursor.getRawCursor()});
    }

    public static CursorWrapper allCursor() {
        return new MirakelQueryBuilder(context).query(MirakelInternalContentProvider.ACCOUNT_URI);
    }
    public static CursorWrapper allMovableToCursor() {
        return allMovableToMQB().query(MirakelInternalContentProvider.ACCOUNT_URI);
    }

    public static List<AccountMirakel> getEnabled(final boolean isEnabled) {
        return new MirakelQueryBuilder(context).and(ENABLED, Operation.EQ,
                isEnabled).getList(AccountMirakel.class);
    }

    public static AccountMirakel getLocal() {
        final Optional<AccountMirakel> a = new MirakelQueryBuilder(context).and(TYPE, Operation.EQ,
                ACCOUNT_TYPES.LOCAL.toInt()).and(ENABLED, Operation.EQ,
                        true).get(AccountMirakel.class);
        if (a.isPresent()) {
            return a.get();
        }
        return newAccount(context.getString(R.string.local_account),
                          ACCOUNT_TYPES.LOCAL, true);
    }

    public static List<AccountMirakel> getRemote() {
        return new MirakelQueryBuilder(context).and(TYPE, Operation.NOT_EQ,
                ACCOUNT_TYPES.LOCAL.toInt()).and(ENABLED, Operation.EQ,
                        true).getList(AccountMirakel.class);
    }


    @NonNull
    public static AccountMirakel newAccount(final String name,
                                            final ACCOUNT_TYPES type, final boolean enabled) {
        final ContentValues cv = new ContentValues();
        cv.put(ModelBase.NAME, name);
        cv.put(TYPE, type.toInt());
        cv.put(ENABLED, enabled);
        final long id = insert(URI, cv);
        return get(id).get();
    }

    public static void update(final Account[] accounts) {
        final List<AccountMirakel> existingAccounts = new ArrayList<>(accounts.length);
        existingAccounts.add(AccountMirakel.getLocal());
        for (final Account a : accounts) {
            if (allowedAccounts.contains(a.type)) {
                Optional<AccountMirakel> existing = AccountMirakel.getByName(a.name);
                if (existing.isPresent()) {
                    existingAccounts.add(existing.get());
                } else {
                    existingAccounts.add(AccountMirakel.newAccount(a.name,
                                         ACCOUNT_TYPES.parseAccountType(a.type), true));
                }
            }
        }

        final MirakelQueryBuilder del = new MirakelQueryBuilder(context).and(ID, Operation.NOT_IN,
                existingAccounts);
        delete(URI, del.getSelection(), del.getSelectionArguments().toArray(new String[0]));
    }

    @NonNull
    private static Optional<AccountMirakel> getByName(final @NonNull String name) {
        return new MirakelQueryBuilder(context).and(NAME, Operation.EQ, name).get(AccountMirakel.class);
    }

    public AccountMirakel(final int id, final String name,
                          final ACCOUNT_TYPES type, final boolean enabled,
                          @NonNull final Optional<String> syncKey) {
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

    public static boolean hasTaskWarriorAccount() {
        return new MirakelQueryBuilder(context).and(ENABLED, Operation.EQ, true).and(TYPE, Operation.EQ,
                ACCOUNT_TYPES.TASKWARRIOR.toInt()).count(URI) > 0;
    }

    public static boolean hasSyncAccount() {
        return countRemoteAccounts() > 0;
    }

    // Parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(this.type.toInt());
        dest.writeByte(enabled ? (byte) 1 : (byte) 0);
        dest.writeString(this.syncKey.orNull());
        dest.writeLong(getId());
        dest.writeString(getName());
    }

    private AccountMirakel(Parcel in) {
        super();
        this.type = ACCOUNT_TYPES.parseInt(in.readInt());
        this.enabled = in.readByte() != 0;
        this.syncKey = fromNullable(in.readString());
        setId(in.readLong());
        setName(in.readString());
    }

    public static final Creator<AccountMirakel> CREATOR = new Creator<AccountMirakel>() {
        public AccountMirakel createFromParcel(final Parcel source) {
            return new AccountMirakel(source);
        }

        public AccountMirakel[] newArray(final int size) {
            return new AccountMirakel[size];
        }
    };
}
