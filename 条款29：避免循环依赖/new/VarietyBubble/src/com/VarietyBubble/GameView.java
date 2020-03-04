package com.VarietyBubble;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.content.SharedPreferences;

public class GameView extends View implements AreaCallback {
	private TextView mStatusText;
	private GameArea mGameArea;
	private WaitingArea mWaitingArea;
	private BlockGrp mBackupBlockGrp = null;
	private Sound mSound;
	
	private int mState = Util.State.READY;
	private int mStateBeforePause = -1;
	private boolean mScreenRevolved = true; //屏幕旋转过
    
	private long mCurScore;   //单次消球得分
	private long mDestroyedBlockNum = 0;
	private boolean mDestroyedBlock;
	private long continueDestroyTimes = 0;
	private int mUpKeyMap = KeyEvent.KEYCODE_D;

	private long mMoveDelay = 1000;
	private long mFallingDelay = 100;
	private long mRollingDelay = 100;
	private long mLastMove = 0;    

	private int mDestroyMode = Util.ONLY_SHAPE;
	private int mMinBlockNumToDestry = Util.OptDft.DESTROY_NUM;
	private int mBlockNumOnShortLine = Util.OptDft.BLOCK_NUM_ON_SHORT_LINE;
	private int mBlockNumOnLongLine = Util.OptDft.BLOCK_NUM_ON_LONG_LINE; //默认长边的泡泡个数由短边的个数和屏幕大小计算得到

	private int mXScreenSize=-1, mYScreenSize=-1;
	
	private Rect mInvalidateRect = null;
	
	private String mUserName = null;
	private Records mRecords = null;

	private final Paint mPaint = new Paint();
	
	private boolean mOptionChanged = false;
	private int mOrientation = -1;
	private boolean mStopDown = false;
	static private Bitmap sStartPicture = null;
	
	private RefreshHandler mRedrawHandler = new RefreshHandler();

	class RefreshHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			GameView.this.update();
			GameView.this.invalidate();
		}

		public void sleep(long delayMillis) {
			this.removeMessages(0);
		 	sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	};
    
	public GameView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setBackgroundColor(0xffffffff);
		mSound = new Sound(context);
	}

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setBackgroundColor(0xffffffff);
		mSound = new Sound(context);
	}
	   
	public void setTextView(TextView newView) {
		mStatusText = newView;
		mStatusText.setText(R.string.state_ready);
		mStatusText.setVisibility(View.VISIBLE);
	}
    
	public boolean isScreenLandScape(){
		if(mXScreenSize > mYScreenSize){
			return true;
		}else{
			return false;
		}
		//return (getOrientation() == Configuration.ORIENTATION_LANDSCAPE);
	}
	
	public int getYScreenSize()	{
		return mYScreenSize;
	}
	
	public int getXScreenSize()	{
		return mXScreenSize;
	}
	
	public int getShortSize(){
		if(mXScreenSize > mYScreenSize)
			return mYScreenSize;
		
		return mXScreenSize;
	}
	public void setOrientation(int orientation){
		mOrientation = orientation;
	}
	
	@Override
	public void invalidate(){
		if(mInvalidateRect == null)
			super.invalidate();
		else{
			super.invalidate(mInvalidateRect);
		}
	}
	
	@Override
	public void invalidate(Rect r){
		int blockSize = Block.getBlockSize();
		r.left = r.left * blockSize;
		r.right = r.right * blockSize;
		r.top = r.top * blockSize;
		r.bottom = r.bottom * blockSize;
		super.invalidate(r);
	}
	
	public void setInvalidateRect(Rect rect){
		mInvalidateRect = rect;
	}
    
	/*
	因为要预留一块区域给WaitingArea用于展示即将进入游戏区的积木块组，而积木块组在X轴和Y轴方向都最多包含3个block。
	因此在计算长边的积木块大小时要多算3个。
	以竖屏模式（短边对应宽度，长边对应高度）为例，根据短边计算block大小为：短边长度/短边积木数，
	根据长边计算为：长边长度/(长边积木数+3)；两个值取较小的一个设为block大小
	*/
	private int calcBlockSize(int w, int h)
	{	
		//实际游戏区的短边（竖屏为gameAreaW，横屏为gameAreaY）为屏幕宽（或高）的39/40。
		//留 1/40 的余量做为边距
		int gameAreaW = w - w/40;
		int gameAreaY = h - h/40;
		if(isScreenLandScape()){
			if(mBlockNumOnLongLine < 0)
				return gameAreaY / mBlockNumOnShortLine;
			else
				return Math.min(gameAreaY / mBlockNumOnShortLine, gameAreaW / (mBlockNumOnLongLine + 3));
		}else{
			if(mBlockNumOnLongLine < 0)
				return gameAreaW / mBlockNumOnShortLine;
			else
				return Math.min(gameAreaW / mBlockNumOnShortLine, gameAreaY / (mBlockNumOnLongLine + 3));
		}
	}
	
	public int getTextSize(){
		//同一个手机 ，ADT的短边为320，Android Studio的短边为1080。因此ADT中除以15，Android Studio中除以50
		return Math.min(mXScreenSize, mYScreenSize) / 15;
	}
	
	private void updateTextSize(){
		mStatusText.setTextSize(getTextSize());
	}
	
	private Rect calcGameAreaRect(int blockSize, int xBlockCount, int yBlockCount){
		Rect rect = new Rect();
		if(isScreenLandScape()){
			rect.left = (mXScreenSize - blockSize * 3 - blockSize * xBlockCount) / 2;
			rect.top = (mYScreenSize - (blockSize * yBlockCount)) / 2;
		}else{
			rect.left = (mXScreenSize - (blockSize * xBlockCount)) / 2;
			rect.top = (mYScreenSize - blockSize * 3 - blockSize * yBlockCount) / 2;
		}
		rect.right = rect.left + blockSize * xBlockCount;
		rect.bottom = rect.top + blockSize * yBlockCount;
		return rect;
	}
	
	private Rect calcWaitingAreaRect(Rect gameAreaRect){
		Rect rect = new Rect();
		if(isScreenLandScape()){
			rect.left = gameAreaRect.right;
			rect.top = 0;
		}else{
			rect.left = 0;
			rect.top = gameAreaRect.bottom;
		}
		rect.right = mXScreenSize;
		rect.bottom = mYScreenSize;
		return rect;
	}
	
	protected void updateAreaInfo(int w, int h, int oldw, int oldh){
		boolean shouldSave = false; //如果是首次运行，长边积木数初始值为-1，该值会被更新，则需要保存。其它情况不用保存
		
		mXScreenSize = w;
		mYScreenSize = h;
		
		setFocusable(true);
		updateTextSize();
		
		//初始化Block对应的bitmap
		int blockSize = calcBlockSize(w, h);
		Resources r = getContext().getResources();
		Block.loadBitMap(r, blockSize);
		
		//游戏区中X轴和Y轴的小球数
		int xBlockCount;
		int yBlockCount;
		if(isScreenLandScape()){
			if(mBlockNumOnLongLine < 0){
				xBlockCount = mXScreenSize / blockSize - 3; //减3是为了保证有地方显示 waitingArea
				mBlockNumOnLongLine = xBlockCount;
				shouldSave = true;
			}else{
				xBlockCount = mBlockNumOnLongLine;
			}
			yBlockCount = mBlockNumOnShortLine;
		}else{
			xBlockCount = mBlockNumOnShortLine;
			if(mBlockNumOnLongLine < 0){
				yBlockCount = mYScreenSize / blockSize - 3; //减3是为了保证有地方显示 waitingArea
				mBlockNumOnLongLine = yBlockCount;
				shouldSave = true;
			}else{
				yBlockCount = mBlockNumOnLongLine;
			}
		}

		Rect gameAreaRect = calcGameAreaRect(blockSize, xBlockCount, yBlockCount);
		Rect waitingAreaRect = calcWaitingAreaRect(gameAreaRect);

		int blockGrpNum; //计算WaitingArea中可以存放几组BlockGrp
		if(isScreenLandScape()){
			blockGrpNum = mYScreenSize / blockSize / 3;
		}else{
			blockGrpNum = mXScreenSize / blockSize / 3;
		}
		
		if(mGameArea == null || mWaitingArea == null){
			//首次运行
			mGameArea = new GameArea(this, gameAreaRect, xBlockCount, yBlockCount);
			mWaitingArea = new WaitingArea(blockGrpNum, waitingAreaRect, isScreenLandScape());
		}
		else if(w == oldh && h == oldw){
			//屏幕旋转
			mGameArea.revolveScreen(isScreenLandScape());
			mWaitingArea.setBlockGrpNum(blockGrpNum);
			mWaitingArea.setRect(waitingAreaRect, isScreenLandScape());

			if(inRunningState())
				nextState(Util.State.FALLING);
			else
				mScreenRevolved = true;
		}else {
			//配置发生变化
			mGameArea.updateGameArea(gameAreaRect, xBlockCount, yBlockCount);
			mWaitingArea.setBlockGrpNum(blockGrpNum);
			mWaitingArea.setRect(waitingAreaRect, isScreenLandScape());
		}
		
		if(shouldSave){
			saveBlockNums(mBlockNumOnShortLine, mBlockNumOnLongLine);
		}
	}
	
	public void saveBlockNums(Integer blockNumOnShortLine, Integer blockNumOnLongLine){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
		if(sp == null)
			return;
		Editor edit = sp.edit();
		edit.putString(Util.OptKey.BLOCK_NUM_ON_SHORT_LINE, blockNumOnShortLine.toString());
		edit.putString(Util.OptKey.BLOCK_NUM_ON_LONG_LINE, blockNumOnLongLine.toString());
		edit.commit();
		Log.i(Util.TAG, String.format("save ok:(%d,%d)", blockNumOnShortLine, blockNumOnLongLine));
	}
    
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if(mXScreenSize == w && mYScreenSize == h)
			return;
		Util.Logi(String.format("onSizeChanged: w=%d h=%d oldw=%d oldh=%d", w, h, oldw, oldh));
		updateAreaInfo(w, h, oldw, oldh);
	}
    
	private void drawNextBlocks(Canvas canvas, Paint paint){
		if(mState == Util.State.READY)
			return;
		
		if(mWaitingArea != null)
			mWaitingArea.draw(canvas, paint, isScreenLandScape());
	}
    
	public int getState(){
		return mState;
	}
	
	public BlockGrp.Point getDroppingGrpPosition(){
		return mGameArea.getDroppingGrpPosition();
	}
	
	public void Resume(){
		if(mScreenRevolved){
			nextState(Util.State.FALLING);
		}else if(mOptionChanged){
			nextState(Util.State.DESTROYING);
		}else if(mStateBeforePause >= 0 && mStateBeforePause != Util.State.ROLLING){
			nextState(mStateBeforePause);
		}else {
			nextState(Util.State.RUNNING);
		}
		mStateBeforePause = -1;
		mOptionChanged = false;
		mScreenRevolved = false;
	}
	
	public void onResume(){
		mScreenRevolved = true;//触发一次下降
	}
	
	public boolean isRunningState(int state){
		if( state != Util.State.PAUSE
		    && state != Util.State.READY
		    && state != Util.State.LOSE)
			return true;

		return false;
	}

	public boolean inRunningState(){
		return isRunningState(mState);
	}
	
	public void Pause(){
		if(inRunningState()){
			mStateBeforePause = mState;
			nextState(Util.State.PAUSE);
		}
	}
    
	public void nextState(int newState) {
		int oldState = mState;
		mState = newState;
		
		Util.Logi2("new state. %d -> %d", oldState, mState);

		if (newState == Util.State.RUNNING & oldState != Util.State.RUNNING) {
			mStatusText.setVisibility(View.INVISIBLE);
			update();
			return;
		}
		
		if (newState == Util.State.ROLLING){
			mSound.play(Sound.SND_ROLLING);
			mWaitingArea.startRolling();
			update();
			return;
		}

		if (newState == Util.State.FALLING && oldState == Util.State.PAUSE)
			update();

		if(newState == Util.State.FALLING && oldState != Util.State.FALLING){
			mGameArea.clearDroppingBlockNum();
			//mStatusText.setVisibility(View.INVISIBLE);
			return;
		}

		if (newState == Util.State.DESTROYING && oldState == Util.State.RUNNING)
			continueDestroyTimes = 0;

		if (newState == Util.State.DESTROYING && oldState != Util.State.DESTROYING){
			mDestroyedBlock = false;
		}
		
		if (newState == Util.State.DESTROYING && oldState == Util.State.PAUSE){
			update();
		}
		
		if (newState == Util.State.DESTROYING && oldState != Util.State.DESTROYING){
			return;
		}

		updateTextView();
		mStatusText.setVisibility(View.VISIBLE);
	}
	
	public void setBackupBlockGrp(BlockGrp bg){
		mBackupBlockGrp = bg;
	}
	
	protected BlockGrp getBlockGrp(){
		BlockGrp bg;
		if(mBackupBlockGrp != null){
			bg = mBackupBlockGrp;
			mBackupBlockGrp = null;
		}else{
			bg = mWaitingArea.getBlockGrp();
			nextState(Util.State.ROLLING);
		}
		
		return bg;
	}
	
	public void setModeDelay(int delay){
		mMoveDelay = delay;
	}
    
	protected void update(){
		if (mState == Util.State.RUNNING) {
			long now = System.currentTimeMillis();

			if (now - mLastMove > mMoveDelay) {
				if(mGameArea.blockNumInBlockGrp() == 0){
					setModeDelay(1000);
					BlockGrp bg = getBlockGrp();
					if( ! mGameArea.setBlockGrp(bg)){
						nextState(Util.State.LOSE);
						mSound.play(Sound.SND_LOSE);
						return;
					}
				}else {
					if( ! mStopDown)
						downOneStep();
					mSound.play(Sound.SND_TICK);
				}
				mLastMove = now;
			}
			mRedrawHandler.sleep(mMoveDelay);
		}

		if (mState == Util.State.FALLING){
			long now = System.currentTimeMillis();
			if (now - mLastMove > mFallingDelay) {
				if( ! mGameArea.dropToFillEmptyPosition()
					&& mGameArea.isDroppingFinished()
					&& mGameArea.isDestroyFinished()){
					nextState(Util.State.DESTROYING);
				}
				else{
					invalidate();
				}
				mLastMove = now;
			}
			mRedrawHandler.sleep(mFallingDelay);
		}
		
		if (mState == Util.State.ROLLING){
			long now = System.currentTimeMillis();
			if (now - mLastMove > mRollingDelay) {
				if( mWaitingArea.isRollingFinished()){
					nextState(Util.State.RUNNING);
				}
				else{
					invalidate();
				}
				mLastMove = now;
			}
			mRedrawHandler.sleep(mRollingDelay);
		}

		if (mState == Util.State.DESTROYING){
			long now = System.currentTimeMillis();
			if (now - mLastMove > mFallingDelay) {
				if( ! mGameArea.isDestroyFinished())
					invalidate();
				else if( ! testAndDestroyBlocks()){
					nextState(Util.State.RUNNING);
				}
				mLastMove = now;
			}
			mRedrawHandler.sleep(mFallingDelay);
		}
	}
	
	public boolean startDown(int keyCode){
		mStopDown = false;
		return true;
	}
	
	public void stopDown(){
		mStopDown = true;
	}
    
	public boolean keyDown(int keyCode) {
		boolean revolved = false;
		boolean tick = false;
		if(mState != Util.State.RUNNING)
			return false;
		
		if(keyCode == KeyEvent.KEYCODE_DPAD_UP)
			keyCode = mUpKeyMap;
		
		switch(keyCode){
		case KeyEvent.KEYCODE_C:
			revolved = mGameArea.revolve(Util.CLOCKWISE);
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			downOneStep();
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			tick = mGameArea.leftOneStep();
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			tick = mGameArea.rightOneStep();
			break;
		case KeyEvent.KEYCODE_H:
			revolved = mGameArea.revolve(Util.HORIZONTAL);
			break;
		case KeyEvent.KEYCODE_V:
			revolved = mGameArea.revolve(Util.VERTICAL);
			break;
		case KeyEvent.KEYCODE_A:
			revolved = mGameArea.revolve(Util.ANTICLOCKWISE);
			break;
		default:
			return false;
		}
		
		invalidate();
		
		if(tick)
			mSound.play(Sound.SND_TICK);
		else if(revolved)
			mSound.play(Sound.SND_REVOLVE);

		return true;
	}
	
	private void drawStartPicture(Canvas canvas, Paint paint){
		final int xSize = Math.min(mXScreenSize, mYScreenSize)*3 / 5;
		final int ySize = xSize/3;

		if(sStartPicture == null){
			Drawable rsc = getResources().getDrawable(R.drawable.start);
			sStartPicture = Bitmap.createBitmap(xSize, ySize, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(sStartPicture);
			rsc.setBounds(0, 0, xSize, ySize);
			rsc.draw(c);
		}
		
		canvas.drawBitmap(sStartPicture, 
        		mXScreenSize/2 - xSize/2,
        		mYScreenSize/2 - ySize/2,
        		paint);
	}
    
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(mState == Util.State.READY){
			mStatusText.setVisibility(View.INVISIBLE);
			drawStartPicture(canvas, mPaint);
			return;
		}
		
		mGameArea.draw(canvas, mPaint);
		drawNextBlocks(canvas, mPaint);
	}
    
    
	public void updateScore(int destroyedBlockNum, int destroyMode){
		int scoreForEachBlock = 1;
		
		destroyedBlockNum -= mMinBlockNumToDestry;
		if(destroyedBlockNum < 0){
			Util.Loge("UpdateScore:destroyedBlockNum error");
			return;
		}
		
		//达到最低消球数的部分，每球一分
		mCurScore = scoreForEachBlock * mMinBlockNumToDestry;
		
		//超过最低消球数的部分，每超过一个得分翻倍
		while(destroyedBlockNum > 0){
			scoreForEachBlock *= 2;
			mCurScore += scoreForEachBlock;
			destroyedBlockNum--;
		}
		
		/*if(Global.COLOR_AND_SHAPE == destroyMode){
			mCurScore *= 2; //颜色和形状相同得分翻倍
		}*/
		
		mCurScore *= Math.pow(2, continueDestroyTimes > 0 ? 1 : 0); //连续消球得分翻倍
		
		mRecords.updateCurrentScore(mCurScore, mUserName);
		
		mDestroyedBlockNum += destroyedBlockNum;
		continueDestroyTimes++;
		
		String str = "+" + mCurScore;
		
		mStatusText.setText(str);
		mStatusText.setVisibility(View.VISIBLE);
		mSound.play(Sound.SND_DESTROY);
		mRedrawHandler.sleep(500);
	}
    
	private int downOneStep(){
		int oldBlockNum = mGameArea.blockNumInBlockGrp();
		int curBlockNum = mGameArea.downOneStep();
		
		if(oldBlockNum > curBlockNum){
			mSound.play(Sound.SND_TOUCH_GROUND);
		}
		
		if(curBlockNum == 0){
			nextState(Util.State.DESTROYING);
		}
		return curBlockNum;
	}

	public void setUserInfo(String name,  int differency){
		if(mUserName != null){
			if(name.compareTo(mUserName) != 0 && mRecords != null){
				mRecords.resetScore();
			}
		}
		mUserName = name;
		
		if(mRecords != null)
			mRecords.setCurUserDifficulty(mUserName, differency);
		updateTextView();
	}
	
	public void setSoundSwitch(boolean on){
		mSound.setSwitch(on);
	}
	
	private void updateTextView(){
		Resources res = getContext().getResources();
		CharSequence str = "";
		if (mState == Util.State.PAUSE) {
			str = res.getText(R.string.state_pause)
				+ res.getString(R.string.state_difficulty) + Util.difficultToStr(mRecords.getCurrentDifficulty())
				+ res.getString(R.string.state_score) + mRecords.getCurrentScore();				
		}
		if (mState == Util.State.READY) {
		    str = res.getText(R.string.state_ready);
		}
		if (mState == Util.State.LOSE) {
			str = res.getString(R.string.state_lose)
				+ res.getString(R.string.state_difficulty) + Util.difficultToStr(mRecords.getCurrentDifficulty())
				+ res.getString(R.string.state_score) + mRecords.getCurrentScore();
		}
		if (mState == Util.State.DESTROYING || mState == Util.State.FALLING){
			str = "+" + mCurScore;
		}

		mStatusText.setText(str);
	}
	
	public CharSequence[] getRecordsInfo(){
		Resources res = getContext().getResources();
		return mRecords.getRecordsInfo(res);
	}
	
	public boolean deleteRecords(DBHelper db, boolean selected[]){
		return mRecords.deleteRecords(db, selected);
	}
	
	public void setUpKeyMap(int key){
		switch(key){
		case 0:
			mUpKeyMap = KeyEvent.KEYCODE_C;
			break;
		case 1:
			mUpKeyMap = KeyEvent.KEYCODE_A;
			break;
		case 2:
			mUpKeyMap = KeyEvent.KEYCODE_V;
			break;
		case 3:
			mUpKeyMap = KeyEvent.KEYCODE_H;
			break;
		}
	}
	
	public void setDestroyMode(int mode){
		mDestroyMode = mode;
		mOptionChanged = true;
	}
	
	public void setDestroyNum(int num){
		mMinBlockNumToDestry = num;
		mOptionChanged = true;
	}
	
	public void setBlockNums(int numOnShortLine, int numOnLongLine){
		if(numOnShortLine == mBlockNumOnShortLine && numOnLongLine == mBlockNumOnLongLine)
			return;
		mBlockNumOnShortLine = numOnShortLine;
		mBlockNumOnLongLine = numOnLongLine;
		mOptionChanged = true;
		if(mXScreenSize > 0 && mYScreenSize > 0)
			updateAreaInfo(mXScreenSize, mYScreenSize, mXScreenSize, mYScreenSize);
	}
    
	public boolean testAndDestroyBlocks(){
		if(mGameArea.testAndDestroyBlocks(mDestroyMode, mMinBlockNumToDestry) > 0){
			mDestroyedBlock = true;
			return true;
		}
		if(mDestroyedBlock){
			nextState(Util.State.FALLING);
		}
		
		return mDestroyedBlock;
	}
	
	public void playTouchGroundSound(){
		if(mState == Util.State.FALLING){
			mSound.play(Sound.SND_TOUCH_GROUND);
		}
	}
	
	public boolean loadRecords(SharedPreferences sp, DBHelper db, Crypto crypto){
		ArrayList<Util.Record> ar = db.loadRecords(crypto);
		mRecords = new Records();
		for(Util.Record r :ar){
			mRecords.add(r);
		}
		
		return mRecords.RecordNum() > 0;
	}
	public int loadGame(SharedPreferences sp, DBHelper db, Crypto crypto){
		while(true){
			int lastState;

			lastState = loadGlobal(db); //必须在 loadBlocks前面
			if(lastState < 0){
				break;
			}
			int blockNum = loadBlocks(db);
			if(blockNum == 0){
				break;
			}
			
			mGameArea.loadFinish();
			mWaitingArea.loadFinish();
			if(mBackupBlockGrp != null)
				mBackupBlockGrp.setCenterBlock();
			
			return lastState;
		}
		return -1;
	}
	
	protected int loadGlobal(DBHelper db){
		int [] global = db.loadGlobal();
		if(global == null || global.length != 3)
			return -1;
	
		Util.Logi(String.format("loadGlobal: w=%d h=%d", global[0], global[1]));
		updateAreaInfo(global[0], global[1], 0 ,0);
		return global[2];
	}
	
	protected int loadBlocks(DBHelper db){
		int[] blocks = db.loadBlocks();
		if(blocks == null){
			return 0;
		}
		
		for(int v : blocks){
			Block b = Block.Create(v);
			if(b == null)
				continue;
			int ascription = b.getAscription();
			if(ascription == Util.BlockAscription.inGameArea){
				if(mGameArea != null){
					mGameArea.addBlock(b);
				}
			}else if(ascription == Util.BlockAscription.inDroppingBlockGrp){
				mGameArea.addBlockToBlockGrp(b);
			}else if(ascription == Util.BlockAscription.inBackupBlockGrp){
				addBlockToBackupBlockGrp(b);
			}else if(ascription >= Util.BlockAscription.inBlockGrpList_0){
				mWaitingArea.addBlock(b, ascription);
			}
		}
		return blocks.length;
	}
    
	public int saveGame(SharedPreferences sp, DBHelper db, Crypto crypto){		
		db.clear();
		mGameArea.save(db);
		if(mBackupBlockGrp != null){
			mBackupBlockGrp.save(db, Util.BlockAscription.inBackupBlockGrp);
		}
		mWaitingArea.save(db);
		db.saveGlobal(mXScreenSize, mYScreenSize, mState);
		mRecords.save(db, crypto);
		
		db.close();		
		return 0;
	}
	
	public void addBlockToBackupBlockGrp(Block d){
		if(mBackupBlockGrp == null){
			mBackupBlockGrp = new BlockGrp(null);
		}
		mBackupBlockGrp.addBlock(d);
	}
    
	public void newGame(){
		mCurScore = 0;
		mDestroyedBlockNum = 0;
		mDestroyedBlock = false;
		continueDestroyTimes = 0;
		mBackupBlockGrp = null;
		mRecords.resetScore();

		mWaitingArea.initGame();
		mGameArea.initGame();
		nextState(Util.State.RUNNING);
	}
}
