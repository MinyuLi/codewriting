package com.zijunlin.Zxing.Demo.callback;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;

import com.google.zxing.Result;
import com.zijunlin.Zxing.Demo.view.ViewfinderView;

public interface ScanCallback {
	public void handleDecode(final Result obj, Bitmap barcode);
	public void startActivity(Intent intent);
	public void drawViewfinder();
	public ViewfinderView getViewfinderView();
	public Handler getHandler();
	public void finish();
	public void setResult(int resultOk, Intent obj);
}
