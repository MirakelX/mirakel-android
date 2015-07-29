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


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;

public class NegatedDeserializer<T extends SpecialListsBooleanProperty> implements
    JsonDeserializer<T> {

    private static final String TAG = "NegatedDeserializer";

    private final Class clazz;

    public NegatedDeserializer(@NonNull final Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T deserialize(final JsonElement json,
                         final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()) {
            Boolean negated = null;// initialize with stuff to mute the
            // compiler
            for (final Map.Entry<String, JsonElement> entry : json
                 .getAsJsonObject().entrySet()) {


                if (entry.getValue().isJsonPrimitive()
                    && ("done".equals(entry.getKey()) || "isSet".equals(entry.getKey()) ||
                        "isset".equals(entry.getKey()))) {
                    negated = entry.getValue().getAsBoolean();
                } else {
                    throw new JsonParseException("unknown format");
                }
            }
            if (negated != null) {
                try {
                    return (T)clazz.getConstructor(boolean.class).newInstance(negated);
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                             InvocationTargetException e) {
                    throw new JsonParseException("Could not create new SetProperty", e);
                }

            }
        }
        throw new JsonParseException("unknown format");
    }
}
