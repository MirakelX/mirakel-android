package de.azapps.mirakel.sync.taskwarrior;

import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.List;

import android.util.Pair;
import de.azapps.mirakel.DefinitionsHelper;

public class Msg {
	private List<Pair<String, String>> _header = new ArrayList<Pair<String, String>>();
	private String _payload;

	public Msg() {
		this._payload = "";
		// All messages are marked with the version number, so that the messages
		// may
		// be properly evaluated in context.
		this._header.add(new Pair<String, String>("client", "Mirakel "
				+ DefinitionsHelper.VERSIONS_NAME));
	}

	public Msg(final Msg m) {
		this._payload = m.getPayload();
		this._header = m._header;
	}

	// public Msg& operator= (const Msg&);
	// public bool operator== (const Msg&) const;

	public void clear() {
		this._header.clear();
		this._payload = "";
	}

	public void set(final String key, final int value) {
		this._header.add(new Pair<String, String>(key, value + ""));
	}

	public void set(final String key, final String value) {
		this._header.add(new Pair<String, String>(key, value));
	}

	public void set(final String key, final double value) {
		this._header.add(new Pair<String, String>(key, value + ""));

	}

	public void setPayload(final String payload) {
		this._payload = payload;
	}

	public String get(final String name) {
		for (final Pair<String, String> p : this._header) {
			if (p.first.equals(name)) {
				return p.second.trim();
			}
		}
		return "";

	}

	public String getPayload() {
		return this._payload;
	}

	public List<String> all() {
		final List<String> names = new ArrayList<String>();
		for (final Pair<String, String> p : this._header) {
			names.add(p.first);
		}
		return names;
	}

	public String serialize() {
		String output = "";
		for (final Pair<String, String> i : this._header) {
			output += i.first + ": " + i.second + "\n";
		}
		output += "\n" + this._payload + "\n";
		return output;
	}

	public boolean parse(final String input) throws MalformedInputException {
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
			this._header.add(new Pair<String, String>(s.substring(0, delimiter)
					.trim(), s.substring(delimiter + 1).trim()));
		}

		this._payload = input.substring(separator + 2).trim();

		return true;
	}

}
