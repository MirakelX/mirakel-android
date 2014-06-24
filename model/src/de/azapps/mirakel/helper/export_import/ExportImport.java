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
package de.azapps.mirakel.helper.export_import;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class ExportImport {
	private static final File dbFile = new File(FileUtils.getMirakelDir()
			+ "databases/mirakel.db");
	private static final String TAG = "ExportImport";

	@SuppressLint("SimpleDateFormat")
	public static void exportDB(final Context ctx) {

		final Date today = new Date();
		final DateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");// SimpleDateFormat.getDateInstance();
		final String filename = "mirakel_" + sdf.format(today) + ".db";
		final File file = new File(FileUtils.getExportDir(), filename);

		try {
			file.createNewFile();
			FileUtils.copyFile(dbFile, file);
			Toast.makeText(
					ctx,
					ctx.getString(R.string.backup_export_ok,
							file.getAbsolutePath()), Toast.LENGTH_LONG).show();
		} catch (final IOException e) {
			Log.e(TAG, e.getMessage(), e);
			ErrorReporter.report(ErrorType.BACKUP_EXPORT_ERROR);
		}
	}

	public static void importDB(final Context ctx,
			final FileInputStream inputstream) {

		try {
			FileUtils.copyByStream(inputstream, new FileOutputStream(dbFile));
			Toast.makeText(ctx, ctx.getString(R.string.backup_import_ok),
					Toast.LENGTH_LONG).show();
			android.os.Process.killProcess(android.os.Process.myPid());
		} catch (final IOException e) {
			Log.e(TAG, e.getMessage(), e);
			ErrorReporter.report(ErrorType.BACKUP_IMPORT_ERROR);
		}
	}

	@SuppressLint("SimpleDateFormat")
	public static boolean importAstrid(final Context context,
			final FileInputStream stream, final String mimetype) {
		switch (mimetype) {
		case "application/zip":
			return importAstridZip(context, stream);
		case "text/xml":
			return importAstridXml(context, stream);
		default:
			Log.d(TAG, "unknown filetype");
		}
		return false;
	}

	private static boolean importAstridXml(final Context context,
			final FileInputStream stream) {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			Log.d(TAG, "cannot configure parser", e);
			return false;
		}
		Document doc;
		try {
			doc = dBuilder.parse(stream);
		} catch (final SAXException e) {
			Log.d(TAG, "cannot parse file", e);
			return false;
		} catch (final IOException e) {
			Log.d(TAG, "cannot read file", e);
			return false;
		}

		// optional, but recommended
		// read this -
		// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();

		final NodeList nList = doc.getDocumentElement().getChildNodes();
		for (int i = 0; i < nList.getLength(); i++) {
			final Node n = nList.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				final NamedNodeMap m = n.getAttributes();
				if (m != null) {
					// Name
					final String name = m.getNamedItem("title")
							.getTextContent();
					// List
					ListMirakel list;
					Node child = null;
					if (n.getChildNodes().getLength() > 1) {
						child = n.getChildNodes().item(1);
					}
					if (child != null && child.getAttributes() != null) {
						final String listname = child.getAttributes()
								.getNamedItem("value").getTextContent();
						list = ListMirakel.findByName(listname);
						if (list == null) {
							list = ListMirakel.newList(listname);
						}
					} else {
						list = MirakelModelPreferences
								.getImportDefaultList(true);
					}
					final Task t = Task.newTask(name, list);
					// Priority
					final int prio = Integer.parseInt(m.getNamedItem(
							"importance").getTextContent());
					switch (prio) {
					case 0:
						t.setPriority(2);
						break;
					case 1:
						t.setPriority(1);
						break;
					case 2:
						t.setPriority(0);
						break;
					case 3:
						t.setPriority(-2);
						break;
					default:
						t.setPriority(0);
					}
					// Due
					final long due = Long.parseLong(m.getNamedItem("dueDate")
							.getTextContent());
					final GregorianCalendar d = new GregorianCalendar();
					d.setTimeInMillis(due);
					t.setDue(d);
					// Created At
					final long created = Long.parseLong(m.getNamedItem(
							"created").getTextContent());
					final GregorianCalendar c = new GregorianCalendar();
					c.setTimeInMillis(created);
					t.setCreatedAt(c);
					// Update At
					final long update = Long.parseLong(m.getNamedItem(
							"modified").getTextContent());
					final GregorianCalendar u = new GregorianCalendar();
					u.setTimeInMillis(update);
					t.setDue(u);
					// Done
					final String done = m.getNamedItem("completed")
							.getTextContent();
					t.setDone(!done.equals("0"));
					final String content = m.getNamedItem("notes")
							.getTextContent();
					t.setContent(content.trim());
					// TODO Reminder
					t.save(false);
				} else {
					Log.w(TAG, "empty node");
				}
			}
		}

		return true;
	}

	@SuppressLint("SimpleDateFormat")
	private static boolean importAstridZip(final Context context,
			final FileInputStream stream) {
		final File outputDir = new File(context.getCacheDir(), "astrid");
		if (!outputDir.isDirectory()) {
			outputDir.mkdirs();
		} else {
			final String[] children = outputDir.list();
			for (final String element : children) {
				new File(outputDir, element).delete();
			}
		}
		try {
			FileUtils.unzip(stream, outputDir);
		} catch (final Exception e) {
			Log.e(TAG, "Could not unzip", e);
			return false;
		}
		FileReader tasks, lists;
		try {
			lists = new FileReader(new File(outputDir, "lists.csv"));
			final CSVReader listsReader = new CSVReader(lists, ',');
			lists.close();
			String[] row;
			listsReader.readNext(); // Skip first line

			final SimpleDateFormat astridFormat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			while ((row = listsReader.readNext()) != null) {
				final String name = row[0];
				if (ListMirakel.findByName(name) == null) {
					ListMirakel.newList(name);
					Log.v(TAG, "created list:" + name);
				}
			}
			tasks = new FileReader(new File(outputDir, "tasks.csv"));
			final CSVReader tasksReader = new CSVReader(tasks, ',');
			tasks.close();
			tasksReader.readNext(); // Skip first line
			while ((row = tasksReader.readNext()) != null) {
				final String name = row[0];
				final String content = row[8];
				final String listName = row[7];
				final int priority = Integer.valueOf(row[5]) - 1;

				// Due
				GregorianCalendar due = new GregorianCalendar();
				try {
					due.setTime(astridFormat.parse(row[4]));
				} catch (final ParseException e) {
					due = null;
				}
				// Done
				final boolean done = !row[9].equals("");

				ListMirakel list = ListMirakel.findByName(listName);
				if (list == null) {
					list = ListMirakel.first();
				}
				final Task t = Task.newTask(name, list);
				t.setContent(content);
				t.setPriority(priority);
				t.setDue(due);
				t.setDone(done);
				t.save(false);
				Log.v(TAG, "created task:" + name);

			}

		} catch (final FileNotFoundException e) {
			Log.e(TAG, "File not found", e);
			return false;
		} catch (final IOException e) {
			Log.e(TAG, "IO error", e);
			return false;
		}
		return true;
	}

	public static String getStringFromStream(final InputStream stream,
			final Context ctx) throws IOException {
		final StringBuilder sb = new StringBuilder();
		int c;

		while ((c = stream.read()) != -1) {
			sb.append((char) c);
		}
		return sb.toString();
	}
}
