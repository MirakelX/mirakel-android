/**
 * 
 */
package de.azapps.mirakel;

import android.content.ContentValues;

/**
 * @author weiznich
 * 
 */
public class List_mirakle {
	private int id;
	private String name;
	// private int user_id;
	private short sort_by;
	private String created_at;
	private String updated_at;
	private int task_count;
	private int sync_state;

	public List_mirakle() {
	}

	public List_mirakle(int id, String name, short sort_by, String created_at,
			String updated_at, int task_count, int sync_state) {
		this.setId(id);
		this.setCreated_at(created_at);
		this.setName(name);
		this.setUpdated_at(updated_at);
		this.setSort_by(sort_by);
		this.setTask_count(task_count);
		this.setSync_state(sync_state);
	}

	public List_mirakle(int id, String name, int task_count) {
		this.setId(id);
		this.setName(name);
		this.setTask_count(task_count);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	public String getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(String updated_at) {
		this.updated_at = updated_at;
	}

	public short getSort_by() {
		return sort_by;
	}

	public void setSort_by(short sort_by) {
		this.sort_by = sort_by;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("_id", id);
		cv.put("name", name);
		cv.put("created_at", created_at);
		cv.put("updated_at", updated_at);
		cv.put("sort_by", sort_by);
		cv.put("sync_state", sync_state);
		return cv;
	}

	public int getTask_count() {
		return task_count;
	}

	public void setTask_count(int task_count) {
		this.task_count = task_count;
	}

	public int getSync_state() {
		return sync_state;
	}

	public void setSync_state(int sync_state) {
		this.sync_state = sync_state;
	}

}
