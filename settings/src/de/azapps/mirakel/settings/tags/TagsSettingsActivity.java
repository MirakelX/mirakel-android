package de.azapps.mirakel.settings.tags;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.R;

public class TagsSettingsActivity extends ListSettings {

    private Tag tag;

    private Tag newTag() {
        return Tag.newTag(getString(R.string.tag_new));
    }

    @Override
    protected OnClickListener getAddOnClickListener() {
        return new OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(final View v) {
                newTag();
                clickOnLast();
                invalidateHeaders();
            }
        };
    }

    @Override
    public OnClickListener getDelOnClickListener() {
        return new OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(final View v) {
                TagsSettingsActivity.this.tag.destroy();
                if (Build.VERSION.SDK_INT < 11 || !onIsMultiPane()) {
                    finish();
                } else {
                    try {
                        if (getHeader().size() > 0) {
                            onHeaderClick(getHeader().get(0), 0);
                        }
                        invalidateHeaders();
                    } catch (final Exception e) {
                        finish();
                    }
                }
            }
        };
    }

    @Override
    protected Class<?> getDestClass() {
        return TagsSettingsActivity.class;
    }

    @Override
    protected Class<?> getDestFragmentClass() {
        return TagsSettingsFragment.class;
    }

    @Override
    protected OnClickListener getHelpOnClickListener() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<Pair<Long, String>> getItems() {
        final List<Tag> tags = Tag.all();
        final List<Pair<Long, String>> items = new ArrayList<>();
        for (final Tag t : tags) {
            items.add(new Pair<>(t.getId(), t.getName()));
        }
        return items;
    }

    @Override
    protected int getSettingsRessource() {
        return R.xml.settings_tag;
    }

    @Override
    protected int getTitleRessource() {
        return R.string.tag_settings;
    }

    @Override
    protected void setupSettings() {
        this.tag = Tag.get(getIntent().getLongExtra("id", 0));
        new TagSettings(this, this.tag).setup();
    }

    public void setTag(final Tag tag) {
        this.tag = tag;
    }

}
