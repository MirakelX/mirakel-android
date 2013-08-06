package de.azapps.mirakel.model.list;

import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.helper.JsonHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.DatabaseHelper;

public class ListHistroy {
	public final static String TABLE="ListHistory";
	private final static String[] all={"_id","new","old","timestamp","list_id"};
	private static final String TAG = "ListHistory";
	private SQLiteDatabase database;
	@SuppressWarnings("unused")
	private Context ctx;
	
	public ListHistroy(Context ctx){
		this.ctx=ctx;
		database=new DatabaseHelper(ctx).getReadableDatabase();
	}
	
	public Pair<ListMirakel, ListMirakel> getLast(){
		Cursor c=database.query(TABLE, all, null, null, null, null, "_id DESC", "1");
		Pair<ListMirakel, ListMirakel> p=historyToPair(c);
		c.close();
		return p;
	}

	private Pair<ListMirakel, ListMirakel> historyToPair(Cursor c) {
		c.moveToFirst();
		Pair<ListMirakel, ListMirakel> p=null;
		if(!c.isAfterLast()){
			JsonParser parser = new JsonParser();
			ListMirakel newList=c.getString(1)==null?null:ListMirakel.parseJson((JsonObject)parser.parse(c.getString(1)));
			ListMirakel old= c.getString(2)==null?null:ListMirakel.parseJson((JsonObject)parser.parse(c.getString(2)));
			p=new Pair<ListMirakel, ListMirakel>(old,newList);
		}
		return p;
	}	
	
	public Pair<ListMirakel, ListMirakel> getLastById(int listId){
		Cursor c=database.query(TABLE, all, "list_id="+listId, null, null, null, "_id DESC", "1");
		Pair<ListMirakel, ListMirakel> p=historyToPair(c);
		c.close();
		return p;
	}	
	
	@SuppressLint("SimpleDateFormat")
	public String getChangesForSync(int ListId,GregorianCalendar lastSync){
		String json="{";
		String[] coll={"new"};
		String newer="timestamp>="+lastSync.getTimeInMillis();
		String id=" list_id="+ListId;
		
		Cursor c=database.query(TABLE, coll , "new like '%\"name\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"name\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"lft\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"lft\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"rgt\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"rgt\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"due\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"due\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"list_id\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"list_id\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"done\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"done\":");
		c.close();
		if(json.equals("{")){
			Log.w(TAG,"no changes to Report");
			return null;
		}
		
		c=database.query(TABLE, coll , newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"updated_at\":");
		c.close();
				
		json+="\"id\":"+ListId+"}";
		
		return json;
	}

	private String addCurser(Cursor c, String key) {
		c.moveToFirst();
		String s=null;
		if(!c.isAfterLast()){
			s=JsonHelper.getPart(key, c.getString(0));
		}			
		return s==null?"":s+",";
	}
}
