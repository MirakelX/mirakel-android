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
package de.azapps.mirakel.model.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.ContentValues;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;

class TaskBase {
	private long id;
	private ListMirakel list;
	private String name;
	private String content;
	private boolean done;
	private GregorianCalendar due;
	private int priority;
	private String createdAt;
	private String updatedAt;
	private Map<String, Boolean> edited = new HashMap<String, Boolean>();
	private int sync_state;
	private GregorianCalendar reminder;

	TaskBase(long id, ListMirakel list, String name, String content,
			boolean done, GregorianCalendar due, GregorianCalendar reminder,
			int priority, String created_at, String updated_at, int sync_state) {
		this.id = id;
		this.setList(list);
		this.setName(name);
		this.setContent(content);
		this.setDone(done);
		this.setDue(due);
		this.setReminder(reminder);
		this.setPriority(priority);
		this.setCreatedAt(created_at);
		this.setUpdatedAt(updated_at);
		this.setSyncState(sync_state);
	}

	TaskBase() {

	}

	TaskBase(String name) {
		this.id = 0;
		this.setList(SpecialList.first());
		this.setName(name);
		this.setContent("");
		this.setDone(false);
		this.setDue(null);
		this.setReminder(null);
		this.setPriority(0);
		this.setCreatedAt(null);
		this.setUpdatedAt(null);
		this.setSyncState(0);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
		edited.put("id", true);
	}

	public ListMirakel getList() {
		return list;
	}

	public void setList(ListMirakel list) {
		this.list = list;
		edited.put("list", true);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		edited.put("name", true);
	}

	public String getContent() {
		try {
			return content.trim().replace("\\n", "\n");
		} catch (NullPointerException e) {
			return "";
		}
	}

	public void setContent(String content) {
		this.content = content;
		edited.put("content", true);
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
		edited.put("done", true);
	}

	public void toggleDone() {
		this.done = !this.done;
		edited.put("done", true);
	}

	public GregorianCalendar getDue() {
		return due;
	}

	public void setDue(GregorianCalendar due) {
		this.due = due;
		edited.put("due", true);
	}

	public GregorianCalendar getReminder() {
		return reminder;
	}

	public void setReminder(GregorianCalendar reminder) {
		this.reminder = reminder;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
		edited.put("priority", true);
	}

	public String getCreated_at() {
		return createdAt;
	}

	public void setCreatedAt(String created_at) {
		this.createdAt = created_at;
	}

	public String getUpdated_at() {
		return updatedAt;
	}

	public void setUpdatedAt(String updated_at) {
		this.updatedAt = updated_at;
	}

	public int getSync_state() {
		return sync_state;
	}

	public void setSyncState(int sync_state) {
		this.sync_state = sync_state;
	}

	@Override
	public String toString() {
		return name;
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("_id", id);
		cv.put("list_id", list.getId());
		cv.put("name", name);
		cv.put("content", content);
		cv.put("done", done);
		String due = (this.due == null ? null : new SimpleDateFormat(
				"yyyy-MM-dd", Locale.getDefault()).format(new Date(this.due
				.getTimeInMillis())));
		cv.put("due", due);
		String reminder = null;
		if (this.reminder != null)
			reminder = new SimpleDateFormat("yyyy-MM-dd'T'kkmmss'Z'",
					Locale.getDefault()).format(new Date(this.reminder
					.getTimeInMillis()));
		cv.put("reminder", reminder);
		cv.put("priority", priority);
		cv.put("created_at", createdAt);
		cv.put("updated_at", updatedAt);
		cv.put("sync_state", sync_state);
		return cv;
	}
}
