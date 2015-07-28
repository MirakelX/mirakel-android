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

package de.azapps.mirakel.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.Map;

import de.azapps.tools.Log;


public class MirakelContentObserver extends ContentObserver {

    //if we use java 8 someday, redirect handleChange(long id) to call the normalfunction
    public interface ObserverCallBack {
        abstract void handleChange();
        //if we use java 8 someday, redirect handleChange(long id) to call the handleChange()
        abstract void handleChange(long id);
    }

    @NonNull
    private static final String TAG = "MirakelContentObserver";
    @NonNull
    private Map<Uri, ObserverCallBack> doWhat;

    public MirakelContentObserver(final @NonNull  Handler h, final @NonNull Context ctx,
                                  final @NonNull Map<Uri, ObserverCallBack> doOnChange) {
        super(h);
        final ContentResolver resolver = ctx.getContentResolver();
        for (final Uri u : doOnChange.keySet()) {
            resolver.registerContentObserver(u, true, this);
        }
        doWhat = doOnChange;
    }

    public MirakelContentObserver(final @NonNull Handler h, final @NonNull Context ctx,
                                  final @NonNull Uri uri, final @NonNull ObserverCallBack doOnChange) {
        super(h);
        final ContentResolver resolver = ctx.getContentResolver();
        resolver.registerContentObserver(uri, true, this);
        doWhat = new HashMap<>(1);
        doWhat.put(uri, doOnChange);
    }

    public void unregister(final @NonNull Context ctx) {
        ctx.getContentResolver().unregisterContentObserver(this);
    }


    // Implement the onChange(boolean) method to delegate the change notification to
    // the onChange(boolean, Uri) method to ensure correct operation on older versions
    // of the framework that did not have the onChange(boolean, Uri) method.
    @Override
    public void onChange(final boolean selfChange) {
        for (final ObserverCallBack f : doWhat.values()) {
            if (f != null) {
                f.handleChange();
            }
        }
    }

    // Implement the onChange(boolean, Uri) method to take advantage of the new Uri argument.
    @Override
    public void onChange(final boolean selfChange, final Uri uri) {
        Optional<Long> id = Optional.absent();
        if (uri.isHierarchical()) {
            try {
                final long t = ContentUris.parseId(uri);
                if (t != -1L) {
                    id = Optional.of(t);
                }
            } catch (final NumberFormatException e) {
                Log.d(TAG, "uri claims to be hierarchical but is not, no problem, eat it", e);
            }
        }
        if (doWhat.containsKey(uri)) {
            final ObserverCallBack doSomething = doWhat.get(uri);
            if (id.isPresent() && (doSomething != null)) {
                doSomething.handleChange(id.get());
            } else {
                doWhat.get(uri).handleChange();
            }
        } else {
            Log.wtf(TAG, "no callback found for this uri: " + uri.toString());
        }
    }

}
