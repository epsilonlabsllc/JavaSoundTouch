package com.epsilonlabsllc.soundtouch;

public abstract class FIFOSamplePipe {
	/**
	 * Returns a pointer to the beginning of the output samples. This function
	 * is provided for accessing the output samples directly. Please be careful
	 * for not to corrupt the book-keeping!
	 * 
	 * When using this function to output samples, also remember to 'remove' the
	 * output samples from the buffer by calling the
	 * 'receiveSamples(numSamples)' function
	 * 
	 * @return the output sample array
	 */
	abstract Sample[] ptrBegin();

	/**
	 * Adds 'numSamples' pcs of samples from the 'samples' memory position to
	 * the sample buffer.
	 * 
	 * @param samples
	 */
	abstract void putSamples(Sample[] samples);

	/**
	 * Moves samples from the 'other' pipe instance to this instance.
	 * 
	 * @param other
	 *            Other pipe instance where from the receive the data.
	 */
	abstract void moveSamples(FIFOSamplePipe other);

	/**
	 * Output samples from beginning of the sample buffer. Copies requested
	 * samples to output buffer and removes them from the sample buffer. If
	 * there are less than 'numsample' samples in the buffer, returns all that
	 * available.
	 * 
	 * @param output
	 *            Buffer where to copy output samples.
	 * @return Number of samples returned.
	 */
	abstract int receiveSamples(Sample[] output);

	/**
	 * Adjusts book-keeping so that given number of samples are removed from
	 * beginning of the sample buffer without copying them anywhere. Used to
	 * reduce the number of samples in the buffer when accessing the sample
	 * buffer directly with 'ptrBegin' function.
	 * 
	 * @param maxSamples
	 *            Remove this many samples from the beginning of pipe.
	 * @return ???
	 */
	abstract int receiveSamples(int maxSamples);

	/**
	 * Returns number of samples currently available.
	 * 
	 * @return number of samples currently available
	 */
	abstract int numSamples();

	/**
	 * Returns true if there aren't any samples available for outputting.
	 * 
	 * @return true if there aren't any samples available for outputting.
	 */
	abstract boolean isEmpty();

	/**
	 * Clears all the samples.
	 */
	abstract void clear();

	/**
	 * allow trimming (downwards) amount of samples in pipeline.
	 * 
	 * @param numSamples
	 *            limit on amount of samples in pipeline?
	 * @return adjusted amount of samples
	 */
	abstract int adjustAmountOfSamples(int numSamples);
}
