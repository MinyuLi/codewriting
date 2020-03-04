package com.VarietyBubble;

import android.content.SharedPreferences;
import android.util.Log;

public class OptionData {
	private SharedPreferences mSp;

	public static final int SOUND = 0;
	public static final int NAME = 1;
	public static final int COLOR_SHAPE = 2;
	public static final int DESTROY_MODE = 3;
	public static final int DESTROY_NUM = 4 ;
	public static final int BLOCK_NUM_ON_SHORT_LINE = 5;
	public static final int BLOCK_NUM_ON_LONG_LINE = 6;
	public static final int UP_KEY_MAP = 7;

	private V mV[] = new V[8];

	public class V{
		String sv;  //以字符串形势表示的配置值
		int iv;     //以整数形式表示的配置值
		String key; //关键字
		String dv;  //默认值
		boolean nl; //是否需要License
	}

	public OptionData(SharedPreferences sp){
		mSp = sp;

		for(int i = 0 ; i < mV.length; i++){
			mV[i] = new V();
		}

		mV[SOUND].key = Util.OptKey.SOUND;
		mV[SOUND].dv  = Util.OptDft.SOUND;
		mV[SOUND].nl  = false;

		mV[NAME].key = Util.OptKey.NAME;
		mV[NAME].dv  = Util.OptDft.NAME;
		mV[NAME].nl  = false;

		mV[COLOR_SHAPE].key = Util.OptKey.COLOR_SHAPE;
		mV[COLOR_SHAPE].dv  = Integer.toString(Util.OptDft.COLOR_SHAPE);
		mV[COLOR_SHAPE].nl  = false;

		mV[DESTROY_MODE].key = Util.OptKey.DESTROY_MODE;
		mV[DESTROY_MODE].dv  = Integer.toString(Util.OptDft.DESTROY_MODE);
		mV[DESTROY_MODE].nl  = false;

		mV[DESTROY_NUM].key = Util.OptKey.DESTROY_NUM;
		mV[DESTROY_NUM].dv  = Integer.toString(Util.OptDft.DESTROY_NUM);
		mV[DESTROY_NUM].nl  = true;

		mV[BLOCK_NUM_ON_SHORT_LINE].key = Util.OptKey.BLOCK_NUM_ON_SHORT_LINE;
		mV[BLOCK_NUM_ON_SHORT_LINE].dv  = Integer.toString(Util.OptDft.BLOCK_NUM_ON_SHORT_LINE);
		mV[BLOCK_NUM_ON_SHORT_LINE].nl  = true;

		mV[BLOCK_NUM_ON_LONG_LINE].key = Util.OptKey.BLOCK_NUM_ON_LONG_LINE;
		mV[BLOCK_NUM_ON_LONG_LINE].dv  = Integer.toString(Util.OptDft.BLOCK_NUM_ON_LONG_LINE);
		mV[BLOCK_NUM_ON_LONG_LINE].nl  = true;

		mV[UP_KEY_MAP].key = Util.OptKey.UP_KEY_MAP;
		mV[UP_KEY_MAP].dv  = Integer.toString(Util.OptDft.UP_KEY_MAP);
		mV[UP_KEY_MAP].nl  = false;

		loadData();
	}

	public void setStringValue(String key, String value){
		for(int i = 0; i < mV.length; i++){
			if(mV[i].key.compareTo(key) == 0){
				mV[i].sv = value;
				try{
					mV[i].iv = Integer.valueOf(value);
				}catch(Exception e){
					mV[i].iv = Integer.MIN_VALUE;
				}
				break;
			}
		}
	}

	public void setIntValue(String key, Integer value){
		for(int i = 0; i < mV.length; i++){
			if(mV[i].key.compareTo(key) == 0){
				mV[i].iv = value;
				mV[i].sv = value.toString();
				break;
			}
		}
	}

	private void loadData(){
		for(int i = 0; i < mV.length; i++){
			String sv;
			if(mV[i].nl && ! Util.HasLicense()){
				sv = mV[i].dv;
			}else{
				if(i == SOUND){
					sv = getBooleanFromPreference(mV[i].key, mV[i].dv);
				}else{
					sv = getStringFromPreference(mV[i].key, mV[i].dv);
				}
			}
			if(i == BLOCK_NUM_ON_SHORT_LINE){
				Log.i(Util.TAG, String.format("key=%s, dv=%s, v = %s",
				    mV[i].key, mV[i].dv, sv));
			}
			mV[i].sv = sv;
			try{
				mV[i].iv = Integer.valueOf(sv);
			}catch(Exception e){
				mV[i].iv = Integer.MIN_VALUE;
			}
		}
	}

	public int calcDifficulty(){
		int difficulty = 0;
		int color_shape = getIntValue(COLOR_SHAPE);
		int destroy_mode = getIntValue(DESTROY_MODE);
		int destroy_num = getIntValue(DESTROY_NUM);
		int short_num = getIntValue(BLOCK_NUM_ON_SHORT_LINE);
		int long_num = getIntValue(BLOCK_NUM_ON_LONG_LINE);

		if(color_shape == 0){
			difficulty += 2;
		}else {
			if(destroy_mode == 2){
				difficulty += 12;
			}else if(destroy_mode == 1){
				difficulty += 5;
			}else if(destroy_mode == 0){
				difficulty += 4;
			}
		}

		if(destroy_mode == 2)
			difficulty += 3* Math.pow(destroy_num-2, 2);
		else
			difficulty += 1* Math.pow(destroy_num-2, 2);

		difficulty -= (short_num-13);
		if(long_num > 0)
			difficulty -= (long_num-17);

		if(difficulty <= 0)
			difficulty = 1; //保护,难度最低为0.1

		return difficulty;
	}

	public int getIntValue(int iKey){
		if(iKey >= mV.length)
			return Integer.MIN_VALUE;

		return mV[iKey].iv;
	}

	public String getStringValue(int iKey){
		if(iKey >= mV.length)
			return "";

		return mV[iKey].sv;
	}

	protected int getIntFromPreference(String key, Integer defValue){
		if(mSp == null){
			return defValue;
		}
		String s = mSp.getString(key, defValue.toString());
		try{
			int iValue = Integer.valueOf(s);
			return iValue;
		}catch(Exception e){

		}
		return defValue;
	}

	protected String getStringFromPreference(String key, String defValue){
		if(mSp == null){
			Util.Loge("mSp is null");
			return defValue;
		}
		return mSp.getString(key, defValue);
	}

	protected String getBooleanFromPreference(String key, String defValue){
		if(mSp == null){
			return defValue;
		}
		boolean dft = defValue.compareTo("true") == 0 ? true : false;
		return (mSp.getBoolean(key, dft) == true) ? "true" : "false";
	}
}
