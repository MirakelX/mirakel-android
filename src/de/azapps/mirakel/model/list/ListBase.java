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
	private int sortBy;
	private String createdAt;
	private String updatedAt;
	private int syncState;
	private int lft, rgt;

	ListBase() {
	}

	ListBase(int id, String name, short sort_by, String created_at,
			String updated_at, int sync_state) {
		this.setId(id);
		this.setCreatedAt(created_at);
		this.setName(name);
		this.setUpdatedAt(updated_at);
		this.setSortBy(sort_by);
		this.setSyncState(sync_state);
	}

	ListBase(int id, String name) {
		this.setId(id);
		this.setName(name);
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

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String created_at) {
		this.createdAt = created_at;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updated_at) {
		this.updatedAt = updated_at;
	}

	public int getSortBy() {
		return sortBy;
	}

	public void setSortBy(int sort_by) {
		this.sortBy = sort_by;
	}

	public int getLft() {
		return lft;
	}

	public void setLft(int lft) {
		this.lft = lft;
	}

	public int getRgt() {
		return rgt;
	}

	public void setRgt(int rgt) {
		this.rgt = rgt;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("_id", id);
		cv.put("name", name);
		cv.put("created_at", createdAt);
		cv.put("updated_at", updatedAt);
		cv.put("sort_by", sortBy);
		cv.put("sync_state", syncState);
		return cv;
	}



	public int getSyncState() {
		return syncState;
	}

	public void setSyncState(int sync_state) {
		this.syncState = sync_state;
	}

}
