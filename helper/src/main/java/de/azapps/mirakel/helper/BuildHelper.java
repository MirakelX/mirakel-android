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

    public static void setPlaystore(final boolean isPlaystore) {
        IS_PLAYSTORE = isPlaystore;
    }
}
