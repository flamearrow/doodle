package com.ml.gb.doodle.activity;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

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
	private Dialog currentDialog;

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
				// it's the acceleration on three axis, we square them to get a positive number
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

	// we do need an option menue in this activity, override this method
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, COLOR_MENU_ID, Menu.NONE, R.string.menuitem_color);
		menu.add(Menu.NONE, WIDTH_MENU_ID, Menu.NONE,
				R.string.menuitem_line_width);
		menu.add(Menu.NONE, ERASE_MENU_ID, Menu.NONE, R.string.menuitem_erase);
		menu.add(Menu.NONE, CLEAR_MENU_ID, Menu.NONE, R.string.menuitem_clear);
		menu.add(Menu.NONE, SAVE_MENU_ID, Menu.NONE,
				R.string.menuitem_save_image);

		// return true to display this munue
		return true;
	}

	// handle option menu item
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case COLOR_MENU_ID:
			showColorDialog();
			break;
		case WIDTH_MENU_ID:
			showLineWidthDialog();
			break;
		case ERASE_MENU_ID:
			doodleView.setDrawingColor(Color.WHITE);
			break;
		case CLEAR_MENU_ID:
			doodleView.clear();
			break;
		case SAVE_MENU_ID:
			doodleView.saveImage();
			break;
		}
		return true;
	}

	private void showColorDialog() {
		currentDialog = new Dialog(this);
		currentDialog.setContentView(R.layout.color_dialog);
		currentDialog.setTitle(R.string.title_color_dialog);
		currentDialog.setCancelable(true);

		SeekBar alphaSB = (SeekBar) currentDialog
				.findViewById(R.id.alphaSeekBar);
		SeekBar redSB = (SeekBar) currentDialog.findViewById(R.id.redSeekBar);
		SeekBar greenSB = (SeekBar) currentDialog
				.findViewById(R.id.greenSeekBar);
		SeekBar blueSB = (SeekBar) currentDialog.findViewById(R.id.blueSeekBar);

		alphaSB.setOnSeekBarChangeListener(colorSBLsnr);
		redSB.setOnSeekBarChangeListener(colorSBLsnr);
		greenSB.setOnSeekBarChangeListener(colorSBLsnr);
		blueSB.setOnSeekBarChangeListener(colorSBLsnr);

		int color = doodleView.getDrawingColor();
		// extract the alpha vector from a color and set to progress bar
		alphaSB.setProgress(Color.alpha(color));
		redSB.setProgress(Color.red(color));
		greenSB.setProgress(Color.green(color));
		blueSB.setProgress(Color.blue(color));

		Button setColorButton = (Button) currentDialog
				.findViewById(R.id.setColorButton);
		setColorButton.setOnClickListener(setColorButtonLsnr);

		dialogIsVisible.set(true);
		currentDialog.show();
	}

	private OnSeekBarChangeListener colorSBLsnr = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			SeekBar alphaSB = (SeekBar) currentDialog
					.findViewById(R.id.alphaSeekBar);
			SeekBar redSB = (SeekBar) currentDialog
					.findViewById(R.id.redSeekBar);
			SeekBar greenSB = (SeekBar) currentDialog
					.findViewById(R.id.greenSeekBar);
			SeekBar blueSB = (SeekBar) currentDialog
					.findViewById(R.id.blueSeekBar);

			// preview the color
			View colorView = currentDialog.findViewById(R.id.colorView);
			colorView.setBackgroundColor(Color.argb(alphaSB.getProgress(),
					redSB.getProgress(), greenSB.getProgress(),
					blueSB.getProgress()));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			Log.d("mlgb", "you started touch!");
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			Log.d("mlgb", "you stopped touch!");
		}
	};

	// when set color is clicked, get the color and pass it to doodleView
	private OnClickListener setColorButtonLsnr = new OnClickListener() {

		@Override
		public void onClick(View v) {
			SeekBar alphaSB = (SeekBar) currentDialog
					.findViewById(R.id.alphaSeekBar);
			SeekBar redSB = (SeekBar) currentDialog
					.findViewById(R.id.redSeekBar);
			SeekBar greenSB = (SeekBar) currentDialog
					.findViewById(R.id.greenSeekBar);
			SeekBar blueSB = (SeekBar) currentDialog
					.findViewById(R.id.blueSeekBar);

			doodleView.setDrawingColor(Color.argb(alphaSB.getProgress(),
					redSB.getProgress(), greenSB.getProgress(),
					blueSB.getProgress()));
			dialogIsVisible.set(false);
			currentDialog.dismiss();
			// we need to nullify as there're other dialogs
			currentDialog = null;
		}

	};

	private void showLineWidthDialog() {
		currentDialog = new Dialog(this);
		currentDialog.setContentView(R.layout.width_dialog);
		currentDialog.setTitle(R.string.title_line_width_dialog);
		currentDialog.setCancelable(true);

		SeekBar widthSeekBar = (SeekBar) currentDialog
				.findViewById(R.id.widthSeekBar);
		widthSeekBar.setOnSeekBarChangeListener(widthSBLsnr);
		widthSeekBar.setProgress(doodleView.getLineWidth());

		Button setWidthButton = (Button) currentDialog
				.findViewById(R.id.widthDialogDoneButton);
		setWidthButton.setOnClickListener(setWidthLsnr);
		dialogIsVisible.set(true);
		currentDialog.show();
	}

	private OnSeekBarChangeListener widthSBLsnr = new OnSeekBarChangeListener() {
		Bitmap bm = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bm);

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			ImageView widthIV = (ImageView) currentDialog
					.findViewById(R.id.widthImageView);

			Paint p = new Paint();
			p.setColor(doodleView.getDrawingColor());
			p.setStrokeCap(Paint.Cap.ROUND);
			p.setStrokeWidth(progress);

			bm.eraseColor(Color.WHITE);
			canvas.drawLine(30, 50, 370, 50, p);
			// setting bm will redraw the image
			// update an image steps:
			//  1) create a  bitmap
			//  2) assign it to a canvas
			//  3) draw stuff on canvas using a Paint
			//  4) assign this bm to the view to update
			// here BM is data to be written, Canvas is where the drawing happens
			widthIV.setImageBitmap(bm);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// nothing
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// nothing

		}
	};

	private OnClickListener setWidthLsnr = new OnClickListener() {

		@Override
		public void onClick(View v) {
			SeekBar widthSB = (SeekBar) currentDialog
					.findViewById(R.id.widthSeekBar);
			doodleView.setLineWidth(widthSB.getProgress());
			dialogIsVisible.set(false);
			currentDialog.dismiss();
			currentDialog = null;
		}

	};
}
