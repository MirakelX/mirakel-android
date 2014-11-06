package de.azapps.mirakel.helper.error;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.Toast;

import de.azapps.mirakel.helper.R;

public class ErrorReporter {
    @Nullable
    private static Context context = null;
    @Nullable
    private static Toast toast = null;

    public static void init(final Context context) {
        ErrorReporter.context = context;
    }

    public static void report(final ErrorType errorType) {
        if (context == null) {
            return;
        }
        Looper.prepare();
        if (toast != null) {
            toast.cancel();
        }
        final String errorName = "error_" + errorType.toString();
        String text;
        try {
            text = context.getString(R.string.class.getField(errorName).getInt(
                                         null));
        } catch (final Exception e) {
            text = errorName;
        }

        toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.show();
    }
}
