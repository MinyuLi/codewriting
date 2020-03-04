package com.VarietyBubble;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

public class GameArea implements BlockCallback {
	private Block[][] mBlocks;
	private boolean[][] mBlockTouched;
	private BlockGrp mBlockGrp = null;
	private Rect mRect = new Rect();
	private int mXBlockCount = -1, mYBlockCount = -1;
	//private int mXOffset, mYOffset;
	private int mDroppingBlockNum = 0; //经过消球后，需要下降的球的个数
	private boolean mDestroyFinished = true;
	private int mDestroyBlockNum = 0;
	private int mFrameLeft, mFrameRight, mFrameTop, mFrameBottom; //绘制矩形框
	private Paint mLinePaint = new Paint();
	private int mLineWidth = 2;

	private AreaCallback mNotify; //消除循环依赖

	public GameArea(AreaCallback callback, Rect rect, int xBlockCount, int yBlockCount){
		mNotify = callback;
		mLineWidth = Math.min(rect.left+rect.right, rect.top+rect.bottom) / 160;
		if(mLineWidth <= 0){
			mLineWidth = 2;
		}
		mLinePaint.setARGB(255, 128, 128, 128);
		mLinePaint.setStyle(Paint.Style.STROKE);
		mLinePaint.setStrokeWidth(mLineWidth);

		updateGameArea(rect, xBlockCount, yBlockCount);
	}

	public void updateGameArea(Rect rect, int xBlockCount, int yBlockCount){
		if(xBlockCount == mXBlockCount && yBlockCount == mYBlockCount){
			return;
		}
		if(xBlockCount <= 0 || yBlockCount <= 0){
			Log.e("VarietyBubble", String.format("updateGameArea:error! [%d,%d]", xBlockCount, yBlockCount));
			return;
		}
		mRect.set(rect);
		mXBlockCount = xBlockCount;
		mYBlockCount = yBlockCount;

		//对边框的坐标进行修正，为了使边框线条的中心线处于mRect的位置，做如下调整
		mFrameLeft = rect.left - mLineWidth/2;
		mFrameTop = rect.top - mLineWidth/2;
		mFrameRight = rect.right + mLineWidth/2;
		mFrameBottom = rect.bottom + mLineWidth/2;
		if((mFrameLeft + mFrameRight) % 2 != 0){
			mFrameRight++; //确保左右对称
		}
		if((mFrameTop + mFrameBottom) % 2 != 0 ){
			mFrameBottom++; //确保上下对称
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
			//X轴积木数变多
			newX = (tmpBlocks.length - mBlocks.length)/2;
			oldX = 0;
		}else{
			//X轴积木数变少,则扔掉一些block
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

	public void revolveScreen(boolean isScreenLandScape){
		int tmp;

		tmp = mXBlockCount;
		mXBlockCount = mYBlockCount;
		mYBlockCount = tmp;

		//重新计算每个block的坐标
		Block[][] tmpBlocks = new Block[mXBlockCount][mYBlockCount];
		boolean[][] tmpBlockTouched = new boolean[mXBlockCount][mYBlockCount];
		if(isScreenLandScape){
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

		Rect tmpRect = new Rect();
		tmpRect.left = mRect.top;
		tmpRect.top = mRect.left;
		tmpRect.right = mRect.bottom;
		tmpRect.bottom = mRect.right;
		mRect.set(tmpRect);

		tmp = mFrameLeft;
		mFrameLeft = mFrameTop;
		mFrameTop = tmp;

		tmp = mFrameRight;
		mFrameRight = mFrameBottom;
		mFrameBottom = tmp;

		setBlockGrpToBackup();
	}

	private void setBlockGrpToBackup(){
		if(mBlockGrp != null){
			mBlockGrp.backToInitialState();
			mNotify.setBackupBlockGrp(mBlockGrp);
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
		//消除循环依赖 mBlocks[x][y].setFather(this);
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

	public void blockExplodeFinished(int x, int y){
		mBlocks[x][y]=null;
		if(mDestroyBlockNum > 0)
			mDestroyBlockNum --;
		if(mDestroyBlockNum == 0)
			mDestroyFinished = true;
	}

	public void blockMoveFinished(){
		if(mDroppingBlockNum > 0)
			mDroppingBlockNum--;//减保护

		//球多的时候响声太多，减少一些响声
		if(mDroppingBlockNum < 4 || (mDroppingBlockNum & 1) == 0)
			mNotify.playTouchGroundSound();
	}

	public BlockGrp.Point getDroppingGrpPosition(){
		if(mBlockGrp == null)
			return null;
		BlockGrp.Point pt = new BlockGrp.Point((int)(mRect.left + (mBlockGrp.getXCenter()+0.5)*Block.getBlockSize()),
		    (int)(mRect.top + (mBlockGrp.getYCenter()+0.5)*Block.getBlockSize()));

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
					mNotify.updateScore(sameBlockNum, destroyMode);
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
		canvas.drawLine(mFrameLeft, mFrameTop, mFrameRight, mFrameTop,    mLinePaint);
		canvas.drawLine(mFrameRight, mFrameTop, mFrameRight, mFrameBottom, mLinePaint);
		canvas.drawLine(mFrameRight, mFrameBottom, mFrameLeft, mFrameBottom, mLinePaint);
		canvas.drawLine(mFrameLeft, mFrameBottom, mFrameLeft, mFrameTop,    mLinePaint);

		if(mBlockGrp != null){
			mBlockGrp.draw(canvas, paint, mRect.left, mRect.top);
		}

		for(int x = 0; x < mXBlockCount; x++){
			for(int y = mYBlockCount-1; y >= 0 ; y--){
				if(mBlocks[x][y] != null){
					Block.DrawResult dr = mBlocks[x][y].draw(canvas, paint, mRect.left, mRect.top);
					if(dr == Block.DrawResult.explodeFinished){
						blockExplodeFinished(mBlocks[x][y].getX(), mBlocks[x][y].getY());
					}else if(dr == Block.DrawResult.moveFinished){
						blockMoveFinished();
					}else if(dr == Block.DrawResult.beginMove){
						moveBlockTo(x, y, mBlocks[x][y]);
					}
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
					int dbItem = mBlocks[x][y].getDbItem(Util.BlockAscription.inGameArea);
					db.saveBlock(dbItem);
					//mBlocks[x][y].save(db, DBHelper.BlockAscription.inGameArea);
				}
			}
		}

		if(mBlockGrp != null){
			mBlockGrp.save(db, Util.BlockAscription.inDroppingBlockGrp);
		}
	}
}
