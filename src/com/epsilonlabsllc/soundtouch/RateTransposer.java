package com.epsilonlabsllc.soundtouch;

public abstract class RateTransposer extends FIFOProcessor {
	protected AAFilter pAAFilter;

	protected float fRate;

	protected int numChannels;

	/**
	 * Buffer for collecting samples to feed the anti-alias filter between two
	 * batches
	 */
	protected FIFOSampleBuffer storeBuffer;

	/**
	 * Buffer for keeping samples between transposing & anti-alias filter
	 */
	protected FIFOSampleBuffer tempBuffer;

	/**
	 * Output sample buffer
	 */
	protected FIFOSampleBuffer outputBuffer;

	protected boolean bUseAAFilter;

	protected int iSlopeCount;
	protected int iRate;
	protected float sPrevSampleL, sPrevSampleR;

	protected abstract void resetRegisters();

	protected abstract int transposeStereo(SampleSet dest, final SampleSet src);

	protected abstract int transposeMono(SampleSet dest, final SampleSet src);

	/**
	 * Transposes the sample rate of the given samples using linear
	 * interpolation. Returns the number of samples returned in the "dest"
	 * buffer
	 * 
	 * @param dest
	 * @param src
	 * @return
	 */
	protected int transpose(SampleSet dest, final SampleSet src) {
		if (numChannels == 2) {
			return transposeStereo(dest, src);
		} else {
			return transposeMono(dest, src);
		}
	}

	/**
	 * Transposes down the sample rate, causing the observed playback 'rate' of
	 * the sound to increase
	 * 
	 * @param src
	 */
	protected void downsample(SampleSet src) {
		int count, sizeTemp;

		// If the parameter 'uRate' value is larger than 'SCALE', first apply
		// the
		// anti-alias filter to remove high frequencies (prevent them from
		// folding
		// over the lover frequencies), then transpose.

		// Add the new samples to the end of the storeBuffer
		storeBuffer.putSamples(src);

		// Anti-alias filter the samples to prevent folding and output the
		// filtered
		// data to tempBuffer. Note : because of the FIR filter length, the
		// filtering routine takes in 'filter_length' more samples than it
		// outputs.
		assert (tempBuffer.isEmpty());
		sizeTemp = storeBuffer.numSamples();

		count = pAAFilter.evaluate(tempBuffer.ptrEnd(sizeTemp),
				storeBuffer.ptrBegin(), (int) numChannels);

		if (count == 0)
			return;

		// Remove the filtered samples from 'storeBuffer'
		storeBuffer.receiveSamples(count);

		// Transpose the samples (+16 is to reserve some slack in the
		// destination buffer)
		sizeTemp = (int) ((float) src.size() / fRate + 16.0f);
		count = transpose(outputBuffer.ptrEnd(sizeTemp), tempBuffer.ptrBegin());
		outputBuffer.putSamples(count);
	}

	/**
	 * Transposes up the sample rate, causing the observed playback 'rate' of
	 * the sound to decrease
	 * 
	 * @param src
	 */
	protected void upsample(SampleSet src) {
		int count, sizeTemp, num;

		// If the parameter 'uRate' value is smaller than 'SCALE', first
		// transpose
		// the samples and then apply the anti-alias filter to remove aliasing.

		// First check that there's enough room in 'storeBuffer'
		// (+16 is to reserve some slack in the destination buffer)
		sizeTemp = (int) ((float) src.size() / this.fRate + 16.0f);

		// Transpose the samples, store the result into the end of "storeBuffer"
		count = transpose(storeBuffer.ptrEnd(sizeTemp), src);
		storeBuffer.putSamples(count);

		// Apply the anti-alias filter to samples in "store output", output the
		// result to "dest"
		num = storeBuffer.numSamples();
		count = pAAFilter.evaluate(outputBuffer.ptrEnd(num),
				storeBuffer.ptrBegin(), (int) this.numChannels);
		outputBuffer.putSamples(count);

		// Remove the processed samples from "storeBuffer"
		storeBuffer.receiveSamples(count);
	}

	public RateTransposer() {
		this.numChannels = 2; // Default to stereo
		this.bUseAAFilter = true;
		this.fRate = 0.0f;

		// Instantiates the anti-alias filter with default tap length
		// of 32
		pAAFilter = new AAFilter(32);
	}

	/**
	 * Transposes sample rate by applying anti-alias filter to prevent folding.
	 * Returns amount of samples returned in the "dest" buffer. The maximum
	 * amount of samples that can be returned at a time is set by the
	 * 'set_returnBuffer_size' function.
	 * 
	 * @param samples
	 *            samples to be processed?
	 */
	protected void processSamples(SampleSet samples) {
		int count;
		int sizeReq;

		if (samples.size() == 0)
			return;
		assert (pAAFilter != null) : "The pAAFilter cannot be null.";

		// If anti-alias filter is turned off, simply transpose without applying
		// the filter
		if (bUseAAFilter == false) {
			sizeReq = (int) ((float) samples.size() / fRate + 1.0f);
			count = transpose(outputBuffer.ptrEnd(sizeReq), samples);
			outputBuffer.putSamples(count);
			return;
		}

		// Transpose with anti-alias filter
		if (fRate < 1.0f) {
			upsample(samples);
		} else {
			downsample(samples);
		}
	}

	/**
	 * Use this function instead of "new" operator to create a new instance of
	 * this class. This function automatically chooses a correct implementation,
	 * depending on if integer ot floating point arithmetics are to be used.
	 * 
	 * @return a new <code>RateTransponser</code>
	 */
	public static RateTransposer newInstance() {
		// Currently the only implementation
		return new RateTransposerInteger();
	}

	/**
	 * Returns the output buffer object
	 * 
	 * @return the output buffer
	 */
	public FIFOSamplePipe getOutput() {
		return this.outputBuffer;
	}

	/**
	 * Returns the store buffer object
	 * 
	 * @return the store buffer
	 */
	public FIFOSamplePipe getStore() {
		return this.storeBuffer;
	}

	/**
	 * Return anti-alias filter object
	 * 
	 * @return anti-alias filter
	 */
	public AAFilter getAAFilter() {
		return this.pAAFilter;
	}

	/**
	 * Enables/disables the anti-alias filter. Zero to disable, nonzero to
	 * enable
	 * 
	 * @param aaFilterEnabled
	 */
	public void enableAAFilter(boolean aaFilterEnabled) {
		this.bUseAAFilter = aaFilterEnabled;
	}

	/**
	 * Returns true if anti-alias filter is enabled.
	 * 
	 * @return true if anti-alias filter is enabled
	 */
	public boolean isAAFilterEnabled() {
		return this.bUseAAFilter;
	}

	/**
	 * Sets new target rate. Normal rate = 1.0, smaller values represent slower
	 * rate, larger faster rates.
	 * 
	 * @param newRate
	 *            the new target rate
	 */
	public void setRate(float newRate) {
		double fCutoff;

		this.fRate = newRate;

		// design a new anti-alias filter
		if (newRate > 1.0f) {
			fCutoff = 0.5f / newRate;
		} else {
			fCutoff = 0.5f * newRate;
		}

		this.pAAFilter.setCutoffFreq(fCutoff);
	}

	/**
	 * Sets the number of channels, 1 = mono, 2 = stereo
	 * 
	 * @param channels
	 *            either mono or stereo
	 */
	public void setChannels(int nChannels) {
		assert (nChannels > 0);
		if (numChannels == nChannels)
			return;

		assert (nChannels == 1 || nChannels == 2);
		numChannels = nChannels;

		storeBuffer.setChannels(numChannels);
		tempBuffer.setChannels(numChannels);
		outputBuffer.setChannels(numChannels);

		// Inits the linear interpolation registers
		resetRegisters();
	}

	/**
	 * Adds 'numSamples' pcs of samples from the 'samples' memory position into
	 * the input of the object.
	 * 
	 * @param samples
	 *            the new samples
	 */
	@Override
	public void putSamples(SampleSet samples) {
		processSamples(samples);
	}

	/**
	 * Clears all the samples in the object
	 * 
	 */
	@Override
	public void clear() {
		outputBuffer.clear();
	    storeBuffer.clear();
	}
	
	@Override
	boolean isEmpty() {
		boolean res;

	    res = super.isEmpty();
	    if (res == false) return false;
	    return storeBuffer.isEmpty();
	}

}
