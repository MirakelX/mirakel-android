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

package de.azapps.mirakel.helper;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class AnalyticsWrapperBase {
    @Nullable
    protected static AnalyticsWrapperBase singleton;


    public static AnalyticsWrapperBase getWrapper() {
        if (singleton == null) {
            singleton = new AnalyticsWrapperBase() {
            };
        }
        return singleton;
    }

    public AnalyticsWrapperBase() {

    }

    public static AnalyticsWrapperBase init(final AnalyticsWrapperBase analyticsWrapperBase) {
        singleton = analyticsWrapperBase;
        return singleton;
    }

    public void doNotTrack() {

    }

    public enum CATEGORY {
        DUMB_USER, NORMAL_USER, ADVANCED_USER, POWER_USER, HANDLE_INTENT
    }

    public enum ACTION {
        // DUMB USER
        IMPLICIT_SAVE_TASK_NAME(CATEGORY.DUMB_USER), DISMISS_CRASH_DIALOG(CATEGORY.DUMB_USER),

        // NORMAL USER
        ADD_FILE(CATEGORY.NORMAL_USER), ADD_PHOTO(CATEGORY.NORMAL_USER), ADD_RECORDING(CATEGORY.NORMAL_USER),
        ADD_SUBTASK(CATEGORY.NORMAL_USER), SET_PROGRESS(CATEGORY.NORMAL_USER), SET_PRIORITY(CATEGORY.NORMAL_USER),
        ADD_NOTE(CATEGORY.NORMAL_USER), SEARCHED(CATEGORY.NORMAL_USER), SET_REMINDER(CATEGORY.NORMAL_USER), SET_DUE(CATEGORY.NORMAL_USER),

        // ADVANCED USER
        USED_SEMANTICS(CATEGORY.ADVANCED_USER), SET_RECURRING_REMINDER(CATEGORY.ADVANCED_USER), ADD_TAG(CATEGORY.ADVANCED_USER),

        // POWER USER
        CREATED_SEMANTIC(CATEGORY.POWER_USER), CREATED_META_LIST(CATEGORY.POWER_USER), ACTIVATED_DEVELOPMENT_SETTINGS(CATEGORY.POWER_USER);

        private CATEGORY category;

        ACTION(CATEGORY category) {
            this.category = category;
        }

        public CATEGORY getCategory() {
            return category;
        }
    }

    public static void track(final ACTION action) {
        getWrapper().track(action.getCategory(), action.toString(), null, null);
    }

    public static void track(final CATEGORY category, final String label) {
        getWrapper().track(category, label, null, null);

    }

    public static void setScreen(Object screen) {
        getWrapper().mSetScreen(screen);
    }

    public void mSetScreen(Object screen) {

    }

    public void track(@NonNull final CATEGORY category, @NonNull final String action,
                      @Nullable final String label, @Nullable final Long value) {

    }
}
