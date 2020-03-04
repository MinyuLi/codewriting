package com.VarietyBubble;

import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.*;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.Cursor;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
	private final int VERSION = 1;
	
	private class TBlocks{
		public static final String tbName = "TBlocks";
		public static final String key = "_id";
		public static final String block = "_block";
	}
	
	private class TGlobal{
		public static final String tbName = "TGlobal";
		public static final String key = "_id";
		public static final String version = "_version";
		public static final String screenX = "_screenX";
		public static final String screenY = "_screenY";
		public static final String orient = "_orient";
	}
	
	private class TRecords{
		public static final String tbName = "TRecords";
		public static final String key = "_id";
		public static final String difficulty = "_difficulty"; //由上层保证不同 differency 的只能有一条记录
		public static final String userName = "_userName";
		public static final String score = "_score";
		public static final String record = "_record";
	}
	
	public class BlockAscription{
		public static final int inGameArea = 0;
		public static final int inDroppingBlockGrp = 1;
		public static final int inBackupBlockGrp = 2;
		public static final int inBlockGrpList_0 = 3;
	}
	public DBHelper(Context context, String name, 
			CursorFactory factory,int version) {
		super(context, name, factory, version);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " 
				+ TGlobal.tbName + " (" 
				+ TGlobal.key + " INTEGER PRIMARY KEY," 
				+ TGlobal.version + " INTEGER,"
				+ TGlobal.screenX + " INTEGER,"
				+ TGlobal.screenY + " INTEGER,"
				+ TGlobal.orient + " INTEGER"
				+ ")");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " 
				+ TBlocks.tbName + " ("
				+ TBlocks.key + " INTEGER PRIMARY KEY," 
				+ TBlocks.block + " INTEGER"
				+ ")");
		
		createTRecords(db);
	}
	
	private void createTRecords(SQLiteDatabase db){
		db.execSQL("CREATE TABLE IF NOT EXISTS "
				+ TRecords.tbName + " ("
				+ TRecords.key + " INTEGER PRIMARY KEY,"
				+ TRecords.difficulty + " INTEGER,"
				+ TRecords.userName + " VARCHAR(33),"
				+ TRecords.score + " VARCHAR(33),"
				+ TRecords.record + " VARCHAR(33)"
				+ ")");
	}
	
	/*ascription: 
	 * 		0表示属于 GameView; 
	 * 		1表示属于正在下落的BlockGrp; 
	 * 		2表示mBackupBlockGrp
	 * 		其他表示BlockGrpList中的BlockGrp*/
	public long saveBlock(int x, int y, int color, int shape, int ascription){
		try{
			SQLiteDatabase db = getWritableDatabase();
	        
			int blockInfo = ((shape << 4) | color) & 0xff ;
			int value = Util.Make4BytesToInt((byte)x, (byte)y, (byte)blockInfo, 
					(byte)ascription);

			ContentValues values = new ContentValues();
			values.put(TBlocks.block, value);
			long id = db.insert(TBlocks.tbName, TBlocks.key, values);
			//Log.i("DBHelper:saveBlock", String.format("id=%d, value=0x%x", id, value));
			return id;
		}catch(SQLiteException e){
			Log.e("DBHelper:saveBlock", e.getMessage());
			return -1;
		}
	}
	
	public int[] loadBlocks(){
		try{
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(TBlocks.tbName,null,null,null,null,null,
					TBlocks.key+" DESC");
			int [] value = new int[c.getCount()];
			int pos = 0;
			final int blockIndex = c.getColumnIndexOrThrow(TBlocks.block);
			for (c.moveToFirst();!(c.isAfterLast());c.moveToNext()) {
				if(pos >= c.getCount()){
					Log.e("DBHelper:loadBlocks", String.format("pos[%d] out of range, c.count=%d",
						pos, c.getCount()));
					break;
				}
				value[pos] = c.getInt(blockIndex);
				pos++;
			}
			return value;
		}catch(SQLiteException e){
			Log.e("DBHelper:loadBlocks", e.getMessage());
			return null;
		}
	}
	
	public long saveRecord(Records.Record r, Crypto crypto){
		try{
			SQLiteDatabase db = getWritableDatabase();
			ContentValues value = new ContentValues();
			value.put(TRecords.difficulty, r.difficulty);
			value.put(TRecords.userName, r.userName);
			value.put(TRecords.score, crypto.encryptLongToString(r.score, r.difficulty));
			value.put(TRecords.record, crypto.encryptLongToString(r.record, r.difficulty));	
			long id = db.insert(TRecords.tbName, TRecords.key, value);
			//Log.i(Global.TAG, String.format("DBHelper:saveRecord id=%d", id));
			return id;
		}catch(SQLiteException e){
			Log.e(Util.TAG, e.getMessage());
			return -1;
		}
	}
	
	public long deleteRecord(int difficulty){
		try{
			SQLiteDatabase db = getWritableDatabase();
			String sql = "delete from " + TRecords.tbName + " where " +
						TRecords.difficulty + " = " + difficulty;
			db.execSQL(sql);
			
			return 0;
		}catch(SQLiteException e){
			Log.e(Util.TAG, e.getMessage());
			return -1;
		}
	}
	
	public Records loadRecords(Crypto crypto){
		Records records = new Records();
		try{
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(TRecords.tbName,null,null,null,null,null,
					TRecords.key+" DESC");
			int pos = 0;
			final int userNameIndex = c.getColumnIndexOrThrow(TRecords.userName);
			final int differencyIndex = c.getColumnIndexOrThrow(TRecords.difficulty);
			final int scoreIndex = c.getColumnIndexOrThrow(TRecords.score);
			final int recordIndex = c.getColumnIndexOrThrow(TRecords.record);
			for (c.moveToFirst();!(c.isAfterLast());c.moveToNext()) {
				if(pos >= c.getCount()){
					Log.e(Util.TAG, String.format("DBHelper:loadRecords pos[%d] out of range, c.count=%d",
						pos, c.getCount()));
					break;
				}
				Records.Record v = records.CreateRecord();
				v.userName = c.getString(userNameIndex);
				v.difficulty = c.getInt(differencyIndex);
				v.score = crypto.decryptStringToLong(c.getString(scoreIndex), v.difficulty);
				v.record = crypto.decryptStringToLong(c.getString(recordIndex), v.difficulty);
				records.add(v);
			}
		}catch(SQLiteException e){
			//适合升级时使用
			Log.e("DBHelper:loadRecords", e.getMessage());
			createTRecords(getWritableDatabase());
		}
		return records;
	}
	
	public long saveGlobal(int screenX, int screenY, int orient){
		try{
			SQLiteDatabase db = getWritableDatabase();
			
			ContentValues value = new ContentValues();
			value.put(TGlobal.version, VERSION);
			value.put(TGlobal.screenX, screenX);
			value.put(TGlobal.screenY, screenY);
			value.put(TGlobal.orient, orient);
			long id = db.insert(TGlobal.tbName, TGlobal.key, value);
			//Log.i("DBHelper:saveGlobal", String.format("id=%d", id));
			return id;
		}catch(SQLiteException e){
			Log.e("DBHelper:saveGlobal", e.getMessage());
			return -1;
		}
	}
	
	public int[] loadGlobal(){
		try{
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(TGlobal.tbName,null,null,null,null,null,
					TGlobal.key+" DESC");
			
			if(c.getCount() != 1){
				Log.e("DBHelper:loadGlobal", String.format("c.count=%d", c.getCount()));
				return null;
			}
			
			int [] value = new int[3];
			final int xIndex = c.getColumnIndexOrThrow(TGlobal.screenX);
			final int yIndex = c.getColumnIndexOrThrow(TGlobal.screenY);
			final int oIndex = c.getColumnIndexOrThrow(TGlobal.orient);
			c.moveToFirst();
				
			value[0] = c.getInt(xIndex);
			value[1] = c.getInt(yIndex);
			value[2] = c.getInt(oIndex);

			return value;
		}catch(SQLiteException e){
			Log.e("DBHelper:loadGlobal", e.getMessage());
			return null;
		}
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, 
		int oldVersion, int newVersion) {
		//需要升级规则
		
		//删除以前的旧表，创建一张新的空表
		db.execSQL("DROP TABLE IF EXISTS "+TBlocks.tbName);
		db.execSQL("DROP TABLE IF EXISTS "+TGlobal.tbName);
		db.execSQL("DROP TABLE IF EXISTS "+TRecords.tbName);
		onCreate(db);
	}
	
	public void clear(){
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TBlocks.tbName, null, null);
		db.delete(TGlobal.tbName, null, null);
		db.delete(TRecords.tbName, null, null);
		Util.Loge("clear db");
	}
}
