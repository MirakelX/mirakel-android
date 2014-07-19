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

package de.azapps.mirakel.model.list.meta;

import android.content.Context;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class SpecialListsDueProperty extends SpecialListsBaseProperty {

    public enum Unit {
        DAY, MONTH, YEAR
    }

    private static final String TAG = "SpecialListsDueProperty";

    protected Unit unit;
    protected int lenght;

    public SpecialListsDueProperty(final Unit unit, final int length) {
        this.lenght = length;
        this.unit = unit;
        if (this.unit == null) {
            this.unit = Unit.DAY;
        }
    }

    public Unit getUnit() {
        return this.unit;
    }

    public int getLenght() {
        return this.lenght;
    }

    public void setLenght(final int l) {
        this.lenght = l;
    }

    public void setUnit(final Unit u) {
        this.unit = u;
    }

    @Override
    public String getWhereQuery() {
        String query = Task.DUE + " IS NOT NULL AND date(" + Task.DUE
                       + ",'unixepoch','localtime')<=date('now','";
        if (this.lenght == 0) {
            return query + "localtime')";
        }
        if (this.lenght > 0) {
            query += "+";
        }
        query += this.lenght + " ";
        switch (this.unit) {
        case DAY:
            query += "day";
            break;
        case MONTH:
            query += "month";
            break;
        case YEAR:
            query += "year";
            break;
        default:
            break;
        }
        return query + "','localtime')";
    }

    @Override
    public String serialize() {
        if (this.unit == null) {
            Log.wtf(TAG, "unit is null");
        }
        String ret = "\"" + Task.DUE + "\":{";
        ret += "\"unit\":" + this.unit.ordinal();
        ret += ",\"length\":" + this.lenght;
        return ret + "}";
    }

    @Override
    public String getSummary(final Context mContext) {
        return this.lenght
               + " "
               + mContext.getResources().getStringArray(
                   this.lenght == 1 ? R.array.due_day_year_values
                   : R.array.due_day_year_values_plural)[this.unit
                           .ordinal()];
        // TODO use plurals here
    }

}
