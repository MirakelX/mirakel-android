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

package de.azapps.mirakel.sync.taskwarrior.network_helper;

import com.google.common.base.Optional;

import java.nio.charset.MalformedInputException;
import java.util.HashMap;
import java.util.Map;

import de.azapps.mirakel.DefinitionsHelper;

public class Msg {
    private final Map<String, String> _header = new HashMap<>(5);
    private String _payload;

    public Msg() {
        this._payload = "";
        // All messages are marked with the version number, so that the messages
        // may be properly evaluated in context.
        this._header.put("client", "Mirakel " + DefinitionsHelper.VERSIONS_NAME);
    }

    public void clear() {
        this._header.clear();
        this._payload = "";
    }

    public void set(final String key, final int value) {
        this._header.put(key, String.valueOf(value));
    }

    public void set(final String key, final String value) {
        this._header.put(key, value);
    }

    public void set(final String key, final double value) {
        this._header.put(key, String.valueOf(value));
    }

    public void setPayload(final String payload) {
        this._payload = payload;
    }


    public Optional<String> getHeader(final String name) {
        if (_header.containsKey(name)) {
            return Optional.of(_header.get(name));
        } else {
            return Optional.absent();
        }
    }

    public String getPayload() {
        return this._payload;
    }


    public String serialize() {
        final StringBuilder output = new StringBuilder();
        for (final Map.Entry<String, String> entry : this._header.entrySet()) {
            output.append(entry.getKey() + ": " + entry.getValue() + '\n');
        }
        output.append('\n' + this._payload + '\n');
        return output.toString();
    }

    public void parse(final String input) throws MalformedInputException {
        this._header.clear();
        this._payload = "";
        final int separator = input.indexOf("\n\n");
        if (separator == -1) {
            throw new MalformedInputException(input.length());
        }
        // Parse header.
        final String[] a = input.substring(0, separator).split("\n");
        for (final String s : a) {
            final int delimiter = s.indexOf(':');
            if (delimiter == -1) {
                throw new MalformedInputException(s.length());
            }
            this._header.put(s.substring(0, delimiter)
                             .trim(), s.substring(delimiter + 1).trim());
        }
        this._payload = input.substring(separator + 2).trim();
    }

}
