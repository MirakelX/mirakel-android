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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Optional;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class Either<L, R> {

    enum State {
        LEFT,
        RIGHT
    }

    @NonNull
    private final Optional<L> left;
    @NonNull
    private final Optional<R> right;

    public static <L, R> Either<L, R> Left(final @NonNull L left) {
        return new Either<>(of(left), Optional.<R>absent());
    }

    public static <L, R> Either<L, R> Right(final @NonNull R right) {
        return new Either<>(Optional.<L>absent(), of(right));
    }

    private Either(final @NonNull Optional<L> left, final @NonNull Optional<R> right) {
        this.left = left;
        this.right = right;
    }

    @SuppressWarnings("unchecked")
    public <T>Either(final @NonNull T value, final @NonNull State p) {
        switch (p) {
        case LEFT:
            left = of((L)value);
            right = absent();
            break;
        case RIGHT:
            left = absent();
            right = of((R)value);
            break;
        }
        throw new  IllegalArgumentException("Unknown position");
    }

    public State getPosition() {
        if (left.isPresent() && !right.isPresent()) {
            return State.LEFT;
        } else if (right.isPresent() && !left.isPresent()) {
            return State.RIGHT;
        }
        throw new IllegalStateException("Unknown state of Either");
    }

    public boolean isLeft() {
        return left.isPresent();
    }

    public boolean isRight() {
        return right.isPresent();
    }

    @NonNull
    public Optional<L> getLeft() {
        return left;
    }

    @Nullable
    public L getLeftOrNull() {
        return left.orNull();
    }

    @NonNull
    public L getLeftOr(final @Nullable L or ) {
        return left.or( or );
    }

    @NonNull
    public L getLeftOrThrow() throws IllegalStateException {
        if (left.isPresent()) {
            return left.get();
        }
        throw new IllegalStateException("Left is empty");
    }

    @NonNull
    public Optional<R> getRight() {
        return right;
    }

    @Nullable
    public R getRightOrNull() {
        return right.orNull();
    }

    @NonNull
    public R getRightOr(final @Nullable R or ) {
        return right.or( or );
    }

    @NonNull
    public R getRightOrThrow() throws IllegalStateException {
        if (right.isPresent()) {
            return right.get();
        }
        throw new IllegalStateException("Right is empty");
    }
}
