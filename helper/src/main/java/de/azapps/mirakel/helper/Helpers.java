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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;

import com.google.common.base.Optional;

import java.util.Locale;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class Helpers {
    /**
     * Wrapper-Class
     *
     * @author az
     */
    public interface ExecInterface {
        void exec();
    }

    private static String TAG = "Helpers";

    // Contact
    public static void contact(final Context context) {
        contact(context, context.getString(R.string.contact_subject));
    }

    public static void contact(final Context context, final String title) {
        String mirakelVersion = "unknown";
        try {
            mirakelVersion = context.getPackageManager().getPackageInfo(
                                 context.getPackageName(), 0).versionName;
        } catch (final NameNotFoundException e) {
            Log.e(TAG, "could not get version name from manifest!", e);
        }
        contact(context, title,
                context.getString(R.string.contact_text, mirakelVersion,
                                  android.os.Build.VERSION.SDK_INT,
                                  android.os.Build.DEVICE));
    }

    public static void contact(final Context context, final String subject,
                               final String content) {
        final Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL,
                   new String[] { context.getString(R.string.contact_email) });
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, content);
        try {
            final Intent ci = Intent.createChooser(i,
                                                   context.getString(R.string.contact_chooser));
            ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(ci);
        } catch (final android.content.ActivityNotFoundException ex) {
            ErrorReporter.report(ErrorType.CONTACT_NO_CLIENT);
        }
    }

    public static Locale getLocale(final Context ctx) {
        final String current = MirakelCommonPreferences.getLanguage();
        final Locale locale = "-1".equals(current) ? Locale.getDefault()
                              : new Locale(current);
        Locale.setDefault(locale);
        final Configuration config = new Configuration();
        config.locale = locale;
        ctx.getApplicationContext()
        .getResources()
        .updateConfiguration(
            config,
            ctx.getApplicationContext().getResources()
            .getDisplayMetrics());
        return locale;
    }



    // MISC


    public static void openURL(final Context ctx, final String url) {
        final Intent i2 = new Intent(Intent.ACTION_VIEW);
        i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i2.setData(Uri.parse(url));
        ctx.startActivity(i2);
    }

    public static void restartApp(final Context context) {
        PendingIntent intent;
        try {
            intent = PendingIntent
                     .getActivity(
                         context,
                         0,
                         new Intent(
                             context,
                             Class.forName("de.azapps.mirakel.new_ui.activities.SplashScreenActivity")),
                         0);
        } catch (final ClassNotFoundException e) {
            Log.wtf(TAG, "splashscreen not found");
            return;
        }
        final AlarmManager manager = (AlarmManager) context
                                     .getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, intent);
        System.exit(2);
    }

    public static void showFileChooser(final int code, final String title,
                                       final Activity activity) {
        final Intent fileDialogIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileDialogIntent.setType("*/*");
        fileDialogIntent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            activity.startActivityForResult(
                Intent.createChooser(fileDialogIntent, title), code);
        } catch (final android.content.ActivityNotFoundException ex) {
            ErrorReporter.report(ErrorType.NO_FILEMANAGER);
        }
    }

    public static Bitmap getBitmap(int resId, Context ctx) {
        int mLargeIconWidth = (int) ctx.getResources().getDimension(
                                  android.R.dimen.notification_large_icon_width);
        int mLargeIconHeight = (int) ctx.getResources().getDimension(
                                   android.R.dimen.notification_large_icon_height);
        Drawable d = ctx.getResources().getDrawable(resId);
        Bitmap b = Bitmap.createBitmap(mLargeIconWidth, mLargeIconHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, mLargeIconWidth, mLargeIconHeight);
        d.draw(c);
        return b;
    }

    public static Optional<Class<?>> getMainActivity() {
        try {
            return (Optional<Class<?>>) of(Class.forName(DefinitionsHelper.MIRAKEL_ACTIVITY_CLASS));
        } catch (ClassNotFoundException e) {
            return absent();
        }
    }

}
