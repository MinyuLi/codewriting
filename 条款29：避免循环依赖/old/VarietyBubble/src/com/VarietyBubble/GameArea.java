package com.VarietyBubble;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

public class GameArea {
	private Block[][] mBlocks;
	private boolean[][] mBlockTouched;
	private BlockGrp mBlockGrp = null;
	private int mXBlockCount = -1, mYBlockCount = -1;
	private int mXOffset, mYOffset;
	private int mDroppingBlockNum = 0; //经过消球后，需要下降的球的个数
	private boolean mDestroyFinished = true;
	private int mDestroyBlockNum = 0;
	private int mXLeft, mXRight, mYTop, mYBottom;
	private Paint mLinePaint = new Paint();
	private int mLineWidth = 2;
	
	private GameView mFather;
	
	public GameArea(GameView view, Rect rect, int xBlockCount, int yBlockCount){
		mFather = view;
		
		updateGameArea(rect, xBlockCount, yBlockCount);
		
		mLinePaint.setARGB(255, 128, 128, 128);
		mLinePaint.setStyle(Paint.Style.STROKE);
		mLinePaint.setStrokeWidth(mLineWidth);
	}
	
	public void updateGameArea(Rect rect, int xBlockCount, int yBlockCount){
		if(xBlockCount == mXBlockCount && yBlockCount == mYBlockCount){
			return;
		}
		if(xBlockCount <= 0 || yBlockCount <= 0){
			Log.e("VarietyBubble", String.format("updateGameArea:error! [%d,%d]", xBlockCount, yBlockCount));
			return;
		}
		
		mXOffset = rect.left;
		mYOffset = rect.top;
		mXBlockCount = xBlockCount;
		mYBlockCount = yBlockCount;
		
		mXLeft = rect.left - mLineWidth/2;
		mYTop = rect.top - mLineWidth/2;
		mXRight = rect.right + mLineWidth/2;
		mYBottom = rect.bottom + mLineWidth/2;
		if((mXLeft + mXRight) % 2 != 0){
			mXRight++; //确保左右对称
		}
		if((mYTop + mYBottom) % 2 != 0 ){
			mYBottom++; //确保上下对称
		}
		
		Block[][] tmpBlocks = new Block[xBlockCount][yBlockCount];
		boolean[][] tmpBlockTouched = new boolean[xBlockCount][yBlockCount];
		if(mBlocks == null || mBlockTouched == null){
			mBlocks = tmpBlocks;
			mBlockTouched = tmpBlockTouched;
			return;
		}
		
		int newX, newY, oldX, oldY;
		if(tmpBlocks.length > mBlocks.length){
			//X变大了
			newX = (tmpBlocks.length - mBlocks.length)/2;
			oldX = 0;
		}else{
			//X变小了,扔掉一些block
			oldX = (mBlocks.length - tmpBlocks.length)/2;
			newX = 0;
		}
		
		for( ; oldX < mBlocks.length && newX < tmpBlocks.length; oldX++, newX++){
			for(oldY = mBlocks[0].length-1, newY = tmpBlocks[0].length-1;
					oldY >= 0 && newY >= 0; oldY--, newY--)
			{
				tmpBlocks[newX][newY] = mBlocks[oldX][oldY];
				if(tmpBlocks[newX][newY] != null)
					tmpBlocks[newX][newY].setPosition(newX, newY);
				tmpBlockTouched[newX][newY] = mBlockTouched[oldX][oldY];
			}
		}
		
		mBlocks = tmpBlocks;
		mBlockTouched = tmpBlockTouched;
		
		setBlockGrpToBackup();
	}
	
	public void revolveScreen(){
		int tmp;
		
		tmp = mXBlockCount;
		mXBlockCount = mYBlockCount;
		mYBlockCount = tmp;
		
		Block[][] tmpBlocks = new Block[mXBlockCount][mYBlockCount];
		boolean[][] tmpBlockTouched = new boolean[mXBlockCount][mYBlockCount];
		
		if(mFather.isScreenLandScape()){
			for(int x = 0; x < mXBlockCount; x++){
				for(int y = 0; y < mYBlockCount; y++){
					tmpBlocks[x][y] = mBlocks[mYBlockCount-y-1][x];
					tmpBlockTouched[x][y] = mBlockTouched[mYBlockCount-y-1][x];
					if(tmpBlocks[x][y] != null)
						tmpBlocks[x][y].setPosition(x, y);
				}
			}
		}else{
			for(int x = 0; x < mXBlockCount; x++){
				for(int y = 0; y < mYBlockCount; y++){
					tmpBlocks[x][y] = mBlocks[y][mXBlockCount-x-1];
					tmpBlockTouched[x][y] = mBlockTouched[y][mXBlockCount-x-1];
					if(tmpBlocks[x][y] != null)
						tmpBlocks[x][y].setPosition(x, y);
				}
			}
		}

		mBlocks = tmpBlocks;
		mBlockTouched = tmpBlockTouched;		
		
		tmp = mXOffset;
		mXOffset = mYOffset;
		mYOffset = tmp;
		
		tmp = mXLeft;
		mXLeft = mYTop;
		mYTop = tmp;
		
		tmp = mXRight;
		mXRight = mYBottom;
		mYBottom = tmp;
		
		setBlockGrpToBackup();
	}
	
	private void setBlockGrpToBackup(){
		if(mBlockGrp != null){
			mBlockGrp.backToInitialState();
			mFather.setBackupBlockGrp(mBlockGrp);
			mBlockGrp = null;
		}
	}
	
	public boolean setBlockGrp(BlockGrp blockGrp){
		blockGrp.SetFather(this);
		if( ! blockGrp.move(mXBlockCount/2-1, 0))
			return false;
		
		mBlockGrp = blockGrp;
		return true;
	}
	
	public boolean addBlockToBlockGrp(Block b){
		if( ! isPositionEmpty(b.getX(), b.getY()))
			return false;
		
		if(mBlockGrp == null){
			mBlockGrp = new BlockGrp(this);	
		}
		mBlockGrp.addBlock(b);
		return true;
	}
	
	public void loadFinish(){
		if(mBlockGrp != null){
			if( ! mBlockGrp.setCenterBlock())
				mBlockGrp = null;
		}
	}
	
	public int blockNumInBlockGrp(){
		if(mBlockGrp == null)
			return 0;
		
		return mBlockGrp.getBlockNum();
	}
	
	public boolean isPositionEmpty(int x, int y){
		if(x < 0 || y < 0 || x >= mXBlockCount || y >= mYBlockCount)
			return false;
		
		if(mBlocks[x][y] != null)
			return false;
		
		return true;
	}
	
	private boolean isPositionValid(int x, int y){
		if(x < 0 || y < 0 || x >= mXBlockCount || y >= mYBlockCount)
			return false;
		
		if(mBlocks[x][y] == null)
			return false;
		
		if( mBlockTouched[x][y])
			return false;
		
		return true;
	}
	
	public boolean addBlock(Block block){
		int x = block.getX();
		int y = block.getY();
		if ( ! isPositionEmpty(x, y))
			return false;
		
		mBlocks[x][y] = block;
		mBlocks[x][y].setFather(this);
		return true;
	}
	
	public boolean moveBlockTo(int originalX, int originalY, Block block){
		if ( ! isPositionEmpty(block.getX(), block.getY()))
			return false;
		
		if (isPositionEmpty(originalX, originalY))
			return false;
		
		mBlocks[originalX][originalY]=null;
		mBlocks[block.getX()][block.getY()] = block;
		return true;
	}
	
	public void notifyExplodeFinished(int x, int y){
		mBlocks[x][y]=null;
		if(mDestroyBlockNum > 0)
			mDestroyBlockNum --;
		if(mDestroyBlockNum == 0)
			mDestroyFinished = true;
	}
	
	public void notifyMoveFinished(){
		if(mDroppingBlockNum > 0)
			mDroppingBlockNum--;//减保护
		
		//球多的时候响声太多，减少一些响声
		if(mDroppingBlockNum < 4 || (mDroppingBlockNum & 1) == 0)
			mFather.playTouchGroundSound();
	}
	
	public BlockGrp.Point getDroppingGrpPosition(){
		if(mBlockGrp == null)
			return null;
		BlockGrp.Point pt = new BlockGrp.Point((int)(mXOffset + (mBlockGrp.getXCenter()+0.5)*Block.getBlockSize()),
				(int)(mYOffset + (mBlockGrp.getYCenter()+0.5)*Block.getBlockSize()));
		
		//Global.Logi2("offset:{%d,%d}", mXOffset, mYOffset);
		//Global.Logi2("center:{%d,%d}", (int)mBlockGrp.getXCenter(), (int)mBlockGrp.getYCenter());
		return pt;
	}
	
	public int downOneStep(){
		if(mBlockGrp == null)
			return 0;
		
		return mBlockGrp.downOneStep();
	}
	
	public boolean leftOneStep(){
		if(mBlockGrp == null)
			return false;
		return mBlockGrp.leftOneStep();
	}
	
	public boolean rightOneStep(){
		if(mBlockGrp == null)
			return false;
		return mBlockGrp.rightOneStep();
	}
	
	public boolean revolve(int direction){
		if(mBlockGrp == null)
			return false;
		return mBlockGrp.revolve(direction);
	}
	
	private void clearBlockTouchFlag(){
		for(int x = 0; x < mXBlockCount; x++){
			for(int y = 0; y < mYBlockCount; y++){
				mBlockTouched[x][y]= false;
			}
		}
	}
	
	private boolean isBlockSame(Block b1, Block b2, int compareMode){
		switch(compareMode){
		case Util.ONLY_COLOR:
			return b1.isColorSame(b2);
		case Util.ONLY_SHAPE:
			return b1.isShapeSame(b2);
		case Util.COLOR_AND_SHAPE:
			return b1.isColorSame(b2) && b1.isShapeSame(b2);
		}
		return false;
	}
	
	private int destroySomeBlockFromPosition(int x, int y, int destroyMode, Block originalBlock){
		int blockNum = 0;
		if( ! isPositionValid(x,y))
			return 0;
		
		mBlockTouched[x][y] = true;
		if( ! isBlockSame(mBlocks[x][y], originalBlock, destroyMode))
			return 0;
		
		blockNum += destroySomeBlockFromPosition(x+1, y, destroyMode, originalBlock);
		blockNum += destroySomeBlockFromPosition(x-1, y, destroyMode, originalBlock);
		blockNum += destroySomeBlockFromPosition(x, y+1, destroyMode, originalBlock);
		blockNum += destroySomeBlockFromPosition(x, y-1, destroyMode, originalBlock);
		
		destropBlock(x, y);
		return blockNum+1;
	}
	
	private int calcSameBlockFromPosition(int x, int y, int destroyMode, Block originalBlock){
		int blockNum = 0;
		if( ! isPositionValid(x,y))
			return 0;
		
		mBlockTouched[x][y] = true;
		if( ! isBlockSame(mBlocks[x][y], originalBlock, destroyMode))
			return 0;
		
		blockNum += calcSameBlockFromPosition(x+1, y, destroyMode, originalBlock);
		blockNum += calcSameBlockFromPosition(x-1, y, destroyMode, originalBlock);
		blockNum += calcSameBlockFromPosition(x, y+1, destroyMode, originalBlock);
		blockNum += calcSameBlockFromPosition(x, y-1, destroyMode, originalBlock);
		
		return blockNum+1;
	}
	
	public int testAndDestroyBlocks(int destroyMode, int minBlockNumToDestry){
		int destroyedBlockNum = 0;
		
		for(int x = 0; x < mXBlockCount; x++){
			for(int y = 0; y < mYBlockCount; y++){
				if(mBlocks[x][y] == null)
					continue;
				clearBlockTouchFlag();
				int sameBlockNum = calcSameBlockFromPosition(x, y, destroyMode, mBlocks[x][y]);
				if(sameBlockNum >= minBlockNumToDestry){
					clearBlockTouchFlag();
					destroySomeBlockFromPosition(x, y, destroyMode, mBlocks[x][y]);
					mFather.updateScore(sameBlockNum, destroyMode);
					destroyedBlockNum += sameBlockNum;
					mDestroyBlockNum = destroyedBlockNum;
					mDestroyFinished = false;
					return destroyedBlockNum;
				}
			}
		}
		
		return destroyedBlockNum;
	}
	
	private int getDestY(int x, int y){
		while(isPositionEmpty(x, ++y));
		return --y;
	}
	
	public boolean dropToFillEmptyPosition(){
		if( ! mDestroyFinished)
			return false;
		boolean dropped = false;
		for(int x = 0; x < mXBlockCount; x++){
			for(int y = 0; y < mYBlockCount; y++){
				if(mBlocks[x][y] != null){
					if(isPositionEmpty(x, y+1)){
						int destY = getDestY(x, y+1);
						if (mBlocks[x][y].dropTo(destY - y))
							mDroppingBlockNum++; //避免重复计算, dropTo 返回成功才计算
							
						dropped = true;
					}
				}
			}
		}
		return dropped;
	}
	
	public boolean isDroppingFinished(){
		return (mDroppingBlockNum == 0);
	}
	
	public boolean isDestroyFinished(){
		return mDestroyFinished;
	}
	
	public void clearDroppingBlockNum(){
		mDroppingBlockNum = 0;
	}
	
	private void destropBlock(int x,  int y){
		mBlocks[x][y].goToDie();
	}
	
	public void draw(Canvas canvas, Paint paint){
		canvas.drawLine(mXLeft,  mYTop,    mXRight, mYTop,    mLinePaint);
		canvas.drawLine(mXRight, mYTop,    mXRight, mYBottom, mLinePaint);
		canvas.drawLine(mXRight, mYBottom, mXLeft,  mYBottom, mLinePaint);
		canvas.drawLine(mXLeft,  mYBottom, mXLeft,  mYTop,    mLinePaint);

		if(mBlockGrp != null){
			mBlockGrp.draw(canvas, paint, mXOffset, mYOffset);
		}
		
		for(int x = 0; x < mXBlockCount; x++){
			for(int y = mYBlockCount-1; y >= 0 ; y--){
				if(mBlocks[x][y] != null){
					mBlocks[x][y].draw(canvas, paint, mXOffset, mYOffset);
				}
			}
		}
	}
	
	public void initGame()
	{
		for(int x = 0; x < mXBlockCount; x++){
			for(int y = 0; y < mYBlockCount; y++){
				if(mBlocks[x][y] != null){
					mBlocks[x][y] = null;
				}
			}
		}
		
		mBlockGrp = null;
	}
	
	public void save(DBHelper db){
		for(int x = 0; x < mXBlockCount; x++){
			for(int y = 0; y < mYBlockCount; y++){
				if(mBlocks[x][y] != null){
					mBlocks[x][y].save(db, DBHelper.BlockAscription.inGameArea);
				}
			}
		}
		
		if(mBlockGrp != null){
			mBlockGrp.save(db, DBHelper.BlockAscription.inDroppingBlockGrp);
		}
	}
}
