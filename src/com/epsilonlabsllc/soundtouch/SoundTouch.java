package com.epsilonlabsllc.soundtouch;

public class SoundTouch extends FIFOProcessor {
	private RateTransposer pRateTransposer;
	private TDStretch pTDStretch;

	// / Virtual pitch parameter. Effective rate & tempo are calculated from
	// these parameters.
	private float virtualRate;

	// / Virtual pitch parameter. Effective rate & tempo are calculated from
	// these parameters.
	private float virtualTempo;

	// / Virtual pitch parameter. Effective rate & tempo are calculated from
	// these parameters.
	private float virtualPitch;

	// / Flag: Has sample rate been set?
	private boolean bSrateSet;

	// / Number of channels
	protected int channels;

	// / Effective 'rate' value calculated from 'virtualRate', 'virtualTempo'
	// and 'virtualPitch'
	protected float rate;

	// / Effective 'tempo' value calculated from 'virtualRate', 'virtualTempo'
	// and 'virtualPitch'
	protected float tempo;
}
