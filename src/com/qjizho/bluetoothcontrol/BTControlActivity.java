package com.qjizho.bluetoothcontrol;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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
	private TextView txt;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		img = (ImageView) findViewById(R.id.img);
		img.setOnClickListener(this);
		txt = (TextView) findViewById(R.id.txt);
//		Button btn2 = (Button) findViewById(R.id.btn2);
//		btn2.setOnClickListener(this);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.img:
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
			break;
//		case R.id.btn2:
//			Log.d("qiqi", "send hello.");
//			if(null != mConnectedThread){
//				Log.d("qiqi", "send hello.");
//				mConnectedThread.write(new String("hello").getBytes());
//			}
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
			Log.d("qiqi", "mmSocket connected successfully.");
			mConnectedThread = new ConnectedThread(mmSocket);
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
	        } catch (IOException connectException) {
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return false;
	        }
			return true;
		}
		
	}
}
