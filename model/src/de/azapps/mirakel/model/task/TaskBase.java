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
package de.azapps.mirakel.model.task;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentValues;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.tools.Log;

class TaskBase {
	public static final String ADDITIONAL_ENTRIES = "additional_entries";
	public static final String CONTENT = "content";
	public static final String DONE = "done";
	public static final String DUE = "due";
	public static final String LIST_ID = "list_id";
	public static final String PRIORITY = "priority";
	public static final String PROGRESS = "progress";
	public static final String RECURRING = "recurring";
	public static final String RECURRING_REMINDER = "recurring_reminder";
	public static final String REMINDER = "reminder";
	private static final String TAG = "TaskBase";

	public static final String UUID = "uuid";
	private Map<String, String> additionalEntries = null;
	private String additionalEntriesString;
	private String content;
	private Calendar createdAt;
	private boolean done;
	private Calendar due;
	protected Map<String, Boolean> edited = new HashMap<String, Boolean>();
	private long id = 0;
	private ListMirakel list;
	private String name;
	private int priority;
	private int progress;
	private int recurrence;
	private int recurring_reminder;
	private Calendar reminder;
	private SYNC_STATE sync_state;
	private Calendar updatedAt;
	private String uuid = "";

	TaskBase() {
		// nothing
	}

	TaskBase(final long id, final String uuid, final ListMirakel list,
			final String name, final String content, final boolean done,
			final Calendar due, final Calendar reminder, final int priority,
			final Calendar created_at, final Calendar updated_at,
			final SYNC_STATE sync_state, final String additionalEntriesString,
			final int recurring, final int recurring_reminder,
			final int progress) {
		this.id = id;
		this.uuid = uuid;
		setList(list, false);
		setName(name);
		setContent(content);
		setDone(done);
		setDue(due);
		setReminder(reminder);
		this.priority = priority;
		this.setCreatedAt(created_at);
		this.setUpdatedAt(updated_at);
		setSyncState(sync_state);
		this.additionalEntriesString = additionalEntriesString;
		this.recurrence = recurring;
		this.recurring_reminder = recurring_reminder;
		this.progress = progress;
		clearEdited();
	}

	TaskBase(final String name) {
		this.id = 0;
		this.uuid = java.util.UUID.randomUUID().toString();
		setList(ListMirakel.first(), false);
		setName(name);
		setContent("");
		setDone(false);
		setDue(null);
		setReminder(null);
		this.priority = 0;
		this.setCreatedAt((Calendar) null);
		this.setUpdatedAt((Calendar) null);
		setSyncState(SYNC_STATE.NOTHING);
		this.recurrence = -1;
		this.recurring_reminder = -1;
		this.progress = 0;
		clearEdited();
	}

	public void addAdditionalEntry(final String key, final String value) {
		initAdditionalEntries();
		Log.d(TaskBase.TAG, "add: " + key + ":" + value);
		this.additionalEntries.put(key, value);
	}

	void clearEdited() {
		this.edited.clear();
	}

	public Map<String, String> getAdditionalEntries() {
		initAdditionalEntries();
		return this.additionalEntries;
	}

	protected String getAdditionalEntriesString() {
		initAdditionalEntries();
		this.additionalEntriesString = serializeAdditionalEntries(this.additionalEntries);
		return this.additionalEntriesString;
	}

	public static String serializeAdditionalEntries(
			final Map<String, String> additionalEntries) {
		String additionalEntriesStr = "{";
		boolean first = true;
		for (final Entry<String, String> p : additionalEntries.entrySet()) {
			additionalEntriesStr += (first ? "" : ",") + "\"" + p.getKey()
					+ "\":" + p.getValue();
			first = false;
		}
		return additionalEntriesStr + "}";
	}

	public String getContent() {
		if (this.content == null) {
			return "";
		}
		return this.content;
	}

	public ContentValues getContentValues() throws NoSuchListException {
		final ContentValues cv = new ContentValues();
		cv.put(DatabaseHelper.ID, this.id);
		cv.put(TaskBase.UUID, this.uuid);
		if (this.list == null) {
			// If the list of the task vanished, we should move the task in some
			// list so we can do something with it.
			this.list = ListMirakel.first();
			// Bad things happened… Now we can throw our beloved exception
			if (this.list == null) {
				throw new NoSuchListException();
			}
		}
		cv.put(TaskBase.LIST_ID, this.list.getId());
		cv.put(DatabaseHelper.NAME, this.name);
		cv.put(TaskBase.CONTENT, this.content);
		cv.put(TaskBase.DONE, this.done);
		if (this.due != null) {
			if (this.due.get(Calendar.HOUR) == 0
					&& this.due.get(Calendar.MINUTE) == 0
					&& this.due.get(Calendar.SECOND) == 0) {
				cv.put(TaskBase.DUE, this.due.getTimeInMillis() / 1000);
			} else {
				cv.put(TaskBase.DUE, DateTimeHelper.getUTCTime(this.due));
			}
		} else {
			cv.put(TaskBase.DUE, (Integer) null);
		}
		if (this.reminder != null) {
			cv.put(TaskBase.REMINDER, DateTimeHelper.getUTCTime(this.reminder));
		} else {
			cv.put(TaskBase.REMINDER, (Integer) null);
		}
		cv.put(TaskBase.PRIORITY, this.priority);
		if (this.createdAt != null) {
			cv.put(DatabaseHelper.CREATED_AT,
					this.createdAt.getTimeInMillis() / 1000);
		} else {
			cv.put(DatabaseHelper.CREATED_AT,
					new GregorianCalendar().getTimeInMillis() / 1000);
		}
		if (this.updatedAt != null) {
			cv.put(DatabaseHelper.UPDATED_AT,
					this.updatedAt.getTimeInMillis() / 1000);
		} else {
			cv.put(DatabaseHelper.UPDATED_AT,
					new GregorianCalendar().getTimeInMillis());
		}
		cv.put(DatabaseHelper.SYNC_STATE_FIELD, this.sync_state.toInt());
		cv.put(TaskBase.RECURRING, this.recurrence);
		cv.put(TaskBase.RECURRING_REMINDER, this.recurring_reminder);
		cv.put(TaskBase.PROGRESS, this.progress);

		cv.put("additional_entries", getAdditionalEntriesString());
		return cv;
	}

	public Calendar getCreatedAt() {
		return this.createdAt;
	}

	public Calendar getDue() {
		return this.due;
	}

	public Map<String, Boolean> getEdited() {
		return this.edited;
	}

	public long getId() {
		return this.id;
	}

	public ListMirakel getList() {
		return this.list;
	}

	public String getName() {
		return this.name;
	}

	public int getPriority() {
		return this.priority;
	}

	public int getProgress() {
		return this.progress;
	}

	public int getRecurrenceId() {
		return this.recurrence;
	}

	public Recurring getRecurring() {
		final Recurring r = Recurring.get(this.recurrence);
		return r;
	}

	public Recurring getRecurringReminder() {
		return Recurring.get(this.recurring_reminder);
	}

	public int getRecurringReminderId() {
		return this.recurring_reminder;
	}

	public Calendar getReminder() {
		return this.reminder;
	}

	public SYNC_STATE getSyncState() {
		return this.sync_state;
	}

	public Calendar getUpdatedAt() {
		return this.updatedAt;
	}

	public String getUUID() {
		return this.uuid;
	}

	public boolean hasRecurringReminder() {
		return this.recurring_reminder > 0;
	}

	/**
	 * This function parses the additional fields only if it is necessary
	 */
	private void initAdditionalEntries() {
		if (this.additionalEntries == null) {
			this.additionalEntries = parseAdditionalEntries(this.additionalEntriesString);
		}
	}

	public static Map<String, String> parseAdditionalEntries(
			final String additionalEntriesString) {
		final Map<String, String> ret = new HashMap<String, String>();
		if (additionalEntriesString != null
				&& !additionalEntriesString.trim().equals("")
				&& !additionalEntriesString.trim().equals("null")) {
			final String t = additionalEntriesString.substring(1,
					additionalEntriesString.length() - 1);// remove {
															// and }
			final String[] parts = t.split(",");
			String key = null;
			for (final String p : parts) {
				final String[] keyValue = p.split(":");
				if (keyValue.length == 2) {
					key = keyValue[0];
					key = key.replaceAll("\"", "");
					ret.put(key, keyValue[1]);
				} else if (keyValue.length == 1 && key != null) {
					ret.put(key, ret.get(key) + "," + keyValue[0]);
				}
			}
		}
		return ret;
	}

	public boolean isDone() {
		return this.done;
	}

	boolean isEdited() {
		return this.edited.size() > 0;
	}

	boolean isEdited(final String key) {
		return this.edited.containsKey(key);
	}

	public void removeAdditionalEntry(final String key) {
		initAdditionalEntries();
		this.additionalEntries.remove(key);
	}

	public void setAdditionalEntries(final Map<String, String> additionalEntries) {
		if (this.additionalEntries != null
				&& this.additionalEntries.equals(additionalEntries)) {
			return;
		}
		this.additionalEntries = additionalEntries;
		this.edited.put("additionalEntries", true);
	}

	public void setContent(final String content) {
		if (this.content != null && this.content.equals(content)) {
			return;
		}
		if (content != null) {
			this.content = content.trim().replace("\\n", "\n");
			this.content = this.content.replace("\\\"", "\"");
			this.content = this.content.replace("\b", "");
		} else {
			this.content = null;
		}
		this.edited.put(TaskBase.CONTENT, true);
	}

	public void setCreatedAt(final Calendar created_at) {
		this.createdAt = created_at;
	}

	public void setCreatedAt(final String created_at) {
		try {
			setCreatedAt(DateTimeHelper.parseDateTime(created_at));
		} catch (final ParseException e) {
			setCreatedAt((Calendar) null);
		}
	}

	public void setDone(final boolean done) {
		if (this.done == done) {
			return;
		}
		this.done = done;
		this.edited.put(TaskBase.DONE, true);
		if (done && this.recurrence != -1 && this.due != null) {
			if (getRecurring() != null) {
				this.due = getRecurring().addRecurring(this.due);
				if (this.reminder != null) {
					// Fix for #84
					// Update Reminder if set Task done
					this.reminder = getRecurring().addRecurring(this.reminder);
				}
				this.done = false;
			} else {
				Log.wtf(TaskBase.TAG, "Reccuring vanish");
			}
		} else if (done) {
			this.progress = 100;
		}
	}

	public void setDue(final Calendar due) {
		if (this.due != null && this.due.equals(due)) {
			return;
		}
		this.due = due;
		this.edited.put(TaskBase.DUE, true);
		if (due == null) {
			setRecurrence(-1);
		}
	}

	protected void setId(final long id) {
		this.id = id;
	}

	/**
	 * Replaces the id of the current task by the foreign task. This is needed
	 * if we want to override the current task by a remote task.
	 * 
	 * @param t
	 *            other task
	 */
	public void takeIdFrom(final Task t) {
		this.setId(t.getId());
	}

	public void setList(final ListMirakel list) {
		setList(list, false);
	}

	public void setList(final ListMirakel list, final boolean removeNoListFalg) {
		if (this.list != null && list != null
				&& this.list.getId() == list.getId()) {
			return;
		}
		this.list = list;
		this.edited.put(TaskBase.LIST_ID, true);
		if (removeNoListFalg) {
			if (this.additionalEntries == null) {
				initAdditionalEntries();
			}
			this.additionalEntries.remove(DefinitionsHelper.TW_NO_PROJECT);
		}
	}

	public void setName(final String name) {
		if (this.name != null && this.name.equals(name)) {
			return;
		}
		this.name = name;
		this.edited.put(DatabaseHelper.NAME, true);
	}

	public void setPriority(final int priority) {
		if (this.priority == priority) {
			return;
		}
		this.priority = priority;
		this.edited.put(TaskBase.PRIORITY, true);
	}

	public void setProgress(final int progress) {
		if (this.progress == progress) {
			return;
		}
		this.edited.put("progress", true);
		this.progress = progress;
	}

	public void setRecurrence(final int recurrence) {
		if (this.recurrence == recurrence) {
			return;
		}
		this.recurrence = recurrence;
		this.edited.put(TaskBase.RECURRING, true);
	}

	public void setRecurringReminder(final int recurrence) {
		if (this.recurring_reminder == recurrence) {
			return;
		}
		this.recurring_reminder = recurrence;
		this.edited.put(TaskBase.RECURRING_REMINDER, true);
	}

	public void setReminder(final Calendar reminder) {
		setReminder(reminder, false);
	}

	public void setReminder(final Calendar reminder, final boolean force) {
		if (this.reminder != null && this.reminder.equals(reminder) && !force) {
			return;
		}

		this.reminder = reminder;
		this.edited.put(TaskBase.REMINDER, true);
		if (reminder == null) {
			setRecurringReminder(-1);
		}
	}

	public void setSyncState(final SYNC_STATE sync_state) {
		this.sync_state = sync_state;
	}

	public void setUpdatedAt(final Calendar updated_at) {
		this.updatedAt = updated_at;
	}

	public void setUpdatedAt(final String updated_at) {
		try {
			setUpdatedAt(DateTimeHelper.parseDateTime(updated_at));
		} catch (final ParseException e) {
			setUpdatedAt((Calendar) null);
		}
	}

	public void setUUID(final String uuid) {
		this.uuid = uuid;
	}

	public void toggleDone() {
		setDone(!this.done);
	}

	@Override
	public String toString() {
		return this.name;
	}

}
