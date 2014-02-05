package de.azapps.tools.taptest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class TapTest {
	private static int	testNumber	= 1;
	private PrintWriter	writer;
	private Context		context;

	public TapTest(Context context, File outputFile) throws FileNotFoundException {
		this.context = context;
		writer = new PrintWriter(outputFile);
		writer.println("public class TestTaskFragment extends\n"
				+ "ActivityInstrumentationTestCase2<MainActivity> {");
		writer.println("@Override\n"
				+ "protected void setUp() throws Exception solo.waitForActivity{\n"
				+ "		super.setUp();" + "		randomGenerator = new Random();\n"
				+ "		solo = new Solo(getInstrumentation(), getActivity());\n"
				+ "		solo.waitForActivity(MainActivity.class);" + ");\n}");
		writer.println("public void test" + testNumber + "() {");
		writer.flush();
	}

	public void newTest() {
		writer.println("}");
		testNumber++;
		writer.println("public void test" + testNumber + "() {");
		writer.flush();
	}

	public void write(String txt) {
		writer.println("\t" + txt);
		writer.flush();
	}

	private final static String	newTapTestString	= "New TapTest",
			newTapTestComment = "New TapTest Comment";

	public void addToMenu(Menu menu) {
		menu.add(newTapTestString);
		menu.add(newTapTestComment);
	}

	public void handleMenuItem(MenuItem item) {
		if (item.getTitle().equals(newTapTestString)) {
			newTest();
		} else if (item.getTitle().equals(newTapTestComment)) {
			final EditText input = new EditText(context);
			new AlertDialog.Builder(context)
					.setTitle("Comment")
					.setView(input)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									write("// " + input.getText().toString());
								}
							}).show();
		}
	}
}
