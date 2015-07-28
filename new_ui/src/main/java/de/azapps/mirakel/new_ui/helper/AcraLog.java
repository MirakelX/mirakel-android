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

package de.azapps.mirakel.new_ui.helper;

import org.acra.log.ACRALog;

import de.azapps.tools.Log;

public class AcraLog implements ACRALog {
    @Override
    public int v(final String tag, final String msg) {
        return Log.v(tag, msg);
    }

    @Override
    public int v(final String tag, final String msg, final Throwable tr) {
        return Log.v(tag, msg, tr);
    }

    @Override
    public int d(final String tag, final String msg) {
        return Log.d(tag, msg);
    }

    @Override
    public int d(final String tag, final String msg, final Throwable tr) {
        return Log.d(tag, msg, tr);
    }

    @Override
    public int i(final String tag, final String msg) {
        return Log.i(tag, msg);
    }

    @Override
    public int i(final String tag, final String msg, final Throwable tr) {
        return Log.i(tag, msg, tr);
    }

    @Override
    public int w(final String tag, final String msg) {
        return Log.w(tag, msg);
    }

    @Override
    public int w(final String tag, final String msg, final Throwable tr) {
        return Log.w(tag, msg, tr);
    }

    @Override
    public int w(final String tag, final Throwable tr) {
        return Log.w(tag, "", tr);
    }

    @Override
    public int e(final String tag, final String msg) {
        return Log.e(tag, msg);
    }

    @Override
    public int e(final String tag, final String msg, final Throwable tr) {
        return Log.e(tag, msg, tr);
    }

    @Override
    public String getStackTraceString(final Throwable tr) {
        return android.util.Log.getStackTraceString(tr);
    }
}
