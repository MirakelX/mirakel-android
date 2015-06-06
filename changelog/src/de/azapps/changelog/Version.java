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

package de.azapps.changelog;

import java.util.ArrayList;
import java.util.List;

class Version {

    private final int code;
    private final String name;
    private final String date;
    private final List<String> features, text;

    public Version(final int code, final String name, final String date) {
        this.code = code;
        this.name = name;
        this.date = date;
        this.features = new ArrayList<String>();
        this.text = new ArrayList<String>();
    }

    public int getCode() {
        return this.code;
    }

    public String getName() {
        return this.name;
    }

    public String getDate() {
        return this.date;
    }

    public List<String> getFeatures() {
        return this.features;
    }

    public List<String> getText() {
        return this.text;
    }

    public void addFeature(final String feature) {
        this.features.add(feature);
    }

    public void addText(final String text) {
        this.text.add(text);
    }
}
