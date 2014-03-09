package de.azapps.mirakel.model;

import org.dmfs.provider.tasks.TaskContract;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;


public class MirakelInternalContentProvider extends ContentProvider {

	private static final String TAG = "MirakelInternalContentProvider";
	
	private static final UriMatcher	uriMatcher;
	private static final int TASKS=0;
	private static final int LISTS=1;;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(DefinitionsHelper.AUTHORITY_INTERNAL,
				"tasks", TASKS);


		uriMatcher.addURI(DefinitionsHelper.AUTHORITY_INTERNAL,
				"lists", LISTS);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		String table;
		switch (uriMatcher.match(uri)) {
		case TASKS:
			table=Task.TABLE;
			break;
		case LISTS:
			table=ListMirakel.TABLE;
		default:
			Log.wtf(TAG,"where are the dragons");
			return null;
		}
		Log.w(TAG,"query:"+table+" "+selection);
		return MirakelContentProvider.getReadableDatabase().query(table, projection, selection,
				selectionArgs, null, null, sortOrder);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
