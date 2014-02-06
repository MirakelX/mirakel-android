package de.azapps.mirakel.helper;

import de.azapps.mirakel.Mirakel;


public class BuildHelper {

	public static boolean isBeta() {
		return MirakelPreferences.isDebug();
	}

	public static boolean isForFDroid() {
		return !Mirakel.IS_PLAYSTORE;
	}

	public static boolean isForPlayStore() {
		return Mirakel.IS_PLAYSTORE;
	}

	public static boolean isNightly() {
		return MirakelPreferences.isDebug();
	}

	public static boolean isRelease() {
		return !MirakelPreferences.isDebug();
	}

	public static boolean useAutoUpdater() {
		return !(isForPlayStore() || isForFDroid());
	}
}
