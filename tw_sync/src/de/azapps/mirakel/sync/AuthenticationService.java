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
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Modified by weiznich 2013
 */

package de.azapps.mirakel.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder.
 */
public class AuthenticationService extends Service {

    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        this.mAuthenticator = new Authenticator(this);
    }

    @Override
    public void onDestroy() {
        // Do nothing
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return this.mAuthenticator.getIBinder();
    }
}
