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

package de.azapps.mirakel.model.list.meta;

import android.support.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SetDeserializer<T extends SpecialListsSetProperty> implements
    JsonDeserializer<T> {

    private static final String TAG = "SetDeserializer";

    private Class clazz;

    public SetDeserializer(@NonNull final Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T deserialize(final JsonElement json,
                         final Type typeOfT, final JsonDeserializationContext context) {
        if (json.isJsonObject()) {
            List<Integer> content = null;
            Boolean negated = null;// initialize with stuff to mute the
            // compiler
            for (final Map.Entry<String, JsonElement> entry : json
                 .getAsJsonObject().entrySet()) {


                if (entry.getValue().isJsonArray()
                    && "content".equals(entry.getKey())) {
                    content = new ArrayList<>(entry.getValue().getAsJsonArray().size());
                    for (final JsonElement el : ((JsonArray)entry.getValue())) {
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                            content.add(el.getAsInt());
                        }
                    }
                } else if (entry.getValue().isJsonPrimitive()
                           && ("isNegated".equals(entry.getKey()) || "isSet".equals(entry.getKey()))) {
                    negated = entry.getValue().getAsBoolean();
                } else {
                    throw new JsonParseException("unknown format");
                }
            }
            if ((content != null) && (negated != null)) {
                try {
                    return (T)clazz.getConstructor(boolean.class, List.class).newInstance(negated, content);
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                             InvocationTargetException e) {
                    throw new JsonParseException("Could not create new SetProperty", e);
                }

            }
        }
        throw new JsonParseException("unknown format");
    }
}
