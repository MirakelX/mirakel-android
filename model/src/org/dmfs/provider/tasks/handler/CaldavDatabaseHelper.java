package org.dmfs.provider.tasks.handler;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import de.azapps.mirakel.model.MirakelInternalContentProvider;

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
