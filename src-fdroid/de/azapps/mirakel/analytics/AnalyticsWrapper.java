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

package de.azapps.mirakel.analytics;

import android.app.Application;
import android.widget.Toast;

public class AnalyticsWrapper {

    public static void init(Application application) {
        Toast.makeText(application.getApplicationContext(), "Fdroid", Toast.LENGTH_LONG).show();
    }

    /*
    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    public static void init(Application application) {
        analytics = GoogleAnalytics.getInstance(application);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker("UA-XXXXX-Y"); // Replace with actual tracker/property Id
        tracker.enableExceptionReporting(false);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);
    }
    */
}
