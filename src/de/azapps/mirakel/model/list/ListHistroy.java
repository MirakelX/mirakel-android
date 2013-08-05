package de.azapps.mirakel.model.list;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.model.DatabaseHelper;

public class ListHistroy {
	public final static String TABLE="ListHistory";
	private final static String[] all={"_id","new","old","timestamp","list_id"};
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
}
