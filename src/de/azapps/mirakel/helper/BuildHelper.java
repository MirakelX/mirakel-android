package de.azapps.mirakel.helper;


public class BuildHelper {

	private static boolean IS_PLAYSTORE = false;

	public static boolean isBeta() {
		return MirakelCommonPreferences.isDebug();
	}

	public static boolean isForFDroid() {
		return !IS_PLAYSTORE;
	}

	public static boolean isForPlayStore() {
		return IS_PLAYSTORE;
	}

	public static boolean isNightly() {
		return MirakelCommonPreferences.isDebug();
	}

	public static boolean isRelease() {
		return !MirakelCommonPreferences.isDebug();
	}

	public static boolean useAutoUpdater() {
		return !(isForPlayStore() || isForFDroid());
	}
	
	public static void setPlaystore(boolean isPlaystore){
		IS_PLAYSTORE=isPlaystore;
	}
}
