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

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;

/**
 * Created by az on 06.07.14.
 */
public class CaldavDatabaseHelper {
    public static Uri getListsUri() {
        return MirakelInternalContentProvider.CALDAV_LISTS_URI;
    }

    public static Uri getTasksUri(boolean withProperties) {
        if (withProperties) {
            return MirakelInternalContentProvider.CALDAV_TASKS_PROPERTY_URI;
        } else {
            return MirakelInternalContentProvider.CALDAV_TASKS_URI;
        }
    }

    public static Uri getInstancesUri(boolean withProperties) {
        if (withProperties) {
            return MirakelInternalContentProvider.CALDAV_INSTANCE_PROPERTIES_URI;
        } else {
            return MirakelInternalContentProvider.CALDAV_INSTANCES_URI;
        }
    }

    public static Uri getPropertiesUri() {
        return MirakelInternalContentProvider.CALDAV_PROPERTIES_URI;
    }

    public static Uri getCategoriesUri() {
        return MirakelInternalContentProvider.CALDAV_CATEGORIES_URI;
    }

    public static Uri getAlarmsUri() {
        return MirakelInternalContentProvider.CALDAV_ALARMS_URI;
    }

    public static ContentResolver getContentProvider(Context ctx) {
        return ctx.getContentResolver();
    }
}
