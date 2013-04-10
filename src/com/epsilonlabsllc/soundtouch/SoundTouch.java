package com.epsilonlabsllc.soundtouch;

public class SoundTouch extends FIFOProcessor {
	public static final int SAMPLE_TYPE_SIZE = 4;
	
	private RateTransposer pRateTransposer;
	private TDStretch pTDStretch;

	// / Internal pipe where processed samples are put.
	protected FIFOSamplePipe output;

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

	public SoundTouch() {
		pRateTransposer = RateTransposer.newInstance();
		pTDStretch = TDStretch.newInstance();

		setOutPipe(pTDStretch);
		rate = tempo = 0;

		virtualPitch = 1.0f;
		virtualRate = 1.0f;
		virtualTempo = 1.0f;

		calcEffectiveRateAndTempo();

		channels = 0;
		bSrateSet = false;
	}

	private void calcEffectiveRateAndTempo() {
		float oldTempo = tempo;
		float oldRate = rate;

		tempo = virtualTempo / virtualPitch;
		rate = virtualPitch * virtualRate;

		if (!Util.testFloatEqual(rate, oldRate))
			pRateTransposer.setRate(rate);
		if (!Util.testFloatEqual(tempo, oldTempo))
			pTDStretch.setTempo(tempo);

		if (SoundTouchSettings.SOUNDTOUCH_PREVENT_CLICK_AT_RATE_CROSSOVER) {
			if (rate <= 1.0f) {
				if (output != pTDStretch) {
					FIFOSamplePipe tempoOut;

					assert (output == pRateTransposer);
					// move samples in the current output buffer to the output
					// of pTDStretch
					tempoOut = pTDStretch.getOutput();
					tempoOut.moveSamples(output);
					// move samples in pitch transposer's store buffer to tempo
					// changer's input
					pTDStretch.moveSamples(pRateTransposer.getStore());

					output = pTDStretch;
				}
			} else if (output != pRateTransposer) {
				FIFOSamplePipe transOut;

				assert (output == pTDStretch);
				// move samples in the current output buffer to the output of
				// pRateTransposer
				transOut = pRateTransposer.getOutput();
				transOut.moveSamples(output);
				// move samples in tempo changer's input to pitch transposer's
				// input
				pRateTransposer.moveSamples(pTDStretch.getInput());

				output = pRateTransposer;
			}
		} else {
			if (output != pRateTransposer) {
				FIFOSamplePipe transOut;

				assert (output == pTDStretch);
				// move samples in the current output buffer to the output of
				// pRateTransposer
				transOut = pRateTransposer.getOutput();
				transOut.moveSamples(output);
				// move samples in tempo changer's input to pitch transposer's
				// input
				pRateTransposer.moveSamples(pTDStretch.getInput());

				output = pRateTransposer;
			}
		}
	}

	// Sets new rate control value. Normal rate = 1.0, smaller values
	// represent slower rate, larger faster rates.
	void setRate(float newRate) {
		virtualRate = newRate;
		calcEffectiveRateAndTempo();
	}

	// Sets new rate control value as a difference in percents compared
	// to the original rate (-50 .. +100 %)
	void setRateChange(float newRate) {
		virtualRate = 1.0f + 0.01f * newRate;
		calcEffectiveRateAndTempo();
	}

	// Sets new tempo control value. Normal tempo = 1.0, smaller values
	// represent slower tempo, larger faster tempo.
	void setTempo(float newTempo) {
		virtualTempo = newTempo;
		calcEffectiveRateAndTempo();
	}

	// Sets new tempo control value as a difference in percents compared
	// to the original tempo (-50 .. +100 %)
	void setTempoChange(float newTempo) {
		virtualTempo = 1.0f + 0.01f * newTempo;
		calcEffectiveRateAndTempo();
	}

	// Sets new pitch control value. Original pitch = 1.0, smaller values
	// represent lower pitches, larger values higher pitch.
	void setPitch(float newPitch) {
		virtualPitch = newPitch;
		calcEffectiveRateAndTempo();
	}

	// Sets pitch change in octaves compared to the original pitch
	// (-1.00 .. +1.00)
	void setPitchOctaves(float newPitch) {
		virtualPitch = (float) Math.exp(0.69314718056f * newPitch);
		calcEffectiveRateAndTempo();
	}

	// Sets pitch change in semi-tones compared to the original pitch
	// (-12 .. +12)
	void setPitchSemiTones(int newPitch) {
		setPitchOctaves((float) newPitch / 12.0f);
	}

	void setPitchSemiTones(float newPitch) {
		setPitchOctaves(newPitch / 12.0f);
	}

	// Sets sample rate.
	void setSampleRate(int srate) {
		bSrateSet = true;
		// set sample rate, leave other tempo changer parameters as they are.
		pTDStretch.setSampleRate(srate);
	}
	
	void putSamples(SampleVector samples) {
		// Adds 'numSamples' pcs of samples from the 'samples' memory position into
		// the input of the object.
		if (this.bSrateSet == false) {
			throw new RuntimeException("SoundTouch : Sample rate not defined");
		} else if (channels == 0) {
			throw new RuntimeException("SoundTouch : Number of channels not defined");
		}
		
		if (SoundTouchSettings.SOUNDTOUCH_PREVENT_CLICK_AT_RATE_CROSSOVER) {
			if (rate <= 1.0f) 
		    {
		        // transpose the rate down, output the transposed sound to tempo changer buffer
		        assert(output == pTDStretch);
		        pRateTransposer.putSamples(samples);
		        pTDStretch.moveSamples(pRateTransposer);
		    } 
		    else {
		        // evaluate the tempo changer, then transpose the rate up, 
		        assert(output == pRateTransposer);
		        pTDStretch.putSamples(samples);
		        pRateTransposer.moveSamples(pTDStretch);
		    }
		} else {
			// evaluate the tempo changer, then transpose the rate up, 
	        assert(output == pRateTransposer);
	        pTDStretch.putSamples(samples);
	        pRateTransposer.moveSamples(pTDStretch);
		}
	}
	
	// Flushes the last samples from the processing pipeline to the output.
	// Clears also the internal processing buffers.
	//
	// Note: This function is meant for extracting the last samples of a sound
	// stream. This function may introduce additional blank samples in the end
	// of the sound stream, and thus it's not recommended to call this function
	// in the middle of a sound stream.
	void flush()
	{
	    int i;
	    int nUnprocessed;
	    int nOut;
	    SampleVector buff = new SampleVector(64*2);   // note: allocate 2*64 to cater 64 sample frames of stereo sound

	    // check how many samples still await processing, and scale
	    // that by tempo & rate to get expected output sample count
	    nUnprocessed = numUnprocessedSamples();
	    nUnprocessed = (int)((double)nUnprocessed / (tempo * rate) + 0.5);

	    nOut = numSamples();        // ready samples currently in buffer ...
	    nOut += nUnprocessed;       // ... and how many we expect there to be in the end
	    
	    Util.memset(buff, 0, 64 * channels);
	    // "Push" the last active samples out from the processing pipeline by
	    // feeding blank samples into the processing pipeline until new, 
	    // processed samples appear in the output (not however, more than 
	    // 8ksamples in any case)
	    for (i = 0; i < 128; i ++) 
	    {
	        putSamples(buff.size(64));
	        if ((int)numSamples() >= nOut) 
	        {
	            // Enough new samples have appeared into the output!
	            // As samples come from processing with bigger chunks, now truncate it
	            // back to maximum "nOut" samples to improve duration accuracy 
	            adjustAmountOfSamples(nOut);

	            // finish
	            break;  
	        }
	    }

	    // Clear working buffers
	    pRateTransposer.clear();
	    pTDStretch.clearInput();
	    // yet leave the 'tempoChanger' output intouched as that's where the
	    // flushed samples are!
	}

	private int numUnprocessedSamples() {
		// TODO Auto-generated method stub
		return 0;
	}
}
