package de.azapps.mirakel.settings.tags;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.R;
import de.azapps.tools.Log;

@SuppressLint("NewApi")
public class TagsSettingsFragment extends PreferenceFragment {
    private static final String TAG = "TagsSettingsFragment";
    private Tag tag;

    public TagsSettingsFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_tag);
        final ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        final Bundle b = getArguments();
        if (b != null) {
            this.tag = Tag.getTag(getArguments().getInt("id"));
            ((TagsSettingsActivity) getActivity()).setTag(this.tag);
            actionBar.setTitle(this.tag.getName());
            if (!MirakelCommonPreferences.isTablet()) {
                final ImageButton delTag = new ImageButton(getActivity());
                delTag.setBackgroundResource(android.R.drawable.ic_menu_delete);
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                                            ActionBar.DISPLAY_SHOW_CUSTOM);
                actionBar.setCustomView(delTag, new ActionBar.LayoutParams(
                                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                                            Gravity.CENTER_VERTICAL
                                            | DefinitionsHelper.GRAVITY_RIGHT));
                delTag.setOnClickListener(((ListSettings) getActivity())
                                          .getDelOnClickListener());
            }
            new TagSettings(this, this.tag).setup();
        } else {
            Log.d(TAG, "bundle null");
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            getActivity().finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
