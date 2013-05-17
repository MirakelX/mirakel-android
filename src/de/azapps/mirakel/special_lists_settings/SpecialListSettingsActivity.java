package de.azapps.mirakel.special_lists_settings;

import de.azapps.mirakel.R;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.static_activities.SettingsFragment;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

public class SpecialListSettingsActivity extends Activity {
	public static final String SLIST_ID="de.azapps.mirakel.SpecialListSettings/list_id";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i=getIntent();
		SpecialList sl=SpecialList.getSpecialList(i.getIntExtra(SLIST_ID, 1));
		setContentView(R.layout.special_list_preferences);
		
		EditText name=(EditText) findViewById(R.id.special_list_name);
		name.setText(sl.getName());
		
		CheckBox active=(CheckBox) findViewById(R.id.special_list_active);
		active.setChecked(sl.isActive());
		
		EditText where=(EditText) findViewById(R.id.special_list_where);
		where.setText(sl.getWhereQuery());
		
		
	}
}
