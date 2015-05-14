package de.azapps.mirakel.settings.custom_views;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import de.azapps.material_elements.views.Slider;
import de.azapps.mirakel.settings.R;

public class SliderPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    private int max = 100;
    private int current = 0;
    @Nullable
    private Slider slider;

    public SliderPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        final View v = LayoutInflater.from(getContext()).inflate(R.layout.slider_wrapper, null);
        slider = (Slider) v.findViewById(R.id.progress_bar);
        slider.setMax(max);
        slider.setProgress(current);
        slider.setOnSeekBarChangeListener(this);
        builder.setView(v);
    }

    public void setProgress(final int current) {
        this.current = current;
        if (slider != null) {
            slider.setProgress(current);
        }
    }

    public void setMax(final int max) {
        this.max = max;
        if (slider != null) {
            slider.setMax(max);
        }
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            callChangeListener(current);
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        return super.onCreateView(parent);
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if (fromUser) {
            current = progress;
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {

    }
}
