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

package de.azapps.mirakel.model.list;

import android.content.ContentValues;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel.SORT_BY;
import de.azapps.tools.Log;

abstract class ListBase  extends ModelBase {
    // db-columns
    public final static String LFT = "lft";
    public final static String RGT = "rgt";
    public final static String COLOR = "color";
    public final static String SORT_BY_FIELD = "sort_by";
    public final static String ACCOUNT_ID = "account_id";



    private SORT_BY sortBy;
    private String createdAt;
    private String updatedAt;
    private SYNC_STATE syncState;
    private int lft, rgt;
    private int color;
    private long accountID;
    private AccountMirakel accountMirakel;
    protected boolean isSpecial = false;

    private static final String TAG = "ListBase";

    ListBase() {
        super(0, "");
    }

    ListBase(final long id, final String name, final SORT_BY sortBy,
             final String createdAt, final String updatedAt,
             final SYNC_STATE syncState, final int lft, final int rgt,
             final int color, final AccountMirakel a) {
        super(id, name);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
        this.setSortBy(sortBy);
        this.setSyncState(syncState);
        this.setLft(lft);
        this.setRgt(rgt);
        this.setColor(color);
        this.setAccount(a);
    }

    ListBase(final long id, final String name) {
        super(id, name);
    }

    protected ListBase(final long id, final String name, final SORT_BY sortBy,
                       final String createdAt, final String updatedAt,
                       final SYNC_STATE syncState, final int lft, final int rgt,
                       final int color, final int account) {
        super(id, name);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
        this.setSortBy(sortBy);
        this.setSyncState(syncState);
        this.setLft(lft);
        this.setRgt(rgt);
        this.setColor(color);
        this.setAccount(account);
    }

    public void setListName(final String name) throws ListMirakel.ListAlreadyExistsException {
        ListMirakel listMirakel = ListMirakel.getByName(name, getAccount());
        if (listMirakel != null && listMirakel.getId() != getId()) {
            throw new ListMirakel.ListAlreadyExistsException("List " + getName() + " already exists as ID: " +
                    listMirakel.getId());
        }
        setName(name);
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(final String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public SORT_BY getSortBy() {
        return this.sortBy;
    }

    public void setSortBy(final SORT_BY sortBy) {
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

    public AccountMirakel getAccount() {
        if (this.accountMirakel != null) {
            return this.accountMirakel;
        }
        return AccountMirakel.get(this.accountID);
    }

    public void setAccount(final AccountMirakel a) {
        setAccount(a.getId());
        this.accountMirakel = a;
    }

    protected void setAccount(final long account) {
        this.accountMirakel = null;
        this.accountID = account;
    }

    public boolean isSpecial() {
        return this.isSpecial;
    }


    public ContentValues getContentValues() {
        final ContentValues cv;
        try {
            cv = super.getContentValues();
        } catch (DefinitionsHelper.NoSuchListException e) {
            Log.wtf(TAG, "dies could not happen", e);
            return new ContentValues();
        }
        cv.put(DatabaseHelper.CREATED_AT, this.createdAt);
        cv.put(DatabaseHelper.UPDATED_AT, this.updatedAt);
        cv.put(SORT_BY_FIELD, this.sortBy.getShort());
        cv.put(DatabaseHelper.SYNC_STATE_FIELD, this.syncState.toInt());
        cv.put(LFT, this.lft);
        cv.put(RGT, this.rgt);
        cv.put(COLOR, this.color);
        cv.put(ACCOUNT_ID, this.accountID);
        return cv;
    }

    public SYNC_STATE getSyncState() {
        return this.syncState;
    }

    public void setSyncState(final SYNC_STATE syncState) {
        this.syncState = syncState;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)this.accountID;
        result = prime
                 * result
                 + (this.getAccount() == null ? 0 : this.getAccount()
                    .hashCode());
        result = prime * result + this.color;
        result = prime * result
                 + (this.createdAt == null ? 0 : this.createdAt.hashCode());
        result = prime * result + (int)getId();
        result = prime * result + (this.isSpecial ? 1231 : 1237);
        result = prime * result + this.lft;
        result = prime * result
                 + (getName() == null ? 0 : getName().hashCode());
        result = prime * result + this.rgt;
        result = prime * result + this.sortBy.getShort();
        result = prime * result
                 + (this.syncState == null ? 0 : this.syncState.hashCode());
        result = prime * result
                 + (this.updatedAt == null ? 0 : this.updatedAt.hashCode());
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
        if (this.getAccount() == null) {
            if (other.getAccount() != null) {
                return false;
            }
        } else if (!this.getAccount().equals(other.getAccount())) {
            return false;
        }
        if (this.color != other.color) {
            return false;
        }
        if (this.createdAt == null) {
            if (other.createdAt != null) {
                return false;
            }
        } else if (!this.createdAt.equals(other.createdAt)) {
            return false;
        }
        if (this.getId() != other.getId()) {
            return false;
        }
        if (this.isSpecial != other.isSpecial) {
            return false;
        }
        if (this.lft != other.lft) {
            return false;
        }
        if (getName() == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!this.getName().equals(other.getName())) {
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
        return true;
    }

}
