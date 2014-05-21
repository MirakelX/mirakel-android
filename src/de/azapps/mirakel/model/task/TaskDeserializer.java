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

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.tools.Log;

public class TaskDeserializer implements JsonDeserializer<Task> {

	private static final String TAG = "TaskDeserializer";
	private final boolean isTW;
	private final AccountMirakel account;
	private final Context context;

	public TaskDeserializer(final boolean isTW, final AccountMirakel account,
			final Context ctx) {
		this.isTW = isTW;
		this.account = account;
		this.context = ctx;
	}

	@Override
	public Task deserialize(final JsonElement json, final Type type,
			final JsonDeserializationContext ctx) throws JsonParseException {
		final JsonObject el = json.getAsJsonObject();
		Task t = null;
		JsonElement id = el.get("id");
		if (id != null && !this.isTW) {// use uuid for tw-sync
			t = Task.get(id.getAsLong());
		} else {
			id = el.get("uuid");
			if (id != null) {
				t = Task.getByUUID(id.getAsString());
			}
		}
		if (t == null) {
			t = new Task();
		}
		if (this.isTW) {
			t.setDue(null);
			t.setDone(false);
			t.setContent(null);
			t.setPriority(0);
			t.setProgress(0);
			t.setList(null, false);
		}
		boolean setPriorityFromNumber = false;

		// Name
		final Set<Entry<String, JsonElement>> entries = el.entrySet();
		for (final Entry<String, JsonElement> entry : entries) {
			final String key = entry.getKey();
			final JsonElement val = entry.getValue();
			if (key == null || key.equalsIgnoreCase("id")) {
				continue;
			}
			if (key.equals("uuid")) {
				t.setUUID(val.getAsString());
			} else if (key.equalsIgnoreCase("name")
					|| key.equalsIgnoreCase("description")) {
				t.setName(val.getAsString());
			} else if (key.equalsIgnoreCase("content")) {
				String content = val.getAsString();
				if (content == null) {
					content = "";
				}
				t.setContent(content);
			} else if ((key.equalsIgnoreCase("priority") || key
					.equalsIgnoreCase("priorityNumber"))
					&& !setPriorityFromNumber) {
				final String prioString = val.getAsString().trim();
				if (prioString.equalsIgnoreCase("L") && t.getPriority() != -1) {
					t.setPriority(-2);
				} else if (prioString.equalsIgnoreCase("M")) {
					t.setPriority(1);
				} else if (prioString.equalsIgnoreCase("H")) {
					t.setPriority(2);
				} else if (!prioString.equalsIgnoreCase("L")) {
					t.setPriority((int) val.getAsFloat());
					setPriorityFromNumber = true;
				}
			} else if (key.equalsIgnoreCase("progress")) {
				final int progress = (int) val.getAsDouble();
				t.setProgress(progress);
			} else if (key.equalsIgnoreCase("list_id")) {
				ListMirakel list = ListMirakel.get(val.getAsInt());
				if (list == null) {
					list = SpecialList.firstSpecial().getDefaultList();
				}
				t.setList(list, true);
			} else if (key.equalsIgnoreCase("project")) {
				ListMirakel list = ListMirakel.findByName(val.getAsString(),
						this.account);
				if (list == null
						|| list.getAccount().getId() != this.account.getId()) {
					list = ListMirakel.newList(val.getAsString(),
							ListMirakel.SORT_BY_OPT, this.account);
				}
				t.setList(list, true);
			} else if (key.equalsIgnoreCase("created_at")) {
				t.setCreatedAt(val.getAsString().replace(":", ""));
			} else if (key.equalsIgnoreCase("updated_at")) {
				t.setUpdatedAt(val.getAsString().replace(":", ""));
			} else if (key.equalsIgnoreCase("entry")) {
				t.setCreatedAt(handleDate(val));
			} else if (key.equalsIgnoreCase("modification")
					|| key.equalsIgnoreCase("modified")) {
				t.setUpdatedAt(handleDate(val));
			} else if (key.equalsIgnoreCase("done")) {
				t.setDone(val.getAsBoolean());
			} else if (key.equalsIgnoreCase("status")) {
				final String status = val.getAsString();
				if (status.equalsIgnoreCase("completed")) {
					t.setDone(true);
				} else if (status.equalsIgnoreCase("deleted")) {
					t.setSyncState(SYNC_STATE.DELETE);
				} else {
					t.setDone(false);
					t.addAdditionalEntry(key, "\"" + val.getAsString() + "\"");
					// TODO don't ignore waiting and recurring!!!
				}
			} else if (key.equalsIgnoreCase("due")) {
				Calendar due = parseDate(val.getAsString(), "yyyy-MM-dd");
				if (due == null) {
					due = parseDate(val.getAsString(),
							this.context.getString(R.string.TWDateFormat));
					// try to workaround timezone-bug
					if (due != null) {
						due.setTimeInMillis(due.getTimeInMillis()
								+ DateTimeHelper.getTimeZoneOffset(true, due));
					}
				}
				t.setDue(due);
			} else if (key.equalsIgnoreCase("reminder")) {
				Calendar reminder = parseDate(val.getAsString(), "yyyy-MM-dd");
				if (reminder == null) {
					reminder = parseDate(val.getAsString(),
							this.context.getString(R.string.TWDateFormat));
				}
				t.setReminder(reminder);
			} else if (key.equalsIgnoreCase("annotations")) {
				t.setContent(handleContent(val));
			} else if (key.equalsIgnoreCase("content")) {
				t.setContent(val.getAsString());
			} else if (key.equalsIgnoreCase("sync_state")) {
				t.setSyncState(SYNC_STATE.parseInt(val.getAsInt()));
			} else if (key.equalsIgnoreCase("depends")) {
				t.setDependencies(val.getAsString().split(","));
			} else if (key.equalsIgnoreCase("tags")) {
				handleTags(t, val);
			} else {
				handleAdditionalEnties(t, key, val);
			}
		}
		return t;
	}

	private static void handleAdditionalEnties(final Task t, final String key,
			final JsonElement val) {
		if (val.isJsonPrimitive()) {
			final JsonPrimitive p = (JsonPrimitive) val;
			if (p.isBoolean()) {
				t.addAdditionalEntry(key, val.getAsBoolean() + "");
			} else if (p.isNumber()) {
				t.addAdditionalEntry(key, val.getAsInt() + "");
			} else if (p.isJsonNull()) {
				t.addAdditionalEntry(key, "null");
			} else if (p.isString()) {
				t.addAdditionalEntry(key, "\"" + val.getAsString() + "\"");
			} else {
				Log.w(TAG, "unkown json-type");
			}
		} else if (val.isJsonArray()) {
			final JsonArray a = (JsonArray) val;
			String s = "[";
			boolean first = true;
			for (final JsonElement e : a) {
				if (e.isJsonPrimitive()) {
					final JsonPrimitive p = (JsonPrimitive) e;
					String add;
					if (p.isBoolean()) {
						add = p.getAsBoolean() + "";
					} else if (p.isNumber()) {
						add = p.getAsInt() + "";
					} else if (p.isString()) {
						add = "\"" + p.getAsString() + "\"";
					} else if (p.isJsonNull()) {
						add = "null";
					} else {
						Log.w(TAG, "unkown json-type");
						break;
					}
					s += (first ? "" : ",") + add;
					first = false;
				} else {
					Log.w(TAG, "unkown json-type");
				}
			}
			t.addAdditionalEntry(key, s + "]");
		} else {
			Log.w(TAG, "unkown json-type");
		}
	}

	private static void handleTags(final Task t, final JsonElement val) {
		final JsonArray tags = val.getAsJsonArray();
		final List<Tag> currentTags = t.getTags();
		for (final JsonElement tag : tags) {
			if (tag.isJsonPrimitive()) {
				String tagName = tag.getAsString();
				tagName = tagName.replace("_", " ");

				final Tag newTag = Tag.newTag(tagName);
				if (!currentTags.remove(newTag)) {
					// tag is not linked with this task
					t.addTag(newTag, false);
				}
			}
		}
		for (final Tag tag : currentTags) {
			// remove unused tags
			t.removeTag(tag, false);
		}
	}

	private static String handleContent(final JsonElement val) {
		String content = "";
		try {
			final JsonArray annotations = val.getAsJsonArray();
			boolean first = true;
			for (final JsonElement a : annotations) {
				if (first) {
					first = false;
				} else {
					content += "\n";
				}
				content += a.getAsJsonObject().get("description").getAsString();
			}
		} catch (final Exception e) {
			Log.e(TAG, "cannot parse json");
		}
		return content;
	}

	private Calendar handleDate(final JsonElement val) {
		Calendar createdAt = parseDate(val.getAsString(),
				this.context.getString(R.string.TWDateFormat));
		if (createdAt == null) {
			createdAt = new GregorianCalendar();
		} else {
			createdAt.add(Calendar.SECOND,
					DateTimeHelper.getTimeZoneOffset(false, createdAt));
		}
		return createdAt;
	}

	private static Calendar parseDate(final String date, final String format) {
		final GregorianCalendar temp = new GregorianCalendar();
		try {
			temp.setTime(new SimpleDateFormat(format, Locale.getDefault())
					.parse(date));
			return temp;
		} catch (final ParseException e) {
			return null;
		}

	}

}
