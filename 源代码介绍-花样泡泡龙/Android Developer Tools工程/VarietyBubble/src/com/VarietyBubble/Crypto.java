package com.VarietyBubble;

import java.security.InvalidKeyException;
import java.security.Key;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

public class Crypto {
	private Key mKey;
	private Cipher mCipher;
	private final String mKeyStr = "bd741d88800f6afc";
	private final String mMagicStr = "mIny";
	private final int mMagicStrOffset = 12;
	private final int mMaxValidRandomValue = 200;
    
	public Crypto(String algorithm)
	{
		try{
			byte keyData[] = new byte[16];
			char c[] = mKeyStr.toCharArray();
			for(int i = 0; i < keyData.length && i < c.length; i++)
				keyData[i] = (byte)c[i];
			mKey = new SecretKeySpec(keyData, algorithm); 
			mCipher = Cipher.getInstance(algorithm) ;
		}catch (java.security.NoSuchAlgorithmException e1){
			e1.printStackTrace();
			mKey = null;
			mCipher = null;
		}catch (java.lang.Exception e2){
			e2.printStackTrace();
			mKey = null;
			mCipher = null;
		}
	}
    
	//加密算法
	public byte[] encrypt (byte[] buf)
	{
		byte[] result = null;
		try{
			mCipher.init(Cipher.ENCRYPT_MODE, mKey) ;
			result = mCipher.doFinal(buf) ;
		}catch(IllegalBlockSizeException e){
			Util.Loge("encrypt:IllegalBlockSizeException");
		}catch(BadPaddingException e){
			Util.Loge("encrypt:BadPaddingException");
		}catch(InvalidKeyException e){
			Util.Loge("encrypt:InvalidKeyException");
		}

		return result ;
	}

	//解密算法
	public byte[] decrypt(byte[] buf)
	{
		byte[] result = null;
		try{
			mCipher.init(Cipher.DECRYPT_MODE, mKey) ;
			result = mCipher.doFinal(buf);
		}catch(IllegalBlockSizeException e){
			Util.Loge("decrypt:IllegalBlockSizeException");
		}catch(BadPaddingException e){
			Util.Loge("decrypt:BadPaddingException");
		}catch(InvalidKeyException e){
			Util.Loge("decrypt:InvalidKeyException");
		}
		 
		return result ;
	}
    
	public byte[] encryptLongToByte(final long v, int magic)
	{
		char c[] = mMagicStr.toCharArray();
		byte buf[] = new byte[((c.length + mMagicStrOffset - 1)/16 + 1) * 16];
		buf[0] = (byte)(v >> 0  & 0xff);
		buf[1] = (byte)(v >> 8  & 0xff);
		buf[2] = (byte)(v >> 16 & 0xff);
		buf[3] = (byte)(v >> 24 & 0xff);
		buf[4] = (byte)(v >> 32 & 0xff);
		buf[5] = (byte)(v >> 40 & 0xff);
		buf[6] = (byte)(v >> 48 & 0xff);
		buf[7] = (byte)(v >> 56 & 0xff);
		
		int random = Util.RandomInt(mMaxValidRandomValue+1);
		buf[8] = (byte)(random >> 0  & 0xff);
		buf[9] = (byte)(random >> 8  & 0xff);
		
		buf[10]= (byte)(magic >> 0 & 0xff);
		buf[11]= (byte)(magic >> 16 & 0xff);
		
		for(int i = 0; i < c.length; i ++){
			buf[i+mMagicStrOffset] = (byte)c[i];
		}
		
		return encrypt(buf);
	}
    
	public long decryptByteToLong(final byte buf[], int magic)
	{
		byte result[] = decrypt(buf);
		if(result == null || result.length <= mMagicStrOffset)
			return -1;
		
		char c[] = new char[result.length-mMagicStrOffset];
		for(int i = 0; i < c.length; i++){
			c[i] = (char)result[i+mMagicStrOffset];
		}
		
		int magicStrLen = mMagicStr.length();
		for(int i = 0; i < magicStrLen; i++){
			if( c[i] != mMagicStr.charAt(i))
				return -2;//解密失败
		}
		
		int random = Util.Make2BytesToInt(result[8],result[9]);
		if(random > mMaxValidRandomValue){
			return -3;
		}
		int tmpMagic = result[10] | result[11] << 16;
		if(magic != tmpMagic){
			return -4;
		}
		
		long tmp = Util.Make8BytesToLong(result[0],result[1],result[2],result[3],
				result[4],result[5],result[6],result[7]);
		
		return tmp ;
	}
	
	public String encryptLongToString(final long v, int magic){
		byte b[] = encryptLongToByte(v, magic);
		return Util.ByteToString(b);
	}
	
	public long decryptStringToLong(final String str, int magic){
		byte b[] = Util.StringToByte(str);
		return decryptByteToLong(b, magic);
	}
}
