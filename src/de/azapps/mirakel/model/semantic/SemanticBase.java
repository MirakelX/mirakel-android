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

package de.azapps.mirakel.model.semantic;

import android.content.ContentValues;

import java.util.Locale;

import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;

abstract class SemanticBase  extends ModelBase {
    private Integer priority;
    private Integer due;
    private ListMirakel list;
    private Integer weekday;
    public static final String CONDITION = "condition", PRIORITY = "priority",
                               LIST = "default_list_id", DUE = "due", WEEKDAY = "weekday";

    public SemanticBase(final int id, final String condition,
                        final Integer priority, final Integer due, final ListMirakel list,
                        final Integer weekday) {
        super(id, condition.toLowerCase(Locale.getDefault()));
        this.priority = priority;
        this.list = list;
        this.due = due;
        this.weekday = weekday;
    }


    public String getCondition() {
        return getName();
    }

    public void setCondition(final String condition) {
        setName(condition.toLowerCase(Locale.getDefault()));
    }

    public Integer getPriority() {
        return this.priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public Integer getDue() {
        return this.due;
    }

    public void setDue(final Integer due) {
        this.due = due;
    }

    public ListMirakel getList() {
        return this.list;
    }

    public void setList(final ListMirakel list) {
        this.list = list;
    }

    public Integer getWeekday() {
        return this.weekday;
    }

    public void setWeekday(final Integer weekday) {
        this.weekday = weekday;
    }


    @Override
    public ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.put(ID, getId());
        cv.put(CONDITION, getName());
        cv.put(LIST, this.list == null ? null : this.list.getId());
        cv.put(PRIORITY, this.priority);
        cv.put(DUE, this.due);
        cv.put(WEEKDAY, this.weekday);
        return cv;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SemanticBase)) {
            return false;
        }
        final SemanticBase other = (SemanticBase) obj;
        if (this.getName() == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!this.getName().equals(other.getName())) {
            return false;
        }
        if (this.due == null) {
            if (other.due != null) {
                return false;
            }
        } else if (!this.due.equals(other.due)) {
            return false;
        }
        if (this.getId() != other.getId()) {
            return false;
        }
        if (this.list == null) {
            if (other.list != null) {
                return false;
            }
        } else if (!this.list.equals(other.list)) {
            return false;
        }
        if (this.priority == null) {
            if (other.priority != null) {
                return false;
            }
        } else if (!this.priority.equals(other.priority)) {
            return false;
        }
        if (this.weekday == null) {
            if (other.weekday != null) {
                return false;
            }
        } else if (!this.weekday.equals(other.weekday)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + (this.getName() == null ? 0 : this.getName().hashCode());
        result = prime * result + (this.due == null ? 0 : this.due.hashCode());
        result = prime * result + (int)this.getId();
        result = prime * result
                 + (this.list == null ? 0 : this.list.hashCode());
        result = prime * result
                 + (this.priority == null ? 0 : this.priority.hashCode());
        result = prime * result
                 + (this.weekday == null ? 0 : this.weekday.hashCode());
        return result;
    }

}
