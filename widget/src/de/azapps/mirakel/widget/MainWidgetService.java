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
package de.azapps.mirakel.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.List;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.WidgetHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MainWidgetService extends RemoteViewsService {

    private static final String TAG = "MainWidgetService";

    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        Log.wtf(TAG, "create");
        return new MainWidgetViewsFactory(getApplicationContext(), intent);
    }

}

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class MainWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "MainWidgetViewsFactory";
    private final Context mContext;
    private List<Task> tasks;
    private final int widgetId;
    private ListMirakel list;

    public MainWidgetViewsFactory(final Context context, final Intent intent) {
        if (intent.getIntExtra(MainWidgetProvider.EXTRA_WIDGET_LAYOUT,
                               MainWidgetProvider.NORMAL_WIDGET) != MainWidgetProvider.NORMAL_WIDGET) {
            Log.wtf(TAG, "wrong provider");
        }
        this.mContext = context;
        this.widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                           0);
    }

    /**
     * Define and open the DataSources
     */
    @Override
    public void onCreate() {
        updateList();
    }

    private void updateList() {
        final long identityToken = Binder.clearCallingIdentity();
        this.list = WidgetHelper.getList(this.mContext, this.widgetId);
        Binder.restoreCallingIdentity(identityToken);
        getCount();
    }

    @Override
    public void onDestroy() {
        // nothing
    }

    @Override
    public int getCount() {
        final long identityToken = Binder.clearCallingIdentity();
        // get the tasks here because sometimes this returns a wrong value, if
        // the count is not refreshed
        this.tasks = Task.getTasks(this.list, this.list.getSortBy(),
                                   WidgetHelper.showDone(this.mContext, this.widgetId));
        Binder.restoreCallingIdentity(identityToken);
        return this.tasks.size();
    }

    @Override
    public RemoteViews getViewAt(final int position) {
        if (position >= this.tasks.size()) {
            Log.w(TAG, "wrong position");
            return null;
        }
        final Task task = this.tasks.get(position);
        // Get The Task
        RemoteViews rv = new RemoteViews(this.mContext.getPackageName(),
                                         R.layout.widget_row_minimal);
        // Set the Contents of the Row
        rv = WidgetHelper.configureItem(rv, task, this.mContext,
                this.widgetId);
        // Set the Click–Intent
        // We need to do so, because we can not start the Activity directly from
        // the Service
        final Intent fillInIntent = new Intent(MainWidgetProvider.CLICK_TASK);
        final Bundle b = new Bundle();
        b.putParcelable(MainWidgetProvider.EXTRA_TASK, task);
        // dirty workaround to pass parcelables in pending intents
        fillInIntent.putExtra(DefinitionsHelper.BUNDLE_WRAPPER, b);
        rv.setOnClickFillInIntent(R.id.tasks_row, fillInIntent);
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        // We aren't going to return a default loading view in this sample
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public void onDataSetChanged() {
        updateList();
    }

}
