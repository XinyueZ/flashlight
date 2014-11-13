package com.lightplus;

import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.RelativeLayout.LayoutParams;

public class LightControlFragment extends Fragment {
	private final static String EXTRA_MODE = "mode";
	private final static String EXTRA_ON = "on";
	int mCurrentMode;
	boolean mOn;
	TransitionDrawable mDrawable;
	ImageButton mBulb;

	// Empty constructor required
	public LightControlFragment() {
	}

	/**
	 * Create a new instance, providing the current light mode as an argument.
	 */
	static LightControlFragment newInstance(Context cxt, int mode, boolean on) {
		LightControlFragment fragment = (LightControlFragment) LightControlFragment.instantiate(cxt,
				LightControlFragment.class.getName());

		Bundle args = new Bundle();
		args.putInt(EXTRA_MODE, mode);
		args.putBoolean(EXTRA_ON, on);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mCurrentMode = getArguments().getInt(EXTRA_MODE);
		mOn = getArguments().getBoolean(EXTRA_ON);

		switch (mCurrentMode) {
		case R.id.mode_viewfinder:
			return inflater.inflate(R.layout.viewfinder, container, false);
		default:
		case R.id.mode_blackout:
			return inflater.inflate(R.layout.black, container, false);
		}
	}


	@Override
	public void onResume() {

		switch (mCurrentMode) {
		case R.id.mode_blackout:
			mBulb = (ImageButton) getActivity().findViewById(R.id.button_black);
			Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
			mBulb.startAnimation(fadeOut);
			break;
		case R.id.mode_viewfinder:
			mBulb = (ImageButton) getActivity().findViewById(R.id.button_bulb);

			break;
		}

		mDrawable = (TransitionDrawable) mBulb.getDrawable();
		mDrawable.setCrossFadeEnabled(true);
		if (mOn) {
			mDrawable.startTransition(0);
		}

		PreviewSurface surface = (PreviewSurface) getActivity().findViewById(R.id.surface);
		if (mCurrentMode == R.id.mode_viewfinder) {
			surface.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		} else {
			surface.setLayoutParams(new LayoutParams(1, 1));
		}

		super.onResume();
	}

	@Override
	public void onStop() {
		stopLightControl();
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		stopLightControl();
		super.onDestroyView();
	}

	private void stopLightControl() {
		switch (mCurrentMode) {
		case R.id.mode_viewfinder:
			// kill any ongoing transition so it's not still finishing when we resume
			mDrawable.resetTransition();
			break;
		}
	}

	public void toggleLightControl(boolean on) {
		if (on) {
			mDrawable.startTransition(200);
		} else {
			mDrawable.reverseTransition(200);
		}
	}

}
