package de.azapps.mirakel.helper;

import de.azapps.mirakelandroid.BuildConfig;

public class BuildHelper {
	public static boolean isNightly() {
		return BuildConfig.DEBUG;
	}

	public static boolean isBeta() {
		return BuildConfig.DEBUG;
	}

	public static boolean isRelease() {
		return !BuildConfig.DEBUG;
	}

	public static boolean isForPlayStore() {
		return false;
	}

	public static boolean isForFDroid() {
		return true;
	}

	public static boolean useAutoUpdater() {
		return !(isForPlayStore() || isForFDroid());
	}
}
