/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel.static_activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.SpecialList;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.sync.AuthenticatorActivity;

public class StartActivity extends Activity {
	private static final String TAG = "StartActivity";
	private static final int PrepareLogin=1;
	private static final int ShowHelp=2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int version = Mirakel.getReadableDatabase().getVersion();
		if (version != Mirakel.DATABASE_VERSION) {
			Log.v(TAG, "SET DB-VERSION " + Mirakel.DATABASE_VERSION);
			Mirakel.getReadableDatabase().setVersion(Mirakel.DATABASE_VERSION);
		}
		setContentView(R.layout.activity_start);
		Button start=(Button)findViewById(R.id.Start);
		start.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.v(TAG, "Click");
				// generateDummies();
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				if (settings.getBoolean("showHelp", true)) {
					Intent intent = new Intent(StartActivity.this,
							HelpActivity.class);
					startActivityForResult(intent,ShowHelp);
				} else {
					if(((CheckBox)findViewById(R.id.use_server)).isChecked()){
						Intent intent = new Intent(StartActivity.this,AuthenticatorActivity.class);
						//intent.setAction(MainActivity.SHOW_LISTS);
						startActivityForResult(intent, PrepareLogin);
					}else{
						settings = PreferenceManager
								.getDefaultSharedPreferences(getApplicationContext());
						SharedPreferences.Editor editor = settings.edit();
						  editor.putBoolean("syncUse", false);
						  editor.commit();
						Intent intent = new Intent(StartActivity.this,
							MainActivity.class);
						intent.putExtra("listId", SpecialList.first().getId());
						startActivity(intent);
					}

				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

	public void generateDummies() {
		Mirakel.getWritableDatabase().execSQL(
				"INSERT INTO lists (name) VALUES (?),(?)",
				new String[] { "Foo", "Bar" });
		Mirakel.getWritableDatabase()
				.execSQL(
						"INSERT INTO tasks (list_id,name) VALUES (1,'First task'), (1,'Second'), (2,'another')");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent intent = new Intent(StartActivity.this,
					SettingsActivity.class);
			startActivity(intent);
			break;
		}
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode==RESULT_OK){
			switch(requestCode){
				case PrepareLogin:
					SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean("syncUse", true);
					editor.commit();
					Intent intent = new Intent(StartActivity.this,
							MainActivity.class);
					intent.putExtra("listId", SpecialList.first().getId());
					startActivity(intent);
					break;
				case ShowHelp:
					break;
				default:
					Log.v(TAG,"Unknown Requestcode");
			}
		}
	}
}
