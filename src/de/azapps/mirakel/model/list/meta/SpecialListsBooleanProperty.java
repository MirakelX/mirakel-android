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

package de.azapps.mirakel.model.list.meta;


import android.os.Parcel;
import android.support.annotation.NonNull;

public abstract class SpecialListsBooleanProperty extends
    SpecialListsBaseProperty {
    protected boolean isSet;

    @NonNull
    abstract protected String getPropertyName();

    public SpecialListsBooleanProperty(final boolean isNegated) {
        this.isSet = isNegated;
    }

    protected SpecialListsBooleanProperty(final @NonNull Parcel in) {
        this.isSet = in.readByte() != 0;
    }

    protected SpecialListsBooleanProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        isSet = oldProperty instanceof SpecialListsBooleanProperty &&
                ((SpecialListsBooleanProperty) oldProperty).isSet;
    }

    public boolean isSet() {
        return this.isSet;
    }

    public void setIsNegated(final boolean isNegated) {
        this.isSet = isNegated;
    }

    @NonNull
    @Override
    public String serialize() {
        String ret = "{\"" + getPropertyName() + "\":{";
        ret += "\"done\":" + (this.isSet ? "true" : "false");
        return ret + "} }";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeByte(isSet ? (byte) 1 : (byte) 0);
    }

}
