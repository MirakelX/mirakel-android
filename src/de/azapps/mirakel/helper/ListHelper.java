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

package de.azapps.mirakel.helper;

import android.content.Intent;

import com.google.common.base.Optional;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.list.ListMirakel;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public final class ListHelper {
    public static Optional<ListMirakel> getListMirakelFromIntent(final Intent intent) {
        if (intent.hasExtra(DefinitionsHelper.EXTRA_LIST)) {
            return of((ListMirakel) intent.getParcelableExtra(DefinitionsHelper.EXTRA_LIST));
        } else if (intent.hasExtra(DefinitionsHelper.EXTRA_LIST_ID)) {
            return ListMirakel.get(intent.getLongExtra(DefinitionsHelper.EXTRA_LIST_ID, 0));
        } else {
            return absent();
        }
    }
}
