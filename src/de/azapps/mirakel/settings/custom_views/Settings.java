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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Parcel;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.generic.IGenericElementInterface;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.SettingsActivity;
import de.azapps.mirakel.settings.fragments.AboutSettingsFragment;
import de.azapps.mirakel.settings.fragments.BackupSettingsFragment;
import de.azapps.mirakel.settings.fragments.CreditsFragment;
import de.azapps.mirakel.settings.fragments.DevSettingsFragment;
import de.azapps.mirakel.settings.fragments.DonationFragmentWrapper;
import de.azapps.mirakel.settings.fragments.NotificationSettingsFragment;
import de.azapps.mirakel.settings.fragments.SemanticFragment;
import de.azapps.mirakel.settings.fragments.UISettingsFragment;
import de.azapps.mirakel.settings.model_settings.account.AccountSettingsActivity;
import de.azapps.mirakel.settings.model_settings.special_list.SpecialListListActivity;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public enum  Settings implements IGenericElementInterface {


    ABOUT(R.string.settings_about, R.drawable.ic_info_24px),
    BACKUP(R.string.settings_backup, R.drawable.ic_settings_backup_restore_24px),
    DEV(R.string.settings_dev, R.drawable.ic_settings_development),
    NOTIFICATION(R.string.notification_title, R.drawable.ic_notifications_24px),
    UI( R.string.ui_settings_title, R.drawable.ic_stay_current_portrait_24px),
    DONATE( R.string.title_donations, R.drawable.ic_favorite_24px),
    CREDITS(R.string.action_credits),
    SYNC( R.string.sync_title, R.drawable.ic_sync_24px),
    SPECIAL_LISTS( R.string.special_lists_click, R.drawable.ic_local_offer_24px),
    TASK_TEMPLATES(R.string.settings_semantics_title),
    WIDGET(R.string.settings_widget_title);

    private static final Map<Integer, List<Settings>> all = new ArrayMap<>(4);
    static{
        all.put(R.string.general, Arrays.asList(UI, NOTIFICATION));
        all.put(R.string.settings_about, Arrays.asList(DONATE, ABOUT));
        all.put(R.string.settings_advanced, Arrays.asList(SPECIAL_LISTS, BACKUP, SYNC, DEV));
    }

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
        case UI:
            return UISettingsFragment.class.getName();
        case DONATE:
            return DonationFragmentWrapper.class.getName();
        case CREDITS:
            return CreditsFragment.class.getName();
        case SYNC:
            break;
        case SPECIAL_LISTS:
            break;
        case TASK_TEMPLATES:
            return SemanticFragment.class.getName();

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
        case UI:
            return new UISettingsFragment();
        case CREDITS:
            return new CreditsFragment();
        case DONATE:
            return DonationFragmentWrapper.newInstance();
        case SYNC:
            break;
        case SPECIAL_LISTS:
            break;
        case TASK_TEMPLATES:
            return new SemanticFragment();
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

    static class SettingsHeader extends Preference{

        private final int iconPadding;
        public SettingsHeader(final Context context) {
            super(context);
            iconPadding = (int) context.getResources().getDimension(R.dimen.padding_settings_header_icon);
        }

        @Override
        public View getView(final View convertView, final ViewGroup parent) {
            final View v = super.getView(convertView, parent);
            final ImageView icon = (ImageView) v.findViewById(android.R.id.icon);
            icon.setColorFilter(ThemeManager.getAccentThemeColor());
            icon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);

            final TextView text = (TextView) v.findViewById(android.R.id.title);
            text.setTextColor(ThemeManager.getColor(
                R.attr.colorTextBlack));
            text.setTypeface(Typeface.DEFAULT);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0F);
            return v;
        }
    }

    Preference getPreference(final @NonNull OnItemClickedListener<Settings> onClick) {
        final Preference p = new SettingsHeader(ctx);
        p.setKey(toString());
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                onClick.onItemSelected(Settings.this);
                return true;
            }
        });
        if (iconResId.isPresent()) {
            p.setIcon(iconResId.get());
        }
        p.setTitle(titleResId);
        return p;
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
        case UI:
        case DONATE:
        case CREDITS:
        case TASK_TEMPLATES:
            return new Intent(ctx, SettingsActivity.class);
        }
        throw new IllegalArgumentException("Implement me");
    }


    public static PreferenceScreen inflateHeaders(final @NonNull PreferenceScreen screen, final @NonNull OnItemClickedListener<Settings> onClick) {
        for (final Map.Entry<Integer, List<Settings>> id : all.entrySet()) {
            final PreferenceCategory cat = new PreferenceCategory(ctx);
            screen.addPreference(cat);
            cat.setTitle(id.getKey());
            for (final Settings setting : id.getValue()) {
                if (setting == DEV && !MirakelCommonPreferences.isEnabledDebugMenu()) {
                    continue;
                }
                cat.addItemFromInflater(setting.getPreference(onClick));
            }
        }
        return screen;

    }

}
