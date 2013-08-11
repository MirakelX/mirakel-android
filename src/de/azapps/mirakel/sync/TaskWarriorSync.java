package de.azapps.mirakel.sync;

import de.azapps.mirakel.helper.Log;
import android.accounts.Account;
import android.content.Context;

public class TaskWarriorSync {

	public static final String TYPE = "TaskWarrior";
	private static final String TAG = "TaskWarroirSync";
	private Context mContext;
	
	public TaskWarriorSync(Context ctx){
		mContext=ctx;
	}
	
	public void sync(Account account){
		Log.w(TAG,"Not implemented yet");
	}

}
