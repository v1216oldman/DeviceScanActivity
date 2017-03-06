/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import android.R.integer;
import android.R.string;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
	private final static String TAG = DeviceControlActivity.class
			.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	private TextView mConnectionState;
	private TextView mDataField, data_info;
	private String mDeviceName;
	private String mDeviceAddress;
	private Button button_clear,button_UBI_Info,button_UBI_Service,button_UBI_Error,button_UBI_Tire_Pressure;
	private ExpandableListView mGattServicesList;
	private BluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	private BluetoothGattCharacteristic mNotifyCharacteristic;

	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";

	private BluetoothGattCharacteristic bluetoothGattCharacteristic_UBI;

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device. This can be a
	// result of read
	// or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				updateConnectionState(R.string.connected);
				invalidateOptionsMenu();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				mConnected = false;
				updateConnectionState(R.string.disconnected);
				invalidateOptionsMenu();
				clearUI();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// Show all the supported services and characteristics on the
				// user interface.
				displayGattServices(mBluetoothLeService
						.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent
						.getStringExtra(BluetoothLeService.EXTRA_DATA));
				displayData_info(intent
						.getStringExtra(BluetoothLeService.EXTRA_DATA_Info));

	
			}
		}
	};

	// If a given GATT characteristic is selected, check for supported features.
	// This sample
	// demonstrates 'Read' and 'Notify' features. See
	// http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for
	// the complete
	// list of supported characteristic features.
	private final ExpandableListView.OnChildClickListener servicesListClickListner = new ExpandableListView.OnChildClickListener() {
		@Override
		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			if (mGattCharacteristics != null) {
				final BluetoothGattCharacteristic characteristic = mGattCharacteristics
						.get(groupPosition).get(childPosition);
				final int charaProp = characteristic.getProperties();
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
					// If there is an active notification on a characteristic,
					// clear
					// it first so it doesn't update the data field on the user
					// interface.
					if (mNotifyCharacteristic != null) {
						mBluetoothLeService.setCharacteristicNotification(
								mNotifyCharacteristic, false);
						mNotifyCharacteristic = null;
					}
					mBluetoothLeService.readCharacteristic(characteristic);
				}
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
					mNotifyCharacteristic = characteristic;
					mBluetoothLeService.setCharacteristicNotification(
							characteristic, true);
				}
				return true;
			}
			return false;
		}
	};

	private void clearUI() {
		mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
		mDataField.setText(R.string.no_data);
		data_info.setText(R.string.no_data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gatt_services_characteristics);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		// mGattServicesList = (ExpandableListView)
		// findViewById(R.id.gatt_services_list);
		// mGattServicesList.setOnChildClickListener(servicesListClickListner);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mDataField = (TextView) findViewById(R.id.data_value);
		data_info = (TextView) findViewById(R.id.data_info);

		GetButtonView();
		setButtonEvent();

		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	private void setButtonEvent() {
		// TODO Auto-generated method stub
		
		/*
		 * 		
		 * button_clear
		 * button_UBI_Info
		 * button_UBI_Service
		 * button_UBI_Error
		 * button_UBI_Tire_Pressure	
		 * 
		 * */
		button_clear.setOnClickListener(new ButtonListener());
		button_UBI_Info.setOnClickListener(new ButtonListener());
		button_UBI_Service.setOnClickListener(new ButtonListener());
		button_UBI_Error.setOnClickListener(new ButtonListener());
		button_UBI_Tire_Pressure.setOnClickListener(new ButtonListener());

	}

	private class ButtonListener implements OnClickListener {

		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_clear:
				mDataField.setText("");
				data_info.setText("");
				break;
			case R.id.button_UBI_Info:

				String str_N = "AT+N=01";
				byte[] bytes_N = str_N.getBytes();

				bluetoothGattCharacteristic_UBI.setValue(bytes_N);
				mBluetoothLeService
						.writeCharacteristic(bluetoothGattCharacteristic_UBI);
				mBluetoothLeService.setCharacteristicNotification(
						bluetoothGattCharacteristic_UBI, true);
								
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mBluetoothLeService
						.readCharacteristic(bluetoothGattCharacteristic_UBI);

						
				

				break;
			case R.id.button_UBI_Service:

				String str_A = "AT+A=01";
				byte[] bytes_A = str_A.getBytes();	

				bluetoothGattCharacteristic_UBI.setValue(bytes_A);
				mBluetoothLeService
						.writeCharacteristic(bluetoothGattCharacteristic_UBI);
				mBluetoothLeService.setCharacteristicNotification(
						bluetoothGattCharacteristic_UBI, true);

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mBluetoothLeService
						.readCharacteristic(bluetoothGattCharacteristic_UBI);

				/*
				 * 
				 * //mBluetoothLeService.readCharacteristic(
				 * bluetoothGattCharacteristic_UBI); String
				 * str="9F687C1031000F009B667C0E2A0000000B0001000C0000000000";
				 * byte[] checksumstr =transform.hexToBytes(str); String
				 * Checksum = String.format("%#x",
				 * (CRC.calculate_crc(checksumstr, checksumstr.length))); String
				 * Checksum1 = String.format("%#x",
				 * (CRC.calculate_crc_reverse(checksumstr,checksumstr.length)));
				 * //System.out.print("CRC_print"+Checksum); Log.d(TAG,
				 * "Reverse: "+transform.reverse(Checksum));
				 * mDataField.setText("Reverse: "+Checksum1); Log.d(TAG,
				 * "CRC: "+Checksum); Log.d(TAG, "CRC: "+Checksum1);
				 */
				break;
				
			case R.id.button_UBI_Error:

				String str_M = "AT+M=01";
				byte[] bytes_M = str_M.getBytes();

				bluetoothGattCharacteristic_UBI.setValue(bytes_M);
				mBluetoothLeService
						.writeCharacteristic(bluetoothGattCharacteristic_UBI);
				mBluetoothLeService.setCharacteristicNotification(
						bluetoothGattCharacteristic_UBI, true);

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mBluetoothLeService
						.readCharacteristic(bluetoothGattCharacteristic_UBI);

				break;
				
			case R.id.button_UBI_Tire_Pressure:

				String str_T = "AT+T=01";
				byte[] bytes_T = str_T.getBytes();

				bluetoothGattCharacteristic_UBI.setValue(bytes_T);
				mBluetoothLeService
						.writeCharacteristic(bluetoothGattCharacteristic_UBI);
				mBluetoothLeService.setCharacteristicNotification(
						bluetoothGattCharacteristic_UBI, true);

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mBluetoothLeService
						.readCharacteristic(bluetoothGattCharacteristic_UBI);

				break;

			default:
				break;
			}
		}

	}

	private void GetButtonView() {
		// TODO Auto-generated method stub
/*
 * 		
 * button_clear
 * button_UBI_Info
 * button_UBI_Service
 * button_UBI_Error
 * button_UBI_Tire_Pressure	
 * 
 * */
		button_clear = (Button) findViewById(R.id.button_clear);
		button_UBI_Info = (Button) findViewById(R.id.button_UBI_Info);
		button_UBI_Service = (Button) findViewById(R.id.button_UBI_Service);
		button_UBI_Error = (Button) findViewById(R.id.button_UBI_Error);
		button_UBI_Tire_Pressure = (Button) findViewById(R.id.button_UBI_Tire_Pressure);

	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
		if (mConnected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connect:
			mBluetoothLeService.connect(mDeviceAddress);
			return true;
		case R.id.menu_disconnect:
			mBluetoothLeService.disconnect();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateConnectionState(final int resourceId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectionState.setText(resourceId);
			}
		});
	}

	private void displayData(String data) {
		if (data != null) {
			mDataField.setText(data);				

		}
	}
	
	private void displayData_info(String data) {
		if (data != null) {
			
			/*
			String str=data.replace(" ", "");
			int a=str.length();
			*/
			
			String[] DD = data.split(" ");
			int a=DD.length;	
			Log.d(TAG, "Info_data length="+ a);

      
          
           final int ubi_info=28;
           final int ubi_info_server=33;
           final int ubi_error=19;
           final int ubi_tire_pressure=10;
            
            switch (a) {
			case ubi_info:		
								
				int VC=Integer.parseInt(DD[9],16);
				int DH=Integer.parseInt(DD[15],16);
				int DM=Integer.parseInt(DD[16],16);
				int DL=Integer.parseInt(DD[17],16);					
				int distance=(DH*256*256)+(DM*256)+DL;
				int V=(Integer.parseInt(DD[18],16))-127;
				int TH=Integer.parseInt(DD[21],16);
				int TM=Integer.parseInt(DD[22],16);
				int TL=Integer.parseInt(DD[23],16);				    
				int Idle=(TH*256*256)+(TM*256)+TL;								
				
				data_info.setText(this.getString(R.string.speed)+" : "+ VC+ " Km/h"+ "\n"
						+this.getString(R.string.travel_distance)+" : "+ distance+ " m"+ "\n"
						+this.getString(R.string.sharp_turn_acceleration)+" : "+ V+ " g/s2"+ "\n"
						+this.getString(R.string.idle_time)+" : "+ Idle+ " s"+ "\n");			
              			
				break;
			case ubi_info_server:
				int OC=Integer.parseInt(DD[2],16);
				int EL=(Integer.parseInt(DD[3],16))*100/255;
				int ECT=(Integer.parseInt(DD[4],16))-40;
				int MAF=(Integer.parseInt(DD[5],16))/3;
				int IAT=(Integer.parseInt(DD[6],16))-40;
				int byte7=Integer.parseInt(DD[7],16);
				int byte8=Integer.parseInt(DD[8],16);
				int RS=(byte7*256)+byte8;
				int TP=(Integer.parseInt(DD[10],16))*100/255;
				int BV=(Integer.parseInt(DD[11],16))/10;
				int PSI=(Integer.parseInt(DD[12],16))/5;
				int FC=(Integer.parseInt(DD[13],16))/5;
				int GPS_Long=(Integer.parseInt(DD[19],16))*12/17-90;
				int GPS_Lat=(Integer.parseInt(DD[20],16))*24/17-180;
				
				data_info.setText(this.getString(R.string.Oil_condition)+" : "+ OC+ "\n"
								 +this.getString(R.string.Engine_load)+" : "+ EL+ " %"+ "\n"
								 +this.getString(R.string.Engine_coolant_temperature)+" : "+ ECT+ " °C"+ "\n"
								 +this.getString(R.string.Air_flow)+" : "+ MAF+ " g/s"+ "\n"
								 +this.getString(R.string.Intake_air_temperature)+" : "+ IAT+ " °C"+ "\n"
								 +this.getString(R.string.Rotating_speed)+" : "+ RS+ " RPM"+ "\n"
								 +this.getString(R.string.Throttle_position)+" : "+ TP+ " %"+ "\n"
								 +this.getString(R.string.Battery_voltage)+" : "+ BV+ " volt"+ "\n"
								 +this.getString(R.string.Tire_pressure_condition)+" : "+ PSI+ " PSI"+ "\n"
								 +this.getString(R.string.Fuel_consumption)+" : "+ FC+ " km/L"+ "\n"
								 +this.getString(R.string.Global_Positioning_System)+" : "+ GPS_Long+ " 度"+","+GPS_Lat+ " 度");
	
				break;
			case ubi_error:
				String U1=DD[2];
				String U2=DD[3];				
				String V1=DD[4];
				String V2=DD[5];
				String W1=DD[6];
				String W2=DD[7];
				String X1=DD[8];
				String X2=DD[9];
				String Y1=DD[10];
				String Y2=DD[11];
				
				data_info.setText(this.getString(R.string.U1U2)+" : "+ U1+U2+ "\n"
								 +this.getString(R.string.V1V2)+" : "+ V1+V2+ "\n"
								 +this.getString(R.string.W1W2)+" : "+ W1+W2+ "\n"
								 +this.getString(R.string.X1X2)+" : "+ X1+X2+ "\n"
								 +this.getString(R.string.Y1Y2)+" : "+ Y1+Y2+ "\n"
						
						);
				
				break;
			case ubi_tire_pressure:
				String ID1=DD[2];
				String ID2=DD[3];
				String ID3=DD[4];
				String ID4=DD[5];
				int T=(Integer.parseInt(DD[6],16))-50;
				int P=Integer.parseInt(DD[7],16);
				int TPV=(Integer.parseInt(DD[8],16))/50;
				
				data_info.setText(this.getString(R.string.ID1)+" : "+ ID1+ "\n"
								 +this.getString(R.string.ID2)+" : "+ ID2+ "\n"
								 +this.getString(R.string.ID3)+" : "+ ID3+ "\n"
								 +this.getString(R.string.ID4)+" : "+ ID4+ "\n"
								 +this.getString(R.string.Temperature)+" : "+ T+" °C"+ "\n"
								 +this.getString(R.string.Pressure)+" : "+ P+" psi"+ "\n"
								 +this.getString(R.string.Tire_pressure_Voltage)+" : "+ TPV+" volt"+ "\n"
						
						);
				
				
				break;

			default:
				break;
			}
			

			

		}
	}

	// Demonstrates how to iterate through the supported GATT
	// Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the
	// ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {

		UUID UUID_UBI = UUID
				.fromString(SampleGattAttributes.NORMAL_DATA_CHARACTERISTIC_UUID);

		if (gattServices == null)
			return;
		String uuid = null;
		String unknownServiceString = getResources().getString(
				R.string.unknown_service);
		String unknownCharaString = getResources().getString(
				R.string.unknown_characteristic);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			currentServiceData.put(LIST_NAME,
					SampleGattAttributes.lookup(uuid, unknownServiceString));
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();
				currentCharaData.put(LIST_NAME,
						SampleGattAttributes.lookup(uuid, unknownCharaString));
				currentCharaData.put(LIST_UUID, uuid);
				gattCharacteristicGroupData.add(currentCharaData);

				// Check if it is "HM_10"
				if (uuid.equals(SampleGattAttributes.NORMAL_DATA_CHARACTERISTIC_UUID)) {
					bluetoothGattCharacteristic_UBI = gattService
							.getCharacteristic(UUID_UBI);

				}

			}
			mGattCharacteristics.add(charas);
			gattCharacteristicData.add(gattCharacteristicGroupData);
		}

		SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
				this, gattServiceData,
				android.R.layout.simple_expandable_list_item_2, new String[] {
						LIST_NAME, LIST_UUID }, new int[] { android.R.id.text1,
						android.R.id.text2 }, gattCharacteristicData,
				android.R.layout.simple_expandable_list_item_2, new String[] {
						LIST_NAME, LIST_UUID }, new int[] { android.R.id.text1,
						android.R.id.text2 }

		);
		// mGattServicesList.setAdapter(gattServiceAdapter);

	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}
}
