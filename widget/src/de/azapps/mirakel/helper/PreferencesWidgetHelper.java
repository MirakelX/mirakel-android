package de.azapps.mirakel.helper;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.animation.DecelerateInterpolator;
import android.widget.SeekBar;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.settings.ColorPickerPref;
import de.azapps.mirakel.widget.R;

public class PreferencesWidgetHelper extends PreferencesHelper {
    public PreferencesWidgetHelper(final PreferenceActivity c) {
        super(c);
    }

    public PreferencesWidgetHelper(final PreferenceFragment c) {
        super(c);
    }

    @SuppressLint("NewApi")
    public void setFunctionsWidget(final Context context, final int widgetId) {
        final List<ListMirakel> lists = ListMirakel.all();
        final CharSequence entryValues[] = new String[lists.size()];
        final CharSequence entries[] = new String[lists.size()];
        int i = 0;
        for (final ListMirakel list : lists) {
            entryValues[i] = String.valueOf(list.getId());
            entries[i] = list.getName();
            i++;
        }
        final ListMirakel list = WidgetHelper.getList(context, widgetId);
        if (list == null) {
            return;
        }
        final CheckBoxPreference isDark = (CheckBoxPreference) findPreference("isDark");
        final ListPreference widgetListPreference = (ListPreference) findPreference("widgetList");
        widgetListPreference.setEntries(entries);
        widgetListPreference.setEntryValues(entryValues);
        widgetListPreference.setSummary(this.activity.getString(
                                            R.string.widget_list_summary, list));
        widgetListPreference.setValue(String.valueOf(list.getId()));
        widgetListPreference
        .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                WidgetHelper.setList(context, widgetId,
                                     Integer.parseInt((String) newValue));
                final String list = ListMirakel.get(
                                        Integer.parseInt((String) newValue)).getName();
                widgetListPreference
                .setSummary(PreferencesWidgetHelper.this.activity
                            .getString(
                                R.string.notifications_list_summary,
                                list));
                return true;
            }
        });
        isDark.setChecked(WidgetHelper.isDark(context, widgetId));
        isDark.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                WidgetHelper.setDark(context, widgetId, (Boolean) newValue);
                return true;
            }
        });
        final CheckBoxPreference isMinimalistic = (CheckBoxPreference) findPreference("isMinimalistic");
        if (isMinimalistic != null) {
            isMinimalistic.setChecked(WidgetHelper.isMinimalistic(context,
                                      widgetId));
            isMinimalistic
            .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                    final Preference preference,
                    final Object newValue) {
                    WidgetHelper.setMinimalistic(context, widgetId,
                                                 (Boolean) newValue);
                    return true;
                }
            });
        }
        final CheckBoxPreference noGradient = (CheckBoxPreference) findPreference("widgetUseGradient");
        if (noGradient != null) {
            noGradient.setChecked(WidgetHelper
                                  .gethasGradient(context, widgetId));
            noGradient
            .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                    final Preference preference,
                    final Object newValue) {
                    WidgetHelper.setHasGradient(context, widgetId,
                                                (Boolean) newValue);
                    return true;
                }
            });
        }
        final CheckBoxPreference showDone = (CheckBoxPreference) findPreference("showDone");
        showDone.setChecked(WidgetHelper.showDone(context, widgetId));
        showDone.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                WidgetHelper.setDone(context, widgetId, (Boolean) newValue);
                return true;
            }
        });
        final CheckBoxPreference dueColors = (CheckBoxPreference) findPreference("widgetDueColors");
        dueColors.setChecked(WidgetHelper.dueColors(context, widgetId));
        dueColors
        .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                WidgetHelper.setDueColors(context, widgetId,
                                          (Boolean) newValue);
                return true;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final Preference widgetTransparency = findPreference("widgetTransparency");
            final ColorPickerPref widgetFontColor = (ColorPickerPref) findPreference("widgetFontColor");
            // ((SettingsFragment)ctx).getActivity().findViewById(R.id.color_box).setBackgroundColor(WidgetHelper.getFontColor(context,
            // widgetId));
            widgetFontColor.setColor(WidgetHelper.getFontColor(context,
                                     widgetId));
            widgetFontColor.setOldColor(WidgetHelper.getFontColor(context,
                                        widgetId));
            widgetFontColor
            .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                    final Preference preference,
                    final Object newValue) {
                    WidgetHelper.setFontColor(context, widgetId,
                                              widgetFontColor.getColor());
                    return false;
                }
            });
            widgetTransparency.setSummary(this.activity.getString(
                                              R.string.widget_transparency_summary, 100 - Math
                                              .round(WidgetHelper.getTransparency(context,
                                                      widgetId) / 255f * 1000) / 10));
            widgetTransparency
            .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(
                    final Preference preference) {
                    final SeekBar sb = new SeekBar(context);
                    sb.setMax(255);
                    sb.setInterpolator(new DecelerateInterpolator());
                    sb.setProgress(255 - WidgetHelper.getTransparency(
                                       context, widgetId));
                    sb.setPadding(20, 30, 20, 30);
                    new AlertDialog.Builder(context)
                    .setTitle(R.string.widget_transparency)
                    .setView(sb)
                    .setPositiveButton(android.R.string.ok,
                    new OnClickListener() {
                        @Override
                        public void onClick(
                            final DialogInterface dialog,
                            final int which) {
                            // Fix Direction
                            WidgetHelper
                            .setTransparency(
                                context,
                                widgetId,
                                255 - sb.getProgress());
                            final float t = 100 - Math.round(WidgetHelper
                                                             .getTransparency(
                                                                 context,
                                                                 widgetId) / 255f * 1000) / 10;
                            widgetTransparency
                            .setSummary(PreferencesWidgetHelper.this.activity
                                        .getString(
                                            R.string.widget_transparency_summary,
                                            t));
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                                       null).show();
                    return false;
                }
            });
        } else {
            removePreference("widgetTransparency");
            removePreference("widgetFontColor");
            if (isMinimalistic != null) {
                removePreference("isMinimalistic");
            }
        }
    }

}
