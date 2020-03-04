package com.VarietyBubble;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class WaitingArea {
	private ArrayList<BlockGrp> mBlockGrpList = new ArrayList<BlockGrp>();
	private int mBlockGrpNum = 0;
	private int mRollingOffset = 0;
	private Rect mRect = new Rect();
	private int mXOffset, mYOffset;
	private boolean mIsRolling = false;

	public WaitingArea(int blockGrpNum, Rect rect, boolean isScreenLandScape){
		mBlockGrpNum = blockGrpNum;
		setRect(rect, isScreenLandScape);
	}

	public void setBlockGrpNum(int num){
		if(num == mBlockGrpNum)
			return;

		if(num > mBlockGrpNum){
			for(BlockGrp bg : mBlockGrpList){
				bg.backToInitialState();
			}
			for(int i = 0; i < num - mBlockGrpNum; i++){
				mBlockGrpList.add(new BlockGrp());
			}
		}else{
			for(int i = 0; i < mBlockGrpNum - num ; i++){
				if(mBlockGrpList.size() > 0)
					mBlockGrpList.remove(mBlockGrpList.size()-1);
			}
			for(BlockGrp bg : mBlockGrpList){
				bg.backToInitialState();
			}
		}

		mBlockGrpNum = num;
	}

	public void setRect(Rect rect, boolean isScreenLandScape){
		mRect.set(rect);
		int blockSize = Block.getSmallBlockSize();

		//n个BlockGrp就有(n+1)个空隙
		if(isScreenLandScape){
			mXOffset = mRect.left + (mRect.width() - (3*blockSize))/(1+1);
			mYOffset = mRect.top + (mRect.height() - (mBlockGrpNum * 3* blockSize))/(mBlockGrpNum+1);
		}else{
			mXOffset = mRect.left + (mRect.width() - (mBlockGrpNum * 3*blockSize))/(mBlockGrpNum+1);
			mYOffset = mRect.top + (mRect.height() - (3* blockSize))/(1+1);
		}
	}

	public void initGame(){
		mBlockGrpList.clear();
		for(int i = 0; i < mBlockGrpNum; i++){
			mBlockGrpList.add(new BlockGrp());
		}
	}

	public BlockGrp getBlockGrp(){
		if(mBlockGrpList.size() != mBlockGrpNum){
			int correct = mBlockGrpNum;
			mBlockGrpNum = mBlockGrpList.size();
			setBlockGrpNum(correct);
		}

		BlockGrp bg = mBlockGrpList.get(0);
		mBlockGrpList.add(new BlockGrp());

		return bg;
	}

	protected void stopRolling(){
		if(mBlockGrpList.size() > 0)
			mBlockGrpList.remove(0);

		mRollingOffset = 0;
		mIsRolling = false;
	}

	public void startRolling(){
		if(mIsRolling)
			return;
		mIsRolling = true;
		mRollingOffset = 0;
	}

	public boolean isRollingFinished(){
		return ! mIsRolling;
	}

	public void draw(Canvas canvas, Paint paint, boolean isScreenLandScape){
		int xOffset = mXOffset;
		int yOffset = mYOffset;
		int blockSize = Block.getSmallBlockSize();
		int rollingStep = blockSize*2 / 3;

		if( mIsRolling && mRollingOffset > blockSize * 3){
			stopRolling();
			rollingStep = 0;
		}

		if(isScreenLandScape){
			for(BlockGrp bg : mBlockGrpList){
				bg.drawSmallSize(canvas, paint, xOffset, yOffset - mRollingOffset);
				yOffset += (blockSize*3+mYOffset);
			}
		}else{
			xOffset = mRect.right - mXOffset - blockSize * 3;
			for(BlockGrp bg : mBlockGrpList){
				bg.drawSmallSize(canvas, paint, xOffset + mRollingOffset, yOffset);
				xOffset -= (blockSize*3+mXOffset);
			}
		}

		if(mIsRolling)
			mRollingOffset += rollingStep;
	}

	public void save(DBHelper db){
		int pos = mIsRolling ? 1 : 0;
		int ascription = Util.BlockAscription.inBlockGrpList_0;

		for( ; pos < mBlockGrpList.size(); pos++){
			mBlockGrpList.get(pos).save(db, ascription);
			ascription++;
		}
	}

	public boolean addBlock(Block b, int ascription){
		int pos = ascription - Util.BlockAscription.inBlockGrpList_0;
		if(pos < 0 || pos >= mBlockGrpNum)
			return false;

		if(mBlockGrpList.size() == 0){
			for(int i = 0; i < mBlockGrpNum; i++){
				mBlockGrpList.add(new BlockGrp(null));
			}
		}

		mBlockGrpList.get(pos).addBlock(b);
		return true;
	}

	public void loadInit(){
		mBlockGrpList.clear();
	}

	public void loadFinish(){
		for(BlockGrp bg : mBlockGrpList){
			bg.setCenterBlock();
		}
	}
}
