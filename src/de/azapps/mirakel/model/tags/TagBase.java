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

package de.azapps.mirakel.model.tags;

import android.content.ContentValues;
import android.graphics.Color;
import android.support.annotation.NonNull;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.tools.Log;

abstract class TagBase extends ModelBase {
    private static final String TAG = "TagBase";

    public static final String DARK_TEXT = "dark_text";
    public static final String BACKGROUND_COLOR = "color";

    protected boolean isDarkText;
    // Color as specified in android.graphics.Color
    protected int backgroundColor;

    public TagBase(final long id, @NonNull final String name, final int backColor, final boolean dark) {
        super(id, name);
        setBackgroundColor(backColor);
        setDarkText(dark);
    }

    public TagBase() {
        // Do nothing
    }

    @Override
    @NonNull
    public ContentValues getContentValues() {
        final ContentValues cv;
        try {
            cv = super.getContentValues();
        } catch (DefinitionsHelper.NoSuchListException e) {
            Log.wtf(TAG, "How could this happen? ", e);
            return new ContentValues();
        }
        cv.put(TagBase.DARK_TEXT, this.isDarkText);
        cv.put(TagBase.BACKGROUND_COLOR, this.backgroundColor);
        return cv;
    }

    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(final int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean isDarkText() {
        return this.isDarkText;
    }

    public void setDarkText(final boolean isDarkText) {
        this.isDarkText = isDarkText;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)this.getId();
        result = prime * result + this.backgroundColor;
        result = prime * result + (this.isDarkText ? 1231 : 1237);
        result = prime * result + (this.getName().hashCode());
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
        if (!(obj instanceof TagBase)) {
            return false;
        }
        final TagBase other = (TagBase) obj;
        if (this.getId() != other.getId()) {
            return false;
        }
        if (this.backgroundColor != other.backgroundColor) {
            return false;
        }
        if (this.isDarkText != other.isDarkText) {
            return false;
        }
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        return true;
    }

}
