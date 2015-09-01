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

package de.azapps.mirakel.settings.model_settings.special_list;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelListActivity;

import static com.google.common.base.Optional.of;

public class SpecialListListActivity extends GenericModelListActivity<SpecialList> {
    @Override
    protected boolean isSupport() {
        return false;
    }


    @NonNull
    @Override
    protected Optional<Fragment> getDetailFragment(final @NonNull SpecialList item) {
        return of((Fragment)new SpecialListDetailFragment());
    }


    @NonNull
    @Override
    protected Class<? extends GenericModelListActivity> getSelf() {
        return SpecialListListActivity.class;
    }

    @Override
    public boolean hasFab() {
        return false;
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @NonNull
    @Override
    protected SpecialList getDefaultItem() {
        return SpecialList.firstSpecialSafe();
    }

    @Override
    protected void createItem(final @NonNull Context ctx) {
        try {
            onItemSelected(SpecialList.newList(ctx.getString(R.string.special_lists_new),
                                               Optional.<SpecialListsBaseProperty>absent(), true));
        } catch (final ListMirakel.ListAlreadyExistsException e) {
            //eat it
        }
    }

    @Override
    protected String getTextTitle() {
        return getString(R.string.special_lists_title);
    }


    @Override
    protected Class<SpecialList> getItemClass() {
        return SpecialList.class;
    }

    @Override
    protected Cursor getQuery() {
        return new MirakelQueryBuilder(this).and(DatabaseHelper.SYNC_STATE_FIELD,
                MirakelQueryBuilder.Operation.NOT_EQ,
                DefinitionsHelper.SYNC_STATE.DELETE.toInt())
               .and(ListMirakel.IS_SPECIAL, MirakelQueryBuilder.Operation.EQ, true).sort(ListMirakel.LFT,
                       MirakelQueryBuilder.Sorting.ASC).query(ListMirakel.URI).getRawCursor();
    }

}
