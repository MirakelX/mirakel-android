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

package de.azapps.mirakel.settings.custom_views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.helper.PreferencesHelper;


public class ExpandablePreference extends PreferenceCategory implements View.OnClickListener {

    private final PreferenceScreen mSreen;
    private LinearLayout childWrapper;
    private LinearLayout.LayoutParams childWrapperParams;
    private boolean isExanded = false;
    private LinearLayout globalWrapper;
    private ImageButton expand;


    public ExpandablePreference(final Context context, PreferenceScreen screen) {
        super(context, null);
        mSreen = screen;
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        globalWrapper = new LinearLayout(getContext());
        globalWrapper.setOrientation(LinearLayout.VERTICAL);
        expand = new ImageButton(getContext());
        expand.setImageResource(R.drawable.ic_expand_more_36px);
        expand.setColorFilter(Color.BLACK);
        expand.setOnClickListener(this);
        expand.setBackgroundColor(Color.TRANSPARENT);
        final LayoutInflater layoutInflater =
            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View header = layoutInflater.inflate(R.layout.android_standard_preference, parent, false);
        ((LinearLayout)header.findViewById(android.R.id.widget_frame)).addView(expand);
        ((TextView)header.findViewById(android.R.id.title)).setText(getTitle());
        ((TextView)header.findViewById(android.R.id.summary)).setText(getSummary());
        globalWrapper.addView(header);
        return updateView(parent, false);
    }

    private View updateView(final ViewGroup parent, final boolean respectExpand) {
        if (childWrapper != null) {
            globalWrapper.removeView(childWrapper);
        }
        childWrapper = new LinearLayout(getContext());
        childWrapper.setOrientation(LinearLayout.VERTICAL);
        if (!respectExpand) {
            childWrapperParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    isExanded ? ViewGroup.LayoutParams.WRAP_CONTENT : 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                childWrapperParams.setMarginStart((int) getContext().getResources().getDimension(
                                                      R.dimen.padding_settings_card));
            } else {
                childWrapperParams.leftMargin = (int) getContext().getResources().getDimension(
                                                    R.dimen.padding_settings_card);
            }
        }
        childWrapper.setLayoutParams(childWrapperParams);
        setChildToWrapper(this);
        globalWrapper.addView(childWrapper);
        return globalWrapper;
    }

    @Override
    protected boolean onPrepareAddPreference(final Preference preference) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            preference.onParentChanged(this, shouldDisableDependents());
        } else if (!super.isEnabled()) {
            preference.setEnabled(false);
        }
        return true;
    }

    private void setChildToWrapper(final @NonNull PreferenceGroup group) {

        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference preference = group.getPreference(i);
            final OnPreferenceChangeListener onChange = preference.getOnPreferenceChangeListener();
            preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean ret = (onChange != null) && onChange.onPreferenceChange(preference, newValue);
                    if (globalWrapper != null) {
                        onBindView(updateView(null, true));
                    }
                    return ret;
                }
            });
            final View view = preference.getView(null, null);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final Integer pos = PreferencesHelper.findPreferenceScreenForPreference(preference.getKey(),
                                        mSreen);
                    if (pos != null) {
                        mSreen.onItemClick(null, null, pos, 0L);
                    }
                    if (!(preference instanceof DialogPreference)) {
                        notifyChanged();
                    }
                }
            });

            childWrapper.addView(view);
            if (preference instanceof PreferenceGroup) {
                setChildToWrapper((PreferenceGroup) preference);
            }
        }
    }

    private void animate(final int start, final int end) {
        final ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(start, end);
        animator.setDuration(250L);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                childWrapperParams.height = (int) animation.getAnimatedValue();
                childWrapper.setLayoutParams(childWrapperParams);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(final Animator animator) {
                if (end == 0) {
                    childWrapper.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationCancel(final Animator animation) {

            }

            @Override
            public void onAnimationRepeat(final Animator animation) {

            }
        });
        animator.start();
    }

    public void setExanded(final boolean value) {
        if (isExanded != value) {
            onClick(null);
            isExanded = value;
        }
    }

    @Override
    public void onClick(final View v) {
        if (childWrapper == null) {
            return;//view not inflated
        }
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        childWrapper.measure(widthSpec, heightSpec);
        if (isExanded) {
            animate(childWrapper.getMeasuredHeight(), 0);
            expand.setImageResource(R.drawable.ic_expand_more_36px);
        } else {
            childWrapper.setVisibility(View.VISIBLE);
            animate(0, childWrapper.getMeasuredHeight());
            expand.setImageResource(R.drawable.ic_expand_less_36px);
        }
        isExanded = !isExanded;
    }


}
