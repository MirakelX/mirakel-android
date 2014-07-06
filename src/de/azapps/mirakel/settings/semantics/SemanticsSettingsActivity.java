/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.settings.semantics;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.R;

public class SemanticsSettingsActivity extends ListSettings {
    private Semantic semantic;

    private Semantic newSemantic() {
        return Semantic.newSemantic(getString(R.string.semantic_new), null,
                                    null, null, null);
    }

    @SuppressLint("NewApi")
    @Override
    protected OnClickListener getAddOnClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(final View v) {
                newSemantic();
                clickOnLast();
                invalidateHeaders();
            }
        };
    }

    @Override
    protected int getSettingsRessource() {
        return R.xml.settings_semantics;
    }

    @Override
    protected void setupSettings() {
        this.semantic = Semantic.get(getIntent().getLongExtra("id", 0));
        new SemanticsSettings(this, this.semantic).setup();
    }

    @Override
    protected List<Pair<Long, String>> getItems() {
        final List<Semantic> semantics = Semantic.all();
        final List<Pair<Long, String>> items = new ArrayList<>();
        for (final Semantic s : semantics) {
            items.add(new Pair<>(s.getId(), s.getCondition()));
        }
        return items;
    }

    @Override
    protected Class<?> getDestClass() {
        return SemanticsSettingsActivity.class;
    }

    @Override
    protected Class<?> getDestFragmentClass() {
        return SemanticsSettingsFragment.class;
    }

    @Override
    protected int getTitleRessource() {
        return R.string.settings_semantics_title;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (Build.VERSION_CODES.ICE_CREAM_SANDWICH > Build.VERSION.SDK_INT) {
            if (getIntent().hasExtra("id")) {
                menu.add(R.string.delete);
            } else {
                menu.add(R.string.add);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            if (item.getTitle().equals(getString(R.string.delete))) {
                if (this.semantic != null) {
                    this.semantic.destroy();
                }
                finish();
                return true;
            } else if (item.getTitle().equals(getString(R.string.add))) {
                final Semantic s = newSemantic();
                final Intent intent = new Intent(this,
                                                 SemanticsSettingsActivity.class);
                intent.putExtra("id", s.getId());
                startActivity(intent);
                return true;
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected OnClickListener getHelpOnClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(final View v) {
                Helpers.openHelp(getApplicationContext(), "semantic-new-tasks");
            }
        };
    }

    @Override
    public OnClickListener getDelOnClickListener() {
        // TODO Auto-generated method stub
        return new OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(final View v) {
                SemanticsSettingsActivity.this.semantic.destroy();
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

    public void setSemantic(final Semantic s) {
        this.semantic = s;
    }
}