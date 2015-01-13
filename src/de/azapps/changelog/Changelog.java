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

package de.azapps.changelog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.webkit.WebView;
import de.azapps.mirakel.changelog.R;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.tools.Log;

public class Changelog {
    private final int last_version;
    private int current_version;
    private static final String VERSION_CODE = "PREFS_VERSION_CODE";
    public static final int NO_VERSION = 0;
    private static final String TAG = "de.azapps.changelog";
    private final Context context;
    private final SharedPreferences settings;

    public Changelog(final Context context) {
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.context = context;
        this.last_version = this.settings.getInt(VERSION_CODE, NO_VERSION);
        try {
            this.current_version = context.getPackageManager().getPackageInfo(
                                       context.getPackageName(), 0).versionCode;
        } catch (final NameNotFoundException e) {
            this.current_version = NO_VERSION;
            Log.e(TAG, "could not get version name from manifest!", e);
        }
    }

    public boolean isUpdated() {
        return this.current_version > this.last_version;
    }

    public void showChangelog() {
        showChangelog(this.last_version);
    }

    public void showChangelog(final int sinceVersion) {
        final List<Version> versions;
        if (sinceVersion == this.current_version) {
            return;
        }
        try {
            versions = parseXML(sinceVersion);
        } catch (final XmlPullParserException e) {
            return;
        } catch (final IOException e) {
            return;
        }
        if (versions.size() == 0) {
            return;
        }
        final Editor editor = this.settings.edit();
        editor.putInt(VERSION_CODE, this.current_version);
        editor.commit();
        final String changelog = parseVersions(versions);
        final AlertDialog dialog = getDialog(changelog, sinceVersion);
        dialog.show();
    }

    private AlertDialog getDialog(String changelog, final int sinceVersion) {
        final WebView wv = new WebView(new ContextThemeWrapper(this.context,
                                       R.style.Dialog));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            wv.setBackgroundColor(0); // transparent
        } else {
            if (MirakelCommonPreferences.isDark()) {
                wv.setBackgroundColor(Color.BLACK);
            } else {
                wv.setBackgroundColor(Color.WHITE);
            }
        }
        if (MirakelCommonPreferences.isDark()) {
            changelog = "<font color='"
                        + String.format("#%06X", 0xFFFFFF & this.context
                                        .getResources().getColor(R.color.holo_blue_light))
                        + "'>" + changelog + "</font>";
        }
        wv.loadDataWithBaseURL(null, changelog, "text/html", "UTF-8", null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(
            this.context).setView(wv).setTitle("Changelog")
        .setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                dialog.cancel();
            }
        });
        // "more …" button
        if (sinceVersion != NO_VERSION) {
            builder.setNegativeButton(R.string.changelog_show_full,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog,
                                    final int id) {
                    showChangelog(NO_VERSION);
                }
            });
        }
        return builder.create();
    }

    private String parseVersions(final List<Version> versions) {
        String changelog = "";
        for (final Version v : versions) {
            changelog += "\n<h1>" + v.getName() + "</h1>";
            changelog += "<i>" + v.getDate() + "</i>";
            for (final String text : v.getText()) {
                changelog += "<p>" + text + "</p>";
            }
            final List<String> features = v.getFeatures();
            if (features.size() != 0) {
                changelog += "<ul>";
                for (final String f : features) {
                    changelog += "<li>" + f + "</li>";
                }
                changelog += "</ul>";
            }
        }
        return changelog;
    }

    enum STATE {
        TEXT, FEATURE, NONE
    }

    private List<Version> parseXML(final int sinceVersion)
    throws XmlPullParserException, IOException {
        final List<Version> versions = new ArrayList<Version>();
        Version currentVersion = null;
        final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        final XmlPullParser xpp = factory.newPullParser();
        final InputStream ins = this.context.getResources().openRawResource(
                                    R.raw.changelog);
        xpp.setInput(new BufferedReader(new InputStreamReader(ins)));
        int eventType = xpp.getEventType();
        STATE state = STATE.NONE;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                // Version
                if (xpp.getName().equals("version")) {
                    int code = 0;
                    String name = "";
                    String date = "";
                    for (int i = 0; i < xpp.getAttributeCount(); i++) {
                        if (xpp.getAttributeName(i).equals("code")) {
                            code = Integer.valueOf(xpp.getAttributeValue(i));
                        } else if (xpp.getAttributeName(i).equals("name")) {
                            name = xpp.getAttributeValue(i);
                        } else if (xpp.getAttributeName(i).equals("date")) {
                            date = xpp.getAttributeValue(i);
                        }
                    }
                    if (code <= sinceVersion) {
                        break;
                    }
                    currentVersion = new Version(code, name, date);
                } else if (xpp.getName().equals("feature")) {
                    state = STATE.FEATURE;
                } else if (xpp.getName().equals("text")) {
                    state = STATE.TEXT;
                }
            } else if (state != STATE.NONE
                       && xpp.getEventType() == XmlPullParser.TEXT
                       && currentVersion != null && !xpp.getText().equals("")) {
                switch (state) {
                case FEATURE:
                    currentVersion.addFeature(xpp.getText());
                    break;
                case TEXT:
                    currentVersion.addText(xpp.getText());
                    break;
                default:
                    break;
                }
                state = STATE.NONE;
            } else if (xpp.getEventType() == XmlPullParser.END_TAG
                       && xpp.getName().equals("version")
                       && currentVersion != null) {
                versions.add(currentVersion);
                currentVersion = null;
            }
            eventType = xpp.next();
        }
        return versions;
    }
}
