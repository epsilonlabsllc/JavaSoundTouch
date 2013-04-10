package com.epsilonlabsllc.soundtouch;

public class TDStretch extends FIFOProcessor {
	/*****************************************************************************
	 * 
	 * Constant definitions
	 * 
	 *****************************************************************************/
	public static final int DEFAULT_SEQUENCE_MS = 0;
	public static final int DEFAULT_SEEKWINDOW_MS = 0;
	public static final int DEFAULT_OVERLAP_MS = 0;

	public static final int SAMPLE_TYPE_SIZE = 4;

	// Adjust tempo param according to tempo, so that variating processing
	// sequence length is used
	// at varius tempo settings, between the given low...top limits
	private static final float AUTOSEQ_TEMPO_LOW = 0.5f; // auto setting low
															// tempo range
															// (-50%)
	private static final float AUTOSEQ_TEMPO_TOP = 2.0f; // auto setting top
															// tempo range
															// (+100%)

	// sequence-ms setting values at above low & top tempo
	private static final float AUTOSEQ_AT_MIN = 125.0f;
	private static final float AUTOSEQ_AT_MAX = 50.0f;
	private static final float AUTOSEQ_K = ((AUTOSEQ_AT_MAX - AUTOSEQ_AT_MIN) / (AUTOSEQ_TEMPO_TOP - AUTOSEQ_TEMPO_LOW));
	private static final float AUTOSEQ_C = (AUTOSEQ_AT_MIN - (AUTOSEQ_K) * (AUTOSEQ_TEMPO_LOW));

	// seek-window-ms setting values at above low & top tempo
	private static final float AUTOSEEK_AT_MIN = 25.0f;
	private static final float AUTOSEEK_AT_MAX = 15.0f;
	private static final float AUTOSEEK_K = ((AUTOSEEK_AT_MAX - AUTOSEEK_AT_MIN) / (AUTOSEQ_TEMPO_TOP - AUTOSEQ_TEMPO_LOW));
	private static final float AUTOSEEK_C = (AUTOSEEK_AT_MIN - (AUTOSEEK_K) * (AUTOSEQ_TEMPO_LOW));

	// Table for the hierarchical mixing position seeking algorithm
	private static final short[][] _scanOffsets = {
			{ 124, 186, 248, 310, 372, 434, 496, 558, 620, 682, 744, 806, 868, 930, 992, 1054, 1116, 1178, 1240, 1302, 1364, 1426, 1488, 0 },
			{ -100, -75, -50, -25, 25, 50, 75, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ -20, -15, -10, -5, 5, 10, 15, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ -4, -3, -2, -1, 1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 121, 114, 97, 114, 98, 105, 108, 32, 104, 99, 117, 111, 116, 100, 110, 117, 111, 115, 0, 0, 0, 0, 0, 0 } };

	protected int channels;
	protected int sampleReq;
	protected float tempo;

	protected SampleSet pMidBuffer;
	protected SampleSet pMidBufferUnaligned;
	protected int overlapLength;
	protected int seekLength;
	protected int seekWindowLength;
	protected int overlapDividerBits;
	protected int slopingDivider;
	protected float nominalSkip;
	protected float skipFract;
	protected FIFOSampleBuffer outputBuffer;
	protected FIFOSampleBuffer inputBuffer;
	protected boolean bQuickSeek;

	protected int sampleRate;
	protected int sequenceMs;
	protected int seekWindowMs;
	protected int overlapMs;
	protected boolean bAutoSeqSetting;
	protected boolean bAutoSeekSetting;

	public TDStretch() {
		bQuickSeek = false;
		channels = 2;

		pMidBuffer = null;
		pMidBufferUnaligned = null;
		overlapLength = 0;

		bAutoSeqSetting = true;
		bAutoSeekSetting = true;

		// outDebt = 0;
		skipFract = 0;

		tempo = 1.0f;
		setParameters(44100, DEFAULT_SEQUENCE_MS, DEFAULT_SEEKWINDOW_MS, DEFAULT_OVERLAP_MS);
		setTempo(1.0f);

		clear();
	}

	public static TDStretch newInstance() {
		return new TDStretch();
	}

	public void setSampleRate(int aSampleRate) {
		// accept only positive parameter values - if zero or negative, use old
		// values instead
		if (aSampleRate > 0) {
			this.sampleRate = aSampleRate;
			calcSeqParameters();
			calculateOverlapLength(overlapMs);
			// set tempo to recalculate 'sampleReq'
			setTempo(tempo);
		}
	}

	// Set new overlap length parameter & reallocate RefMidBuffer if necessary.
	protected void acceptNewOverlapLength(int newOverlapLength) {
		int prevOvl;

		assert (newOverlapLength >= 0);
		prevOvl = overlapLength;
		overlapLength = newOverlapLength;

		if (overlapLength > prevOvl) {
			pMidBufferUnaligned = new SampleSet(overlapLength * 2 + 16 / SAMPLE_TYPE_SIZE);
			// ensure that 'pMidBuffer' is aligned to 16 byte boundary for
			// efficiency
			// pMidBuffer = (SAMPLETYPE
			// *)SOUNDTOUCH_ALIGN_POINTER_16(pMidBufferUnaligned);
			// We don't need to do this ^
			clearMidBuffer();
		}
	}

	// clear cross correlation routine state if necessary
	protected void clearCrossCorrState() {
		// default implementation is empty.
	}

	// / Calculates overlap period length in samples.
	// / Integer version rounds overlap length to closest power of 2
	// / for a divide scaling operation.
	protected void calculateOverlapLength(int aoverlapMs) {
		int newOvl;

		assert (aoverlapMs >= 0);

		// calculate overlap length so that it's power of 2 - thus it's easy to
		// do
		// integer division by right-shifting. Term "-1" at end is to account
		// for
		// the extra most significatnt bit left unused in result by signed
		// multiplication
		overlapDividerBits = _getClosest2Power((sampleRate * aoverlapMs) / 1000.0) - 1;
		if (overlapDividerBits > 9)
			overlapDividerBits = 9;
		if (overlapDividerBits < 3)
			overlapDividerBits = 3;
		newOvl = (int) Math.pow(2.0, (int) overlapDividerBits + 1); // +1 =>
																	// account
																	// for -1
																	// above

		acceptNewOverlapLength(newOvl);

		// calculate sloping divider so that crosscorrelation operation won't
		// overflow 32-bit register. Max. sum of the crosscorrelation sum
		// without
		// divider would be 2^30*(N^3-N)/3, where N = overlap length
		slopingDivider = (newOvl * newOvl - 1) / 3;
	}

	protected double calcCrossCorr(final SampleSet mixingPos, final SampleSet compare) {
		long corr;
		long norm;
		int i;

		corr = norm = 0;
		// Same routine for stereo and mono. For stereo, unroll loop for better
		// efficiency and gives slightly better resolution against rounding.
		// For mono it same routine, just unrolls loop by factor of 4
		for (i = 0; i < channels * overlapLength; i += 4) {
			corr += (mixingPos.samples()[i] * compare.samples()[i] + mixingPos.samples()[i + 1] * compare.samples()[i + 1] + mixingPos.samples()[i + 2]
					* compare.samples()[i + 2] + mixingPos.samples()[i + 3] * compare.samples()[i + 3]) >> overlapDividerBits;
			norm += (mixingPos.samples()[i] * mixingPos.samples()[i] + mixingPos.samples()[i + 1] * mixingPos.samples()[i + 1] + mixingPos.samples()[i + 2]
					* mixingPos.samples()[i + 2] + mixingPos.samples()[i + 3] * mixingPos.samples()[i + 3]) >> overlapDividerBits;
		}

		// Normalize result by dividing by sqrt(norm) - this step is easiest
		// done using floating point operation
		if (norm == 0)
			norm = 1; // to avoid div by zero
		return (double) corr / Math.sqrt((double) norm);
	}

	// Seeks for the optimal overlap-mixing position. The 'stereo' version of
	// the
	// routine
	//
	// The best position is determined as the position where the two overlapped
	// sample sequences are 'most alike', in terms of the highest
	// cross-correlation
	// value over the overlapping period
	protected int seekBestOverlapPositionFull(final SampleSet refPos) {
		int bestOffs;
		double bestCorr, corr;
		int i;

		bestCorr = Float.MIN_NORMAL;
		bestOffs = 0;

		// Scans for the best correlation value by testing each possible
		// position
		// over the permitted range.
		for (i = 0; i < seekLength; i++) {
			// Calculates correlation value for the mixing position
			// corresponding
			// to 'i'
			corr = calcCrossCorr(refPos.shift(channels * i), pMidBuffer);
			// heuristic rule to slightly favour values close to mid of the
			// range
			double tmp = (double) (2 * i - seekLength) / (double) seekLength;
			corr = ((corr + 0.1) * (1.0 - 0.25 * tmp * tmp));

			// Checks for the highest correlation value
			if (corr > bestCorr) {
				bestCorr = corr;
				bestOffs = i;
			}
		}
		// clear cross correlation routine state if necessary (is so e.g. in MMX
		// routines).
		clearCrossCorrState();

		return bestOffs;
	}

	// Seeks for the optimal overlap-mixing position. The 'stereo' version of
	// the
	// routine
	//
	// The best position is determined as the position where the two overlapped
	// sample sequences are 'most alike', in terms of the highest
	// cross-correlation
	// value over the overlapping period
	protected int seekBestOverlapPositionQuick(final SampleSet refPos) {
		int j;
		int bestOffs;
		double bestCorr, corr;
		int scanCount, corrOffset, tempOffset;

		bestCorr = Float.MIN_NORMAL;
		bestOffs = _scanOffsets[0][0];
		corrOffset = 0;
		tempOffset = 0;

		// Scans for the best correlation value using four-pass hierarchical
		// search.
		//
		// The look-up table 'scans' has hierarchical position adjusting steps.
		// In first pass the routine searhes for the highest correlation with
		// relatively coarse steps, then rescans the neighbourhood of the
		// highest
		// correlation with better resolution and so on.
		for (scanCount = 0; scanCount < 4; scanCount++) {
			j = 0;
			while (_scanOffsets[scanCount][j] != 0) {
				tempOffset = corrOffset + _scanOffsets[scanCount][j];
				if (tempOffset >= seekLength)
					break;

				// Calculates correlation value for the mixing position
				// corresponding
				// to 'tempOffset'
				corr = (double) calcCrossCorr(refPos.shift(channels * tempOffset), pMidBuffer);
				// heuristic rule to slightly favour values close to mid of the
				// range
				double tmp = (double) (2 * tempOffset - seekLength) / seekLength;
				corr = ((corr + 0.1) * (1.0 - 0.25 * tmp * tmp));

				// Checks for the highest correlation value
				if (corr > bestCorr) {
					bestCorr = corr;
					bestOffs = tempOffset;
				}
				j++;
			}
			corrOffset = bestOffs;
		}
		// clear cross correlation routine state if necessary (is so e.g. in MMX
		// routines).
		clearCrossCorrState();

		return bestOffs;
	}

	// Seeks for the optimal overlap-mixing position.
	protected int seekBestOverlapPosition(final SampleSet refPos) {
		if (bQuickSeek) {
			return seekBestOverlapPositionQuick(refPos);
		} else {
			return seekBestOverlapPositionFull(refPos);
		}
	}

	// Overlaps samples in 'midBuffer' with the samples in 'input'. The 'Stereo'
	// version of the routine.
	protected void overlapStereo(SampleSet poutput, final SampleSet input) {
		int i;
		short temp;
		int cnt2;

		for (i = 0; i < overlapLength; i++) {
			temp = (short) (overlapLength - i);
			cnt2 = 2 * i;
			poutput.samples()[cnt2] = (input.samples()[cnt2] * i + pMidBuffer.samples()[cnt2] * temp) / overlapLength;
			poutput.samples()[cnt2 + 1] = (input.samples()[cnt2 + 1] * i + pMidBuffer.samples()[cnt2 + 1] * temp) / overlapLength;
		}
	}

	// Calculates the x having the closest 2^x value for the given value
	static int _getClosest2Power(double value) {
		return (int) (Math.log(value) / Math.log(2.0) + 0.5);
	}

	protected void overlapMono(SampleSet pOutput, final SampleSet pInput) {
		int i;
		int m1, m2;

		m1 = 0;
		m2 = (int) overlapLength;

		for (i = 0; i < overlapLength; i++) {
			pOutput.samples()[i] = (pInput.samples()[i] * m1 + pMidBuffer.samples()[i] * m2) / overlapLength;
			m1 += 1;
			m2 -= 1;
		}
	}

	protected void clearMidBuffer() {
		Util.memset(pMidBuffer, 0, 2 * overlapLength);
	}

	// Overlaps samples in 'midBuffer' with the samples in 'pInputBuffer' at
	// position of 'ovlPos'.
	protected final void overlap(SampleSet pOutput, final SampleSet pInput, int ovlPos) {
		if (channels == 2) {
			// stereo sound
			overlapStereo(pOutput, pInput.shift(2 * ovlPos));
		} else {
			// mono sound.
			overlapMono(pOutput, pInput.shift(ovlPos));
		}
	}

	// Calculates processing sequence length according to tempo setting
	protected void calcSeqParameters() {
		double seq, seek;

		if (bAutoSeqSetting) {
			seq = AUTOSEQ_C + AUTOSEQ_K * tempo;
			seq = Util.checkLimits(seq, AUTOSEQ_AT_MAX, AUTOSEQ_AT_MIN);
			sequenceMs = (int) (seq + 0.5);
		}

		if (bAutoSeekSetting) {
			seek = AUTOSEEK_C + AUTOSEEK_K * tempo;
			seek = Util.checkLimits(seek, AUTOSEEK_AT_MAX, AUTOSEEK_AT_MIN);
			seekWindowMs = (int) (seek + 0.5);
		}

		// Update seek window lengths
		seekWindowLength = (sampleRate * sequenceMs) / 1000;
		if (seekWindowLength < 2 * overlapLength) {
			seekWindowLength = 2 * overlapLength;
		}
		seekLength = (sampleRate * seekWindowMs) / 1000;
	}

	// / Changes the tempo of the given sound samples.
	// / Returns amount of samples returned in the "output" buffer.
	// / The maximum amount of samples that can be returned at a time is set by
	// / the 'set_returnBuffer_size' function.
	protected void processSamples() {
		int ovlSkip, offset;
		int temp;

		/*
		 * Removed this small optimization - can introduce a click to sound when
		 * tempo setting crosses the nominal value if (tempo == 1.0f) { // tempo
		 * not changed from the original, so bypass the processing
		 * processNominalTempo(); return; }
		 */

		// Process samples as long as there are enough samples in 'inputBuffer'
		// to form a processing frame.
		while ((int) inputBuffer.numSamples() >= sampleReq) {
			// If tempo differs from the normal ('SCALE'), scan for the best
			// overlapping
			// position
			offset = seekBestOverlapPosition(inputBuffer.ptrBegin());

			// Mix the samples in the 'inputBuffer' at position of 'offset' with
			// the
			// samples in 'midBuffer' using sliding overlapping
			// ... first partially overlap with the end of the previous sequence
			// (that's in 'midBuffer')
			overlap(outputBuffer.ptrEnd((int) overlapLength), inputBuffer.ptrBegin(), (int) offset);
			outputBuffer.putSamples((int) overlapLength);

			// ... then copy sequence samples from 'inputBuffer' to output:

			// length of sequence
			temp = (seekWindowLength - 2 * overlapLength);

			// crosscheck that we don't have buffer overflow...
			if ((int) inputBuffer.numSamples() < (offset + temp + overlapLength * 2)) {
				continue; // just in case, shouldn't really happen
			}

			outputBuffer.putSamples(inputBuffer.ptrBegin().shift(channels * (offset + overlapLength)).setSize(temp));

			// Copies the end of the current sequence from 'inputBuffer' to
			// 'midBuffer' for being mixed with the beginning of the next
			// processing sequence and so on
			assert ((offset + temp + overlapLength * 2) <= (int) inputBuffer.numSamples());
			Util.memcpy(pMidBuffer, inputBuffer.ptrBegin().shift(channels * (offset + temp + overlapLength)), channels * SAMPLE_TYPE_SIZE * overlapLength);

			// Remove the processed samples from the input buffer. Update
			// the difference between integer & nominal skip step to 'skipFract'
			// in order to prevent the error from accumulating over time.
			skipFract += nominalSkip; // real skip size
			ovlSkip = (int) skipFract; // rounded to integer skip
			skipFract -= ovlSkip; // maintain the fraction part, i.e. real vs.
									// integer skip
			inputBuffer.receiveSamples((int) ovlSkip);
		}
	}

	// / Returns the output buffer object
	public FIFOSamplePipe getOutput() {
		return outputBuffer;
	};

	// / Returns the input buffer object
	public FIFOSamplePipe getInput() {
		return inputBuffer;
	};

	// / Sets new target tempo. Normal tempo = 'SCALE', smaller values represent
	// slower
	// / tempo, larger faster tempo.
	public void setTempo(float newTempo) {
		int intskip;

		tempo = newTempo;

		// Calculate new sequence duration
		calcSeqParameters();

		// Calculate ideal skip length (according to tempo value)
		nominalSkip = tempo * (seekWindowLength - overlapLength);
		intskip = (int) (nominalSkip + 0.5f);

		// Calculate how many samples are needed in the 'inputBuffer' to
		// process another batch of samples
		// sampleReq = max(intskip + overlapLength, seekWindowLength) +
		// seekLength / 2;
		sampleReq = Util.max(intskip + overlapLength, seekWindowLength) + seekLength;
	}

	// / Returns nonzero if there aren't any samples available for outputting.
	public void clear() {
		outputBuffer.clear();
		clearInput();
	}

	// / Clears the input buffer
	public void clearInput() {
		inputBuffer.clear();
		clearMidBuffer();
	}

	// / Sets the number of channels, 1 = mono, 2 = stereo
	public void setChannels(int numChannels) {
		assert (numChannels > 0);
		if (channels == numChannels)
			return;
		assert (numChannels == 1 || numChannels == 2);

		channels = numChannels;
		inputBuffer.setChannels(channels);
		outputBuffer.setChannels(channels);
	}

	// / Enables/disables the quick position seeking algorithm. Zero to disable,
	// / nonzero to enable
	public void enableQuickSeek(boolean enable) {
		bQuickSeek = enable;
	}

	// / Returns nonzero if the quick seeking algorithm is enabled.
	public boolean isQuickSeekEnabled() {
		return bQuickSeek;
	}

	// / Sets routine control parameters. These control are certain time
	// constants
	// / defining how the sound is stretched to the desired duration.
	//
	// / 'sampleRate' = sample rate of the sound
	// / 'sequenceMS' = one processing sequence length in milliseconds
	// / 'seekwindowMS' = seeking window length for scanning the best
	// overlapping
	// / position
	// / 'overlapMS' = overlapping length
	public void setParameters(int aSampleRate, // /< Samplerate of sound being
												// processed (Hz)
			int aSequenceMS, // /< Single processing sequence length (ms)
			int aSeekWindowMS, // /< Offset seeking window length (ms)
			int aOverlapMS // /< Sequence overlapping length (ms)
	) {
		// accept only positive parameter values - if zero or negative, use old
		// values instead
		if (aSampleRate > 0)
			this.sampleRate = aSampleRate;
		if (aOverlapMS > 0)
			this.overlapMs = aOverlapMS;

		if (aSequenceMS > 0) {
			this.sequenceMs = aSequenceMS;
			bAutoSeqSetting = false;
		} else if (aSequenceMS == 0) {
			// if zero, use automatic setting
			bAutoSeqSetting = true;
		}

		if (aSeekWindowMS > 0) {
			this.seekWindowMs = aSeekWindowMS;
			bAutoSeekSetting = false;
		} else if (aSeekWindowMS == 0) {
			// if zero, use automatic setting
			bAutoSeekSetting = true;
		}

		calcSeqParameters();
		calculateOverlapLength(overlapMs);

		// set tempo to recalculate 'sampleReq'
		setTempo(tempo);
	}

	// / Get routine control parameters, see setParameters() function.
	// / Any of the parameters to this function can be NULL, in such case
	// corresponding parameter
	// / value isn't returned.
	// TODO firgure out where this is used to replace pointer logic
	// public void getParameters(Integer pSampleRate, Integer pSequenceMs,
	// Integer pSeekWindowMs, Integer pOverlapMs) {
	// if (pSampleRate == null)
	// {
	// pSampleRate = sampleRate;
	// }
	//
	// if (pSequenceMs == null)
	// {
	// pSequenceMs = (bAutoSeqSetting) ? (USE_AUTO_SEQUENCE_LEN) : sequenceMs;
	// }
	//
	// if (pSeekWindowMs == null)
	// {
	// pSeekWindowMs = (bAutoSeekSetting) ? (USE_AUTO_SEEKWINDOW_LEN) :
	// seekWindowMs;
	// }
	//
	// if (pOverlapMs == null)
	// {
	// pOverlapMs = overlapMs;
	// }
	// }

	// / Adds 'numsamples' pcs of samples from the 'samples' memory position
	// into
	// / the input of the object.
	public void putSamples(SampleSet samples) {
		// Add the samples into the input buffer
		inputBuffer.putSamples(samples);
		// Process the samples in input buffer
		processSamples();
	}

	// / return nominal input sample requirement for triggering a processing
	// batch
	public int getInputSampleReq() {
		return (int) (nominalSkip + 0.5);
	}

	// / return nominal output sample amount when running a processing batch
	public int getOutputBatchSize() {
		return seekWindowLength - overlapLength;
	}
}
