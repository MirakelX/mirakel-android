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
package de.azapps.mirakel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Looper;
import android.os.StrictMode;

import net.danlew.android.joda.JodaTimeAndroid;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.Locale;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.analytics.AnalyticsWrapper;
import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.BuildHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.new_ui.helper.AcraLog;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

@ReportsCrashes(
    formUri = "http://couchdb.azapps.de/acra-mirakel/_design/acra-storage/_update/report",
    formUriBasicAuthLogin = "mirakel", formUriBasicAuthPassword = "Ieshi8Egheic0etaipeeTeibo",
    reportType = org.acra.sender.HttpSender.Type.JSON,
    httpMethod = org.acra.sender.HttpSender.Method.PUT,
    mode = ReportingInteractionMode.DIALOG,
    resToastText =
        R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
    resDialogText = R.string.crash_dialog_text,
    resDialogIcon = R.drawable.ic_info_grey600_24dp, //optional. default is a warning sign
    resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
    resDialogCommentPrompt =
        R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
    resDialogOkToast =
        R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.
)
public class Mirakel extends Application {
    // Public Constants
    @SuppressLint ("InlinedApi")
    @Override
    public void onCreate () {

        if (BuildConfig.DEBUG) {
            try {
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                                           .detectAll()
                                           .penaltyLog()
                                           .build());
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                       .detectLeakedSqlLiteObjects()
                                       .detectLeakedClosableObjects()
                                       .penaltyLog()
                                       .penaltyDeath()
                                       .build());
            } catch (NullPointerException e) {
                //happend during tests;)
            }
        }
        super.onCreate();

        init(this);
        // do this as soon as possible
        AnalyticsWrapperBase.init(new AnalyticsWrapper(Mirakel.this));
        // Stuff we can do in another thread
        new Thread (new Runnable () {
            @Override
            public void run () {
                Looper.prepare();
                // File access should not happen on the main thread
                ACRA.init(Mirakel.this);
                ACRA.setLog(new AcraLog());

                NotificationService.updateServices(Mirakel.this);
                DatabaseHelper.getDatabaseHelper(Mirakel.this);
                ReminderAlarm.init(Mirakel.this);
                // Notifications
                if (!MirakelCommonPreferences.useNotifications()
                    && (startService(new Intent(Mirakel.this,
                                                NotificationService.class)) != null)) {
                    stopService (new Intent (Mirakel.this,
                                             NotificationService.class));
                }
                if (MirakelCommonPreferences.writeLogsToFile ()) {
                    Log.enableLoggingToFile();
                }

            }
        }).start ();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ReminderAlarm.destroy();
        Log.destroy();
    }

    public static void init (final Context ctx) {
        JodaTimeAndroid.init(ctx);
        // This we have to initialize as early as possible
        DefinitionsHelper.init(ctx, BuildConfig.FLAVOR);
        MirakelPreferences.init(ctx);
        ErrorReporter.init(ctx);
        ThemeManager.init(ctx, R.style.MirakelBaseTheme, R.style.MirakelDialogTheme);
        CursorGetter.init(ctx);
        ModelBase.init(ctx);
        Settings.init(ctx);
        final Locale locale = Helpers.getLocale(ctx);
        Locale.setDefault(locale);
        BuildHelper.setPlaystore(ctx.getResources ().getBoolean (
                                     R.bool.is_playstore));
        final Configuration config = new Configuration ();
        config.locale = locale;
        ctx.getResources().updateConfiguration(config,
                                               ctx.getResources().getDisplayMetrics());
    }

}
