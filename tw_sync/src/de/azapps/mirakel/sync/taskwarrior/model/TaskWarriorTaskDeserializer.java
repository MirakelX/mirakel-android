/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.sync.taskwarrior.model;

import android.support.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;
import java.util.Map.Entry;

public class TaskWarriorTaskDeserializer implements JsonDeserializer<TaskWarriorTask> {

    private static final String TAG = "TaskWarriorTaskDeserializer";

    @Override
    public TaskWarriorTask deserialize(final JsonElement json, final Type type,
                                       final JsonDeserializationContext ctx) throws JsonParseException {
        final JsonObject el = json.getAsJsonObject();
        final JsonElement uuid = el.get("uuid");
        final JsonElement status = el.get("status");
        final JsonElement entry = el.get("entry");
        final JsonElement description = el.get("description");
        if (uuid == null || status == null || entry == null || description == null
            || !uuid.isJsonPrimitive() || !status.isJsonPrimitive() || !entry.isJsonPrimitive() ||
            !description.isJsonPrimitive() || !uuid.getAsJsonPrimitive().isString() ||
            !status.getAsJsonPrimitive().isString() || !entry.getAsJsonPrimitive().isString() ||
            !description.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Invalid syntax, missing required field");
        }
        final TaskWarriorTask task = new TaskWarriorTask(uuid.getAsString(), status.getAsString(),
                parseDate(entry.getAsString()), description.getAsString());
        for (final Entry<String, JsonElement> element : el.entrySet()) {
            switch (element.getKey()) {
            case "uuid":
            case "description":
            case "entry":
            case "status":
                break;
            case "priority":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    task.setPriority(element.getValue().getAsString());
                } else {
                    throw new JsonParseException("priority is not a json primitive");
                }
                break;
            case "priorityNumber":
                // taskd does not handle numbers in the right way
                if (element.getValue().isJsonPrimitive()) {
                    task.setPriorityNumber((int) element.getValue().getAsDouble());
                } else {
                    throw new JsonParseException("priority is not a json primitive");
                }
                break;
            case "progress":
                // taskd does not handle numbers in the right way
                if (element.getValue().isJsonPrimitive()) {
                    task.setProgress((int) element.getValue().getAsDouble());
                } else {
                    throw new JsonParseException("progress is not a json primitive");
                }
                break;
            case "project":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    task.setProject(element.getValue().getAsString());
                } else {
                    throw new JsonParseException("project is not a json primitive");
                }
                break;
            case "modification":
            case "modified":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    task.setModified(parseDate(element.getValue().getAsString()));
                } else {
                    throw new JsonParseException("modified is not a json primitive");
                }
                break;
            case "due":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    task.setDue(parseDate(element.getValue().getAsString()));
                } else {
                    throw new JsonParseException("due is not a json primitive");
                }
                break;
            case "reminder":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    task.setReminder(parseDate(element.getValue().getAsString()));
                } else {
                    throw new JsonParseException("reminder is not a json primitive");
                }
                break;
            case "annotations":
                if (element.getValue().isJsonArray()) {
                    final JsonArray annotations = element.getValue().getAsJsonArray();
                    for (int i = 0; i < annotations.size(); i++) {
                        if (annotations.get(i).isJsonObject()) {
                            final JsonElement descr = annotations.get(i).getAsJsonObject().get("description");
                            final JsonElement annotationEntry = annotations.get(i).getAsJsonObject().get("entry");
                            if (descr == null || annotationEntry == null || !descr.isJsonPrimitive() ||
                                !annotationEntry.isJsonPrimitive() || !descr.getAsJsonPrimitive().isString() ||
                                !annotationEntry.getAsJsonPrimitive().isString()) {
                                throw new JsonParseException("Annotation is not valid");
                            } else {
                                task.addAnnotation(descr.getAsString(), parseDate(annotationEntry.getAsString()));
                            }
                        } else {
                            throw new JsonParseException("Annotation is not a json object");
                        }
                    }
                } else {
                    throw new JsonParseException("annotations is not a json array");
                }
                break;
            case "depends":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    final String depends = element.getValue().getAsString();
                    task.addDepends(depends.split(","));
                } else {
                    throw new JsonParseException("depends is not a json primitive");
                }
                break;
            case "tags":
                if (element.getValue().isJsonArray()) {
                    final JsonArray tags = element.getValue().getAsJsonArray();
                    for (int i = 0; i < tags.size(); i++) {
                        if (tags.get(i).isJsonPrimitive() && tags.get(i).getAsJsonPrimitive().isString()) {
                            task.addTags(tags.get(i).getAsString());
                        } else {
                            throw new JsonParseException("tag is not a string");
                        }
                    }
                } else {
                    throw new JsonParseException("tags is not a json array");
                }
                break;
            case "recur":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    task.setRecur(element.getValue().getAsString());
                } else {
                    throw new JsonParseException("recur is not a json primitive");
                }
                break;
            case "imask":
                if (element.getValue().isJsonPrimitive()) {
                    task.setImask((int) element.getValue().getAsDouble());
                } else {
                    throw new JsonParseException("imask is not a json primitive");
                }
                break;
            case "parent":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    task.setParent(element.getValue().getAsString());
                } else {
                    throw new JsonParseException("parent is not a json primitive");
                }
                break;
            case "mask":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    task.setMask(element.getValue().getAsString());
                } else {
                    throw new JsonParseException("mask is not a json primitive");
                }
                break;
            case "until":
                if (element.getValue().isJsonPrimitive() && element.getValue().getAsJsonPrimitive().isString()) {
                    task.setUntil(parseDate(element.getValue().getAsString()));
                } else {
                    throw new JsonParseException("until is not a json primitive");
                }
                break;
            default:
                task.addUDA(element.getKey(), element.getValue().getAsString());
                break;
            }
        }

        return task;
    }

    @NonNull
    private static DateTime parseDate(final String date) {
        return ISODateTimeFormat.basicDateTimeNoMillis().parseDateTime(date);
    }

}
