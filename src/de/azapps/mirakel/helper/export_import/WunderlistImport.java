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
package de.azapps.mirakel.helper.export_import;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.util.Pair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class WunderlistImport {
	private static final String			TAG	= "WunderlistImport";
	private static Map<String, Integer>	taskMapping;

	public static Boolean exec(Context ctx, String file_path) {
		String json;
		try {
			json = ExportImport.getStringFromFile(file_path, ctx);
		} catch (IOException e) {
			Log.e(TAG, "cannot read File");
			return false;
		}
		Log.i(TAG, json);
		JsonObject i;
		try {
			i = new JsonParser().parse(json).getAsJsonObject();
		} catch (JsonSyntaxException e2) {
			Log.e(TAG, "malformed backup");
			return false;
		}
		Set<Entry<String, JsonElement>> f = i.entrySet();
		Map<String, Integer> listMapping = new HashMap<String, Integer>();
		List<Pair<Integer, String>> contents = new ArrayList<Pair<Integer, String>>();
		taskMapping = new HashMap<String, Integer>();
		for (Entry<String, JsonElement> e : f) {
			if (e.getKey().equals("lists")) {
				Iterator<JsonElement> iter = e.getValue().getAsJsonArray()
						.iterator();
				while (iter.hasNext()) {
					listMapping = parseList(iter.next().getAsJsonObject(),
							listMapping);
				}
			} else if (e.getKey().equals("tasks")) {
				Iterator<JsonElement> iter = e.getValue().getAsJsonArray()
						.iterator();
				while (iter.hasNext()) {
					contents = parseTask(iter.next().getAsJsonObject(),
							listMapping, contents, ctx);
				}
			} else {
				Log.d(TAG, e.getKey());
			}
		}
		for (Pair<Task, String> pair : subtasks) {
			try {
				Task parent = Task.get(Long.valueOf(taskMapping
						.get(pair.second)));
				parent.addSubtask(pair.first);
			} catch (Exception e) {
				// Blame yourself…
			}
		}
		return true;
	}

	private static Map<String, Integer> parseList(JsonObject jsonList, Map<String, Integer> listMapping) {
		String name = jsonList.get("title").getAsString();
		String id = jsonList.get("id").getAsString();
		ListMirakel l = ListMirakel.newList(name);
		l.setCreatedAt(jsonList.get("created_at").getAsString());
		l.setUpdatedAt(jsonList.get("updated_at").getAsString());
		l.save(false);
		listMapping.put(id, l.getId());
		return listMapping;

	}

	/**
	 * <Subtask, id of parent>
	 */
	private static List<Pair<Task, String>>	subtasks	= new ArrayList<Pair<Task, String>>();

	private static List<Pair<Integer, String>> parseTask(JsonObject jsonTask, Map<String, Integer> listMapping, List<Pair<Integer, String>> contents, Context ctx) {
		String name = jsonTask.get("title").getAsString();
		String list_id_string = jsonTask.get("list_id").getAsString();
		Integer listId = listMapping.get(list_id_string);
		ListMirakel list = null;
		if (listId != null) {
			list = ListMirakel.getList(listId);
		}
		if (list == null) {
			list = ListMirakel.safeFirst(ctx);
		}
		Task t = Task.newTask(name, list);
		taskMapping.put(jsonTask.get("id").getAsString(), (int) t.getId());
		if (jsonTask.has("due_date")) {
			try {
				Calendar due = DateTimeHelper.parseDate(jsonTask
						.get("due_date").getAsString());
				t.setDue(due);
			} catch (ParseException e) {

			}
		}
		if (jsonTask.has("note")) {
			t.setContent(jsonTask.get("note").getAsString());
		}
		if (jsonTask.has("completed_at")) {
			t.setDone(true);
			try {
				Calendar completed = DateTimeHelper.parseDate(jsonTask.get(
						"completed_at").getAsString());
				t.setUpdatedAt(completed);
			} catch (ParseException e) {

			}
		}
		if (jsonTask.has("starred") && jsonTask.get("starred").getAsBoolean()) {
			t.setPriority(2);
		}
		if (jsonTask.has("parent_id")) {
			subtasks.add(new Pair<Task, String>(t, jsonTask.get("parent_id")
					.getAsString()));
		}
		t.safeSave(false);
		return contents;
	}
}
