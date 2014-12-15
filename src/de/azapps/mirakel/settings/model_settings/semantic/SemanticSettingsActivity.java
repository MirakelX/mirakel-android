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

package de.azapps.mirakel.settings.model_settings.semantic;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelListActivity;

import static com.google.common.base.Optional.of;

public class SemanticSettingsActivity extends GenericModelListActivity<Semantic> {

    @NonNull
    @Override
    protected boolean isSupport() {
        return false;
    }

    @NonNull
    @Override
    protected Optional<android.app.Fragment> getDetailFragment(final @NonNull Semantic item) {
        return of((android.app.Fragment) new SemanticDetailFragment());
    }

    @NonNull
    @Override
    protected Class<? extends GenericModelListActivity> getSelf() {
        return SemanticSettingsActivity.class;
    }



    @NonNull
    @Override
    protected Semantic getDefaultItem() {
        Optional<Semantic> semanticOptional = Semantic.first();
        if (!semanticOptional.isPresent()) {
            return Semantic.newSemantic("", null, null, Optional.<ListMirakel>absent(), null);
        } else {
            return semanticOptional.get();
        }
    }

    @NonNull
    @Override
    protected void createItem(@NonNull Context ctx) {
        onItemSelected(Semantic.newSemantic(getString(R.string.semantic_new), null,
                                            null, Optional.<ListMirakel>absent(), null));
    }

    @Override
    protected String getTextTitle() {
        return getString(R.string.settings_semantics_title);
    }

    @Override
    protected Class<Semantic> getItemClass() {
        return Semantic.class;
    }

    @Override
    protected Cursor getQuery() {
        return new MirakelQueryBuilder(this).query(Semantic.URI);
    }


}
