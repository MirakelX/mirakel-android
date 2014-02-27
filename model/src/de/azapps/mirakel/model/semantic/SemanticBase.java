package de.azapps.mirakel.model.semantic;

import java.util.Locale;

import android.content.ContentValues;
import de.azapps.mirakel.model.list.ListMirakel;

public class SemanticBase {
	private int id;
	private String condition;
	private Integer priority;
	private Integer due;
	private ListMirakel list;
	private Integer weekday;
	public static final String CONDITION = "condition", PRIORITY = "priority",
			LIST = "list", DUE = "due", WEEKDAY = "weekday";

	public SemanticBase(int id, String condition, Integer priority,
			Integer due, ListMirakel list, Integer weekday) {
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

	public void setId(int id) {
		this.id = id;
	}

	public String getCondition() {
		return this.condition;
	}

	public void setCondition(String condition) {
		this.condition = condition.toLowerCase(Locale.getDefault());
	}

	public Integer getPriority() {
		return this.priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Integer getDue() {
		return this.due;
	}

	public void setDue(Integer due) {
		this.due = due;
	}

	public ListMirakel getList() {
		return this.list;
	}

	public void setList(ListMirakel list) {
		this.list = list;
	}

	public Integer getWeekday() {
		return this.weekday;
	}

	public void setWeekday(Integer weekday) {
		this.weekday = weekday;
	}

	@Override
	public String toString() {
		return this.condition;
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("_id", this.id);
		cv.put("condition", this.condition);
		cv.put("default_list_id", this.list == null ? null : this.list.getId());
		cv.put("priority", this.priority);
		cv.put("due", this.due);
		cv.put("weekday", this.weekday);
		return cv;
	}

}
