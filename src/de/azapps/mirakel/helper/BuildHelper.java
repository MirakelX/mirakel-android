package de.azapps.mirakel.helper;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakelandroid.BuildConfig;


public class BuildHelper {
	public static boolean	DEBUG				= BuildConfig.DEBUG;
	public static boolean isBeta() {
		return DEBUG;
	}

	public static boolean isForFDroid() {
		return !Mirakel.IS_PLAYSTORE;
	}

	public static boolean isForPlayStore() {
		return Mirakel.IS_PLAYSTORE;
	}

	public static boolean isNightly() {
		return DEBUG;
	}

	public static boolean isRelease() {
		return !DEBUG;
	}

	public static boolean useAutoUpdater() {
		return !(isForPlayStore() || isForFDroid());
	}
}
