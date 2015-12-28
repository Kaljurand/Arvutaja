package ee.ioc.phon.android.arvutaja;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

public class MicButton extends ImageButton {

	// TODO: take these from some configuration
	private static final float DB_MIN = 15.0f;
	private static final float DB_MAX = 30.0f;

	private Drawable mDrawableMic;
	private Drawable mDrawableMicTranscribing;

	private List<Drawable> mVolumeLevels;

	private Animation mAnimFadeIn;
	private Animation mAnimFadeOut;
	private Animation mAnimFadeInOutInf;

	private int mVolumeLevel = 0;
	private int mMaxLevel;

	public MicButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAnimations(context);
	}

	public MicButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAnimations(context);
	}


	public MicButton(Context context) {
		super(context);
		initAnimations(context);
	}


	public void setState(Constants.State state) {
		switch(state) {
		case INIT:
			clearAnimation();
			setBackgroundDrawable(mDrawableMic);
			break;
		case RECORDING:
			setBackgroundDrawable(mVolumeLevels.get(0));
			break;
		case LISTENING:
			break;
		case TRANSCRIBING:
			setBackgroundDrawable(mDrawableMicTranscribing);
			startAnimation(mAnimFadeInOutInf);
			break;
		case ERROR:
			clearAnimation();
			setBackgroundDrawable(mDrawableMic);
			break;
		default:
			break;
		}
	}


	public void setVolumeLevel(float rmsdB) {
		int index = (int) ((rmsdB - DB_MIN) / (DB_MAX - DB_MIN) * mMaxLevel);
		int level = Math.min(Math.max(0, index), mMaxLevel);
		if (level != mVolumeLevel) {
			mVolumeLevel = level;
			setBackgroundDrawable(mVolumeLevels.get(level));
		}
	}


	public void fadeIn() {
		Animations.startFadeAnimation(mAnimFadeIn, this, View.VISIBLE);
	}


	public void fadeOut() {
		Animations.startFadeAnimation(mAnimFadeOut, this, View.INVISIBLE);
	}


	private void initAnimations(Context context) {
		Resources res = getResources();
		mDrawableMic = res.getDrawable(R.drawable.button_mic);
		mDrawableMicTranscribing = res.getDrawable(R.drawable.button_mic_transcribing);

		mVolumeLevels = new ArrayList<>();
		mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_0));
		mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_1));
		mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_2));
		mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_3));
		mMaxLevel = mVolumeLevels.size() - 1;

		mAnimFadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
		mAnimFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
		mAnimFadeInOutInf = AnimationUtils.loadAnimation(context, R.anim.fade_inout_inf);
	}
}
