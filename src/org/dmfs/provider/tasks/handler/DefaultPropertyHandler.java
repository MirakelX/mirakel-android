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

package org.dmfs.provider.tasks.handler;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;


/**
 * This class is used to handle properties with unknown / unsupported mime-types.
 *
 * @author Tobias Reinsch <tobias@dmfs.org>
 *
 */
public class DefaultPropertyHandler extends PropertyHandler {

    /**
     * Validates the content of the alarm prior to insert and update transactions.
     *
     * @param db
     *            The {@link SQLiteDatabase}.
     * @param isNew
     *            Indicates that the content is new and not an update.
     * @param values
     *            The {@link ContentValues} to validate.
     * @param isSyncAdapter
     *            Indicates that the transaction was triggered from a SyncAdapter.
     * @param ctx
     *            A generic context
     *
     * @return The valid {@link ContentValues}.
     *
     * @throws IllegalArgumentException
     *             if the {@link ContentValues} are invalid.
     */
    @Override
    public ContentValues validateValues(ContentResolver db, boolean isNew,
                                        ContentValues values,
                                        boolean isSyncAdapter, final Context ctx) {
        return values;
    }

}
