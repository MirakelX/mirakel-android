/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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

package de.azapps.mirakel.settings;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import de.azapps.mirakel.model.IGenericElementInterface;
import de.azapps.mirakel.settings.fragments.AboutSettingsFragment;
import de.azapps.mirakel.settings.fragments.BackupSettingsFragment;
import de.azapps.mirakel.settings.fragments.CreditsFragment;
import de.azapps.mirakel.settings.fragments.DevSettingsFragment;
import de.azapps.mirakel.settings.fragments.DonationFragmentWrapper;
import de.azapps.mirakel.settings.fragments.NotificationSettingsFragment;
import de.azapps.mirakel.settings.fragments.TaskSettingsFragment;
import de.azapps.mirakel.settings.fragments.UISettingsFragment;
import de.azapps.mirakel.settings.model_settings.account.AccountSettingsActivity;
import de.azapps.mirakel.settings.model_settings.special_list.SpecialListListActivity;
import de.azapps.mirakel.settings.taskfragment.TaskFragmentSettingsFragment;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public enum  Settings implements IGenericElementInterface {


    ABOUT(R.string.settings_about, R.drawable.settings_about),
    BACKUP(R.string.settings_backup, R.drawable.settings_backup),
    DEV(R.string.settings_dev, R.drawable.settings_dev),
    NOTIFICATION(R.string.notification_title, R.drawable.settings_notifications),
    TASK( R.string.settings_tasks_title, R.drawable.settings_tasks),
    UI( R.string.ui_settings_title, R.drawable.settings_ui),
    TASKUI(R.string.settings_task_fragment),
    DONATE( R.string.title_donations),
    CREDITS(R.string.action_credits),
    SYNC( R.string.sync_title, R.drawable.settings_sync),
    SPECIAL_LISTS( R.string.special_lists_click);



    @Override
    public String toString() {
        switch (this) {
        case ABOUT:
            return AboutSettingsFragment.class.getName();
        case BACKUP:
            return BackupSettingsFragment.class.getName();
        case DEV:
            return DevSettingsFragment.class.getName();
        case NOTIFICATION:
            return NotificationSettingsFragment.class.getName();
        case TASK:
            return TaskSettingsFragment.class.getName();
        case UI:
            return UISettingsFragment.class.getName();
        case TASKUI:
            return TaskFragmentSettingsFragment.class.getName();
        case DONATE:
            return DonationFragmentWrapper.class.getName();
        case CREDITS:
            return CreditsFragment.class.getName();

        }
        return super.toString();
    }

    public Fragment getFragment() {
        switch (this) {
        case ABOUT:
            return new AboutSettingsFragment();
        case BACKUP:
            return new BackupSettingsFragment();
        case DEV:
            return new DevSettingsFragment();
        case NOTIFICATION:
            return new NotificationSettingsFragment();
        case TASK:
            return new TaskSettingsFragment();
        case UI:
            return new UISettingsFragment();
        case TASKUI:
            return new TaskFragmentSettingsFragment();
        case CREDITS:
            return new CreditsFragment();
        case DONATE:
            return DonationFragmentWrapper.newInstance();
        case SYNC:
            break;
        case SPECIAL_LISTS:
            break;
        }
        return new UISettingsFragment();
    }



    @NonNull
    private int titleResId;
    @NonNull
    private Optional<Integer> iconResId;
    @NonNull
    private static Context ctx;

    public static void init(@NonNull final Context ctx) {
        Settings.ctx = ctx;
    }


    Settings(final int titleResId) {
        this.titleResId = titleResId;
        iconResId = absent();
    }

    Settings(final int titleResId, final int iconResId) {
        this.titleResId = titleResId;
        this.iconResId = of(iconResId);
    }


    @Override
    public String getName() {
        return ctx.getString(titleResId);
    }

    @Override
    public void save() {
        //nothing
    }

    @Override
    public void destroy() {
        //nothing
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(this.titleResId);
        OptionalUtils.writeToParcel(dest, iconResId);
    }

    private Settings(final Parcel in) {
        this.titleResId = in.readInt();
        this.iconResId = OptionalUtils.readFromParcel(in, Integer.class);
    }

    public static final Creator<Settings> CREATOR = new Creator<Settings>() {
        public Settings createFromParcel(Parcel source) {
            return UI;
        }

        public Settings[] newArray(int size) {
            return new Settings[size];
        }
    };

    @NonNull
    public Optional<Integer> getIconResId() {
        return iconResId;
    }

    public Intent getIntent(final @NonNull Context ctx) {
        switch (this) {
        case SYNC:
            return new Intent(ctx, AccountSettingsActivity.class);
        case SPECIAL_LISTS:
            return new Intent(ctx, SpecialListListActivity.class);
        case ABOUT:
        case BACKUP:
        case DEV:
        case NOTIFICATION:
        case TASK:
        case UI:
        case TASKUI:
        case DONATE:
        case CREDITS:
            return new Intent(ctx, SettingsActivity.class);
        }
        throw new IllegalArgumentException("Implement me");
    }
}
