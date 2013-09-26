package de.azapps.mirakel.model.semantic;

import android.content.ContentValues;
import de.azapps.mirakel.model.list.ListMirakel;

public class SemanticBase {
	private int id;
	private String condition;
	private int priority;
	private ListMirakel list;

	public SemanticBase(int id, String condition, int priority, ListMirakel list) {
		super();
		this.id = id;
		this.condition = condition;
		this.priority = priority;
		this.list = list;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public ListMirakel getList() {
		return list;
	}

	public void setList(ListMirakel list) {
		this.list = list;
	}

	@Override
	public String toString() {
		return this.condition;
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("_id", id);
		cv.put("condition", condition);
		cv.put("default_list_id", list.getId());
		cv.put("priority", priority);
		return cv;
	}

}
