package de.azapps.mirakel.model.semantic;

import java.util.Locale;

import android.content.ContentValues;
import de.azapps.mirakel.model.list.ListMirakel;

class SemanticBase {
    private int id;
    private String condition;
    private Integer priority;
    private Integer due;
    private ListMirakel list;
    private Integer weekday;
    public static final String CONDITION = "condition", PRIORITY = "priority",
                               LIST = "list", DUE = "due", WEEKDAY = "weekday";

    public SemanticBase(final int id, final String condition,
                        final Integer priority, final Integer due, final ListMirakel list,
                        final Integer weekday) {
        super();
        this.id = id;
        this.condition = condition.toLowerCase(Locale.getDefault());
        this.priority = priority;
        this.list = list;
        this.due = due;
        this.weekday = weekday;
    }

    public int getId() {
        return this.id;
    }

    protected void setId(final int id) {
        this.id = id;
    }

    public String getCondition() {
        return this.condition;
    }

    public void setCondition(final String condition) {
        this.condition = condition.toLowerCase(Locale.getDefault());
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
    public String toString() {
        return this.condition;
    }

    public ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.put("_id", this.id);
        cv.put("condition", this.condition);
        cv.put("default_list_id", this.list == null ? null : this.list.getId());
        cv.put("priority", this.priority);
        cv.put("due", this.due);
        cv.put("weekday", this.weekday);
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
        if (this.condition == null) {
            if (other.condition != null) {
                return false;
            }
        } else if (!this.condition.equals(other.condition)) {
            return false;
        }
        if (this.due == null) {
            if (other.due != null) {
                return false;
            }
        } else if (!this.due.equals(other.due)) {
            return false;
        }
        if (this.id != other.id) {
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
                 + (this.condition == null ? 0 : this.condition.hashCode());
        result = prime * result + (this.due == null ? 0 : this.due.hashCode());
        result = prime * result + this.id;
        result = prime * result
                 + (this.list == null ? 0 : this.list.hashCode());
        result = prime * result
                 + (this.priority == null ? 0 : this.priority.hashCode());
        result = prime * result
                 + (this.weekday == null ? 0 : this.weekday.hashCode());
        return result;
    }

}
