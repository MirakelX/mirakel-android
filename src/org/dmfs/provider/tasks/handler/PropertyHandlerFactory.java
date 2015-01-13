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

package org.dmfs.provider.tasks.handler;

import org.dmfs.provider.tasks.TaskContract.Property.Alarm;
import org.dmfs.provider.tasks.TaskContract.Property.Category;


/**
 * A factory that creates the matching {@link PropertyHandler} for the given mimetype.
 *
 * @author Tobias Reinsch <tobias@dmfs.org>
 *
 */
public class PropertyHandlerFactory {
    /**
     * Creates a specific {@link PropertyHandler}.
     *
     * @param mimeType
     *            The mimetype of the property.
     * @return The matching {@link PropertyHandler} for the given mimetype or <code>null</code>
     */
    public static PropertyHandler create(String mimeType) {
        if (Category.CONTENT_ITEM_TYPE.equals(mimeType)) {
            return new CategoryHandler();
        }
        if (Alarm.CONTENT_ITEM_TYPE.equals(mimeType)) {
            return new AlarmHandler();
        }
        return new DefaultPropertyHandler();
    }
}
