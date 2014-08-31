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

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.Locale;

import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;

abstract class SemanticBase  extends ModelBase {
    protected Integer priority;
    protected Integer due;
    protected Optional<ListMirakel> list = absent();
    protected Integer weekday;
    public static final String CONDITION = "condition", PRIORITY = "priority",
                               LIST = "default_list_id", DUE = "due", WEEKDAY = "weekday";

    public SemanticBase(final int id, final String condition,
                        final Integer priority, final Integer due, final Optional<ListMirakel> list,
                        final Integer weekday) {
        super(id, condition.toLowerCase(Locale.getDefault()));
        this.priority = priority;
        this.list = list;
        this.due = due;
        this.weekday = weekday;
    }
    SemanticBase(final long id, final String condition) {
        super(id, condition);
    }

    protected SemanticBase() {
        // Just for Parcelable
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

    public Optional<ListMirakel> getList() {
        return this.list;
    }

    public void setList(final Optional<ListMirakel> list) {
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
        cv.put(LIST, OptionalUtils.transformOrNull(this.list, new Function<ListMirakel, Long>() {
            @Override
            public Long apply(ListMirakel input) {
                return input.getId();
            }
        }));
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
        if (!this.list.isPresent()) {
            if (other.list.isPresent()) {
                return false;
            }
        } else if (!this.list.get().equals(other.list.get())) {
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
        int listNum = 0;
        if (this.list.isPresent()) {
            listNum = this.list.get().hashCode();
        }
        result = prime * result
                 + listNum;
        result = prime * result
                 + (this.priority == null ? 0 : this.priority.hashCode());
        result = prime * result
                 + (this.weekday == null ? 0 : this.weekday.hashCode());
        return result;
    }

}
