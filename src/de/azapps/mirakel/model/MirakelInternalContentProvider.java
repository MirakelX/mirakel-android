package de.azapps.mirakel.model;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class MirakelInternalContentProvider extends ContentProvider {

    private static final String TAG = "MirakelInternalContentProvider";

    private static final UriMatcher uriMatcher;
    private static final int TASKS = 0;
    private static final int LISTS = 1;
    private static final int TAGS = 2;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(DefinitionsHelper.AUTHORITY_INTERNAL, "tasks", TASKS);
        uriMatcher.addURI(DefinitionsHelper.AUTHORITY_INTERNAL, "lists", LISTS);
        uriMatcher.addURI(DefinitionsHelper.AUTHORITY_INTERNAL, "tags", TAGS);
    }

    @Override
    public int delete(final Uri uri, final String selection,
                      final String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(final Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection,
                        final String selection, final String[] selectionArgs,
                        final String sortOrder) {
        String table;
        switch (uriMatcher.match(uri)) {
        case TASKS:
            table = Task.TABLE;
            break;
        case LISTS:
            table = ListMirakel.TABLE;
            break;
        case TAGS:
            table = Tag.TABLE;
            Log.w(TAG, selectionArgs[0]);
            break;
        default:
            Log.wtf(TAG, "where are the dragons");
            return null;
        }
        return MirakelContentProvider.getReadableDatabase().query(table,
                projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public int update(final Uri uri, final ContentValues values,
                      final String selection, final String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

}
