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

import android.content.ContentValues;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;

abstract class AccountBase extends ModelBase {
    private final static String TAG = "AccountBase";

    public final static String TYPE = "type";
    public final static String ENABLED = "enabled";
    public static final String SYNC_KEY = "sync_key";

    @NonNull
    protected ACCOUNT_TYPES type = ACCOUNT_TYPES.LOCAL;
    protected boolean enabled;
    @NonNull
    protected Optional<String> syncKey = absent();

    public AccountBase(final int id, final String name,
                       @NonNull final ACCOUNT_TYPES type, final boolean enabled,
                       @NonNull final Optional<String> syncKey) {
        super(id, name);
        this.setType(type);
        this.setEnabled(enabled);
        this.setSyncKey(syncKey);
    }

    protected AccountBase() {
        super();
        // Do nothing thats just for the Parcelable stuff
    }


    public ACCOUNT_TYPES getType() {
        return this.type;
    }

    public void setType(@NonNull final ACCOUNT_TYPES type) {
        this.type = type;
    }

    @NonNull
    public ContentValues getContentValues() {
        final ContentValues cv;
        try {
            cv = super.getContentValues();
        } catch (DefinitionsHelper.NoSuchListException e) {
            Log.wtf(TAG, "how could this ever happen?", e);
            return new ContentValues();
        }
        cv.put(TYPE, this.type.toInt());
        cv.put(ENABLED, this.enabled);
        cv.put(SYNC_KEY, this.syncKey.orNull());
        return cv;
    }


    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public Optional<String> getSyncKey() {
        return this.syncKey;
    }

    public void setSyncKey(final Optional<String> syncKey) {
        this.syncKey = syncKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) getId();
        result = prime * result + (this.enabled ? 1231 : 1237);
        result = prime * result + (getName().hashCode());
        result = prime * result
                 + (!this.syncKey.isPresent() ? 0 : this.syncKey.get().hashCode());
        result = prime * result + this.type.toInt();
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
        if (!(obj instanceof AccountBase)) {
            return false;
        }
        final AccountBase other = (AccountBase) obj;
        if (this.getId() != other.getId()) {
            return false;
        }
        if (this.enabled != other.enabled) {
            return false;
        }
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        if (this.syncKey.isPresent() && other.syncKey.isPresent()) {
            if (!this.syncKey.get().equals(other.syncKey.get())) {
                return false;
            }
        } else if (this.syncKey.isPresent() != other.syncKey.isPresent()) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }

}
