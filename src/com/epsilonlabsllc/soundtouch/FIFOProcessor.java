package com.epsilonlabsllc.soundtouch;

/**
 * Base-class for sound processing routines working in FIFO principle. With this
 * base class it's easy to implement sound processing stages that can be chained
 * together, so that samples that are fed into beginning of the pipe
 * automatically go through all the processing stages.<br><br>When samples are input to
 * this class, they're first processed and then put to the FIFO pipe that's
 * defined as output of this class. This output pipe can be either other
 * processing stage or a FIFO sample buffer.
 */
public class FIFOProcessor extends FIFOSamplePipe {
	/** Internal pipe where processed samples are put. */
	private FIFOSamplePipe output;
	
	/**
	 * Sets the output pipe exactly once.
	 * @param output new output pipe
	 */
	public void setOutPipe(FIFOSamplePipe output) {
		assert(this.output == null) : "The output instance variable must be null.";
		assert(output != null) : "The output parameter cannot be null";
        
        this.output = output;
	}
	
	public FIFOProcessor() {
	}
	
	public FIFOProcessor(FIFOSamplePipe output) {
		this.output = output;
	}

	@Override
	float[] ptrBegin() {
		return output.ptrBegin();
	}

	@Override
	void putSamples(float[] samples) {
		this.output.putSamples(samples);
	}

	@Override
	int receiveSamples(float[] output) {
		return this.output.receiveSamples(output);
	}

	@Override
	int receiveSamples(int maxSamples) {
		return this.output.receiveSamples(maxSamples);
	}

	@Override
	int numSamples() {
		return this.output.numSamples();
	}

	@Override
	boolean isEmpty() {
		return this.output.isEmpty();
	}

	@Override
	void clear() {
		this.output.clear();
	}

	@Override
	int adjustAmountOfSamples(int numSamples) {
		return this.output.adjustAmountOfSamples(numSamples);
	}

}
