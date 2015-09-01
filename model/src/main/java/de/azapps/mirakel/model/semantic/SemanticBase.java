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

package de.azapps.mirakel.model.semantic;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.Locale;

import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

abstract class SemanticBase  extends ModelBase {
    @NonNull
    protected Optional<Integer> priority = absent();
    @NonNull
    protected Optional<Integer> due = absent();
    @NonNull
    protected Optional<ListMirakel> list = absent();
    @NonNull
    protected Optional<Integer> weekday = absent();

    @NonNull
    public static final String CONDITION = "condition", PRIORITY = "priority",
                               LIST = "default_list_id", DUE = "due", WEEKDAY = "weekday";

    public SemanticBase(final int id, final @NonNull String condition,
                        final @Nullable Integer priority, final @Nullable Integer due,
                        final @NonNull Optional<ListMirakel> list,
                        final @Nullable Integer weekday) {
        super(id, condition.toLowerCase(Locale.getDefault()));
        this.priority = fromNullable(priority);
        this.list = list;
        this.due = fromNullable(due);
        this.weekday = fromNullable(weekday);
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

    @NonNull
    public Optional<Integer> getPriority() {
        return priority;
    }

    public void setPriority(final @NonNull Optional<Integer> priority) {
        this.priority = priority;
    }

    @NonNull
    public Optional<Integer> getDue() {
        return due;
    }

    public void setDue(final @NonNull Optional<Integer> due) {
        this.due = due;
    }

    @NonNull
    public Optional<ListMirakel> getList() {
        return list;
    }

    public void setList(final @NonNull Optional<ListMirakel> list) {
        this.list = list;
    }

    @NonNull
    public Optional<Integer> getWeekday() {
        return weekday;
    }

    public void setWeekday(final @NonNull Optional<Integer> weekday) {
        this.weekday = weekday;
    }

    @Override
    @NonNull
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
        cv.put(PRIORITY, this.priority.orNull());
        cv.put(DUE, this.due.orNull());
        cv.put(WEEKDAY, this.weekday.orNull());
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
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        if (this.due.isPresent() != other.due.isPresent()) {
            return false;
        } else if (due.isPresent() && !due.get().equals(other.due.get())) {
            return false;
        }
        if (this.getId() != other.getId()) {
            return false;
        }
        if (this.list.isPresent() != other.list.isPresent()) {
            return false;
        } else if (list.isPresent() && !list.get().equals(other.list.get())) {
            return false;
        }
        if (this.priority.isPresent() != other.priority.isPresent()) {
            return false;
        } else if (priority.isPresent() && !priority.get().equals(other.priority.get())) {
            return false;
        }
        if (this.weekday.isPresent() != other.weekday.isPresent()) {
            return false;
        } else if (weekday.isPresent() && !weekday.get().equals(other.weekday.get())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.getName().hashCode());
        result = prime * result + (this.due.hashCode());
        result = prime * result + (int)this.getId();
        int listNum = 0;
        if (this.list.isPresent()) {
            listNum = this.list.get().hashCode();
        }
        result = prime * result
                 + listNum;
        result = prime * result + (this.priority.hashCode());
        result = prime * result + (this.weekday.hashCode());
        return result;
    }

}
