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

package de.azapps.mirakel.model.account;

import de.azapps.mirakel.model.ModelVanishedException;

public class AccountVanishedException extends ModelVanishedException {
    private long accountId;
    private long listId;

    public AccountVanishedException() {
        super();
    }

    public AccountVanishedException(String message) {
        super(message);
    }

    public AccountVanishedException(long accountId) {
        super(accountId);
    }
    public AccountVanishedException(long accountId, long listId) {
        super("Account: " + accountId + " List: " + listId);
        this.accountId = accountId;
        this.listId = listId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getListId() {
        return listId;
    }
}
