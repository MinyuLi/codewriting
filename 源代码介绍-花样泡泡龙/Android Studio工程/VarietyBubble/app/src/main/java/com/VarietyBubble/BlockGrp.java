package com.VarietyBubble;

import java.util.ArrayList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class BlockGrp {
	private ArrayList<Block> mBlockList = new ArrayList<Block>();
	private double mXCenter, mYCenter;
	private int mXOffsetInSelfAreaWhenRolling, mYOffsetInSelfAreaWhenRolling;
	private BlockCallback mNotify;

	static class Point{
		public int x, y;
		public Point(int X, int Y){
			x = X;
			y = Y;
		}
	};

	public double getXCenter(){return mXCenter;}
	public double getYCenter(){return mYCenter;}

	private static Point GrpTemplate [][] = {
		{new Point(0,0), new Point(1,0)},
		{new Point(0,0), new Point(0,1), new Point(1,1)},
		{new Point(0,0), new Point(1,0), new Point(2,0)},
		{new Point(0,0), new Point(0,1), new Point(1,1), new Point(2,1)},/*L形*/
		{new Point(0,1), new Point(1,1), new Point(2,1), new Point(1,0)},/*T形*/
		{new Point(0,0), new Point(1,0), new Point(1,1), new Point(2,1)},/*Z形*/
		{new Point(0,0), new Point(0,1), new Point(1,1), new Point(1,0)},/*方形*/
		//{new Point(0,0), new Point(0,1), new Point(1,1), new Point(2,1), new Point(2,2)},/*竖Z形*/
		//{new Point(0,0), new Point(0,1), new Point(1,1), new Point(2,1), new Point(2,0)},/*U形*/
		//{new Point(0,0), new Point(0,1), new Point(0,2), new Point(1,1), new Point(2,1), new Point(2,2)},/*h形*/
		//{new Point(0,0), new Point(0,1), new Point(0,2), new Point(1,1), new Point(2,1), new Point(2,2), new Point(2,0)},/*H形*/
	};

	public BlockGrp(){
		mNotify = null;
		int grpShape = Util.RandomInt(GrpTemplate.length);
		for(int i = 0; i < GrpTemplate[grpShape].length; i++){
			Block d = new Block();
			d.setPosition(GrpTemplate[grpShape][i].x, GrpTemplate[grpShape][i].y);
			mBlockList.add(d);
		}

		setCenterBlock();
	}

	public BlockGrp(BlockCallback father){
		mNotify = father;
	}

	public void addBlock(Block b){
		mBlockList.add(b);
	}

	public void SetFather(BlockCallback father){
		mNotify = father;
	}

	public int getBlockNum(){
		return mBlockList.size();
	}

	public boolean setCenterBlock(){
		int xMin = Integer.MAX_VALUE, yMin = Integer.MAX_VALUE;
		int xMax = Integer.MIN_VALUE, yMax = Integer.MIN_VALUE;

		if(getBlockNum() == 0){
			mXCenter = mYCenter = 0;
			return false;
		}

		for(Block b : mBlockList){
			int x = b.getX();
			int y = b.getY();
			if(xMin > x){
				xMin = x;
			}
			if(xMax < x){
				xMax = x;
			}

			if(yMin > y){
				yMin = y;
			}
			if(yMax < y){
				yMax = y;
			}
		}

		mXCenter = (xMax + xMin)*1.0 /2;
		mYCenter = (yMax + yMin)*1.0 /2;

		if(xMax-xMin != yMax - yMin){
			int XCenterTmp=0, YCenterTmp=0;
			double minDistanceToCenter = Double.MAX_VALUE;
			for(Block d : mBlockList){
				int x = d.getX();
				int y = d.getY();
				double distance = Math.pow(x-mXCenter, 2)+Math.pow(y-mYCenter, 2);

				if(distance < minDistanceToCenter){
					XCenterTmp = x;
					YCenterTmp = y;
					minDistanceToCenter = distance;
				}
			}
			mXCenter = XCenterTmp;
			mYCenter = YCenterTmp;
		}

		if(xMax-xMin+1 < 3){
			mXOffsetInSelfAreaWhenRolling = (3 - (xMax-xMin+1))*Block.getSmallBlockSize()/2;
		}else{
			mXOffsetInSelfAreaWhenRolling = 0;
		}
		if(yMax-yMin+1 < 3){
			mYOffsetInSelfAreaWhenRolling = (3 - (yMax-yMin+1))*Block.getSmallBlockSize()/2;
		}else{
			mYOffsetInSelfAreaWhenRolling = 0;
		}

		return true;
	}

	public boolean revolve(int direction){
		if(mNotify == null){
			Log.e("BlockGrp:revolve", "mFather is null");
			return false;
		}

		if(mBlockList.size() == 1)
			return false;

		ArrayList<Point> newPointList = new ArrayList<Point>();

		//判断每个 Block 是否可以旋转
		for(Block d : mBlockList){
			Point pt = revolve(direction, d.getX(), d.getY());
			if( ! mNotify.isPositionEmpty(pt.x, pt.y))
				return false; //不能转

			newPointList.add(pt);
		}

		//设置新坐标
		int pos = 0;
		int positionSameNum = 0;
		for(Block d : mBlockList){
			Point pt = newPointList.get(pos);
			if(d.getX() == pt.x && d.getY() == pt.y)
				positionSameNum++;
			else
				d.setPosition(pt.x, pt.y);
			pos++;
		}
		if(positionSameNum == pos)
			return false; //旋转以后位置没有变化，返回失败，不产生音效

		return true;
	}

	//向下取整
	private int getInteger(double v){
		//v += 0.5;
		if(v >= 0.0){
			return (int)v;
		}else{
			return (int)(v);//-1);
		}
	}

	private Point revolve(int direction, int x1, int y1){
		Point pt;
		double x = x1*1.0 - mXCenter;
		double y = y1*1.0 - mYCenter;

		switch(direction){
		default:
		case Util.CLOCKWISE: //(x1,y1)围绕(x0,y0)顺时针旋转90度
			pt = new Point(getInteger(mXCenter - y), 
					 getInteger(mYCenter + x));
			break;
		case Util.ANTICLOCKWISE:  //(x1,y1)围绕(x0,y0)逆时针旋转90度
			pt = new Point(getInteger(mXCenter + y), 
					 getInteger(mYCenter - x));
			break;
		case Util.HORIZONTAL:  //(x1,y1)围绕(x0,y0)水平旋转180度
			pt = new Point(getInteger(mXCenter - x), y1);
			break;
		case Util.VERTICAL:  //(x1,y1)围绕(x0,y0)垂直旋转180度
			pt = new Point(x1, getInteger(mYCenter - y));
			break;
		}

		return pt;
	}

	public int downOneStep(){
		if(mNotify == null){
			Log.e("BlockGrp:downOneStep", "mFather is null");
			return 0;
		}

		int maxY = -1;
		int blockNum = mBlockList.size();

		for(Block b : mBlockList){
			if(b.getY() > maxY)
				maxY = b.getY();
		}

		for(int y = maxY; y >=0; y--){
			for(Block b : mBlockList){
				if(b.getY() != y)
					continue;

				if( ! mNotify.isPositionEmpty(b.getX(), b.getY()+1)){
					mNotify.addBlock(b);
					mBlockList.remove(b);
					y++;
					break; //remove以后再遍历就不安全了，需要break后再来一遍
				}else{
					b.move(0, 1);
				}
			}
		}

		if(blockNum > mBlockList.size()){
			setCenterBlock();
		}else{
			mYCenter += 1.0;
		}
		return mBlockList.size();
	}

	public boolean leftOneStep(){
		return move(-1, 0);
	}

	public boolean rightOneStep(){
		return move(1, 0);
	}

	public void draw(Canvas canvas, Paint paint, int xOffset, int yOffset){
		for(Block d : mBlockList){
			d.draw(canvas, paint, xOffset, yOffset);
		}
	}

	public void drawSmallSize(Canvas canvas, Paint paint, int xOffset, int yOffset){
		if(mNotify != null)
			return;

		xOffset += mXOffsetInSelfAreaWhenRolling;
		yOffset += mYOffsetInSelfAreaWhenRolling;

		for(Block d : mBlockList){
			d.drawSmallSize(canvas, paint, xOffset, yOffset);
		}
	}

	public boolean move(int xOffset, int yOffset){
		if(mNotify == null){
			Log.e("BlockGrp:move", "mFather is null");
			return false;
		}

		for(Block b : mBlockList){
			if( ! mNotify.isPositionEmpty(b.getX()+xOffset, b.getY()+yOffset))
				return false;
		}
		for(Block b : mBlockList){
			b.move(xOffset, yOffset);
		}
		mXCenter += xOffset * 1.0;
		mYCenter += yOffset * 1.0;
		return true;
	}

	public void backToInitialState()
	{
		int xMin = Integer.MAX_VALUE, yMin = Integer.MAX_VALUE;
		for(Block b : mBlockList){
			int x = b.getX();
			int y = b.getY();
			if(xMin > x){
				xMin = x;
			}
			if(yMin > y){
				yMin = y;
			}
		}
		for(Block b : mBlockList){
			int x = b.getX();
			int y = b.getY();
			b.setPosition(x - xMin, y - yMin);
		}

		setCenterBlock();
	}

	public void save(DBHelper db, int ascription){
		for(Block b : mBlockList){
			int dbItem = b.getDbItem(ascription);
			db.saveBlock(dbItem);
		}
	}
}
