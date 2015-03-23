package de.azapps.material_elements.utils;

import android.content.Context;
import android.view.View;
import android.view.animation.TranslateAnimation;

import de.azapps.material_elements.R;

public class AnimationHelper {

    public static void moveViewUp(final Context context, final View view, final int height) {
        final TranslateAnimation anim = new TranslateAnimation(0, 0, 0, -height);
        anim.setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration));
        anim.setFillEnabled(true);
        anim.setFillAfter(true);
        anim.setInterpolator(context, R.interpolator.decelerate_cubic);
        view.startAnimation(anim);
    }
    public static void moveViewDown(final Context context, final View view, final int height) {
        final TranslateAnimation anim = new TranslateAnimation(0, 0, -height, 0);
        anim.setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration));
        anim.setFillEnabled(true);
        anim.setFillAfter(true);
        anim.setInterpolator(context, R.interpolator.accelerate_cubic);
        view.startAnimation(anim);
    }
}
