package de.azapps.mirakel;

import java.io.File;

import de.azapps.mirakel.helper.R;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;

public class DefinitionsHelper {
	public static class NoSuchListException extends Exception {
		static final long serialVersionUID = 1374828057;
	}

	public static class NoSuchTaskException extends Exception {
		static final long serialVersionUID = 1374828058;
	}
	public static final int		RESULT_SPEECH_NAME	= 1, RESULT_SPEECH = 3,
			RESULT_SETTINGS = 4, RESULT_ADD_FILE = 5, RESULT_CAMERA = 6,
			RESULT_ADD_PICTURE = 7;
	public static final String TW_NO_PROJECT = "NO_PROJECT";
	public static final String BUNDLE_SERVER_URL = "url";
	public static final String BUNDLE_CERT = "de.azapps.mirakel.cert";
	public static final String BUNDLE_ORG = "de.azapps.mirakel.org";
	public static final String BUNDLE_SERVER_TYPE = "type";
	public static final String TYPE_TW_SYNC = "TaskWarrior";
	public static final String AUTHORITY_TYP = "de.azapps.mirakel.provider";
	public static final int NOTIF_DEFAULT = 123, NOTIF_REMINDER = 124;
	public static final String MAINACTIVITY_CLASS = "de.azapps.mirakel.main_activity.MainActivity";
	public static final String MAINWIDGET_CLASS = "de.azapps.mirakel.widget.MainWidgetProvider";
	public static final String MAIN_EXTRA_ID = "de.azapps.mirakel.EXTRA_TASKID";
	public static final String MAIN_SHOW_TASK = "de.azapps.mirakel.SHOW_TASK";
	public static final String MAIN_SHOW_LIST = "de.azapps.mirakel.SHOW_LIST";
	public static final String MAIN_SHOW_LISTS = "de.azapps.mirakel.SHOW_LISTS";
	public static final String MAIN_SHOW_LIST_FROM_WIDGET = "de.azapps.mirakel.SHOW_LIST_FROM_WIDGET";
	public static final String MAIN_ADD_TASK_FROM_WIDGET = "de.azapps.mirakel.ADD_TASK_FROM_WIDGET";
	public static final String MAIN_SHOW_TASK_FROM_WIDGET = "de.azapps.mirakel.SHOW_TASK_FROM_WIDGET";
	public static final String MAIN_TASK_ID = "de.azapp.mirakel.TASK_ID";
	public static int				GRAVITY_LEFT, GRAVITY_RIGHT;
	public static boolean freshInstall=false;
	
	public static final File EXPORT_DIR = new File(
			Environment.getExternalStorageDirectory(), "mirakel");
	public static final int REQUEST_FILE_ASTRID = 0;
	public static final int REQUEST_FILE_IMPORT_DB = 1;
	public static final int REQUEST_NEW_ACCOUNT = 2;
	public static final int REQUEST_FILE_ANY_DO = 3;
	public static final int REQUEST_FILE_WUNDERLIST = 4;
	public static final String SYNC_FINISHED="de.azapps.mirakel.sync_finished";
	private static final String TAG = "DefinitionsHelper";
	

	public static String MIRAKEL_DIR;
	public static String APK_NAME;
	public static String			VERSIONS_NAME;
	public static int widgets[] = {};

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static void init(Context ctx) {
		APK_NAME = ctx.getPackageName();
		try {
			VERSIONS_NAME = ctx.getPackageManager().getPackageInfo(
					ctx.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			Log.wtf(TAG, "App not found");
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
		NOTHING, DELETE, ADD, NEED_SYNC, IS_SYNCED;
		@Override
		public String toString() {
			return "" + toInt();
		}

		public short toInt() {
			switch (this) {
			case ADD:
				return 1;
			case DELETE:
				return -1;
			case IS_SYNCED:
				return 3;
			case NEED_SYNC:
				return 2;
			case NOTHING:
			default:
				return 0;
			}
		}

		public static SYNC_STATE parseInt(int i) {
			switch (i) {
			case -1:
				return DELETE;
			case 1:
				return ADD;
			case 2:
				return NEED_SYNC;
			case 3:
				return IS_SYNCED;
			case 0:
			default:
				return NOTHING;
			}
		}
	}
}
