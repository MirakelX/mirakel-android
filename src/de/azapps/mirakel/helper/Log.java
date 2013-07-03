package de.azapps.mirakel.helper;

import de.azapps.mirakel.BuildConfig;

public class Log {
	public static void d(String tag, String msg) {
		if (BuildConfig.DEBUG)
			android.util.Log.d(tag, msg);
	}

	public static void e(String tag, String msg) {
		android.util.Log.e(tag, msg);
	}

	public static void e(String tag, String msg, Throwable tr) {
		android.util.Log.e(tag, msg, tr);
	}

	public static void i(String tag, String msg) {
		if (BuildConfig.DEBUG)
			android.util.Log.i(tag, msg);
	}

	public static void v(String tag, String msg) {
		if (BuildConfig.DEBUG)
			android.util.Log.v(tag, msg);
	}

	public static void w(String tag, String msg) {
		if (BuildConfig.DEBUG)
			android.util.Log.w(tag, msg);
	}

	public static void wtf(String tag, String msg) {
		android.util.Log.wtf(tag, msg);
	}

	public static String getStackTraceString(Throwable tr) {
		return android.util.Log.getStackTraceString(tr);
	}
}
