package de.azapps.mirakel.new_ui.activities;

import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.common.base.Optional;

import de.azapps.mirakel.new_ui.R;

import static com.google.common.base.Optional.fromNullable;
import static de.azapps.tools.OptionalUtils.Procedure;
import static de.azapps.tools.OptionalUtils.withOptional;

public class MirakelActivity extends ActionBarActivity {

	private class ViewHolder {
		private Optional<DrawerLayout> mDrawerLayout;
		private Optional<ActionBarDrawerToggle> mDrawerToggle;
	}

	private ViewHolder viewHolder = new ViewHolder();


	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirakel);

		getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorAccent)));

		initDrawer();

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		withOptional(viewHolder.mDrawerToggle, new Procedure<ActionBarDrawerToggle>() {
			@Override
			public void apply(ActionBarDrawerToggle input) {
				input.syncState();
			}
		});
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		withOptional(viewHolder.mDrawerToggle,new Procedure<ActionBarDrawerToggle>() {
			@Override
			public void apply(ActionBarDrawerToggle input) {
				input.onConfigurationChanged(newConfig);
			}
		});
	}



	private void initDrawer() {
		// Nav drawer
		viewHolder.mDrawerLayout = fromNullable((DrawerLayout) findViewById(R.id.drawer_layout));
		withOptional(viewHolder.mDrawerLayout,new Procedure<DrawerLayout>() {
			@Override
			public void apply(DrawerLayout mDrawerLayout) {
				final ActionBar actionBar = MirakelActivity.this.getSupportActionBar();
				ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(MirakelActivity.this, mDrawerLayout,
						R.drawable.ic_drawer,
						R.string.list_title, /* "open drawer" description */
						R.string.list_title /* "close drawer" description */) {

					/** Called when a drawer has settled in a completely closed state. */
					public void onDrawerClosed(View view) {
						super.onDrawerClosed(view);
						actionBar.setTitle(R.string.list_title);
						invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
					}

					/** Called when a drawer has settled in a completely open state. */
					public void onDrawerOpened(View drawerView) {
						super.onDrawerOpened(drawerView);
						actionBar.setTitle(R.string.list_title);
						invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
					}
				};
				mDrawerLayout.setDrawerListener(mDrawerToggle);
				viewHolder.mDrawerToggle = Optional.of(mDrawerToggle);
				actionBar.setDisplayHomeAsUpEnabled(true);
				actionBar.setHomeButtonEnabled(true);
			}
		});

	}


	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(viewHolder.mDrawerLayout.isPresent()) {
			// For phones
			boolean drawerOpen = viewHolder.mDrawerLayout.get().isDrawerOpen(Gravity.START);
			if (drawerOpen) {
				// TODO list menu
			} else {
				// TODO tasks menu
			}
		} else {
			// For Tablets
		}
		return super.onPrepareOptionsMenu(menu);
	}

		@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mirakel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

		if(viewHolder.mDrawerToggle.isPresent()) {
			// Phone
			if (viewHolder.mDrawerToggle.get().onOptionsItemSelected(item)) {
				return true;
			}
		}
		int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
