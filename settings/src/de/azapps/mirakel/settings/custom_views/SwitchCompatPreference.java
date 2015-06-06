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

import android.app.Activity;
import android.content.Context;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.TextView;

import de.azapps.mirakel.settings.R;


public class SwitchCompatPreference extends SwitchPreference {

    public SwitchCompatPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchCompatPreference(Activity activity) {
        super(activity);
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        final View layout = super.onCreateView(parent);

        final ViewGroup widgetFrame = (ViewGroup) layout
                                      .findViewById(android.R.id.widget_frame);

        if (widgetFrame != null) {
            widgetFrame.removeAllViews();
            final SwitchCompat switchCompat = new SwitchCompat(getContext());
            switchCompat.setId(R.id.switch_widget);
            widgetFrame.addView(switchCompat);
        }
        return layout;
    }

    @Override
    protected void onBindView(@NonNull final View view) {
        super.onBindView(view);

        final View checkableView = view.findViewById(R.id.switch_widget);
        if ((checkableView != null) && (checkableView instanceof Checkable)) {
            if (checkableView instanceof SwitchCompat) {
                final SwitchCompat switchView = (SwitchCompat) checkableView;
                switchView.setOnCheckedChangeListener(null);
            }

            ((Checkable) checkableView).setChecked(isChecked());

            if (checkableView instanceof SwitchCompat) {
                final SwitchCompat switchView = (SwitchCompat) checkableView;
                switchView.setTextOn(getSwitchTextOn());
                switchView.setTextOff(getSwitchTextOff());
                switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        if (!callChangeListener(isChecked)) {
                            // Listener didn't like it, change it back.
                            // CompoundButton will make sure we don't recurse.
                            buttonView.setChecked(!isChecked);
                            return;
                        }
                        SwitchCompatPreference.this.setChecked(isChecked);
                    }
                });
            }
        }

        syncSummaryView(view);
    }

    private void syncSummaryView(final View view) {
        // Sync the summary view
        final TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
        if (summaryView != null) {
            boolean useDefaultSummary = true;
            if (isChecked() && !TextUtils.isEmpty(getSummaryOn())) {
                summaryView.setText(getSummaryOn().toString());
                useDefaultSummary = false;
            } else if (!isChecked() && !TextUtils.isEmpty(getSummaryOff())) {
                summaryView.setText(getSummaryOff().toString());
                useDefaultSummary = false;
            }

            if (useDefaultSummary) {
                final CharSequence summary = getSummary();
                if (!TextUtils.isEmpty(summary)) {
                    summaryView.setText(summary);
                    useDefaultSummary = false;
                }
            }

            int newVisibility = View.GONE;
            if (!useDefaultSummary) {
                // Someone has written to it
                newVisibility = View.VISIBLE;
            }
            if (newVisibility != summaryView.getVisibility()) {
                summaryView.setVisibility(newVisibility);
            }
        }
    }
}
