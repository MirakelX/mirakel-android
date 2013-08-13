package de.azapps.mirakel.sync.taskwarrior;

import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.List;

import android.util.Pair;
import de.azapps.mirakel.Mirakel;

public class Msg {
	private List<Pair<String, String>> _header = new ArrayList<Pair<String,String>>();
	private String _payload;

	public Msg() {
		_payload = "";
		// All messages are marked with the version number, so that the messages
		// may
		// be properly evaluated in context.
		_header.add(new Pair<String, String>("client", "Mirakel "
				+ Mirakel.VERSIONS_NAME));
	}

	public Msg(Msg m) {
		_payload = m.getPayload();
		_header = m._header;
	}

	// public Msg& operator= (const Msg&);
	// public bool operator== (const Msg&) const;

	public void clear() {
		_header.clear();
		_payload = "";
	}

	public void set(String key, int value) {
		_header.add(new Pair<String, String>(key, value + ""));
	}

	public void set(String key, String value) {
		_header.add(new Pair<String, String>(key, value));
	}

	public void set(String key, double value) {
		_header.add(new Pair<String, String>(key, value + ""));

	}

	public void setPayload(String payload) {
		_payload = payload;
	}

	public String get(String name) {
		for (Pair<String, String> p : _header) {
			if (p.first.equals(name))
				return p.second;
		}
		return "";

	}

	public String getPayload() {
		return _payload;
	}

	public List<String> all() {
		List<String> names = new ArrayList<String>();
		for (Pair<String, String> p : _header)
			names.add(p.first);
		return names;
	}

	public String serialize() {
		String output = "";
		for (Pair<String, String> i : _header)
			output += i.first + ": " + i.second + "\n";
		output += "\n" + _payload + "\n";
		return output;
	}

	public boolean parse(String input) throws MalformedInputException {
		_header.clear();
		_payload = "";

		int separator = input.indexOf("\n\n");
		if (separator == -1)
			throw new MalformedInputException(input.length());

		// Parse header.
		String[] a = input.substring(0, separator).split("\n");
		for (String s : a) {
			int delimiter = s.indexOf(':');
			if (delimiter == -1)
				throw new MalformedInputException(s.length());
			_header.add(new Pair<String, String>(s.substring(0, delimiter)
					.trim(), s.substring(delimiter + 1).trim()));
		}

		_payload = input.substring(separator + 2);

		return true;
	}

}
