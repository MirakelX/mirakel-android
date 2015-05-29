package de.azapps.material_elements.utils;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;

import de.azapps.material_elements.R;

public class AnimationHelper {

    public static void moveViewUp(final Context context, final View view, final int height) {
        final TranslateAnimation anim = new TranslateAnimation(0, 0, 0, -height);
        anim.setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration));
        anim.setFillEnabled(true);
        anim.setInterpolator(context, R.interpolator.decelerate_cubic);
        // You do not need the animationlistener when moving it down. I do not understand it totally but it works the way it is now
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // This is a bit hacky, but it works
                view.layout(view.getLeft(), view.getTop() - height, view.getRight(), view.getBottom() - height);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(anim);
    }
    public static void moveViewDown(final Context context, final View view, final int height) {
        final TranslateAnimation anim = new TranslateAnimation(0, 0, -height, 0);
        anim.setDuration(context.getResources().getInteger(R.integer.anim_snackbar_duration));
        anim.setFillEnabled(true);
        anim.setInterpolator(context, R.interpolator.accelerate_cubic);
        view.startAnimation(anim);
    }

    public static void slideIn(final Context context, final View view) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in);
        animation.setInterpolator(context, R.interpolator.decelerate_cubic);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animation);
    }

    public static void slideOut(final Context context, final View view) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_out);
        animation.setInterpolator(context, R.interpolator.accelerate_cubic);
        view.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
