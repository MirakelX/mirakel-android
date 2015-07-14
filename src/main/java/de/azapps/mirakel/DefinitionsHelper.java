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

package de.azapps.mirakel;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.azapps.tools.Log;

public class DefinitionsHelper {


    public static class NoSuchListException extends Exception {
        static final long serialVersionUID = 1374828057L;

        public NoSuchListException() {
            super();
        }

        public NoSuchListException(final String message) {
            super(message);
        }
    }

    public static class NoSuchTaskException extends Exception {
        static final long serialVersionUID = 1374828058L;
    }

    public static final String TW_NO_PROJECT = "NO_PROJECT";
    public static final String BUNDLE_CERT = "de.azapps.mirakel.cert";
    public static final String BUNDLE_CERT_CLIENT = "de.azapps.mirakel.cert.client";
    public static final String BUNDLE_KEY_CLIENT = "de.azapps.mirakel.key.client";
    public static final String BUNDLE_SERVER_TYPE = "type";
    public static final String AUTHORITY_TYP = "de.azapps.mirakel.provider";
    public static final int NOTIF_DEFAULT = 123, NOTIF_REMINDER = 124;
    public static final String MIRAKEL_ACTIVITY_CLASS =
        "de.azapps.mirakel.new_ui.activities.MirakelActivity";
    public static final String MAINWIDGET_CLASS = "de.azapps.mirakel.widget.MainWidgetProvider";
    public static final String EXTRA_LIST = "de.azapps.mirakel.EXTRA_LIST";
    /**
     * Do not use this for Intents. It is only for inter app communication
     */
    public static final String EXTRA_LIST_ID = "de.azapps.mirakel.EXTRA_LIST_ID";
    public static final String EXTRA_TASK = "de.azapps.mirakel.EXTRA_TASK";
    public static final String EXTRA_TASK_REMINDER = "de.azapps.mirakel.reminder.EXTRA_TASK";
    public static final String SHOW_TASK = "de.azapps.mirakel.SHOW_TASK";
    public static final String SHOW_TASK_REMINDER = "de.azapps.mirakel.reminder.SHOW_TASK";
    public static final String SHOW_LIST = "de.azapps.mirakel.SHOW_LIST";
    public static final String SHOW_LISTS = "de.azapps.mirakel.SHOW_LISTS";
    public static final String SHOW_LIST_FROM_WIDGET = "de.azapps.mirakel.SHOW_LIST_FROM_WIDGET";
    public static final String ADD_TASK_FROM_WIDGET = "de.azapps.mirakel.ADD_TASK_FROM_WIDGET";
    public static final String SHOW_TASK_FROM_WIDGET = "de.azapps.mirakel.SHOW_TASK_FROM_WIDGET";
    public static final String SHOW_MESSAGE = "de.azapps.mirakel.SHOW_MESSAGE";
    public static final String BUNDLE_WRAPPER = "de.azapps.mirakel.Bundle.Wrapper";
    public static boolean freshInstall;
    public static final int REQUEST_FILE_ANY_DO = 3;
    public static final String AUTHORITY_INTERNAL = "de.azapps.mirakel.provider.internal";
    private static final String TAG = "DefinitionsHelper";

    public static String APK_NAME;
    public static String VERSIONS_NAME;
    public static int widgets[] = {};
    private static FLAVOR flavor;

    public enum FLAVOR {
        GOOGLE, FDROID;
    }


    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void init(final Context ctx, final String flavor) {
        APK_NAME = ctx.getPackageName();
        if ("google".equals(flavor)) {
            DefinitionsHelper.flavor = FLAVOR.GOOGLE;
        } else {
            DefinitionsHelper.flavor = FLAVOR.FDROID;
        }
        try {
            VERSIONS_NAME = ctx.getPackageManager().getPackageInfo(
                                ctx.getPackageName(), 0).versionName;
        } catch (final NameNotFoundException e) {
            Log.wtf(TAG, "App not found", e);
            VERSIONS_NAME = "";
        }
    }

    public static FLAVOR getFlavor() {
        return flavor;
    }
    public static boolean isGoogle() {
        return flavor == FLAVOR.GOOGLE;
    }
    public static boolean isFdroid() {
        return flavor == FLAVOR.FDROID;
    }

    public enum SYNC_STATE {
        NOTHING((short) 0), DELETE((short) - 1), ADD((short) 1), NEED_SYNC(
            (short) 2), IS_SYNCED((short) 3);
        @Override
        public String toString() {
            return String.valueOf(toInt());
        }

        private final short eventType;

        private static final Map<Short, SYNC_STATE> map = new HashMap<>(SYNC_STATE.values().length);

        static {
            for (final SYNC_STATE eventType : SYNC_STATE.values()) {
                map.put(eventType.eventType, eventType);
            }
        }

        SYNC_STATE(final short eventType) {
            this.eventType = eventType;
        }

        public static SYNC_STATE valueOf(final short eventType) {
            return map.get(eventType);
        }

        /**
         * Use valueOf
         */
        @Deprecated
        public static SYNC_STATE parseInt(final int i) {
            return valueOf((short) i);
        }

        public static Set<SYNC_STATE> all() {
            return new HashSet<>(map.values());
        }

        public short toInt() {
            return this.eventType;
        }

    }
}
