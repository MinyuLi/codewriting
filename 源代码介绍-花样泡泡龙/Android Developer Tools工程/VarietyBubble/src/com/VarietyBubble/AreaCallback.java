package com.VarietyBubble;

public interface AreaCallback {
	public void setBackupBlockGrp(BlockGrp bg);
	public void playTouchGroundSound();
	public void updateScore(int destroyedBlockNum, int destroyMode);
}
