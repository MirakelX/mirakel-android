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

    public static  void report(final ErrorType errorType) {
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
                                             null));
            } catch (final Exception e) {
                text = errorName;
            }

            toast = of(Toast.makeText(context, text, Toast.LENGTH_LONG));
            toast.get().show();
        }
    }
}
