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

package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.utils.ViewHelper;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakelandroid.R;

public class TagsView extends LinearLayout {

    @InjectView(R.id.task_tags_title)
    TextView title;
    @InjectView(R.id.task_tag_add_view)
    AddTagView addTagView;

    public TagsView(final Context context) {
        this(context, null);
    }

    public TagsView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TagsView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_tags, this);
        ButterKnife.inject(this, this);
        final Drawable icon = ThemeManager.getColoredIcon(R.drawable.ic_local_offer_white_18dp,
                              ThemeManager.getColor(R.attr.colorTextGrey));
        ViewHelper.setCompoundDrawable(getContext(), title, icon);
    }

    public void setTags(final List<Tag> tags) {
        addTagView.setTags(tags);
        invalidate();
        requestLayout();
    }

    public List<Tag> getTags() {
        return addTagView.getTags();
    }

    public void setTagChangedListener(AddTagView.TagChangedListener tagChangedListener) {
        addTagView.setTagChangedListener(tagChangedListener);
    }

    private static final String PARENT_STATE = "parent";
    private static final String EDIT_STATE = "edit_state";
    private static final String EDIT_POS = "edit_pos";
    private static final String HAS_FOCUS = "has_focus";

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle out = new Bundle();
        out.putParcelable(PARENT_STATE, super.onSaveInstanceState());
        out.putParcelable(EDIT_STATE, addTagView.onSaveInstanceState());
        out.putInt(EDIT_POS, addTagView.getSelectionEnd());
        out.putBoolean(HAS_FOCUS, addTagView.hasFocus());
        return out;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof Bundle)) {
            return;
        }
        final Bundle saved = (Bundle) state;
        super.onRestoreInstanceState(saved.getParcelable(PARENT_STATE));
        addTagView.onRestoreInstanceState(saved.getParcelable(EDIT_STATE));
        addTagView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (saved.getBoolean(HAS_FOCUS)) {
                    addTagView.clearFocus();
                    addTagView.onClick(addTagView);
                }
            }
        }, 10L);
    }
}
