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
