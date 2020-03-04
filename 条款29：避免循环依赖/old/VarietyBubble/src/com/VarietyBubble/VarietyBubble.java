package com.VarietyBubble;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.os.Vibrator; 
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;


public class VarietyBubble extends Activity {
	private GameView mGame; 
	private static final int DIALOG_RECORDS = 1;
	private static final int DELETE_CONFIRM_DIALOG = 2;
	private static final int NEW_GAME_CONFIRM_DIALOG = 3;
	private static final int DIALOG_HELP = 4;
	private static final int OPTION_RETURN = 89;
	
	private boolean isLoadOkFromFile;
	//Vibrator mVibrator = null; 
	private Menu mMenu = null;
	private Crypto mCryptor = new Crypto("AES");
	private SharedPreferences mSp;
	private DBHelper mDb, mDbBak;
	private CharSequence[] mRecordsCharSeq = null;
	private boolean[] mSelectedRecords = null;
	private int mDownX = -1, mDownY = -1;
	private int mMoveX1 = -1, mMoveY1 = -1;
	private int mMoveX2 = -1, mMoveY2 = -1;
	private boolean mRevolve = false;
	private boolean mNeedSave = true;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean loadFromFirstDb;
		int lastState;
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			    WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.variety_bubble);

		mGame = (GameView) findViewById(R.id.bubble);
		mGame.setTextView((TextView) findViewById(R.id.text));
		mSp = getSharedPreferences("info", MODE_PRIVATE);
		mDb = new DBHelper(this, "info", null, 1);
		mDbBak = new DBHelper(this, "info_bak", null, 1);
		
		if( mGame.loadRecords(mSp, mDb, mCryptor)){
			loadFromFirstDb = true;
		}else{
			loadFromFirstDb = false;
			mGame.loadRecords(mSp, mDbBak, mCryptor);
		}
		
		updateOptionsSettingWhenStart();//先读配置
		updateOptionsSettingRunTime();

		if( loadFromFirstDb){
			Util.Logi("load from first");
			lastState = loadGame(mDb);
			saveBakDbAsync();
		}else{
			Util.Logi("load from backup");
			lastState = loadGame(mDbBak);
		}
		
		if(isLoadOkFromFile){
			if(lastState == Util.State.LOSE)
				mGame.nextState(Util.State.LOSE);
			else
				mGame.nextState(Util.State.PAUSE);
		}else{
			mGame.nextState(Util.State.READY);
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
			Log.i("VarietyBubble", "LANDSCAPE");
			mGame.setOrientation(newConfig.orientation);
		}else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			Log.i("VarietyBubble", "PORTRAIT");
			mGame.setOrientation(newConfig.orientation);
		}
	}
    
	@Override
	public void onStart(){
		Util.Logi("onStart");
		super.onStart();
		mGame.setOrientation(getRequestedOrientation());//getRequestedOrientation 返回 -1!!!
	}
	
	@Override
	public void onDestroy(){
		Util.Logi("onDestroy");
		//if(mVibrator != null)
		//	mVibrator.cancel();
		//saveGame();
		super.onDestroy();
	}
    
	@Override
	public void finish(){
		super.finish();
	}
	
	@Override
	public void onLowMemory(){
		mRecordsCharSeq = null;
		mSelectedRecords = null;
		try{
			removeDialog(DIALOG_RECORDS);
			removeDialog(DELETE_CONFIRM_DIALOG);
			removeDialog(NEW_GAME_CONFIRM_DIALOG);
			removeDialog(DIALOG_HELP);
		}catch(Exception e){
			
		}
	}
	
    
	protected int loadGame(DBHelper db){
		int lastState;

		lastState = mGame.loadGame(mSp, db, mCryptor);
		if(lastState < 0){
			isLoadOkFromFile = false;
		}else{
			isLoadOkFromFile = true;
		}
		
		Util.Logi1("loadGame, lastState=%d", lastState);

		return lastState;
	}
	
	protected void saveBakDbAsync(){
		new Thread( new Runnable() {   
            public void run() {   
                saveGame(mDbBak);   
            }          
         }).start();   
	}
    
	protected int saveGame(){
		saveGame(mDb);
		saveGame(mDbBak);
		return 0;
	}
    
	protected synchronized int saveGame(DBHelper db){
		//if(mShouldSave)
			mGame.saveGame(mSp, db, mCryptor);
		return 0;
	}
	
	private void onTouchUp(MotionEvent event, int hitError){
		int x, y;
		BlockGrp.Point dropping;
		x = (int)event.getX();
		y = (int)event.getY();
		
		if(Math.abs(x - mDownX) < hitError && Math.abs(y - mDownY) < hitError){
			dropping = mGame.getDroppingGrpPosition();
			if(dropping == null)
				return;
			
			if(y >= dropping.y && Math.abs(x - dropping.x) < hitError/2){
				mGame.keyDown(KeyEvent.KEYCODE_DPAD_DOWN);
			}else{
				if(x < dropping.x){
					mGame.keyDown(KeyEvent.KEYCODE_DPAD_LEFT);
				}else if(x > dropping.x){
					mGame.keyDown(KeyEvent.KEYCODE_DPAD_RIGHT);
				}
			}
			//Global.Logi2("dropping:{%d,%d}", dropping.x, dropping.y);
		}else{
			int xoffset = Math.abs(x - mDownX);
			int yoffset = Math.abs(y - mDownY);

			if( !mRevolve && xoffset < hitError && yoffset > hitError){
				mGame.keyDown(KeyEvent.KEYCODE_V); //垂直翻转
			}else if( ! mRevolve && xoffset > hitError && yoffset < hitError){
				mGame.keyDown(KeyEvent.KEYCODE_H); //水平翻转
			}else if(mRevolve && mMoveX1 > 0 && mMoveY1 > 0){
				if(Util.calcAlpha(mDownX, mDownY, x, y) >= 
					Util.calcAlpha(mDownX, mDownY, mMoveX1, mMoveY1)){
					mGame.keyDown(KeyEvent.KEYCODE_C); //顺时针
				}else{
					mGame.keyDown(KeyEvent.KEYCODE_A); //逆时针
				}
			}
		}
		//Global.Logi2("touch:up:(%d,%d)", x, y);
	}

	@Override   
	public boolean onTouchEvent(MotionEvent event) {
		 //hit 的误差
		final int hitError = 2 * mGame.getShortSize() / Util.OptDft.BLOCK_NUM_ON_SHORT_LINE;

		if(mGame.getState() == Util.State.RUNNING){
			BlockGrp.Point dropping;
			
			int action = event.getAction();
			switch(action){
			case MotionEvent.ACTION_DOWN:
				if(mDownX > 0 || mDownY > 0)
					break; //不支持多点触摸
				mDownX = (int)event.getX();
				mDownY = (int)event.getY();
				//Global.Logi2("touch:down{%d,%d}", mDownX, mDownY);
				dropping = mGame.getDroppingGrpPosition();
				if(dropping == null)
					break;
				if(mDownY >= dropping.y && Math.abs(mDownX - dropping.x) < hitError){
					mGame.setModeDelay(100);
				}				
				break;
			case MotionEvent.ACTION_UP:
				mGame.setModeDelay(1000);
				if(mDownX > 0 && mDownY > 0){
					onTouchUp(event, hitError);
				}else{
					Util.Loge("invalid up event");
				}
				mDownX = mDownY = mMoveX1 = mMoveY1 = mMoveX2 = mMoveY2 = -1;
				mRevolve = false;
				break;
			case MotionEvent.ACTION_MOVE:
				mMoveX1 = mMoveX2;
				mMoveY1 = mMoveY2;
				mMoveX2 = (int)event.getX();
				mMoveY2 = (int)event.getY();
				if( ! mRevolve && mDownX > 0 && mDownY > 0){
					if((Math.abs(mDownX - mMoveX2) > hitError) &&
							(Math.abs(mDownY - mMoveY2) > hitError))
						mRevolve = true;
				}
				//Global.Logi2("touch:move:(%d,%d)", mMoveX2, mMoveY2);
				break;
			}
		}
		else if(mGame.getState() == Util.State.PAUSE)
		{
			int x, y;
			x = (int)event.getX();
			y = (int)event.getY();
			int screenSizeX = mGame.getXScreenSize();
			int screenSizeY = mGame.getYScreenSize();
			
			//Global.Logi1("PAUSE:hitError=%d", hitError);
			//Global.Logi2("PAUSE:{x=%d,y=%d}", x, y);
			//Global.Logi2("PAUSE:{screenSizeX=%d,screenSizeY=%d}", screenSizeX, screenSizeY);
			if(Math.abs(y - screenSizeY/2) < hitError/2 
				&& Math.abs(x - screenSizeX/2) < hitError/2)
			{
				mGame.Resume();
			}
		}

		return super.onTouchEvent(event);   
	}  
    
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent msg){
		mGame.startDown(keyCode);
		return super.onKeyUp(keyCode, msg);
	}
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		if(mGame.keyDown(keyCode)){
			if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT
				|| keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
			{
				mGame.stopDown();
			}
			return true;
		}
		
		if(keyCode == KeyEvent.KEYCODE_BACK){
			return super.onKeyDown(keyCode, msg);
		}
		
		if(keyCode == KeyEvent.KEYCODE_MENU){
			int state = mGame.getState();
			if(state > Util.State.LOSE)
				return true;
			Util.Logi1("menu begin:state=%d", state);
			if(mGame.inRunningState())
				mGame.Pause();
			
			updateMenu();
			Util.Logi1("menu end:state=%d", mGame.getState());
		}

		return super.onKeyDown(keyCode, msg);
	}
    
	@Override
	protected void onPause() {
		super.onPause();
		Util.Logi("onPause");
		mGame.Pause();
		if(mNeedSave)
			saveGame();
		else
			mNeedSave = true;
	}
	
	@Override
	protected void onResume(){
		super.onResume(); 
		mGame.onResume();
	}
    
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case DIALOG_RECORDS:
			return new AlertDialog.Builder(VarietyBubble.this)
			.setTitle(R.string.menu_records)
			.setMultiChoiceItems(mRecordsCharSeq, mSelectedRecords,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton,
                                boolean isChecked) {
                        	if(whichButton < mSelectedRecords.length)
                        		mSelectedRecords[whichButton] = isChecked;
                        }
                    })
            .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .setNegativeButton(R.string.record_dialog_delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	showDialog(DELETE_CONFIRM_DIALOG);
                }
            }).create();
			
		case DELETE_CONFIRM_DIALOG:
			return new AlertDialog.Builder(VarietyBubble.this)
            .setTitle(R.string.delete_confirm_dialog)
            .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mGame.deleteRecords(mDb, mSelectedRecords);
                    mGame.deleteRecords(mDbBak, mSelectedRecords);
                }
            })
            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .create();
			
		case NEW_GAME_CONFIRM_DIALOG:
			return new AlertDialog.Builder(VarietyBubble.this)
			.setTitle(R.string.new_game_confirm_dialog)
			.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	mGame.newGame();
                }
            })
            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .create();
			
		case DIALOG_HELP:
			return new AlertDialog.Builder(VarietyBubble.this)
			.setTitle(R.string.dialog_help)
			.setMessage(R.string.dialog_helpinfo)
			.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .create();
			
		}
		return null;
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu){ 	
		mMenu = menu;
		super.onCreateOptionsMenu(mMenu);
		updateMenu();
		return true;
	}
	
	private void startOption(){
		Intent i = new Intent();
		i.setClass(VarietyBubble.this, Option.class);
		startActivityForResult(i, OPTION_RETURN);
		mNeedSave = false;
	}
	
	private void startRecords(){
		/*Intent i = new Intent();
		i.setClass(VarietyBubble.this, RecordsActivity.class);
		Bundle b = new Bundle();
		b.putCharSequence("recordsInfo", mGame.getRecordsInfo());
		b.putInt("textSize", mGame.getTextSize());
		i.putExtras(b);
		startActivity(i);*/
		
		if(mRecordsCharSeq != null)
			removeDialog(DIALOG_RECORDS);
		
		mRecordsCharSeq = mGame.getRecordsInfo();
		mSelectedRecords = new boolean[mRecordsCharSeq.length];
		for(int i = 0; i < mSelectedRecords.length; i++)
			mSelectedRecords[i] = false;
		
		showDialog(DIALOG_RECORDS);
	}
	
	private void startHelp(){
		showDialog(DIALOG_HELP);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == OPTION_RETURN){
			updateOptionsSettingRunTime();
		}
	}
    
	public void saveBlockNums(Integer blockNumOnShortLine, Integer blockNumOnLongLine){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		if(sp == null)
			return;
		Editor edit = sp.edit();
		edit.putString(Util.OptKey.BLOCK_NUM_ON_SHORT_LINE, blockNumOnShortLine.toString());
		edit.putString(Util.OptKey.BLOCK_NUM_ON_LONG_LINE, blockNumOnLongLine.toString());
		edit.commit();
	}
    
	private void updateOptionsSettingRunTime() {
		// Since we're in the same package, we can use this context to get
		// the default shared preferences
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		if(sp == null)
			return;

		OptionData od = new OptionData(sp);

		int color_shape = od.getIntValue(OptionData.COLOR_SHAPE);
		if(color_shape == 0){
			Block.setRandomMode(true, false, false);
			mGame.setDestroyMode(Util.ONLY_SHAPE);
		}else{
			Block.setRandomMode(false, true, true);

			int destroyMode = od.getIntValue(OptionData.DESTROY_MODE);
			if(destroyMode == 0){
				mGame.setDestroyMode(Util.ONLY_COLOR);
			}else if(destroyMode == 1){
				mGame.setDestroyMode(Util.ONLY_SHAPE);
			}else{
				mGame.setDestroyMode(Util.COLOR_AND_SHAPE);
			}
		}

		int upKeyMap = od.getIntValue(OptionData.UP_KEY_MAP);
		mGame.setUpKeyMap(upKeyMap);
		
		int destroyNum = od.getIntValue(OptionData.DESTROY_NUM);
		mGame.setDestroyNum(destroyNum);

		int blockNumOnShortLine = od.getIntValue(OptionData.BLOCK_NUM_ON_SHORT_LINE);
		int blockNumOnLongLine = od.getIntValue(OptionData.BLOCK_NUM_ON_LONG_LINE);
		Util.Logi2("short=%d,long=%d", blockNumOnShortLine, blockNumOnLongLine);
		mGame.setBlockNums(blockNumOnShortLine, blockNumOnLongLine);

		String userName = od.getStringValue(OptionData.NAME);
		int difficulty = od.calcDifficulty();
		mGame.setUserInfo(userName, difficulty);
		
		String soundSwitch = od.getStringValue(OptionData.SOUND);
		mGame.setSoundSwitch(soundSwitch.compareTo("true") == 0 ? true : false);
		
		mGame.invalidate();
	}
    
	private void updateOptionsSettingWhenStart(){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		if(sp == null)
			return;  
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String select = item.getTitle().toString();
		Resources res = getResources();
		
		if(select.compareTo(res.getString(R.string.menu_exit)) == 0){
			finish();
		}else if(select.compareTo(res.getString(R.string.menu_new_game)) == 0){
			int state = mGame.getState();
			if(state == Util.State.READY || state == Util.State.LOSE)
				mGame.newGame();
			else
				showDialog(NEW_GAME_CONFIRM_DIALOG);
		}else if(select.compareTo(res.getString(R.string.menu_resume)) == 0){
			Util.Logi1("Resume: %d", mGame.getState());
			mGame.Resume();
			Util.Logi1("Resume: %d", mGame.getState());
		}else if(select.compareTo(res.getString(R.string.menu_options)) == 0){
			startOption();
		}else if(select.compareTo(res.getString(R.string.menu_records)) == 0){
			startRecords();
		}else if(select.compareTo(res.getString(R.string.menu_help)) == 0){
			startHelp();
		}
		return false;
	}
    
	private void updateMenu(){
		if(mMenu == null)
			return;
		int itemNum = mMenu.size();
		if(mGame.getState() == Util.State.LOSE ||
			mGame.getState() == Util.State.READY){
			if(itemNum == 5)
				return;

			mMenu.clear();
		}else if(isLoadOkFromFile){
			if(itemNum == 6)
				return;
			mMenu.clear();
			mMenu.add(R.string.menu_resume).setIcon(R.drawable.resume);
		}else{
			if(itemNum == 6)
				return;
			
			if(itemNum != 0){
				mMenu.clear();
				mMenu.add(R.string.menu_resume).setIcon(R.drawable.resume);
			}
		}
		
		mMenu.add(R.string.menu_new_game).setIcon(R.drawable.new_game);
		mMenu.add(R.string.menu_exit).setIcon(R.drawable.exit);
		mMenu.add(R.string.menu_records).setIcon(R.drawable.records);
		mMenu.add(R.string.menu_options).setIcon(R.drawable.option);
		mMenu.add(R.string.menu_help).setIcon(R.drawable.help);
	}
}