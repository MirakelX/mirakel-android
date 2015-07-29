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

import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty.Unit;

public class DueDeserializer implements
    JsonDeserializer<SpecialListsDueProperty> {

    @Override
    public SpecialListsDueProperty deserialize(final JsonElement json,
            final Type typeOfT, final JsonDeserializationContext context)
    throws JsonParseException {
        if (json.isJsonObject()) {
            Integer length = 0, unit = 0;// initialize with stuff to mute the
            // compiler
            boolean negate = false; //set this as default to be backward compatible
            for (final Entry<String, JsonElement> entry : json
                 .getAsJsonObject().entrySet()) {
                switch (entry.getKey()) {
                case "unit":
                    if (entry.getValue().isJsonPrimitive()) {
                        unit = entry.getValue().getAsInt();
                        break;
                    }
                //$FALL-THROUGH$
                case "length":
                    if (entry.getValue().isJsonPrimitive()) {
                        length = entry.getValue().getAsInt();
                        break;
                    }
                case "negated":
                    if (entry.getValue().isJsonPrimitive()) {
                        negate = entry.getValue().getAsBoolean();
                        break;
                    }
                //$FALL-THROUGH$
                default:
                    throw new JsonParseException("unknown format");
                }
            }
            if (unit != null && length != null) {
                return new SpecialListsDueProperty(Unit.values()[unit], length, negate);
            }
        }
        throw new JsonParseException("unknown format");
    }
}