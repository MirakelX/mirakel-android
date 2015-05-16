package de.azapps.mirakel.settings.custom_views;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.azapps.material_elements.views.Slider;
import de.azapps.mirakel.settings.R;

public class SliderPreference extends DialogPreference {

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
        if (positiveResult && (slider != null)) {
            current = slider.getProgress();
            callChangeListener(slider.getProgress());
        }
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        return super.onCreateView(parent);
    }
}
