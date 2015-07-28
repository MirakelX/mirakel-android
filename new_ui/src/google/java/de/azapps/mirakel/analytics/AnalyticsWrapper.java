/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * <p/>
 * Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.analytics;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakelandroid.BuildConfig;

public class AnalyticsWrapper extends AnalyticsWrapperBase {
    @Nullable
    private GoogleAnalytics analytics;
    @Nullable
    private Tracker tracker;

    public AnalyticsWrapper(Application application) {
        if (MirakelCommonPreferences.useAnalytics()) {
            analytics = GoogleAnalytics.getInstance(application);
            analytics.setLocalDispatchPeriod(1800);

            tracker = analytics.newTracker(BuildConfig.TRACKER_ID);
            tracker.enableExceptionReporting(false);
            tracker.enableAdvertisingIdCollection(true);
            tracker.enableAutoActivityTracking(false);
        }
    }

    @Override
    public void track(@NonNull final CATEGORY category, @NonNull final String action,
                      @Nullable final String label, @Nullable final Long value) {
        if (tracker != null) {
            HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder();
            builder.setCategory(category.toString())
            .setAction(action);
            if (label != null) {
                builder.setLabel(label);
            }
            if (value != null) {
                builder.setValue(value);
            }
            tracker.send(builder.build());
        }
    }

    @Override
    public void mSetScreen(Object screen) {
        if (tracker != null) {
            tracker.setScreenName(screen.getClass().getSimpleName());
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    @Override
    public void doNotTrack() {
        tracker = null;
    }
}
