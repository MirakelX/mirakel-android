package de.azapps.mirakel.sync.caldav;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.DataDownloadCommand;
import de.azapps.mirakel.sync.Network;
import de.azapps.mirakel.sync.Network.HttpMode;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakelandroid.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class CalDavSync {
	private static final String TAG = "CalDavSync";
	public static final String TYPE = "CalDav";
	private Context ctx;
	private Account account;

	public CalDavSync(Context mContext) {
		this.ctx = mContext;
	}

	public void sync(Account account) {		
		this.account=account;
		final String url = (AccountManager.get(ctx)).getUserData(account,
				SyncAdapter.BUNDLE_SERVER_URL);
		String content = "<c:calendar-query xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\">\n"
				+ "<d:prop>\n"
				+ "<d:getetag />\n"
				+ "<c:calendar-data />\n"
				+ "</d:prop>\n"
				+ "<c:filter>\n"
				+ "<c:comp-filter name=\"VCALENDAR\">\n"
				+ "<c:comp-filter name=\"VTODO\" />\n"
				+ "</c:comp-filter>\n"
				+ "</c:filter>\n"
				+ "</c:calendar-query>";
		Network n= new Network(new DataDownloadCommand() {
			
			@Override
			public void after_exec(String result) {
				if(result==null||result.equals("")){
					Log.d(TAG,"empty resonse");
					return;
				}
				List<Task> fromServer = parseResponse(result);
				if(fromServer!=null){
					Log.i(TAG,"got "+fromServer.size()+" tasks");
					mergeToLokale(fromServer);
				}
				syncToServer(Task.all(),url);
				
			}
		},HttpMode.REPORT,content,ctx,account.name,AccountManager.get(ctx).getPassword(account));
		n.execute(url);

	}

	private void syncToServer(List<Task> tasks,String url) {
		Log.i(TAG,"sync "+tasks.size()+" to server "+url);
//		// Some Headers
		String content = "BEGIN:VCALENDAR\n";
		//Do we need this?
//		 content+="PRODID:-//Ximian//NONSGML Evolution Calendar//EN
		content += "VERSION:2.0\n";
		content += "METHOD:PUBLISH\n";
		for (Task t : tasks) {
			if(t.getSyncState()==SYNC_STATE.ADD||t.getSyncState()==SYNC_STATE.IS_SYNCED){
			// Add Taskcontent
				content += parseTask(t);
			}else{
				Log.d(TAG,"state: "+t.getSyncState()+" |"+t.getName());
			}
		}
		content += "END:VCALENDAR";
		Network n = new Network(new DataDownloadCommand() {

			@Override
			public void after_exec(String result) {
				Log.d(TAG, result);
				
				SQLiteDatabase db = Mirakel.getWritableDatabase();
				db.delete(Task.TABLE, "sync_state IN (" + SYNC_STATE.DELETE
						+ "," + SYNC_STATE.NEED_SYNC + "," + SYNC_STATE.NOTHING
						+ ")", null);
				ContentValues cv= new ContentValues();
				cv.put("sync_state", SYNC_STATE.NOTHING.toInt());
				db.update(Task.TABLE, cv, null, null);
			}
		}, HttpMode.PUT, content, ctx,account.name,AccountManager.get(ctx).getPassword(account));
		n.execute(url);
	}


	protected void mergeToLokale(List<Task> fromServer) {
		SQLiteDatabase db = Mirakel.getWritableDatabase();
		ContentValues cv=new ContentValues();
		cv.put("sync_state", SYNC_STATE.IS_SYNCED.toInt());
		for(Task server:fromServer){
			Task local=Task.getByUUID(server.getUUID());
			if(!local.equals(server)){
				switch (local.getSyncState()) {
				case ADD:
					Log.wtf(TAG,"Task shouldn be on server");
					break;
				case DELETE:
					//Nothing, delete somewhere else
					break;
				case IS_SYNCED:
					Log.wtf(TAG, "is_synced shouldn be there");
					break;
				case NEED_SYNC:
					if(server.getUpdatedAt().compareTo(local.getUpdatedAt())>0){
						//server newer
						saveTask(server);
					}else{
						//local newer
						saveTask(local);
					}
					break;
				case NOTHING:
					saveTask(server);
					break;
				}
			}else{
				db.update(Task.TABLE, cv, "_id="+local.getId(), null);
			}
		}
		
	}
	
	private void saveTask(Task t){
		try {
			t.setSyncState(SYNC_STATE.IS_SYNCED);
			t.save(false);
		} catch (NoSuchListException e) {
			Log.wtf(TAG, "List did vanish");
		}
	}

	// Generate VTODO string
	private String parseTask(Task t) {
		String ret = "BEGIN:VTODO\n";
		ret += "X-MIRAKEL-UUID:" + t.getUUID() + "\n";
		ret += "SUMMARY:" + t.getName() + "\n";
		ret += "PRIORITY:" + t.getPriority() + "\n";
		ret += "CREATED:" + DateTimeHelper.formateCalDav(t.getCreatedAt())
				+ "\n";
		ret += "LAST-MODIFIED:"
				+ DateTimeHelper.formateCalDav(t.getUpdatedAt()) + "\n";
		if (t.getContent() != null && !t.getContent().equals("")) {
			ret += "DESCRIPTION:" + t.getContent().replace("\n", "\\n") + "\n";
		}
		if (t.getDue() != null)
			ret += "DUE;VALUE=DATE:"
					+ DateTimeHelper.formateCalDavDue(t.getDue()) + "\n";
		if(t.getReminder()!=null){
			ret+="BEGIN:VALARM\n";
			ret+="TRIGGER;VALUE=DATE-TIME:"+DateTimeHelper.formatTaskWarrior(t.getReminder())+"\n";
			ret+="END:VALARM\n";
		}
		
		ret += "CLASS:" + t.getList().getName() + "\n";
		ret += "STATUS:" + (t.isDone() ? "COMPLETED" : "NEEDS-ACTION") + "\n";
		return ret + "END:VTODO\n";
	}
	
	//Parse Server response
	private List<Task> parseResponse(String response){
		final String BEGIN="BEGIN:VTODO";
		final String END="END:VTODO";
		int begin=response.indexOf(BEGIN)+BEGIN.length();
		int end=response.lastIndexOf(END);
		if(end<begin){
			Log.d(TAG,"empty response");
			return null;
		}
		String p=response.substring(begin,end);
		if(p==null||p.equals(""))
			return null;
		String[] vtodos=p.split(END+"\n"+BEGIN);
		List<Task> tasks=new ArrayList<Task>();
		for(String s:vtodos){
			Task t=parseVTODO(s);
			if(t==null){
				Log.w(TAG, "Task from server is null");
				continue;
			}
			tasks.add(t);
		}		
		return tasks;
	}

	private Task parseVTODO(String s) {
		String [] lines=s.split("\n");
		String uuid=getUUID(lines);
		Task t=Task.getByUUID(uuid);
		if(t==null){
			ListMirakel list;
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
			int id;
			SpecialList sl=SpecialList.firstSpecial();
			if(sl!=null){
				id=sl.getId();
			}else{
				ListMirakel m=ListMirakel.first();
				if(m!=null){
					id=m.getId();
				}else{
					Toast.makeText(ctx, R.string.no_lists, Toast.LENGTH_LONG).show();
					return null;
				}
			}
			if(preferences.getBoolean("importDefaultList", false)){
				list=ListMirakel.getList(preferences.getInt("defaultImportList", id));
			}else{
				list=ListMirakel.getList(id);
			}
			t=Task.newTask(ctx.getString(R.string.new_task), list);
		}
		for(String l:lines){
			if(l.contains("SUMMARY")){
				t.setName(l.replace("SUMMARY:", ""));
			}else if(l.contains("PRIORITY")){
				int prio=Integer.parseInt(l.replace("PRIORITY:", ""));
				t.setPriority(prio>-3&&prio<3?prio:0);
			}else if(l.contains("CREATED")){
				try {
					t.setCreatedAt(DateTimeHelper.parseCalDav(l.replace("CREATED:", "")));
				} catch (ParseException e) {
					Log.d(TAG,"cannot parse created_at");
				}
			}else if(l.contains("LAST-MODIFIED")){
				try {
					t.setUpdatedAt(DateTimeHelper.parseCalDav(l.replace("LAST-MODIFIED:", "")));
				} catch (ParseException e) {
					Log.d(TAG,"cannot parse updated_at");
				}
			}else if(l.contains("DESCRIPTION")){
				Log.d(TAG, "from server: "+l);
				t.setContent(l.replace("DESCRIPTION:", "").replace("\\n", "\n"));
				Log.d(TAG, "new content: "+t.getContent());
			}else if(l.contains("DUE;VALUE=DATE")){
				try {
					t.setDue(DateTimeHelper.parseCalDavDue(l.replace("DUE;VALUE=DATE:", "")));
				} catch (ParseException e) {
					Log.d(TAG,"cannot parse due");
				}
			}else if(l.contains("STATUS")){
				t.setDone(l.contains("COMPLETED"));	
			}else if(l.contains("TRIGGER;VALUE=DATE-TIME")){
				try {
					t.setReminder(DateTimeHelper.parseTaskWarrior(l.replace("TRIGGER;VALUE=DATE-TIME:", "")));
				} catch (ParseException e) {
					Log.d(TAG,"cannot parse Reminder");
				}
			}
		}
		return t;
	}

	private String getUUID(String[] lines) {
		for(String l:lines){
			if(l!=null&&l.contains("X-MIRAKEL-UUID")){
				return l.replace("X-MIRAKEL-UUID:", "");
			}
		}
		return null;
	}

}
