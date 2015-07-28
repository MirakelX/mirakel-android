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

package de.azapps.tools;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;


public final class OptionalUtils {

    public static interface Procedure<F> {
        public void apply(F input);
    }

    public static <F> void withOptional(@NonNull final Optional<F> optional,
                                        @NonNull final Procedure<F> procedure) {
        if (optional.isPresent()) {
            procedure.apply(optional.get());
        }
    }

    @Nullable
    public static <F, V> V withOptional(@NonNull final Optional<F> optional,
                                        @NonNull final Function<F, V> function,
                                        final V alternative) {
        if (optional.isPresent()) {
            return function.apply(optional.get());
        } else {
            return alternative;
        }
    }

    @Nullable
    public static <F, V> V transformOrNull(@NonNull final Optional<F> optional,
                                           @NonNull final Function<F, V> transformation) {
        if (optional.isPresent()) {
            return transformation.apply(optional.get());
        } else {
            return null;
        }
    }

    public static <T> void writeToParcel(final @NonNull Parcel dest, final @NonNull Optional<T> value) {
        dest.writeValue(value.isPresent());
        if (value.isPresent()) {
            dest.writeValue(value.get());
        }
    }

    @NonNull
    public static <T> Optional<T> readFromParcel(final @NonNull Parcel in,
            final @NonNull Class<T> clazz) {
        if ((Boolean) in.readValue(Boolean.class.getClassLoader())) {
            return of((T) in.readValue(clazz.getClassLoader()));
        } else {
            return absent();
        }
    }
}
