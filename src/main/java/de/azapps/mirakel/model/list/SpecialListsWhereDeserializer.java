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

package de.azapps.mirakel.model.list;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.meta.DueDeserializer;
import de.azapps.mirakel.model.list.meta.NegatedDeserializer;
import de.azapps.mirakel.model.list.meta.ProgressDeserializer;
import de.azapps.mirakel.model.list.meta.SetDeserializer;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList;
import de.azapps.mirakel.model.list.meta.SpecialListsContentProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueExistsProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsFileProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsProgressProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsReminderProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsSubtaskProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsTagProperty;
import de.azapps.mirakel.model.list.meta.StringDeserializer;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public final class SpecialListsWhereDeserializer {

    private final static Gson gson = new GsonBuilder()
    .registerTypeAdapter(SpecialListsDoneProperty.class,
                         new NegatedDeserializer<>(SpecialListsDoneProperty.class))
    .registerTypeAdapter(SpecialListsFileProperty.class,
                         new NegatedDeserializer<>(SpecialListsFileProperty.class))
    .registerTypeAdapter(SpecialListsReminderProperty.class,
                         new NegatedDeserializer<>(SpecialListsReminderProperty.class))
    .registerTypeAdapter(SpecialListsDueExistsProperty.class,
                         new NegatedDeserializer<>(SpecialListsDueExistsProperty.class))
    .registerTypeAdapter(SpecialListsListProperty.class,
                         new SetDeserializer<>(SpecialListsListProperty.class))
    .registerTypeAdapter(SpecialListsTagProperty.class,
                         new SetDeserializer<>(SpecialListsTagProperty.class))
    .registerTypeAdapter(SpecialListsPriorityProperty.class,
                         new SetDeserializer<>(SpecialListsPriorityProperty.class))
    .registerTypeAdapter(SpecialListsDueProperty.class,
                         new DueDeserializer())
    .registerTypeAdapter(SpecialListsContentProperty.class,
                         new StringDeserializer<>(SpecialListsContentProperty.class))
    .registerTypeAdapter(SpecialListsNameProperty.class,
                         new StringDeserializer<>(SpecialListsNameProperty.class))
    .registerTypeAdapter(SpecialListsListNameProperty.class,
                         new StringDeserializer<>(SpecialListsListNameProperty.class))
    .registerTypeAdapter(SpecialListsProgressProperty.class,
                         new ProgressDeserializer()).create();

    private static final String TAG = "SpecialListsWhereDeserializer";

    @NonNull
    public static Optional<SpecialListsBaseProperty> deserializeWhere(final @NonNull String
            whereQuery, final String name) throws IllegalArgumentException {
        if ((whereQuery == null) || TextUtils.isEmpty(whereQuery.trim())) {
            return absent();
        }
        try {
            final JsonElement obj = new JsonParser().parse(whereQuery);
            return parseSpecialListWhere(obj, 0);
        } catch (final JsonSyntaxException e) {
            Log.wtf(TAG, "cannot parse " + whereQuery, e);
            ErrorReporter.report(ErrorType.SPECIAL_LIST_JSON_INVALID, name);
            return absent();
        }
    }

    @NonNull
    private static Optional<SpecialListsBaseProperty> parseSpecialListWhere(
        final @NonNull JsonElement obj, final int deep) throws IllegalArgumentException {
        if (obj.isJsonObject()) {
            return parseSpecialListsCondition(obj);
        } else if (obj.isJsonArray()) {
            final List<SpecialListsBaseProperty> childs = new ArrayList<>(obj.getAsJsonArray().size());
            for (final JsonElement el : obj.getAsJsonArray()) {
                final Optional<SpecialListsBaseProperty> child = parseSpecialListWhere(el, deep + 1);
                if (child.isPresent()) {
                    childs.add(child.get());
                }
            }
            return of((SpecialListsBaseProperty) new SpecialListsConjunctionList(((
                          deep % 2) == 0) ? SpecialListsConjunctionList.CONJUNCTION.AND :
                      SpecialListsConjunctionList.CONJUNCTION.OR, childs));
        } else if (obj.isJsonNull()) {
            return absent();
        } else  {
            throw new IllegalArgumentException("Unknown json type");
        }
    }

    @NonNull
    private static Optional<SpecialListsBaseProperty> parseSpecialListsCondition(
        final @NonNull JsonElement element) {
        final JsonObject obj = element.getAsJsonObject();
        final Class<? extends SpecialListsBaseProperty> className;
        final String key;
        if (obj.has(Task.LIST_ID)) {
            key = Task.LIST_ID;
            className = SpecialListsListProperty.class;
        } else if (obj.has(ModelBase.NAME)) {
            key = Task.NAME;
            className = SpecialListsNameProperty.class;
        } else if (obj.has(Task.PRIORITY)) {
            key = Task.PRIORITY;
            className = SpecialListsPriorityProperty.class;
        } else if (obj.has(Task.DONE)) {
            key = Task.DONE;
            className = SpecialListsDoneProperty.class;
        } else if (obj.has(Task.DUE)) {
            key = Task.DUE;
            className = SpecialListsDueProperty.class;
        } else if (obj.has(Task.CONTENT)) {
            key = Task.CONTENT;
            className = SpecialListsContentProperty.class;
        } else if (obj.has(Task.REMINDER)) {
            key = Task.REMINDER;
            className = SpecialListsReminderProperty.class;
        } else if (obj.has(Task.PROGRESS)) {
            key = Task.PROGRESS;
            className = SpecialListsProgressProperty.class;
        } else if (obj.has(Task.SUBTASK_TABLE)) {
            key = Task.SUBTASK_TABLE;
            className = SpecialListsSubtaskProperty.class;
        } else if (obj.has(FileMirakel.TABLE)) {
            key = FileMirakel.TABLE;
            className = SpecialListsFileProperty.class;
        } else if (obj.has(Tag.TABLE)) {
            key = Tag.TABLE;
            className = SpecialListsTagProperty.class;
        } else if (obj.has(ListMirakel.TABLE + '.' + ListMirakel.NAME)) {
            key = ListMirakel.TABLE + '.' + ListMirakel.NAME;
            className = SpecialListsListNameProperty.class;
        } else if (obj.has(Task.DUE + "_exists")) {
            key = Task.DUE + "_exists";
            className = SpecialListsDueExistsProperty.class;
        } else if ("{}".equals(element.toString())) {
            return absent();
        } else {
            Log.wtf(TAG, "unknown query object: " + obj.toString());
            Log.v(TAG, "implement this?");
            throw new IllegalArgumentException("unknown query object: " + obj.toString());
        }
        final SpecialListsBaseProperty prop = gson.fromJson(
                obj.get(key), className);
        return of(prop);
    }
}
