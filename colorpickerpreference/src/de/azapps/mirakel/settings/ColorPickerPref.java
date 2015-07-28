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

package de.azapps.mirakel.settings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.SVBar;

import de.azapps.mirakel.colorpicker_pref.R;
import de.azapps.tools.Log;

public class ColorPickerPref extends DialogPreference {
    private static final String TAG = "NumPickerPref";
    private final Context ctx;
    protected int COLOR;
    private int OLD_COLOR;
    protected LinearLayout colorBox;

    public ColorPickerPref(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
        this.COLOR = this.ctx.getResources().getColor(R.color.holo_orange_dark);
        this.OLD_COLOR = this.COLOR;
        getColorBox(context);
    }

    private void getColorBox(Context context) {
        colorBox = new LinearLayout(ctx);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int)
                context.getResources().getDimension(R.dimen.colorbox_width),
                (int)context.getResources().getDimension(R.dimen.colorbox_heigth));
        colorBox.setLayoutParams(params);
    }


    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        colorBox.setBackgroundColor(COLOR);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View layout = super.onCreateView(parent);
        final ViewGroup widgetFrame = (ViewGroup) layout
                                      .findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            widgetFrame.setVisibility(View.VISIBLE);
            getColorBox(ctx);
            widgetFrame.addView(colorBox);
        }
        return layout;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
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
