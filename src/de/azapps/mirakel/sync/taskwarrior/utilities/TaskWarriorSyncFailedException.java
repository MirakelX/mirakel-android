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

package de.azapps.mirakel.sync.taskwarrior.utilities;

public class TaskWarriorSyncFailedException extends Exception {
    private static final long serialVersionUID = 3349776187699690118L;
    private final TW_ERRORS error;
    private final String message;

    public TaskWarriorSyncFailedException(final TW_ERRORS type) {
        super();
        this.error = type;
        message = "";
    }

    public TaskWarriorSyncFailedException(final TW_ERRORS type, final String message) {
        super();
        this.error = type;
        this.message = message;
    }

    public TaskWarriorSyncFailedException(final TW_ERRORS type, final String message,
                                          final Throwable cause) {
        super(cause);
        this.error = type;
        this.message = message;
    }

    public TaskWarriorSyncFailedException(final TW_ERRORS type,
                                          final Throwable cause) {
        super(cause);
        this.error = type;
        this.message = cause.getMessage();
    }

    public TW_ERRORS getError() {
        return this.error;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
