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

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import android.content.ContentValues;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;

class TaskBase {
	public static final String		ADDITIONAL_ENTRIES	= "additional_entries";
	public static final String		CONTENT				= "content";
	public static final String		DONE				= "done";
	public static final String		DUE					= "due";
	public static final String		LIST_ID				= "list_id";
	public static final String		PRIORITY			= "priority";
	public static final String		PROGRESS			= "progress";
	public static final String		RECURRING			= "recurring";
	public static final String		RECURRING_REMINDER	= "recurring_reminder";
	public static final String		REMINDER			= "reminder";
	private static final String		TAG					= "TaskBase";

	public static final String		UUID				= "uuid";
	private Map<String, String>		additionalEntries	= null;
	private String					additionalEntriesString;
	private String					content;
	private Calendar				createdAt;
	private boolean					done;
	private Calendar				due;
	protected Map<String, Boolean>	edited				= new HashMap<String, Boolean>();
	private long					id					= 0;
	private ListMirakel				list;
	private String					name;
	private int						priority;
	private int						progress;
	private int						recurrence;
	private int						recurring_reminder;
	private Calendar				reminder;
	private SYNC_STATE				sync_state;
	private Calendar				updatedAt;
	private String					uuid				= "";

	TaskBase() {

	}

	TaskBase(long id, String uuid, ListMirakel list, String name, String content, boolean done, Calendar due, Calendar reminder, int priority, Calendar created_at, Calendar updated_at, SYNC_STATE sync_state, String additionalEntriesString, int recurring, int recurring_reminder, int progress) {
		this.id = id;
		this.uuid = uuid;
		setList(list);
		setName(name);
		setContent(content);
		setDone(done);
		setDue(due);
		setReminder(reminder);
		setPriority(priority);
		this.setCreatedAt(created_at);
		this.setUpdatedAt(updated_at);
		setSyncState(sync_state);
		this.additionalEntriesString = additionalEntriesString;
		this.recurrence = recurring;
		this.recurring_reminder = recurring_reminder;
		this.progress = progress;
	}

	TaskBase(String name) {
		this.id = 0;
		this.uuid = java.util.UUID.randomUUID().toString();
		setList(SpecialList.first());
		setName(name);
		setContent("");
		setDone(false);
		setDue(null);
		setReminder(null);
		setPriority(0);
		this.setCreatedAt((Calendar) null);
		this.setUpdatedAt((Calendar) null);
		setSyncState(SYNC_STATE.NOTHING);
		this.recurrence = -1;
		this.recurring_reminder = -1;
		this.progress = 0;
	}

	public void addAdditionalEntry(String key, String value) {
		initAdditionalEntries();
		Log.d(TAG, "add: " + key + ":" + value);
		this.additionalEntries.put(key, value);
	}

	void clearEdited() {
		this.edited.clear();
	}

	public Map<String, String> getAdditionalEntries() {
		initAdditionalEntries();
		return this.additionalEntries;
	}

	public String getContent() {
		if (this.content == null) return "";
		return this.content;
	}

	public ContentValues getContentValues() throws NoSuchListException {
		Log.d(TAG, "get contentvalues");
		ContentValues cv = new ContentValues();
		cv.put(DatabaseHelper.ID, this.id);
		cv.put(UUID, this.uuid);
		if (this.list == null) {
			// If the list of the task vanished, we should move the task in some
			// list so we can do something with it.
			this.list = ListMirakel.first();
			// Bad things happened… Now we can throw our beloved exception
			if (this.list == null) throw new NoSuchListException();
		}
		cv.put(LIST_ID, this.list.getId());
		cv.put(DatabaseHelper.NAME, this.name);
		cv.put(CONTENT, this.content);
		cv.put(DONE, this.done);
		String due = this.due == null ? null : DateTimeHelper
				.formatDate(getDue());
		cv.put(DUE, due);
		String reminder = null;
		if (this.reminder != null) {
			reminder = DateTimeHelper.formatDateTime(this.reminder);
		}
		cv.put(REMINDER, reminder);
		cv.put(PRIORITY, this.priority);
		String createdAt = null;
		if (this.createdAt != null) {
			createdAt = DateTimeHelper.formatDateTime(this.createdAt);
		}
		cv.put(DatabaseHelper.CREATED_AT, createdAt);
		String updatedAt = null;
		if (this.updatedAt != null) {
			updatedAt = DateTimeHelper.formatDateTime(this.updatedAt);
		}
		cv.put(DatabaseHelper.UPDATED_AT, updatedAt);
		cv.put(SyncAdapter.SYNC_STATE, this.sync_state.toInt());
		cv.put(RECURRING, this.recurrence);
		cv.put(RECURRING_REMINDER, this.recurring_reminder);
		cv.put(PROGRESS, this.progress);

		//		Gson gson = new GsonBuilder().create();
		//		String additionalEntries = gson.toJson(this.additionalEntries);
		String additionalEntries="{";
		boolean first=true;
		Log.w(TAG, this.additionalEntriesString);
		initAdditionalEntries();
		if (this.additionalEntries.entrySet() == null) {
			Log.d(TAG,"set is null");
		}
		Log.wtf(TAG, "here");
		for(Entry<String, String> p:this.additionalEntries.entrySet()){
			additionalEntries+=(first?"":",")+"\""+p.getKey()+"\":"+p.getValue();
			first=false;
		}
		Log.d(TAG, additionalEntries);
		cv.put("additional_entries", additionalEntries+"}");
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
		Recurring r = Recurring.get(this.recurrence);
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
			if (this.additionalEntriesString == null
					|| this.additionalEntriesString.trim().equals("")
					|| this.additionalEntriesString.trim().equals("null")) {
				this.additionalEntries = new HashMap<String, String>();
			} else {
				String t = this.additionalEntriesString.substring(1,
						this.additionalEntriesString.length() - 1);// remove { and }
				String [] parts=t.split(",");
				for(String p:parts){
					String[] keyValue=p.split(":");
					if(keyValue.length==2){
						String key=keyValue[0];
						key=key.replaceAll("\"", "");
						this.additionalEntries.put(key, keyValue[1]);
					}
				}
				//				Gson gson = new Gson();
				//				Type mapType = new TypeToken<Map<String, String>>() {}
				//						.getType();
				//				this.additionalEntries = gson.fromJson(additionalEntriesString,
				//						mapType);
			}
		}
	}

	public boolean isDone() {
		return this.done;
	}

	boolean isEdited() {
		return this.edited.size() > 0;
	}

	boolean isEdited(String key) {
		return this.edited.containsKey(key);
	}

	public void removeAdditionalEntry(String key) {
		initAdditionalEntries();
		this.additionalEntries.remove(key);
	}

	public void setAdditionalEntries(Map<String, String> additionalEntries) {
		this.additionalEntries = additionalEntries;
		this.edited.put("additionalEntries", true);
	}

	public void setContent(String content) {
		if (content != null) {
			this.content = StringUtils.replaceEach(content.trim(),
					new String[] { "\\n", "\\\"", "\b" }, new String[] { "\n",
				"\"", "" });
		} else {
			this.content = null;
		}
		this.edited.put(CONTENT, true);
	}

	public void setCreatedAt(Calendar created_at) {
		this.createdAt = created_at;
	}

	public void setCreatedAt(String created_at) {
		try {
			setCreatedAt(DateTimeHelper.parseDateTime(created_at));
		} catch (ParseException e) {
			setCreatedAt((Calendar) null);
		}
	}

	public void setDone(boolean done) {
		this.done = done;
		this.edited.put(DONE, true);
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
				Log.wtf(TAG, "Reccuring vanish");
			}
		} else if (done) {
			this.progress = 100;
		}
	}

	public void setDue(Calendar due) {
		this.due = due;
		this.edited.put(DUE, true);
		if (due == null) {
			setRecurrence(-1);
		}
	}

	public void setId(long id) {
		this.id = id;
		this.edited.put(DatabaseHelper.ID, true);
	}

	public void setList(ListMirakel list) {
		this.list = list;
		this.edited.put(LIST_ID, true);
	}

	public void setName(String name) {
		this.name = name;
		this.edited.put(DatabaseHelper.NAME, true);
	}

	public void setPriority(int priority) {
		this.priority = priority;
		this.edited.put(PRIORITY, true);
	}

	public void setProgress(int progress) {
		this.edited.put("progress", true);
		this.progress = progress;
	}

	public void setRecurrence(int recurring) {
		this.recurrence = recurring;
		this.edited.put(RECURRING, true);
	}

	public void setRecurringReminder(int recurring) {
		this.recurring_reminder = recurring;
		this.edited.put(RECURRING_REMINDER, true);
	}

	public void setReminder(Calendar reminder) {
		this.reminder = reminder;
		this.edited.put(REMINDER, true);
		if (reminder == null) {
			setRecurringReminder(-1);
		}
	}

	public void setSyncState(SYNC_STATE sync_state) {
		this.sync_state = sync_state;
	}

	public void setUpdatedAt(Calendar updated_at) {
		this.updatedAt = updated_at;
	}

	public void setUpdatedAt(String updated_at) {
		try {
			setUpdatedAt(DateTimeHelper.parseDateTime(updated_at));
		} catch (ParseException e) {
			setUpdatedAt((Calendar) null);
		}
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
		this.edited.put(UUID, true);
	}

	public void toggleDone() {
		setDone(!this.done);
	}

	@Override
	public String toString() {
		return this.name;
	}

}
