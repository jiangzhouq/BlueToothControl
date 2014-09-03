package com.qjizho.bluetoothcontrol;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bluetoothcontrol.R;

public class BTControlActivity extends Activity implements OnClickListener{
	BluetoothAdapter mBluetoothAdapter;
	private ConnectedThread mConnectedThread;
	private ConnectAsyncTask task;
	ImageView img;
	private ImageView blueState;
	private View mDecorView;
	private TextView txt;
	PowerManager pManager;
	WakeLock mWakeLock;
	private boolean connect_state = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		img = (ImageView) findViewById(R.id.img);
		img.setOnClickListener(this);
		blueState = (ImageView) findViewById(R.id.blue_state);
		blueState.setOnClickListener(this);
		txt = (TextView) findViewById(R.id.txt);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		int sensorType = Sensor.TYPE_ACCELEROMETER;
		sm.registerListener(myAccelerometerListener,sm.getDefaultSensor(sensorType),SensorManager.SENSOR_DELAY_NORMAL);
		
		mDecorView = getWindow().getDecorView();
		hideSystemUI();
	}
	private void changeScreenState(int state){
		switch(state){
		case 0:
			img.setVisibility(View.GONE);
			txt.setVisibility(View.GONE);
			blueState.setVisibility(View.VISIBLE);
			break;
		case 1:
			img.setVisibility(View.VISIBLE);
			txt.setVisibility(View.VISIBLE);
			blueState.setVisibility(View.INVISIBLE);
			break;
		}
	}
	private int mSensorCount = 0;
	final SensorEventListener myAccelerometerListener = new SensorEventListener(){  
        
        //复写onSensorChanged方法  
        public void onSensorChanged(SensorEvent sensorEvent){  
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){  
                //图解中已经解释三个值的含义  
                float X_lateral = sensorEvent.values[0];  
                float Y_longitudinal = sensorEvent.values[1];  
                float Z_vertical = sensorEvent.values[2];  
//                if(X_lateral > 5)
//                	Log.d("qiqi","\n heading "+X_lateral); 
                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE  ){
                	if(Y_longitudinal > 3 ){
                		if(X_lateral > 0){
                			mSensorCount ++;
                			if(mSensorCount == 10){
                				mSensorCount = 0;
                				if(null != mConnectedThread){
                					Log.d("qiqi", "send hello.");
                					mConnectedThread.write(new String("right").getBytes());
                				}
                			}
                		}else if(X_lateral < 0){
                			mSensorCount ++;
                			if(mSensorCount == 10){
                				mSensorCount = 0;
                				if(null != mConnectedThread){
                					Log.d("qiqi", "send hello.");
                					mConnectedThread.write(new String("left").getBytes());
                				}
                			}
                		}
                	}else if (Y_longitudinal < -3){
                		if(X_lateral > 0){
                			mSensorCount ++;
                			if(mSensorCount == 10){
                				mSensorCount = 0;
                				if(null != mConnectedThread){
                					Log.d("qiqi", "send hello.");
                					mConnectedThread.write(new String("left").getBytes());
                				}
                			}
                		}else{
                			mSensorCount ++;
                			if(mSensorCount == 10){
                				mSensorCount = 0;
                				if(null != mConnectedThread){
                					Log.d("qiqi", "send hello.");
                					mConnectedThread.write(new String("right").getBytes());
                				}
                			}
                		}
                	}
                }
            }  
        }  
        //复写onAccuracyChanged方法  
        public void onAccuracyChanged(Sensor sensor , int accuracy){  
            Log.d("qiqi", "onAccuracyChanged");  
        }  
    };  
	@Override
	protected void onResume() {
		super.onResume();
		pManager = ((PowerManager) getSystemService(POWER_SERVICE));  
        mWakeLock = pManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK  
                | PowerManager.ON_AFTER_RELEASE, "1");  
        mWakeLock.acquire(); 
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(null != mWakeLock){  
            mWakeLock.release();  
        }  
	}
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.img:
			if(connect_state){
				img.clearAnimation();
				img.setImageResource(R.drawable.bt_connecting);
				txt.setText(R.string.bt_disconnect);
				connect_state = false;
			}else{
				if (mBluetoothAdapter != null) {
					if (!mBluetoothAdapter.isEnabled()) {
					    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					    startActivityForResult(enableBtIntent, 1);
					}else{
						Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
						Log.d("qiqi", "pairedDevices.size():" + pairedDevices.size());
						if (pairedDevices.size() > 0) {
						    for (BluetoothDevice device : pairedDevices) {
						    	Log.d("qiqi", device.getName() + "=====" + device.getAddress());
						    	task = new ConnectAsyncTask(device);
						    	task.execute();
						    	Animation rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
						    	LinearInterpolator lin = new LinearInterpolator();
						    	rotateAnim.setInterpolator(lin);
						    	if(rotateAnim != null){
						    		img.startAnimation(rotateAnim);
						    	}
						    	txt.setText(R.string.bt_connecting);
						    }
						}
					}
				}else{
				}
				connect_state = true;
			}
			break;
		case R.id.blue_state:
			changeScreenState(1);
			break;
//		case R.id.btn2:
//			Log.d("qiqi", "send hello.");
			
//			break;
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(task != null)
			task.cancelSocket();
	}
	private class ConnectAsyncTask extends AsyncTask<Void, Integer, Boolean>{
		private BluetoothSocket mmSocket;
		public ConnectAsyncTask(BluetoothDevice device) {
			BluetoothDevice mmDevice = device;
			BluetoothSocket tmp = null;
			try {
				tmp = mmDevice.createRfcommSocketToServiceRecord(UUID
						.fromString("71f78e96-3024-11e4-89c4-a6c5e4d22fb7"));
			} catch (IOException e) {
			}
			mmSocket = tmp;
			Log.d("qiqi", "create mmSocket.");
		}
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if(result){
				mConnectedThread = new ConnectedThread(mmSocket);
				img.clearAnimation();
				img.setImageResource(R.drawable.bt_connected);
				txt.setText(R.string.bt_conneced);
				connect_state = true;
				changeScreenState(0);
			}else{
				img.clearAnimation();
				img.setImageResource(R.drawable.bt_connecting);
				txt.setText(R.string.bt_connect_wrong);
				connect_state = false;
			}
		}
		public void cancelSocket(){
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
		@Override
		protected Boolean doInBackground(Void... params) {
	        mBluetoothAdapter.cancelDiscovery();
	        try {
	        	Log.d("qiqi", "start to connect mmSocket.");
	            mmSocket.connect();
	        	Log.d("qiqi", "mmSocket connected successfully.");
	        	return true;
	        } catch (IOException connectException) {
	        	Log.d("qiqi", "socket connect failed");
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return false;
	        }
		}
		
	}
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	        super.onWindowFocusChanged(hasFocus);
	    if (hasFocus) {
	    	mDecorView.setSystemUiVisibility(
	                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	                | View.SYSTEM_UI_FLAG_FULLSCREEN
	                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
	}
	// This snippet hides the system bars.
	private void hideSystemUI() {
	    // Set the IMMERSIVE flag.
	    // Set the content to appear under the system bars so that the content
	    // doesn't resize when the system bars hide and show.
	    mDecorView.setSystemUiVisibility(
	            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
	            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
	            | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}
}
