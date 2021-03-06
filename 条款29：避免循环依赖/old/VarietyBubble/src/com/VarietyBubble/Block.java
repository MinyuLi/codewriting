package com.VarietyBubble;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class Block {
	static private int sColorNum = 5, sShapeNum = 5;
	static private boolean sColorRandom = true, sShapeRandom = true;
	static private boolean sIsSimpleMode = true;
	static public void setRandomMode(boolean simpleMode, boolean colorRandom, boolean shapeRandom)
	{
		sIsSimpleMode = simpleMode;
		sColorRandom = colorRandom;
		sShapeRandom = shapeRandom;
	}
	
	private final int mColor;
	private final int mShape;
	private int mX, mY;
	private int mDestinationY = -1; //需要移动时，目的 Y 坐标
	private float mCurrentY;
	private int mDropStep = 0; //每次下降的长度
	private int mDying = 0;
	private boolean mDropFinished = true;
	
	private GameArea mFather = null;
	
	static public int getBlockSize()
	{
		return sBlockSize;
	}
	
	static public int getSmallBlockSize(){
		return sSmallSize;
	}
	
	static private int sBlockSize = 1;
	static private int sSmallSize = 1;
	static private Bitmap[][] sBitMapArray; 
	static private Bitmap[][] sSmallBlockBitMapArray;
	static private Bitmap[][] mExplode;
	static public boolean loadBitMap(Resources r, int blockSize)
	{
		if(blockSize <= 0)
			blockSize = 1;
		
		if(blockSize == sBlockSize)
			return true; //已经load过了
		
		sBlockSize = blockSize;
		sSmallSize = sBlockSize*9 /10;
		
		if(sBlockSize <= 0)
			sBlockSize = 1;
		
		if(sSmallSize <= 0)
			sSmallSize = 1;
		
		sBitMapArray = new Bitmap[sColorNum][sShapeNum];
		sSmallBlockBitMapArray = new Bitmap[sColorNum][sShapeNum];
		doLoadBitMap(0, 0, r.getDrawable(R.drawable.red_0));
		doLoadBitMap(1, 0, r.getDrawable(R.drawable.yellow_0));
		doLoadBitMap(2, 0, r.getDrawable(R.drawable.green_0));
		doLoadBitMap(3, 0, r.getDrawable(R.drawable.blue_0));
		doLoadBitMap(4, 0, r.getDrawable(R.drawable.purple_0));
		
		doLoadBitMap(0, 1, r.getDrawable(R.drawable.red_1));
		doLoadBitMap(1, 1, r.getDrawable(R.drawable.yellow_1));
		doLoadBitMap(2, 1, r.getDrawable(R.drawable.green_1));
		doLoadBitMap(3, 1, r.getDrawable(R.drawable.blue_1));
		doLoadBitMap(4, 1, r.getDrawable(R.drawable.purple_1));
		
		doLoadBitMap(0, 2, r.getDrawable(R.drawable.red_2));
		doLoadBitMap(1, 2, r.getDrawable(R.drawable.yellow_2));
		doLoadBitMap(2, 2, r.getDrawable(R.drawable.green_2));
		doLoadBitMap(3, 2, r.getDrawable(R.drawable.blue_2));
		doLoadBitMap(4, 2, r.getDrawable(R.drawable.purple_2));
		
		doLoadBitMap(0, 3, r.getDrawable(R.drawable.red_3));
		doLoadBitMap(1, 3, r.getDrawable(R.drawable.yellow_3));
		doLoadBitMap(2, 3, r.getDrawable(R.drawable.green_3));
		doLoadBitMap(3, 3, r.getDrawable(R.drawable.blue_3));
		doLoadBitMap(4, 3, r.getDrawable(R.drawable.purple_3));
		
		doLoadBitMap(0, 4, r.getDrawable(R.drawable.red_4));
		doLoadBitMap(1, 4, r.getDrawable(R.drawable.yellow_4));
		doLoadBitMap(2, 4, r.getDrawable(R.drawable.green_4));
		doLoadBitMap(3, 4, r.getDrawable(R.drawable.blue_4));
		doLoadBitMap(4, 4, r.getDrawable(R.drawable.purple_4));
		
		//创建爆炸效果bitmap
		mExplode = new Bitmap[sColorNum][3];
		mExplode[0][0] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_red_1));
		mExplode[0][1] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_red_2));
		mExplode[0][2] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_red_3));
		
		mExplode[1][0] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_yellow_1));
		mExplode[1][1] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_yellow_2));
		mExplode[1][2] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_yellow_3));
		
		mExplode[2][0] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_green_1));
		mExplode[2][1] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_green_2));
		mExplode[2][2] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_green_3));
		
		mExplode[3][0] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_blue_1));
		mExplode[3][1] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_blue_2));
		mExplode[3][2] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_blue_3));
		
		mExplode[4][0] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_purple_1));
		mExplode[4][1] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_purple_2));
		mExplode[4][2] = doLoadOneBitMap(r.getDrawable(R.drawable.explode_purple_3));

		
		return true;
	}
	
	static private Bitmap doLoadOneBitMap(Drawable rsc){
		Bitmap bitmap = Bitmap.createBitmap(sBlockSize, sBlockSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		rsc.setBounds(0, 0, sBlockSize, sBlockSize);
		rsc.draw(canvas);
		return bitmap;
	}
	
	static private void doLoadBitMap(int color, int shape, Drawable tile) 
	{
		Bitmap bitmap = Bitmap.createBitmap(sBlockSize, sBlockSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		tile.setBounds(0, 0, sBlockSize, sBlockSize);
		tile.draw(canvas);
		
		sBitMapArray[color][shape] = bitmap;
		
		bitmap = Bitmap.createBitmap(sSmallSize, sSmallSize, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		tile.setBounds(0, 0, sSmallSize, sSmallSize);
		tile.draw(canvas);
		
		sSmallBlockBitMapArray[color][shape] = bitmap;
	}
	
	public Block()
	{
		if(sIsSimpleMode){
			mColor = Util.RandomInt(sColorNum);
			mShape = mColor;
		}else{
			mColor = sColorRandom ? Util.RandomInt(sColorNum) : 0;
			mShape = sShapeRandom ? Util.RandomInt(sShapeNum) : 0;
		}
		mX = mY = -1;
	}
	
	private Block(int x, int y, int color, int shape){
		mX = x;
		mY = y;
		mColor = color;
		mShape = shape;
	}
	
	public static Block Create(int x, int y, int color, int shape){
		if(color >= sColorNum || shape >= sShapeNum){
			return null;
		}
		
		return new Block(x, y, color, shape);		
	}
	
	public void setFather(GameArea father)
	{
		mFather = father;
	}
	
	public int getX()
	{
		return mX;
	}
	
	public int getY()
	{
		return mY;
	}
	
	public void setPosition(int x, int y)
	{
		mX = x;
		mY = y;
	}
	
	public void move(int xOffset, int yOffset)
	{
		mX += xOffset;
		mY += yOffset;
	}
	
	public boolean dropTo(int yOffset)
	{
		if(yOffset <= 0 || mDestinationY > 0)
			return false;
		mDestinationY = mY + yOffset;
		mCurrentY = mY;
		mDropFinished = false;
		return true;
	}
	
	public boolean isColorSame(Block d)
	{
		if(mColor == d.mColor)
			return true;
		return false;
	}
	
	public void goToDie(){
		mDying = 1;
	}
	
	private void drawExplode(Canvas canvas, Paint paint, int xOffset, int yOffset){
		if(mDying <= mExplode[0].length){
			canvas.drawBitmap(mExplode[mColor][mDying-1], 
	        		xOffset + mX * sBlockSize,
	        		yOffset + mY * sBlockSize,
	        		paint);
			mDying++;
		}else{
			mFather.notifyExplodeFinished(mX, mY);
		}
	}

	public boolean isShapeSame(Block d)
	{
		if(mShape == d.mShape)
			return true;
		return false;
	}
	
	public void draw(Canvas canvas, Paint paint, int xOffset, int yOffset)
	{		
		if(mDying > 0){
			drawExplode(canvas, paint, xOffset, yOffset);
			return;
		}
		if(mDestinationY > 0){
			mCurrentY += 0.2 * Math.pow(mDropStep, 2);
			mDropStep++;
			if(mCurrentY >= mDestinationY){
				mDestinationY = -1;
				mDropStep = 0;
				mFather.notifyMoveFinished();
			}
		}
		
		float fY = mDestinationY > 0 ? mCurrentY : mY;
		canvas.drawBitmap(sBitMapArray[mColor][mShape], 
        		xOffset + mX * sBlockSize,
        		yOffset + fY * sBlockSize,
        		paint);
		
		if(mFather != null && !mDropFinished){
			int originalY = mY;
			mY = mDestinationY;
			mFather.moveBlockTo(mX, originalY, this);
			mDropFinished = true;
		}
	}
	
	public void drawSmallSize(Canvas canvas, Paint paint, int xOffset, int yOffset)
	{
		canvas.drawBitmap(sSmallBlockBitMapArray[mColor][mShape], 
        		xOffset + mX * sSmallSize,
        		yOffset + mY * sSmallSize,
        		paint);
	}
	
	public void save(DBHelper db, int ascription){
		if(mDying != 0)
			return;
		db.saveBlock(mX, mY, mColor, mShape, ascription);
	}
}
