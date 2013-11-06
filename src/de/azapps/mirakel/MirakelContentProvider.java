/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel;

import java.util.ArrayList;
import java.util.List;

import org.dmfs.provider.tasks.TaskContract;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;

public class MirakelContentProvider extends ContentProvider {
	// public static final String PROVIDER_NAME = Mirakel.AUTHORITY_TYP;
	// public static final Uri CONTENT_URI = Uri.parse("content://" +
	// PROVIDER_NAME);
	private SQLiteDatabase database;
	private static final UriMatcher uriMatcher;
	private static final int LISTS = 0;
	private static final int LIST_ID = 1;
	private static final int TASKS = 2;
	private static final int TASK_ID = 3;
	
	private static final String TAG = "MirakelContentProvider";
	//TODO for what we will need this?
	private static final int INSTANCE_ID = 4;
	private static final int INSTANCES = 5;
	private static final int CATEGORIES = 6;
	private static final int CATEGORY_ID = 7;
	
	static
    {
            uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
            uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.TaskLists.CONTENT_URI_PATH, LISTS);

            uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.TaskLists.CONTENT_URI_PATH + "/#", LIST_ID);

            uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Tasks.CONTENT_URI_PATH, TASKS);
            uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Tasks.CONTENT_URI_PATH + "/#", TASK_ID);

            uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Instances.CONTENT_URI_PATH, INSTANCES);
            uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Instances.CONTENT_URI_PATH + "/#", INSTANCE_ID);

            uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Categories.CONTENT_URI_PATH, CATEGORIES);
            uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Categories.CONTENT_URI_PATH + "/#", CATEGORY_ID);
    }

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		//TODO implement
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri))
        {
                case LISTS:
                        return TaskContract.TaskLists.CONTENT_TYPE;
                case LIST_ID:
                        return TaskContract.TaskLists.CONTENT_ITEM_TYPE;
                case TASKS:
                        return TaskContract.Tasks.CONTENT_TYPE;
                case TASK_ID:
                        return TaskContract.Tasks.CONTENT_ITEM_TYPE;
                case INSTANCES:
                        return TaskContract.Instances.CONTENT_TYPE;
                default:
                        throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		//TODO implement
		return null;
	}

	@Override
	public boolean onCreate() {
		database=new DatabaseHelper(getContext()).getWritableDatabase();
		return database==null;
	}
	
	private String getId(Uri uri)
    {
            return uri.getPathSegments().get(1);
    }
	

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		String[]newProjection=transformProjection(projection,sqlBuilder);
		
		
		boolean andNeeded=false;
		switch (uriMatcher.match(uri)) {
		case LIST_ID:
			sqlBuilder.appendWhere(getAnd(andNeeded)+"_id="+getId(uri));
			andNeeded=true;
		case LISTS:
			sqlBuilder.setTables(ListMirakel.TABLE);
			break;
		case TASK_ID:
			sqlBuilder.appendWhere(getAnd(andNeeded)+"_id="+getId(uri));
			andNeeded=true;
		case TASKS:
			sqlBuilder.setTables(Task.TABLE);
			break;	
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		
		
		return null;
	}

	private String[] transformProjection(String[] projection,
			SQLiteQueryBuilder sqlBuilder) {
		Boolean isTaskVar=null;
		List<String> newProjection=new ArrayList<String>();
		for(String p:projection){
			if(isEqual(p,TaskContract.Tasks.PRIORITY,isTask(isTaskVar))){
				newProjection.add(Task.PRIORITY);
			}
		}
		return null;
	}
	
	private boolean isEqual(String p, String s, boolean task) {
		return p.equals(s)&&task;
	}

	private boolean isTask(Boolean b){
		return(b==null||b);
	}
	
	private boolean isList(Boolean b){
		return(b==null||!b);
	}

	private CharSequence getAnd(boolean andNeeded) {
		return andNeeded?" and ":"";
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		//TODO implement this
		return 0;
	}

}
