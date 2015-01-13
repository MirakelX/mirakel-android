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

import de.azapps.tools.Log;

public enum TW_ERRORS {
    ACCESS_DENIED, ACCOUNT_SUSPENDED, CANNOT_CREATE_SOCKET, CANNOT_PARSE_MESSAGE,
    CONFIG_PARSE_ERROR, MESSAGE_ERRORS, NO_ERROR, NOT_ENABLED, TRY_LATER, NO_SUCH_CERT,
    COULD_NOT_FIND_COMMON_ANCESTOR, CLIENT_SYNC_KEY_NOT_FOUND, ACCOUNT_VANISHED, NOT_SUPPORTED_SECONED_RECURRING;

    private static final String TAG = "TW_ERRORS";

    public static TW_ERRORS getError(final int code) {
        switch (code) {
        case 200:
            Log.d(TAG, "Success");
            break;
        case 201:
            Log.d(TAG, "No change");
            break;
        case 300:
            Log.d(TAG,
            "Deprecated message type\n"
            + "This message will not be supported in future task server releases.");
            break;
        case 301:
            Log.d(TAG,
            "Redirect\n"
            + "Further requests should be made to the specified server/port.");
            // TODO
            break;
        case 302:
            Log.d(TAG,
            "Retry\n"
            + "The client is requested to wait and retry the same request.  The wait\n"
            + "time is not specified, and further retry responses are possible.");
            return TW_ERRORS.TRY_LATER;
        case 400:
            Log.e(TAG, "Malformed data");
            return TW_ERRORS.MESSAGE_ERRORS;
        case 401:
            Log.e(TAG, "Unsupported encoding");
            return TW_ERRORS.MESSAGE_ERRORS;
        case 420:
            Log.e(TAG, "Server temporarily unavailable");
            return TW_ERRORS.TRY_LATER;
        case 421:
            Log.e(TAG, "Server shutting down at operator request");
            return TW_ERRORS.TRY_LATER;
        case 430:
            Log.e(TAG, "Access denied");
            return TW_ERRORS.ACCESS_DENIED;
        case 431:
            Log.e(TAG, "Account suspended");
            return TW_ERRORS.ACCOUNT_SUSPENDED;
        case 432:
            Log.e(TAG, "Account terminated");
            return TW_ERRORS.ACCOUNT_SUSPENDED;
        case 500:
            Log.e(TAG, "Syntax error in request");
            return TW_ERRORS.MESSAGE_ERRORS;
        case 501:
            Log.e(TAG, "Syntax error, illegal parameters");
            return TW_ERRORS.MESSAGE_ERRORS;
        case 502:
            Log.e(TAG, "Not implemented");
            return TW_ERRORS.MESSAGE_ERRORS;
        case 503:
            Log.e(TAG, "Command parameter not implemented");
            return TW_ERRORS.MESSAGE_ERRORS;
        case 504:
            Log.e(TAG, "Request too big");
            return TW_ERRORS.MESSAGE_ERRORS;
        default:
            Log.d(TAG, "Unknown code: " + code);
            break;
        }
        return NO_ERROR;
    }
}
