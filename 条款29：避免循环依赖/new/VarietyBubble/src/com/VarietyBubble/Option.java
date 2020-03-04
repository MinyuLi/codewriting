package com.VarietyBubble;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.WindowManager;
import android.content.Context;
import android.content.res.Resources;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

public class Option extends PreferenceActivity {
	private DestroyMode mDM = null;
	private ShortLineNum mSLN = null;
	private LongLineNum mLLN = null;
	private IntegerPreference mDN = null;
	private PreferenceCategory mDifferency = null;
	private final int LICENSE_DIALOG = 1;
	private final int USER_NAME_TOO_LONG = 2;

	private OptionData mData = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        
        mData = new OptionData(PreferenceManager.getDefaultSharedPreferences(Option.this));
        setPreferenceScreen(createSelf());
    }
	
	public void dialog(int id){
		showDialog(id);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case LICENSE_DIALOG:
			return new AlertDialog.Builder(Option.this)
			.setTitle(R.string.app_name)
			.setMessage(R.string.opt_no_license)
			.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .create();
		case USER_NAME_TOO_LONG:
			return new AlertDialog.Builder(Option.this)
			.setTitle(R.string.app_name)
			.setMessage(R.string.opt_user_name_too_long)
			.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .create();
		}
		return null;
	}

	private class NamePreference extends EditTextPreference{
		public NamePreference(Context context) {
			super(context);
			
			setDialogTitle(R.string.opt_user_name_dlg);
	        setKey(Util.OptKey.NAME);
	        setTitle(R.string.opt_user_name);
	        //setSummary(R.string.opt_user_name_summary);

        	String n = mData.getStringValue(OptionData.NAME);
        	setSummary(n);
	        setOnPreferenceChangeListener(changed);
		}
		
		//中文算2个，英文算1个
		private int getStringCharNum(CharSequence str){
			int len = str.length();
			int charNum = 0;

			for(int i = 0; i < len; i++){
				if(str.charAt(i) < 128)
					charNum += 1;
				else
					charNum += 2;
			}
			
			return charNum;
		}
		
		private Preference.OnPreferenceChangeListener changed = new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange (Preference preference, Object newValue){
				final String name = newValue.toString();
				if(getStringCharNum(name) > 12){
					dialog(USER_NAME_TOO_LONG);
					return false;
				}
				setSummary(name);
	            return true;
			}
		};
	}
	
	private class ColorShape extends ListPreference{

		public ColorShape(Context context) {
			super(context);

			setEntries(R.array.opt_color_shape);
	        setEntryValues(R.array.opt_color_shape_value);
	        setDialogTitle(R.string.opt_color_shape);
	        setKey(Util.OptKey.COLOR_SHAPE);
	        setTitle(R.string.opt_color_shape);
	        setDefaultValue(Integer.toString(Util.OptDft.COLOR_SHAPE));
	        
        	String v = mData.getStringValue(OptionData.COLOR_SHAPE);
        	updateSummary(v);
	        
	        setOnPreferenceChangeListener(changed);
		}
		
		private Preference.OnPreferenceChangeListener changed = new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange (Preference preference, Object newValue){
				return updateSummary(newValue.toString());
			}
		};
		
		private boolean updateSummary(String v){
			try{
				final int select = Integer.valueOf(v);
				Resources res = getResources();
				String[] title = res.getStringArray(R.array.opt_color_shape);
				String[] summary = res.getStringArray(R.array.opt_color_shape_summary);
				
				if(select >= title.length || select >= summary.length)
					return false;
				
				setSummary(title[select] + ": " + summary[select]);
				updateDestroyMode(v);
				updateValue(getKey(), select);
	            return true;
			}catch(Exception e){
				
			}
			return false;
		}
	}
	
	private class DestroyMode extends ListPreference{

		public DestroyMode(Context context) {
			super(context);
			setEntries(R.array.opt_destroy_mode);
	        setEntryValues(R.array.opt_destroy_mode_value);
	        setDialogTitle(R.string.opt_destroy_mode);
	        setKey(Util.OptKey.DESTROY_MODE);
	        setTitle(R.string.opt_destroy_mode);
	        setDefaultValue(Integer.toString(Util.OptDft.DESTROY_MODE));
	        
        	String v = mData.getStringValue(OptionData.DESTROY_MODE);
        	updateSummary(v);
	        
	        setOnPreferenceChangeListener(changed);
		}
		
		private Preference.OnPreferenceChangeListener changed = new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange (Preference preference, Object newValue){
				return updateSummary(newValue.toString());
			}
		};
		
		private boolean updateSummary(String v){
			try{
				final int select = Integer.valueOf(v);
				Resources res = getResources();
				String[] summary = res.getStringArray(R.array.opt_destroy_mode_summary);
				
				if(select >= summary.length)
					return false;
				
				setSummary(summary[select]);
				updateValue(getKey(), select);
	            return true;
			}catch(Exception e){
				
			}
			return false;
		}
		
	}
	
	private class IntegerPreference extends EditTextPreference{
		int mMin, mMax;
		public IntegerPreference(Context context, String title, String key, int defaultValue,
				int min, int max) {
			super(context);
			Integer dft = defaultValue;

			setKey(key);
	        setTitle(title);
	        setRange(min, max);
	        
	        
	        setDefaultValue(dft.toString());
	        setSummary(mData.getStringFromPreference(key, dft.toString()));
	        setOnPreferenceChangeListener(changed);
		}
		
		public void setRange(int min, int max){
			String dialogTitle=getResources().getString(R.string.opt_range);
			
			mMin = min;
			mMax = max;
			if(mMin > Integer.MIN_VALUE && mMax == Integer.MAX_VALUE){
	        	dialogTitle += "[" + mMin + ", ..)";
	        }else if(mMin == Integer.MIN_VALUE && mMax < Integer.MAX_VALUE){
	        	dialogTitle += "(.. ," + mMax + "]";
	        }else if(mMin > Integer.MIN_VALUE && mMax < Integer.MAX_VALUE){
	        	dialogTitle += "[" + mMin + ", " + mMax + "]";
	        }else{
	        	dialogTitle += "error!";
	        }
	        //dialogTitle = getTitle() + dialogTitle;
	        setDialogTitle(dialogTitle);
		}
		
		protected void dataChanged(int newValue){
			
		}
		
		private Preference.OnPreferenceChangeListener changed = new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange (Preference preference, Object newValue){
				if( ! Util.HasLicense()){
					dialog(LICENSE_DIALOG);
					return false;
				}
				try{
					Integer v = Integer.valueOf(newValue.toString());
					if(v >= mMin && v <= mMax){
						setSummary(v.toString());
						dataChanged(v);
						updateValue(getKey(), v);
						return true;
					}
				}catch(Exception e){
					
				}
				return false;	
			}
		}; 
	}
	
	private class ShortLineNum extends IntegerPreference{

		public ShortLineNum(Context context, String title, String key,
				int defaultValue, int min, int max) {
			super(context, title, key, defaultValue, min, max);
		}
		
		@Override
		protected void dataChanged(int newValue){
			if(mLLN != null){
				mLLN.setRange(newValue, Util.OptDft.maxLineNum);
			}
		}
	}
	
	private class LongLineNum extends IntegerPreference{

		public LongLineNum(Context context, String title, String key,
				int defaultValue, int min, int max) {
			super(context, title, key, defaultValue, min, max);
		}
		
		@Override
		protected void dataChanged(int newValue){
			if(mSLN != null){
				mSLN.setRange(Util.OptDft.minLineNum, newValue);
			}
		}
	}
	
	private void updateDestroyMode(String v){
		boolean enabled = (v.compareTo("0") == 0) ? false : true;
		if(mDM != null){
			mDM.setEnabled(enabled);
			if(enabled == false)
				mDM.setSummary("");
			else{
				String summary = mData.getStringValue(OptionData.DESTROY_MODE);
				mDM.updateSummary(summary);
			}
		}
	}
	
	private void updateValue(String key, Integer value){
		mData.setIntValue(key, value);
		updateDifficulty();
	}
	
	private void updateDifficulty(){
		String title = getResources().getString(R.string.opt_difficulty);
		int difficulty = mData.calcDifficulty();
        
		title += ": " + (double)(difficulty/10.0);
        mDifferency.setTitle(title);
	}
	
	private PreferenceScreen createSelf() {
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		
		// 用户名
		//PreferenceCategory name = new PreferenceCategory(this);
		//name.setTitle(R.string.opt_user_name);
		//root.addPreference(name);
		
		EditTextPreference nameEdit = new NamePreference(this);
		root.addPreference(nameEdit);
		
		// 音效
		//PreferenceCategory sound = new PreferenceCategory(this);
		//sound.setTitle(R.string.opt_sound);
		//root.addPreference(sound);
		
		CheckBoxPreference soundEdit = new CheckBoxPreference(this);
		soundEdit.setKey(Util.OptKey.SOUND);
		soundEdit.setTitle(R.string.opt_sound);
		soundEdit.setSummaryOn(R.string.opt_sound_summary_on);
		soundEdit.setSummaryOff(R.string.opt_sound_summary_off);
		soundEdit.setDefaultValue(Util.OptDft.SOUND.compareTo("true") == 0 ? true : false);
		root.addPreference(soundEdit);
		
		final ListPreference upKeyMap = new ListPreference(this);
		upKeyMap.setKey(Util.OptKey.UP_KEY_MAP);
		upKeyMap.setTitle(R.string.opt_upkey);
		upKeyMap.setEntries(R.array.opt_up_key_map);
		upKeyMap.setEntryValues(R.array.opt_up_key_map_value);
		upKeyMap.setDialogTitle(R.string.opt_upkey);
		upKeyMap.setDefaultValue(Integer.toString(Util.OptDft.UP_KEY_MAP));
		int select = mData.getIntFromPreference(Util.OptKey.UP_KEY_MAP, Util.OptDft.UP_KEY_MAP);
		final String[] summary = getResources().getStringArray(R.array.opt_up_key_map_summary);
		if(select < summary.length)
			upKeyMap.setSummary(summary[select]);
		upKeyMap.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange (Preference preference, Object newValue){
				try{
					int select = Integer.valueOf(newValue.toString());				
					if(select < summary.length)
						upKeyMap.setSummary(summary[select]);
					return true;
				}
				catch(Exception e){
					
				}
				return false;
			}	
		});
		root.addPreference(upKeyMap);

		// 难度
		mDifferency = new PreferenceCategory(this);
		root.addPreference(mDifferency);

		// 形状&颜色
		mDM = new DestroyMode(this);
        ListPreference color_shape = new ColorShape(this);
        try{
        	mDifferency.addPreference(color_shape);
        }catch(Exception e){
        	Log.e(Util.TAG, e.getMessage());
        }
        
        try{
        	mDifferency.addPreference(mDM);
        }catch(Exception e){
        	Log.e(Util.TAG, e.getMessage());
        }
        
        // 消球个数
        mDN = new IntegerPreference(this, 
        		getResources().getString(R.string.opt_destroy_num), 
        		Util.OptKey.DESTROY_NUM, Util.OptDft.DESTROY_NUM,
        		2, Integer.MAX_VALUE);
        mDifferency.addPreference(mDN);
        
        // 短边的球个数
        int shortLineNum = mData.getIntValue(OptionData.BLOCK_NUM_ON_SHORT_LINE);
        int longLineNum = mData.getIntValue(OptionData.BLOCK_NUM_ON_LONG_LINE);
        
        final int minBlockNum = Util.OptDft.minLineNum;
        final int maxBlockNum = Util.OptDft.maxLineNum;
        if(shortLineNum < minBlockNum){
        	Log.e(Util.TAG, String.format("shortLineNum error[%d]", shortLineNum));
        	shortLineNum = minBlockNum;
        }
        if(shortLineNum > longLineNum){
        	Log.e(Util.TAG, String.format("longLineNum error[%d, %d]", shortLineNum, longLineNum));
        	longLineNum = shortLineNum;
        }
        
        if(longLineNum > maxBlockNum){
        	Util.Logi1("longLineNum(%d) err. ", longLineNum);
        	longLineNum = maxBlockNum;
        }

        mSLN = new ShortLineNum(this, 
        		getResources().getString(R.string.opt_block_num_on_short_line), 
        		Util.OptKey.BLOCK_NUM_ON_SHORT_LINE, Util.OptDft.BLOCK_NUM_ON_SHORT_LINE, 
        		minBlockNum, longLineNum);
        mDifferency.addPreference(mSLN);
        
        // 长边的球个数
        mLLN = new LongLineNum(this,
        		getResources().getString(R.string.opt_block_num_on_long_line),
        		Util.OptKey.BLOCK_NUM_ON_LONG_LINE, longLineNum, shortLineNum, maxBlockNum);
        mDifferency.addPreference(mLLN);
        
        updateDifficulty();
		
		return root;
	}
}
