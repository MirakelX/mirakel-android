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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;
import de.azapps.tools.Log;

public class NumPickerPref extends DialogPreference {
    private static final String TAG = "NumPickerPref";
    private final Context ctx;
    protected int MAX_VAL;
    protected int MIN_VAL;
    private int SUMMARY_ID;
    protected int VALUE;
    private NumberPicker picker;
    private View dialog;

    public NumPickerPref(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            final String attr = attrs.getAttributeName(i);
            if (attr.equals("minimumValue")) {
                this.MIN_VAL = attrs.getAttributeIntValue(i, 0);
            } else if (attr.equals("maximumValue")) {
                this.MAX_VAL = attrs.getAttributeIntValue(i, 0);
            } else if (attr.equals("summaryString")) {
                this.SUMMARY_ID = attrs.getAttributeResourceValue(i, 0);
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        this.dialog = ((Activity) this.ctx).getLayoutInflater().inflate(
                          R.layout.num_picker_pref, null);
        this.picker = (NumberPicker) this.dialog
                      .findViewById(R.id.numberPicker);
        this.picker.setMaxValue(this.MAX_VAL);
        this.picker.setMinValue(this.MIN_VAL);
        this.picker.setValue(this.VALUE);
        this.picker.setWrapSelectorWheel(false);
        this.picker.setOnValueChangedListener(new OnValueChangeListener() {
            @Override
            public void onValueChange(final NumberPicker picker,
                                      final int oldVal, final int newVal) {
                NumPickerPref.this.VALUE = newVal;
                updateSummary();
            }
        });
        updateSummary();
        builder.setPositiveButton(android.R.string.ok,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                callChangeListener(NumPickerPref.this.VALUE);
            }
        });
        builder.setNegativeButton(android.R.string.cancel,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                // Nothing
            }
        });
        builder.setView(this.dialog);
    }

    protected void updateSummary() {
        if (this.SUMMARY_ID != 0 && this.dialog != null) {
            ((TextView) this.dialog.findViewById(R.id.num_picker_pref_summary))
            .setText(this.ctx.getResources().getQuantityString(
                         this.SUMMARY_ID, this.VALUE));
        }
    }

    public int getValue() {
        return this.VALUE;
    }

    public void setValue(final int newValue) {
        if (newValue <= this.MAX_VAL && newValue >= this.MIN_VAL) {
            this.VALUE = newValue;
        }
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        Log.d(TAG, a.getString(index));
        return super.onGetDefaultValue(a, index);
    }

    @Override
    public void onBindDialogView(final View view) {
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
