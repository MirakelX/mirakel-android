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
package de.azapps.tools;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Optional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.azapps.mirakel.helper.MirakelCommonPreferences;

public class Log {
    private static final String TAG = "de.azapps.tools.Log";
    @NonNull
    private static Optional<FileWriter> fileWriter = Optional.absent();
    private static boolean writeToFile = false;

    public static void enableLoggingToFile() {
        writeToFile = true;
    }

    public static void disableLoggingToFile() {
        writeToFile = false;
        fileWriter = Optional.absent();
    }

    public static int d(final @Nullable String tag, final @Nullable  String msg) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        if (MirakelCommonPreferences.isDebug()) {
            write("d", tag, msg);
            return android.util.Log.d(tag, msg);
        }
        return 0;
    }

    public static int  d(final @Nullable  String tag, final @Nullable  String msg, final Throwable e) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        if (MirakelCommonPreferences.isDebug()) {
            write("d", tag, msg, e);
            return android.util.Log.d(tag, msg, e);
        }
        return 0;
    }

    public static int e(final @Nullable  String tag, final @Nullable String msg) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        write("e", tag, msg);
        return android.util.Log.e(tag, msg);
    }

    public static int e(final @Nullable String tag, final @Nullable String msg, final Throwable tr) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        write("e", tag, msg, tr);
        return android.util.Log.e(tag, msg, tr);
    }


    private static String getStackTraceString(final Throwable tr) {
        return android.util.Log.getStackTraceString(tr);
    }


    public static int i(final @Nullable String tag, final @Nullable String msg) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        if (MirakelCommonPreferences.isDebug()) {
            write("i", tag, msg);
            return android.util.Log.i(tag, msg);
        }
        return 0;
    }

    public static int i(final @Nullable String tag, final @Nullable String msg, final Throwable e) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        if (MirakelCommonPreferences.isDebug()) {
            write("i", tag, msg, e);
            return android.util.Log.i(tag, msg, e);
        }
        return 0;

    }

    public static int v(final @Nullable String tag, final @Nullable String msg) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        if (MirakelCommonPreferences.isDebug()) {
            write("v", tag, msg);
            return android.util.Log.v(tag, msg);
        }
        return 0;
    }

    public static int v(final @Nullable String tag, final @Nullable String msg, final Throwable e) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        if (MirakelCommonPreferences.isDebug()) {
            write("v", tag, msg, e);
            return android.util.Log.v(tag, msg, e);
        }
        return 0;
    }

    public static int  w(final @Nullable String tag, final @Nullable String msg) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        if (MirakelCommonPreferences.isDebug()) {
            write("w", tag, msg);
            return android.util.Log.w(tag, msg);
        }
        return 0;
    }

    public static int w(final @Nullable String tag, final @Nullable String msg, final Throwable e) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        if (MirakelCommonPreferences.isDebug()) {
            write("w", tag, msg, e);
            return android.util.Log.w(tag, msg, e);
        }
        return 0;
    }

    public static int wtf(final @Nullable String tag, final @Nullable String msg) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        write("wtf", tag, msg);
        return android.util.Log.wtf(tag, msg);
    }

    public static int wtf(final @Nullable String tag, final @Nullable String msg, final Throwable e) {
        if ((tag == null) || (msg == null)) {
            return 0;
        }
        write("wtf", tag, msg, e);
        return android.util.Log.wtf(tag, e);
    }

    private static String getTime() {
        return new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
    }

    private static void init() {
        if (fileWriter.isPresent()) {
            return;
        }
        try {
            fileWriter = Optional.of(new FileWriter(new File(FileUtils.getLogDir(),
                                                    getTime() + ".log")));
        } catch (final IOException e) {
            writeToFile = false;
            fileWriter = Optional.absent();
            Log.e(TAG, "Could not open file for logging");
        }
    }

    public static void destroy() {
        if (fileWriter.isPresent()) {
            try {
                fileWriter.get().close();
            } catch (final IOException e) {
                android.util.Log.wtf(TAG, "failed to close log", e);
            }
        }
    }

    public static void write(final String critic, final String tag,
                             final String msg) {
        write(critic, tag, msg, null);
    }

    public static void write(final String critic, final String tag,
                             final String msg, final Throwable throwable) {
        if (!writeToFile) {
            return;
        }
        init();
        if (fileWriter.isPresent()) {
            String stacktrace = "";
            if (throwable != null) {
                stacktrace = "\nStackTrace:" + getStackTraceString(throwable);
            }
            try {
                fileWriter.get().write(getTime() + "::" + critic + "::" + tag + "::"
                                       + msg + stacktrace + '\n');
                fileWriter.get().flush();
            } catch (final IOException e) {
                fileWriter = Optional.absent();
                writeToFile = false;// Prevent stackoverflow from recursive
                // calling
                Log.e(TAG, "Could not write to file for logging", e);
            }
        }
    }

    public static void longInfo(final @Nullable String str) {
        if (str == null) {
            return;
        }
        if (str.length() > 4000) {
            Log.i(TAG, str.substring(0, 4000));
            longInfo(str.substring(4000));
        } else {
            Log.i(TAG, str);
        }
    }
}
