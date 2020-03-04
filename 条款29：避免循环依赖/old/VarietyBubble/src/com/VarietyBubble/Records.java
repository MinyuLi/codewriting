package com.VarietyBubble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.content.res.Resources;

public class Records {	
	public class Record{
		public int difficulty;
		public String userName;
		public long score;
		public long record;
	}
	
	public Record CreateRecord(){
		Record r = new Record();
		return r;
	}
	
	private boolean dirty = true;
	public class Mycomparator implements Comparator{

		public int compare(Object o1,Object o2) {
		Record r1=(Record)o1;
		Record r2=(Record)o2;  
		if(r1.difficulty < r2.difficulty) //从大到小排序
			return 1;
		else
			return -1;
		}

	}
	
	private ArrayList<Record> mRecordList = new ArrayList<Record>();
	private Record mCurRecord = null;
	
	public void add(Record r){
		mRecordList.add(r);
		dirty = true;
	}
	
	public int RecordNum(){
		return mRecordList.size();
	}
	
	public void setCurUserDifficulty(String userName, int differency){
		for(Record r: mRecordList){
			if(r.difficulty == differency){
				mCurRecord = r;
				break;
			}
		}
		if(mCurRecord == null 
			|| mCurRecord.difficulty != differency)
		{
			Record r = new Record();
			r.difficulty = differency;
			r.score = 0;
			r.record = 0;
			r.userName = userName;
			
			mRecordList.add(r);
			mCurRecord = r;
			dirty = true;
		}
	}
	
	public void save(DBHelper db, Crypto crypto){
		if(mRecordList != null){
			for(Record r: mRecordList){
				db.saveRecord(r, crypto);
			}
		}
	}
	
	public void resetScore(){
		for(Record r : mRecordList){
			r.score = 0;
		}
	}
	
	public boolean deleteRecords(DBHelper db, boolean selected[]){
		boolean removed = false;
		if(selected.length != mRecordList.size())
			return false;
		
		for(int i = selected.length-1; i >= 0; i--){
			if(selected[i]){
				if(mCurRecord == mRecordList.get(i)){
					mCurRecord.record = 0;
				}else{
					db.deleteRecord(mRecordList.get(i).difficulty);
					mRecordList.remove(i);
				}
				removed = true;
			}
		}
		
		return removed;
	}
	
	public CharSequence[] getRecordsInfo(Resources res){
		sortRecords();
		CharSequence records[] = new CharSequence[mRecordList.size()];
		for(int i = 0; i < mRecordList.size(); i++){
			records[i] = res.getString(R.string.record_record) + mRecordList.get(i).record + " ("
			+ /*res.getString(R.string.record_difficulty) + */Util.difficultToStr(mRecordList.get(i).difficulty) + ")\n"
			+ res.getString(R.string.record_name) + mRecordList.get(i).userName;
				 
		}
		return records;
	}
	
	public void updateCurrentScore(long score, String userName){
		if(mCurRecord == null)
			return;
		mCurRecord.score += score;
		if(mCurRecord.score > mCurRecord.record){
			mCurRecord.record = mCurRecord.score;
			mCurRecord.userName = userName;
		}
	}
	
	public long getCurrentScore(){
		if(mCurRecord == null)
			return 0;
		return mCurRecord.score;
	}
	
	public int getCurrentDifficulty(){
		if(mCurRecord == null)
			return 0;
		
		return mCurRecord.difficulty;
	}
	
	private void sortRecords(){
		if(dirty){
			Comparator comp = new Mycomparator();
			Collections.sort(mRecordList,comp);
			dirty = false;
		}
	}
}
