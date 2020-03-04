package com.VarietyBubble;

import java.util.HashMap;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

public class Sound {
	static public final int SND_TOUCH_GROUND = 0;
	static public final int SND_DESTROY = 1;
	static public final int SND_REVOLVE = 2;
	static public final int SND_TICK = 3;
	static public final int SND_LOSE = 4;
	static public final int SND_ROLLING = 5;
	static public final int SND_BUTT = 6;
	
	static private int rawId[]={
		R.raw.fall_ground,
		R.raw.destroy,
		R.raw.revolve,
		R.raw.tick,
		R.raw.lose,
		R.raw.rolling,
	};
	
	private SoundPool soundPool;
	int streamVolume;
	private HashMap<Integer, Integer> soundPoolMap;
	
	private boolean mOn = false;
	private Context mContext;

	public Sound(Context c){
		mContext = c;
		soundPool = new SoundPool(100, AudioManager.STREAM_MUSIC, 100); 

		soundPoolMap = new HashMap<Integer, Integer>(); 
		   
		AudioManager mgr = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		streamVolume = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
		for(int i = 0; i < rawId.length; i++)
			soundPoolMap.put(i, soundPool.load(mContext, rawId[i], i));
	}
	
	public void setSwitch(boolean on){
		if(on){
			
		}else{
			
		}
		mOn = on;
	}
	
	public void play(int snd){
		if( !mOn || snd >= rawId.length)
			return;
		
		soundPool.play(soundPoolMap.get(snd), streamVolume, streamVolume, 1, 0, 1f);
	}	
}
