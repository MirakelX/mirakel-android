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
package de.azapps.mirakelandroid.test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.net.Uri;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.list.ListMirakel;

public class RandomHelper {

	// its ok to use this here, it's only for testing
	@SuppressLint("TrulyRandom")
	private static SecureRandom random = new SecureRandom();

	public static int getRandomint() {
		return random.nextInt();
	}

	public static boolean getRandomboolean() {
		return random.nextBoolean();
	}

	public static String getRandomString() {
		return new BigInteger(130, random).toString(32);
	}

	public static SYNC_STATE getRandomSYNC_STATE() {
		return SYNC_STATE.values()[random.nextInt(SYNC_STATE.values().length)];
	}

	public static AccountMirakel getRandomAccountMirakel() {
		return new AccountMirakel(
				random.nextInt(),
				getRandomString(),
				ACCOUNT_TYPES.parseInt(random.nextInt(ACCOUNT_TYPES.values().length - 1) + 1),
				random.nextBoolean(), getRandomString());
	}

	public static Integer getRandomInteger() {
		return getRandomint();
	}

	public static ListMirakel getRandomListMirakel() {
		return new ListMirakel(random.nextInt(), getRandomString(),
				(short) random.nextInt(), getRandomString(), getRandomString(),
				getRandomSYNC_STATE(), getRandomint(), getRandomint(),
				getRandomint(), getRandomAccountMirakel());
	}

	public static Calendar getRandomCalendar() {
		final Calendar c = new GregorianCalendar();
		c.setTimeInMillis(random.nextLong());
		return c;
	}

	public static long getRandomlong() {
		return random.nextLong();
	}

	public static Uri getRandomUri() {
		return Uri.parse("http://www." + getRandomString() + ".com");
	}
}
