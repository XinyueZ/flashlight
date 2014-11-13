/*
Copyright 2010 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.lightplus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.lightplus.utils.SystemUiHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class MainActivity extends ActionBarActivity implements PreviewSurface.Callback {
	//private final static String TAG = "SearchLight";
	private final static String MODE_TYPE = "mode_type";


	public static final int LAYOUT = R.layout.activity_main;
	PreviewSurface mSurface;
	boolean on = false;
	boolean paused = false;
	boolean skipAnimate = false;
	boolean mSystemUiVisible = true;
	boolean mCameraReady = false; // to make sure we don't turn on light when preview surface resizes
	int mCurrentMode;


	FragmentManager mFragmentManager;
	LightControlFragment mCurrentFragment;

	/**
	 * The uiHelper classes from <a href="https://gist.github.com/chrisbanes/73de18faffca571f7292">Chris Banes</a>
	 */
	private SystemUiHelper mSystemUiHelper;
	/**
	 * Flag that is {@code true} if the statusbar will show first time.
	 */
	private boolean mFistTimeHide = true;

	/**
	 * The "ActionBar".
	 */
	private Toolbar mToolbar;
	/**
	 * Height of {@link android.support.v7.app.ActionBar}.
	 */
	private int mActionBarHeight;
	/**
	 * Height of statusbar.
	 */
	private int mStatusBarHeight;

	/**
	 * To get height of statusbar.
	 */
	private void calcStatusBarHeight() {
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
		}
	}


	/**
	 * Calculate height of actionbar.
	 */
	private void calcActionBarHeight() {
		int[] abSzAttr;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			abSzAttr = new int[] { android.R.attr.actionBarSize };
		} else {
			abSzAttr = new int[] { R.attr.actionBarSize };
		}
		TypedArray a = obtainStyledAttributes(abSzAttr);
		mActionBarHeight = a.getDimensionPixelSize(0, -1);
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSystemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, 0);
		mSystemUiHelper.hide();
		setContentView(LAYOUT);

		calcStatusBarHeight();
		calcActionBarHeight();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			View decorView = getWindow().getDecorView();
			decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
				@Override
				public void onSystemUiVisibilityChange(int visibility) {
					// Note that system bars will only be "visible" if none of the
					// LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
					if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
						// The system bars are visible.
						animToolActionBar(mStatusBarHeight);
					} else {
						if (mFistTimeHide) {
							mFistTimeHide = false;
							return;
						}
						// The system bars are NOT visible.
						animToolActionBar(0);
					}
				}
			});
		}
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);

		mSurface = (PreviewSurface) findViewById(R.id.surface);
		mSurface.setCallback(this);

		// When user selects mode from menu, there's a mode type
		mCurrentMode = getIntent().getIntExtra(MODE_TYPE, -1);
		// When launched clean, there's no mode in the intent, so check preference
		if (mCurrentMode == -1) {
			SharedPreferences modePreferences = getPreferences(Context.MODE_PRIVATE);
			mCurrentMode = modePreferences.getInt(MODE_TYPE, R.id.mode_blackout);

			// Rewrite the intent to carry the desired mode
			Intent intent = getIntent();
			intent.putExtra(MODE_TYPE, mCurrentMode);
			setIntent(intent);
		}
		// Set up layout with initial controller fragment
		mFragmentManager = getSupportFragmentManager();
		switchControlFragment(mCurrentMode);
	}


	/**
	 * Click event for all light controllers
	 */
	public void toggleLight(View v) {
		if (on) {
			turnOff();
			if (v.getId() == R.id.button_black) {
				Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
				v.startAnimation(fadeIn);
			}
		} else {
			turnOn();
			if (v.getId() == R.id.button_black) {
				Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
				v.startAnimation(fadeOut);
			}
		}
	}

	private void turnOn() {
		if (!on) {
			on = true;
			mSurface.lightOn();
			if(mCurrentFragment != null) {
				mCurrentFragment.toggleLightControl(on);
				mCurrentFragment.animHideButton(mActionBarHeight * 4);
			}
			animToolActionBar(-mActionBarHeight * 4);
			supportInvalidateOptionsMenu();
		}
	}

	private void turnOff() {
		if (on) {
			on = false;
			mSurface.lightOff();
			if(mCurrentFragment != null) {
				mCurrentFragment.toggleLightControl(on);
			}
			animToolActionBar(0);
			supportInvalidateOptionsMenu();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		turnOff();
		mSurface.releaseCamera();
		paused = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (paused) {
			mSurface.initCamera();
		}
		mCameraReady = false;
	}

	@Override
	protected void onStop() {
		super.onStop();

		// Save the current mode so it's not lost when process stops
		SharedPreferences modePreferences = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = modePreferences.edit();
		editor.putInt(MODE_TYPE, mCurrentMode);
		editor.commit();
		finish(); // I give up, the camera surface doesn't come back on resume, so just kill it all
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		skipAnimate = false;

		if (hasFocus && paused) {
			mSurface.startPreview();
			paused = false;
		}

		mSystemUiHelper.hide();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.dialog_camera_na:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.dialog_camera_na).setCancelable(false).setNeutralButton(R.string.dialog_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							MainActivity.this.finish();
						}
					});
			return builder.create();
		default:
			return super.onCreateDialog(id);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		menu.findItem(R.id.action_view_finder).setChecked(mCurrentMode == R.id.mode_viewfinder);
		menu.findItem(R.id.action_blackout).setChecked(mCurrentMode == R.id.mode_blackout);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//		if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
		//			return true;
		//		}
		int id = item.getItemId();
		switch (id) {
		case R.id.action_blackout:
			changeMode(R.id.mode_blackout);
			break;
		case R.id.action_view_finder:
			changeMode(R.id.mode_viewfinder);
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	/**
	 * In case a device has a MENU button, show the mode dialog when it's pressed
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.action_view_finder).setChecked(mCurrentMode == R.id.mode_viewfinder);
		menu.findItem(R.id.action_blackout).setChecked(mCurrentMode == R.id.mode_blackout);
		return true;
	}


	/**
	 * Implement the ModeDialogFragment's callback interface method
	 */
	public void changeMode(int mode) {
		if (mCurrentMode == mode) {
			return;
		}

		mCurrentMode = mode;
		skipAnimate = true;
		if (mCurrentMode != -1) {
			switchControlFragment(mCurrentMode);
		}
	}

	private void switchControlFragment(int mode) {
		// update activity state w/ new mode
		Intent intent = getIntent();
		intent.putExtra(MODE_TYPE, mode);
		setIntent(intent);

		// switch fragments
		LightControlFragment newFragment = LightControlFragment.newInstance(this, mode, on);
		mFragmentManager.beginTransaction().replace(R.id.controller_fragment, newFragment).commit();
		mCurrentFragment = newFragment;
		if (mode == R.id.mode_viewfinder) {
			mSurface.setIsViewfinder();
		}
	}


	public void cameraReady() {
		if (!mCameraReady) {
			mCameraReady = true;
			turnOn();
		}
	}

	public void cameraNotAvailable() {
		showDialog(R.id.dialog_camera_na);
	}



	/**
	 * Animation and moving actionbar(toolbar).
	 *
	 * @param value
	 * 		The property value of animation.
	 */
	private void animToolActionBar(float value) {
		ViewPropertyAnimator animator = ViewPropertyAnimator.animate(mToolbar);
		animator.translationY(value).setDuration(400);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(mCurrentFragment != null) {
			mCurrentFragment.animShowButton();
			h.postDelayed(r, 3000);
		}
		return super.onTouchEvent(event);
	}

	Handler h = new Handler();
	Runnable r = new Runnable() {
		@Override
		public void run() {
			if(on) {
				if(mCurrentFragment != null) {
					mCurrentFragment.animHideButton(mActionBarHeight * 4);

				}
			}
		}
	};
}