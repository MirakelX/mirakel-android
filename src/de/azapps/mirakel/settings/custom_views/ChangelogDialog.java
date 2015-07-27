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

package de.azapps.mirakel.settings.custom_views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.SettingsActivity;
import it.gmariotti.changelibs.library.view.ChangeLogRecyclerView;

public class ChangelogDialog {
    private static final String VERSION_CODE = "PREFS_VERSION_CODE";
    private static final int NO_VERSION = 0;
    public static boolean isUpdated(final Context context) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        final int last_version = settings.getInt(VERSION_CODE, NO_VERSION);
        int currentVersion = getCurrentVersion(context);
        return currentVersion > last_version;
    }

    private static int getCurrentVersion(Context context) {
        int currentVersion;
        try {
            currentVersion = context.getPackageManager().getPackageInfo(
                                 context.getPackageName(), 0).versionCode;
        } catch (final PackageManager.NameNotFoundException e) {
            currentVersion = NO_VERSION;
        }
        return currentVersion;
    }


    public static void show(final Context context, final String package_name) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(
                                            Context.LAYOUT_INFLATER_SERVICE);
        ChangeLogRecyclerView chgList = (ChangeLogRecyclerView) layoutInflater.inflate(
                                            R.layout.changelog_dialog, null);
        new AlertDialog.Builder(context)
        .setTitle(R.string.changelog)
        .setView(chgList)
        .setPositiveButton(R.string.title_donations,
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final Intent intent = new Intent(context,
                                                 SettingsActivity.class);
                intent.putExtra(SettingsActivity.SHOW_FRAGMENT, Settings.DONATE.ordinal());
                context.startActivity(intent);
                dialog.dismiss();
            }
        })
        .setNeutralButton(R.string.rate, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                try {
                    context.startActivity(new Intent(
                                              Intent.ACTION_VIEW, Uri.parse("market://details?id="
                                                      + package_name)));
                } catch (final android.content.ActivityNotFoundException anfe) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW,
                                                     Uri.parse("http://play.google.com/store/apps/details?id="
                                                             + package_name)));
                }
            }
        })
        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
        .show();
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putInt(VERSION_CODE, getCurrentVersion(context));
        editor.commit();
    }

}
