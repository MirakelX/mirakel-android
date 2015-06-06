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

package de.azapps.mirakel.helper.error;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.common.base.Optional;

import de.azapps.mirakel.helper.R;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class ErrorReporter {
    @Nullable
    private static Context context = null;
    @Nullable
    private static Optional<Toast> toast = absent();

    public static void init(final Context context) {
        ErrorReporter.context = context;
    }


    public static  void report(final ErrorType errorType, String... args) {
        if (context == null) {
            return;
        }
        if (Looper.myLooper() == null) { // check already Looper is associated or not.
            Looper.prepare(); // No Looper is defined So define a new one
        }
        synchronized (toast) {
            if (toast.isPresent()) {
                toast.get().cancel();
            }
            final String errorName = "error_" + errorType.toString();
            String text;
            try {
                text = context.getString(R.string.class.getField(errorName).getInt(
                                             null), args);
            } catch (final Exception e) {
                text = errorName;
            }

            toast = of(Toast.makeText(context, text, Toast.LENGTH_LONG));
            toast.get().show();
        }
    }
}
