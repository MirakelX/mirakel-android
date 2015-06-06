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

import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;

import de.azapps.mirakel.model.list.meta.SpecialListsStringProperty.Type;

public class StringDeserializer<T extends SpecialListsStringProperty>
    implements JsonDeserializer<T> {

    private static final String TAG = "StringDeserializer";
    private final Class<T> clazz;

    public StringDeserializer(final Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    @Override
    public T deserialize(final JsonElement json,
                         final java.lang.reflect.Type typeOfT,
                         final JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()) {
            Integer type = null;// initialize with stuff to quite compiler
            String searchString = null;
            Boolean negated = null;
            for (final Entry<String, JsonElement> entry : json
                 .getAsJsonObject().entrySet()) {
                switch (entry.getKey()) {
                case "isNegated":
                case "isSet":
                    if (entry.getValue().isJsonPrimitive()) {
                        negated = entry.getValue().getAsBoolean();
                        break;
                    }
                //$FALL-THROUGH$
                case "type":
                    if (entry.getValue().isJsonPrimitive()) {
                        type = entry.getValue().getAsInt();
                        break;
                    }
                //$FALL-THROUGH$
                case "searchString":
                case "serachString"://this must be here
                    if (entry.getValue().isJsonPrimitive()) {
                        searchString = entry.getValue().getAsString();
                        break;
                    }
                //$FALL-THROUGH$
                default:
                    throw new JsonParseException("unknown format");
                }
            }
            if (((searchString != null) & (type != null)) && (negated != null)) {

                try {
                    return (T)clazz.getConstructor(boolean.class, String.class, Type.class).newInstance(negated,
                            searchString, Type.values()[type]);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                    throw new JsonParseException("Could not create new StringProperty", e);
                }
            }
        }
        throw new JsonParseException("unknown format");
    }

}