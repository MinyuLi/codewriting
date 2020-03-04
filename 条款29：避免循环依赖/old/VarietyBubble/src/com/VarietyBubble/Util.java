package com.VarietyBubble;

import java.util.Random;

import android.util.Log;

public final class Util {
	public static final String TAG = "VarietyBubble";
	private static Random sRandom = new Random();
	public static int RandomInt(int n){
		return sRandom.nextInt(n);
	}
	
	public static Util valueOfGr(){
		return new Util();
	}
	
	static public void Logi(String info){
		Log.i(TAG, info);
	}
	
	static public void Loge(String info){
		Log.e(TAG, info);
	}
	
	static public void Logi1(String fmt, long v){
		Log.i(TAG, String.format(fmt, v));
	}
	
	static public void Logi1(String fmt, String v){
		Log.i(TAG, String.format(fmt, v));
	}
	
	static public void Logi2(String fmt, long v1, long v2){
		Log.i(TAG, String.format(fmt, v1, v2));
	}
	
	public static boolean HasLicense(){
		return true;
		//return false;
	}
	
	//�������
	static public final int ONLY_COLOR = 1;
	static public final int ONLY_SHAPE = 2;
	//static public final int COLOR_OR_SHAPE = 3;
	static public final int COLOR_AND_SHAPE = 4;
	
	//��ת����
	public static final int CLOCKWISE = 1;     //˳ʱ��ת
	public static final int ANTICLOCKWISE = 2; //��ʱ��ת
	public static final int HORIZONTAL = 3;    //ˮƽ��ת
	public static final int VERTICAL = 4;      //��ֱ��ת
	
	//�Ի���ؼ���
	public class OptKey{
		public static final String SOUND = "sound";
		public static final String NAME = "name";
		public static final String COLOR_SHAPE = "color_shape";
		public static final String DESTROY_MODE = "destroy_mode";
		public static final String DESTROY_NUM = "destroy_num";
		public static final String BLOCK_NUM_ON_SHORT_LINE = "block_num_on_short_line";
		public static final String BLOCK_NUM_ON_LONG_LINE = "block_num_on_long_line";
		public static final String UP_KEY_MAP = "up_key_map";
	}
	
	//ÿ���Ի���ؼ��ֶ�Ӧ��Ĭ��ֵ
	public class OptDft{
		public static final String SOUND = "true";
		public static final String NAME = "anonymous";
		public static final int COLOR_SHAPE = 0;
		public static final int DESTROY_MODE = 0;
		public static final int DESTROY_NUM = 3;
		public static final int BLOCK_NUM_ON_SHORT_LINE = 13;
		public static final int BLOCK_NUM_ON_LONG_LINE = -1;
		public static final int UP_KEY_MAP = 0;
		public static final int minLineNum = 10;
		public static final int maxLineNum = 100;
	}
	
	
	//״̬
	public class State{
		public static final int PAUSE = 0;
		public static final int READY = 1;
		public static final int RUNNING = 2;
		public static final int LOSE = 3;
		public static final int DESTROYING = 4;
		public static final int FALLING = 5;
		public static final int ROLLING = 6;
	};
	
	public static boolean IsHexDigital(char c){
		if(c >= '0' && c <= '9')
			return true;
		
		if(c >= 'a' && c <= 'f')
			return true;
		
		if(c >= 'A' && c <= 'F')
			return true;
		
		return false;
	}
	
	public static int CharToInt(char c){
		if(c >= '0' && c <= '9')
			return c - '0';
		
		if(c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		
		if(c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		
		return -1;
	}
	
	public static byte[] StringToByte(String str){
		int strLen = str.length();
		if(strLen % 2 != 0)
			return null;
		
		char c[] = str.toCharArray();
		for(int i = 0; i < c.length; i++){
			if( ! IsHexDigital(c[i]))
				return null;
		}
		
		byte b[] = new byte[strLen / 2];
		for(int i = 0; i < b.length; i++){			
			int left = CharToInt(c[i*2]);
			int right = CharToInt(c[i*2+1]);
			
			b[i] = (byte)( (left << 4) | right);
		}
		
		return b;
	}
	
	public static String ByteToString(byte [] b){
		String str = "";
		String tmp = "";
		for (int i = 0; i < b.length; i++)
		{
			tmp=(java.lang.Integer.toHexString(b[i] & 0xFF));
			if (tmp.length() == 1 ) 
				str = str + "0" + tmp;
			else 
				str = str + tmp;
		}
		return str.toUpperCase();
	}
	
	public static int Make2BytesToInt(byte b0, byte b1){
		int v = (b0 & 0x000000FF)
				| ((b1 << 8) & 0x0000FF00);
		return v;
	}
	
	public static int Make4BytesToInt(byte b0, byte b1, byte b2, byte b3){
		int v = (b0 & 0x000000FF)
				| ((b1 << 8) & 0x0000FF00)
				| ((b2 << 16)& 0x00FF0000)
				| ((b3 << 24)& 0xFF000000);
		
		return v;
	}
	
	public static long Make8BytesToLong(byte b0, byte b1, byte b2, byte b3,
			byte b4, byte b5, byte b6, byte b7)
	{
		long v0 = Make4BytesToInt(b0,b1,b2,b3);
		long v1 = Make4BytesToInt(b4,b5,b6,b7);
		long v = v0 | (v1 << 32);
	
		return v;
	}
	
	/*����(x1,y1)��(x0,y0)��������X��ļн�
	* */
	public static double calcAlpha( double x0,  double y0, double x1,  double y1) {
		double alpha;

		if(x1 > x0){
			double tmp = (y1-y0)/(x1-x0);
			if(y1 >= y0)
				alpha = Math.atan(tmp);
			else
				alpha = 2*Math.PI + Math.atan(tmp);
		}else if(x1 == x0){
			if(y1 >= y0)
				alpha = Math.PI / 2;
			else
				alpha = 3*Math.PI / 2;
		}else{
			double tmp = (y1-y0)/(x1-x0);
			alpha = Math.PI + Math.atan(tmp);
		}

		return alpha;
	}
	
	//��difficult����10������1λС�������ַ�����ʽ���
	static public String difficultToStr(Integer difficult){
		CharSequence s = difficult.toString();
		CharSequence d = "";
		if(s.length() == 1)
			d = "0";
		for(int i = 0; i < s.length(); i++){
			if(i == s.length() - 1)
				d = d + ".";
			d = d + String.valueOf(s.charAt(i));
		}
		
		return d.toString();
	}
}
