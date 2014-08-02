package org.dmfs.provider.tasks.handler;

import org.dmfs.provider.tasks.TaskContract.Property;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;


/**
 * This class is used to handle alarm property values during database transactions.
 *
 * @author Tobias Reinsch <tobias@dmfs.org>
 *
 */
public class AlarmHandler extends PropertyHandler {

    // private static final String[] ALARM_ID_PROJECTION = { Alarms.ALARM_ID };
    // private static final String ALARM_SELECTION = Alarms.ALARM_ID + " =?";

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
                                        ContentValues values, boolean isSyncAdapter) {
        // row id can not be changed or set manually
        if (values.containsKey(Property.Alarm.PROPERTY_ID)) {
            throw new IllegalArgumentException("_ID can not be set manually");
        }
        if (!values.containsKey(Property.Alarm.MINUTES_BEFORE)) {
            throw new IllegalArgumentException("alarm property requires a time offset");
        }
        if (!values.containsKey(Property.Alarm.REFERENCE) ||
            values.getAsInteger(Property.Alarm.REFERENCE) < 0) {
            throw new IllegalArgumentException("alarm property requires a valid reference date ");
        }
        if (!values.containsKey(Property.Alarm.ALARM_TYPE)) {
            throw new IllegalArgumentException("alarm property requires an alarm type");
        }
        return values;
    }


    /**
     * Inserts the alarm into the database.
     *
     * @param db
     *            The {@link SQLiteDatabase}.
     * @param values
     *            The {@link ContentValues} to insert.
     * @param isSyncAdapter
     *            Indicates that the transaction was triggered from a SyncAdapter.
     *
     * @return The row id of the new alarm as <code>long</code>
     */
    @Override
    public Uri insert(ContentResolver db, ContentValues values, boolean isSyncAdapter) {
        values = validateValues(db, true, values, isSyncAdapter);
        return super.insert(db, values, isSyncAdapter);
    }


    /**
     * Updates the alarm in the database.
     *
     * @param db
     *            The {@link SQLiteDatabase}.
     * @param values
     *            The {@link ContentValues} to update.
     * @param selection
     *            The selection <code>String</code> to update the right row.
     * @param selectionArgs
     *            The arguments for the selection <code>String</code>.
     * @param isSyncAdapter
     *            Indicates that the transaction was triggered from a SyncAdapter.
     *
     * @return The number of rows affected.
     */
    @Override
    public int update(ContentResolver db, ContentValues values, String selection,
                      String[] selectionArgs, boolean isSyncAdapter) {
        values = validateValues(db, false, values, isSyncAdapter);
        return super.update(db, values, selection, selectionArgs, isSyncAdapter);
    }
}
