/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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

package de.azapps.mirakel.model.list;

import android.content.ContentValues;

class ListBase {

	private int id;
	private String name;
	private int sort_by;
	private String created_at;
	private String updated_at;
	private int task_count;
	private int sync_state;
	private int lft, rgt;

	ListBase() {
	}

	ListBase(int id, String name, short sort_by, String created_at,
			String updated_at, int sync_state) {
		this.setId(id);
		this.setCreated_at(created_at);
		this.setName(name);
		this.setUpdated_at(updated_at);
		this.setSortBy(sort_by);
		this.setTask_count(task_count);
		this.setSync_state(sync_state);
	}

	ListBase(int id, String name, int task_count) {
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

	public int getSortBy() {
		return sort_by;
	}

	public void setSortBy(int sort_by) {
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
