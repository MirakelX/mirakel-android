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

import android.content.Context;
import android.util.Pair;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public class TaskDeserializer implements JsonDeserializer<Task> {

    private static final String TAG = "TaskDeserializer";


    public TaskDeserializer() {
    }

    @Override
    public Task deserialize(final JsonElement json, final Type type,
                            final JsonDeserializationContext ctx) throws JsonParseException {
        final JsonObject el = json.getAsJsonObject();
        Optional<ListMirakel> taskList = absent();
        Optional<Task> taskOptional = absent();
        JsonElement id = el.get("id");
        if (id != null) {
            taskOptional = Task.get(id.getAsLong());
        }
        Task task;
        if (taskOptional.isPresent()) {
            task = taskOptional.get();
        } else {
            throw new JsonParseException("Id is missing");
        }
        // Name
        final Set<Entry<String, JsonElement>> entries = el.entrySet();
        for (final Entry<String, JsonElement> entry : entries) {
            String key = entry.getKey();
            final JsonElement val = entry.getValue();
            if (key == null || key.equalsIgnoreCase("id")) {
                continue;
            }
            switch (key.toLowerCase()) {
            case "name":
                task.setName(val.getAsString());
                break;
            case "content":
                String content = val.getAsString();
                if (content == null) {
                    content = "";
                }
                task.setContent(content);
                break;
            case "priority":
                task.setPriority((int) val.getAsFloat());
                break;
            case "progress":
                task.setProgress((int) val.getAsDouble());
                break;
            case "list_id": {
                taskList = ListMirakel.get(val.getAsInt());
                if (!taskList.isPresent()) {
                    taskList = Optional.fromNullable(SpecialList.firstSpecialSafe().getDefaultList());
                }
                break;
            }
            case "created_at":
                task.setCreatedAt(val.getAsString().replace(":", ""));
                break;
            case "updated_at":
                task.setUpdatedAt(val.getAsString().replace(":", ""));
                break;
            case "done":
                task.setDone(val.getAsBoolean());
                break;
            case "due":
                Calendar due = parseDate(val.getAsString(), "yyyy-MM-dd");
                task.setDue(fromNullable(due));
                break;
            case "reminder":
                Calendar reminder = parseDate(val.getAsString(), "yyyy-MM-dd");
                task.setReminder(fromNullable(reminder));
                break;
            case "tags":
                handleTags(task, val);
                break;
            default:
                handleAdditionalEntries(task, key, val);
                break;
            }
        }
        if (!taskList.isPresent()) {
            taskList = Optional.of(ListMirakel.safeFirst());
        }
        task.setList(taskList.get(), true);
        return task;
    }

    private static void handleAdditionalEntries(final Task t, final String key,
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
        final List<Tag> currentTags = new ArrayList<>(t.getTags());
        for (final JsonElement tag : tags) {
            if (tag.isJsonPrimitive()) {
                String tagName = tag.getAsString();
                tagName = tagName.replace("_", " ");
                final Tag newTag = Tag.newTag(tagName);
                if (!currentTags.remove(newTag)) {
                    // tag is not linked with this task
                    t.addTag(newTag, false, true);
                }
            }
        }
        for (final Tag tag : currentTags) {
            // remove unused tags
            t.removeTag(tag, false, true);
        }
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
