package de.azapps.mirakel;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.view.Gravity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    public static final int RESULT_SPEECH_NAME = 1, RESULT_SPEECH = 3,
                            RESULT_SETTINGS = 4, RESULT_ADD_FILE = 5, RESULT_CAMERA = 6,
                            RESULT_ADD_PICTURE = 7;
    public static final String TW_NO_PROJECT = "NO_PROJECT";
    public static final String BUNDLE_CERT = "de.azapps.mirakel.cert";
    public static final String BUNDLE_CERT_CLIENT = "de.azapps.mirakel.cert.client";
    public static final String BUNDLE_KEY_CLIENT = "de.azapps.mirakel.key.client";
    public static final String BUNDLE_SERVER_TYPE = "type";
    public static final String TYPE_TW_SYNC = "TaskWarrior";
    public static final String AUTHORITY_TYP = "de.azapps.mirakel.provider";
    public static final int NOTIF_DEFAULT = 123, NOTIF_REMINDER = 124;
    public static final String MAINACTIVITY_CLASS = "de.azapps.mirakel.main_activity.MainActivity";
    public static final String MAINWIDGET_CLASS = "de.azapps.mirakel.widget.MainWidgetProvider";
    public static final String EXTRA_LIST = "de.azapps.mirakel.EXTRA_LIST";
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
    public static int GRAVITY_LEFT, GRAVITY_RIGHT;
    public static boolean freshInstall;
    public static final int REQUEST_FILE_ANY_DO = 3;
    public static final int REQUEST_FILE_WUNDERLIST = 4;
    public static final String AUTHORITY_INTERNAL = "de.azapps.mirakel.provider.internal";
    private static final String TAG = "DefinitionsHelper";

    public static String APK_NAME;
    public static String VERSIONS_NAME;
    public static int widgets[] = {};

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void init(final Context ctx) {
        APK_NAME = ctx.getPackageName();
        try {
            VERSIONS_NAME = ctx.getPackageManager().getPackageInfo(
                                ctx.getPackageName(), 0).versionName;
        } catch (final NameNotFoundException e) {
            Log.wtf(TAG, "App not found", e);
            VERSIONS_NAME = "";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GRAVITY_LEFT = Gravity.START;
            GRAVITY_RIGHT = Gravity.END;
        } else {
            GRAVITY_LEFT = Gravity.LEFT;
            GRAVITY_RIGHT = Gravity.RIGHT;
        }
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
