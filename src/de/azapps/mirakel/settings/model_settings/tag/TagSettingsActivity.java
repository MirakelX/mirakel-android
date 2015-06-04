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

package de.azapps.mirakel.settings.model_settings.tag;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelListActivity;

import static com.google.common.base.Optional.of;

public class TagSettingsActivity extends GenericModelListActivity<Tag> {

    @Override
    protected boolean isSupport() {
        return false;
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @NonNull
    @Override
    protected Optional<Fragment> getDetailFragment(final @NonNull Tag item) {
        return of((android.app.Fragment)new TagDetailFragment());
    }

    @NonNull
    @Override
    protected Class<? extends GenericModelListActivity> getSelf() {
        return TagSettingsActivity.class;
    }


    @NonNull
    @Override
    protected Tag getDefaultItem() {
        return Tag.getSafeFirst();
    }


    @Override
    protected void createItem(@NonNull final Context ctx) {
        onItemSelected(Tag.newTag(getString(R.string.tag_new)));
    }

    @Override
    protected String getTextTitle() {
        return getString(R.string.tag_settings);
    }

    @Override
    protected Class<Tag> getItemClass() {
        return Tag.class;
    }

    @Override
    protected Cursor getQuery() {
        return new MirakelQueryBuilder(this).query(Tag.URI).getRawCursor();
    }

}
