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

package de.azapps.mirakelandroid.test;

import android.content.Context;

import java.util.List;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.model.semantic.Semantic;

public class TestHelper {
    public static void init(final Context ctx) {
        Mirakel.init(ctx);
        Semantic.init(ctx);
    }

    public static void terminate() {
    }

    /**
     * 'cause Java is to dumb to do the simplest things…
     * @param a
     * @param b
     * @param <T>
     * @return
     */
    public static <T> boolean listEquals(List<T> a, List<T> b) {
        if (a == null || b == null) {
            if (a != b) {
                return false;
            }
        } else if (a.size() != b.size()) {
            return false;
        } else {
            for (int i = 0; i < a.size(); i++) {
                T ia = a.get(i); // a donkey!
                T ib = b.get(i);
                if (ia == null) {
                    if (ib != null) {
                        return false;
                    }
                } else if (!ia.equals(ib)) {
                    return false;
                }
            }
        }
        return true;
    }
}
