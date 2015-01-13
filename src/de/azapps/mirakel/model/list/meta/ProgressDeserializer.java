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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Map.Entry;

import de.azapps.mirakel.model.list.meta.SpecialListsProgressProperty.OPERATION;

public class ProgressDeserializer implements
    JsonDeserializer<SpecialListsProgressProperty> {

    private static final String TAG = "ProgressDeserializer";

    @Override
    public SpecialListsProgressProperty deserialize(final JsonElement json,
            final Type typeOfT, final JsonDeserializationContext context)
    throws JsonParseException {
        if (json.isJsonObject()) {
            Integer value = null, op = null;// initialize with stuff to mute the
            // compiler
            for (final Entry<String, JsonElement> entry : json
                 .getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive()
                    && "value".equals(entry.getKey())) {
                    value = entry.getValue().getAsInt();
                } else if (entry.getValue().isJsonPrimitive()
                           && "op".equals(entry.getKey())) {
                    op = entry.getValue().getAsInt();
                } else {
                    throw new JsonParseException("unknown format");
                }
            }
            if (value != null && op != null) {
                return new SpecialListsProgressProperty(value,
                                                        OPERATION.values()[op]);
            }
        }
        throw new JsonParseException("unknown format");
    }
}