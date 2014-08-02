package org.dmfs.provider.tasks.handler;

import android.content.ContentResolver;
import android.content.ContentValues;
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
     *
     * @return The valid {@link ContentValues}.
     *
     * @throws IllegalArgumentException
     *             if the {@link ContentValues} are invalid.
     */
    @Override
    public ContentValues validateValues(ContentResolver db, boolean isNew,
                                        ContentValues values,
                                        boolean isSyncAdapter) {
        return values;
    }

}
