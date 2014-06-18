package com.ml.gb.doodle.activity;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;

import com.ml.gb.doodle.R;
import com.ml.gb.doodle.view.DoodleView;

public class DoodleActivity extends Activity {
	private DoodleView doodleView;
	private SensorManager sensorManager; // accelerometer
	private float acceleration; // acceleration
	private float currentAcceleration;
	private float lastAcceleration;
	private AtomicBoolean dialogIsVisible = new AtomicBoolean();// might be accessed by multiple threads

	// each activity has a default menu, can access them by sequence
	private static final int COLOR_MENU_ID = Menu.FIRST;
	private static final int WIDTH_MENU_ID = Menu.FIRST + 1;
	private static final int ERASE_MENU_ID = Menu.FIRST + 2;
	private static final int CLEAR_MENU_ID = Menu.FIRST + 3;
	private static final int SAVE_MENU_ID = Menu.FIRST + 4;

	// is it really being shaken?
	private static final float ACCELERATION_THESHOLD = 15000;

	// choose color or choose line?
	private Dialog currentDilog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doodle);

		doodleView = (DoodleView) findViewById(R.id.doodleView);

		acceleration = 0.00f;
		// This is the gravity on earth i.e how many N/kg on EARTH?
		currentAcceleration = SensorManager.GRAVITY_EARTH;
		lastAcceleration = SensorManager.GRAVITY_EARTH;

		enableAccelerometerListening();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// when app is sent to background we should stop listening for acceleration
		disableAccelerometerListening();
	}

	// first get SensorManger from system service, then register which service we need
	private void enableAccelerometerListening() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		// get the accelerometer sensor
		// SensorManager.SENSOR_DELAY_NORMAL is the rate we want to receive the info
		//	 -the more frequent the more resource intensive
		sensorManager.registerListener(sensorEventListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	private void disableAccelerometerListening() {
		if (sensorManager != null) {
			sensorManager.unregisterListener(sensorEventListener,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
			sensorManager = null;
		}
	}

	// capture the SensorEvent
	private SensorEventListener sensorEventListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			// dialogIsVisible dictates all dialogs
			// we only prompts this erase operation when other dialogs are NOT visible
			if (!dialogIsVisible.get()) {
				float x = event.values[0];
				float y = event.values[1];
				float z = event.values[2];

				lastAcceleration = currentAcceleration;

				currentAcceleration = x * x + y * y + z * z;

				// delta
				acceleration = currentAcceleration
						* (currentAcceleration - lastAcceleration);

				// if shake, prompt to erase
				if (acceleration > ACCELERATION_THESHOLD) {
					// can't directly use this because it's inner class
					AlertDialog.Builder builder = new AlertDialog.Builder(
							DoodleActivity.this);

					builder.setMessage(R.string.message_erase);
					builder.setCancelable(true);

					builder.setPositiveButton(R.string.button_erase,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialogIsVisible.set(false);
									doodleView.clear();
								}
							});
					builder.setNegativeButton(R.string.button_cancel,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialogIsVisible.set(false);
									dialog.cancel();
								}
							});
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// nothing
		}

	};
}
