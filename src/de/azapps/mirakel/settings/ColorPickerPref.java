package de.azapps.mirakel.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.SVBar;

import de.azapps.mirakel.colorpicker_pref.R;
import de.azapps.tools.Log;

public class ColorPickerPref extends DialogPreference {
    private static final String TAG = "NumPickerPref";
    private final Context ctx;
    protected int COLOR;
    private int OLD_COLOR;
    protected View colorBox;

    public ColorPickerPref(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
        this.COLOR = this.ctx.getResources().getColor(R.color.holo_orange_dark);
        this.OLD_COLOR = this.COLOR;
    }

    @SuppressLint("NewApi")
    @Override
    public View getView(final View convertView, final ViewGroup parent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return new View(this.ctx);
        }
        final View v = ((Activity) this.ctx).getLayoutInflater().inflate(
                           R.layout.color_pref, null);
        /*
         * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
         * v.setBackground(ctx.getResources().getDrawable(
         * android.R.attr.selectableItemBackground)); }
         */
        this.colorBox = v.findViewById(R.id.color_box);
        this.colorBox.setBackgroundColor(this.COLOR);
        ((TextView) v.findViewById(android.R.id.title)).setText(getTitle());
        v.findViewById(android.R.id.summary).setVisibility(View.GONE);
        return v;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        // super.onPrepareDialogBuilder(builder);
        final View v = ((LayoutInflater) this.ctx
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                           R.layout.color_picker, null);
        final ColorPicker cp = (ColorPicker) v.findViewById(R.id.color_picker);
        final SVBar op = (SVBar) v.findViewById(R.id.svbar_color_picker);
        cp.addSVBar(op);
        cp.setColor(this.COLOR);
        cp.setOldCenterColor(this.OLD_COLOR);
        builder.setView(v)
        .setPositiveButton(android.R.string.ok,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                ColorPickerPref.this.COLOR = cp.getColor();
                ColorPickerPref.this.colorBox
                .setBackgroundColor(cp.getColor());
                callChangeListener(ColorPickerPref.this.COLOR);
            }
        })
        .setNegativeButton(android.R.string.cancel,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                // Nothing
            }
        });
    }

    public int getColor() {
        return this.COLOR;
    }

    public void setColor(final int newValue) {
        this.COLOR = newValue;
    }

    public void setOldColor(final int newValue) {
        this.OLD_COLOR = newValue;
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        Log.d(TAG, a.getString(index));
        return super.onGetDefaultValue(a, index);
    }

    @Override
    public void onBindDialogView(final View view) {
        Log.d(TAG, "bar");
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            persistBoolean(!getPersistedBoolean(true));
        }
    }

}
