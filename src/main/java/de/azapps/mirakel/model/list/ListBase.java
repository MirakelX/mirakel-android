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

package de.azapps.mirakel.model.list;

import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import org.joda.time.DateTime;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountVanishedException;
import de.azapps.mirakel.model.list.ListMirakel.SORT_BY;
import de.azapps.tools.Log;

abstract class ListBase extends ModelBase {
    // db-columns
    public final static String LFT = "lft";
    public final static String RGT = "rgt";
    public final static String COLOR = "color";
    public final static String SORT_BY_FIELD = "sort_by";
    public final static String ACCOUNT_ID = "account_id";
    public final static String ICON_PATH = "icon_path";
    public static final String IS_SPECIAL = "is_special";


    @NonNull
    protected SORT_BY sortBy = SORT_BY.OPT;
    @NonNull
    protected DateTime createdAt;
    @NonNull
    protected DateTime updatedAt;
    @NonNull
    protected SYNC_STATE syncState;
    protected int lft;
    protected int rgt;
    protected int color;
    protected long accountID;
    @Nullable
    private AccountMirakel accountMirakel;

    @NonNull
    protected Optional<Uri> iconPath = Optional.absent();
    protected boolean isSpecial;

    private static final String TAG = "ListBase";

    ListBase() {
        super(0L, "");
    }

    ListBase(final long id, @NonNull final String name, @NonNull final SORT_BY sortBy,
             @NonNull final DateTime createdAt, @NonNull final DateTime updatedAt,
             @NonNull final SYNC_STATE syncState, final int lft, final int rgt,
             final int color, @NonNull final AccountMirakel a, @NonNull final Optional<Uri> iconPath,
             final boolean special) {
        super(id, name);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.setSortBy(sortBy);
        this.setSyncState(syncState);
        this.setLft(lft);
        this.setRgt(rgt);
        this.setColor(color);
        this.setAccount(a);
        this.setIconPath(iconPath);
        this.isSpecial = special;
    }

    protected ListBase(final long id, @NonNull final String name, @NonNull final SORT_BY sortBy,
                       @NonNull final DateTime createdAt, @NonNull final DateTime updatedAt,
                       @NonNull final SYNC_STATE syncState, final int lft, final int rgt,
                       final int color, final int accountId, @NonNull final Optional<Uri> iconPath,
                       final boolean isSpecial) {
        super(id, name);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.setSortBy(sortBy);
        this.setSyncState(syncState);
        this.setLft(lft);
        this.setRgt(rgt);
        this.setColor(color);
        this.setAccount(accountId);
        this.setIconPath(iconPath);
        this.isSpecial = isSpecial;
    }

    public void setListName(@NonNull final String name) throws ListMirakel.ListAlreadyExistsException {
        final Optional<ListMirakel> listMirakel = ListMirakel.getByName(name, getAccount());
        if (listMirakel.isPresent() && listMirakel.get().getId() != getId()) {
            throw new ListMirakel.ListAlreadyExistsException("List " + getName() + " already exists as ID: " +
                    listMirakel.get().getId());
        }
        setName(name);
    }

    @NonNull
    public DateTime getCreatedAt() {
        return this.createdAt;
    }

    protected void setCreatedAt(@NonNull final DateTime createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public DateTime getUpdatedAt() {
        return this.updatedAt;
    }

    protected void setUpdatedAt(@NonNull final DateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public
    @NonNull
    SORT_BY getSortBy() {
        return this.sortBy;
    }

    public void setSortBy(@NonNull final SORT_BY sortBy) {
        this.sortBy = sortBy;
    }

    public int getLft() {
        return this.lft;
    }

    public void setLft(final int lft) {
        this.lft = lft;
    }

    public int getRgt() {
        return this.rgt;
    }

    public void setRgt(final int rgt) {
        this.rgt = rgt;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(final int color) {
        this.color = color;
    }

    @NonNull
    public Optional<Uri> getIconPath() {
        return iconPath;
    }

    @Nullable
    protected String getIconPathString() {
        if (iconPath.isPresent()) {
            return iconPath.get().toString();
        } else {
            return null;
        }
    }


    public void setIconPath(@NonNull Optional<Uri> iconPath) {
        this.iconPath = iconPath;
    }

    @NonNull
    public AccountMirakel getAccount() {
        if (this.accountMirakel == null) {
            Optional<AccountMirakel> accountMirakelOptional = AccountMirakel.get(this.accountID);
            if (accountMirakelOptional.isPresent()) {
                this.accountMirakel = accountMirakelOptional.get();
            } else {
                throw new AccountVanishedException(accountID, getId());
            }
        }
        return this.accountMirakel;
    }

    public void setAccount(@NonNull final AccountMirakel a) {
        accountID = a.getId();
        this.accountMirakel = a;
    }

    protected void setAccount(final long account) {
        this.accountMirakel = null;
        this.accountID = account;
    }

    public boolean isSpecial() {
        return isSpecial;
    }


    @NonNull
    public ContentValues getContentValues() {
        final ContentValues cv;
        try {
            cv = super.getContentValues();
        } catch (DefinitionsHelper.NoSuchListException e) {
            Log.wtf(TAG, "dies could not happen", e);
            return new ContentValues();
        }
        cv.put(ACCOUNT_ID, this.accountID);
        cv.put(DatabaseHelper.CREATED_AT, this.createdAt.getMillis());
        cv.put(DatabaseHelper.UPDATED_AT, this.updatedAt.getMillis());
        cv.put(SORT_BY_FIELD, this.sortBy.getShort());
        cv.put(DatabaseHelper.SYNC_STATE_FIELD, this.syncState.toInt());
        cv.put(LFT, this.lft);
        cv.put(RGT, this.rgt);
        cv.put(COLOR, this.color);
        cv.put(ICON_PATH, getIconPathString());
        cv.put(IS_SPECIAL, isSpecial);
        return cv;
    }

    @NonNull
    public SYNC_STATE getSyncState() {
        return this.syncState;
    }

    public void setSyncState(@NonNull final SYNC_STATE syncState) {
        this.syncState = syncState;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (int) this.accountID;
        result = (prime * result) + (this.getAccount()
                                     .hashCode());
        result = (prime * result) + this.color;
        result = (prime * result) + (this.createdAt.hashCode());
        result = (prime * result) + (int) getId();
        result = (prime * result) + this.lft;
        result = (prime * result) + (getName().hashCode());
        result = (prime * result) + this.rgt;
        result = (prime * result) + this.sortBy.getShort();
        result = (prime * result) + (this.syncState.hashCode());
        result = (prime * result) + (this.updatedAt.hashCode());
        result = (prime * result) + (this.iconPath.hashCode());
        result = (prime * result) + (this.isSpecial ? 42 : 43);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ListBase)) {
            return false;
        }
        final ListBase other = (ListBase) obj;
        if (this.accountID != other.accountID) {
            return false;
        }
        if (this.color != other.color) {
            return false;
        }
        if (!Objects.equal(this.createdAt, other.createdAt)) {
            return false;
        }
        if (this.getId() != other.getId()) {
            return false;
        }
        if (this.lft != other.lft) {
            return false;
        }
        if (!Objects.equal(this.getName(), other.getName())) {
            return false;
        }
        if (this.rgt != other.rgt) {
            return false;
        }
        if (this.sortBy != other.sortBy) {
            return false;
        }
        if (this.syncState != other.syncState) {
            return false;
        }
        if (!Objects.equal(this.iconPath, other.iconPath)) {
            return false;
        }
        if (isSpecial != other.isSpecial) {
            return false;
        }
        return true;
    }

}
