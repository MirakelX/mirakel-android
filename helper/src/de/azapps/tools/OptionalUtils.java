/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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

package de.azapps.tools;

import android.support.annotation.NonNull;

import com.google.common.base.Function;
import com.google.common.base.Optional;


public class OptionalUtils {
    public static interface Procedure<F> {
        public void apply(F input);
    }
    public static <F> void withOptional(@NonNull Optional<F> optional,
                                        @NonNull Procedure<F> procedure) {
        if (optional.isPresent()) {
            procedure.apply(optional.get());
        }
    }
    public static <F, V> V withOptional(@NonNull Optional<F> optional, @NonNull Function<F, V> function,
                                        V alternative) {
        if (optional.isPresent()) {
            return function.apply(optional.get());
        } else {
            return alternative;
        }
    }
    public static <F, V> V transformOrNull(@NonNull Optional<F> optional,
                                           @NonNull Function<F, V> transformation) {
        if (optional.isPresent()) {
            return transformation.apply(optional.get());
        } else {
            return null;
        }
    }
}
