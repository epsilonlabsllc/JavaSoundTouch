package com.epsilonlabsllc.soundtouch;

public class FIFOSampleBuffer extends FIFOSamplePipe {
	public static final int SAMPLE_TYPE_SIZE = 4;

	// / Sample buffer.
	private SampleVector buffer;

	// Raw unaligned buffer memory. 'buffer' is made aligned by pointing it to
	// first
	// 16-byte aligned location of this buffer
	@SuppressWarnings("unused")
	private SampleVector bufferUnaligned;

	// / Sample buffer size in bytes
	private int sizeInBytes;

	// / How many samples are currently in buffer.
	private int samplesInBuffer;

	// / Channels, 1=mono, 2=stereo.
	private int channels;

	// / Current position pointer to the buffer. This pointer is increased when
	// samples are
	// / removed from the pipe so that it's necessary to actually rewind buffer
	// (move data)
	// / only new data when is put to the pipe.
	private int bufferPos;

	public FIFOSampleBuffer(int numChannels) {
		assert (numChannels > 0);
		sizeInBytes = 0; // reasonable initial value
		buffer = null;
		bufferUnaligned = null;
		samplesInBuffer = 0;
		bufferPos = 0;
		channels = (int) numChannels;
		ensureCapacity(32); // allocate initial capacity
	}

	// Returns the current buffer capacity in terms of samples
	final int getCapacity() {
		return sizeInBytes / (channels * SAMPLE_TYPE_SIZE);
	}

	// Ensures that the buffer has enought capacity, i.e. space for _at least_
	// 'capacityRequirement' number of samples. The buffer is grown in steps of
	// 4 kilobytes to eliminate the need for frequently growing up the buffer,
	// as well as to round the buffer size up to the virtual memory page size.
	private void ensureCapacity(int capacityRequirement) {
		SampleVector tempUnaligned, temp;

		if (capacityRequirement > getCapacity()) {
			// enlarge the buffer in 4kbyte steps (round up to next 4k boundary)
			sizeInBytes = (capacityRequirement * channels * SAMPLE_TYPE_SIZE + 4095) & Util.toUnsignedInt(-4096);
			assert (sizeInBytes % 2 == 0);
			tempUnaligned = new SampleVector(sizeInBytes / SAMPLE_TYPE_SIZE + 16 / SAMPLE_TYPE_SIZE);
			// Align the buffer to begin at 16byte cache line boundary for
			// optimal performance
			temp = Util.alignPointer(tempUnaligned);
			if (samplesInBuffer != 0) {
				Util.memcpy(temp, ptrBegin(), samplesInBuffer * channels);
			}
			buffer = temp;
			bufferUnaligned = tempUnaligned;
			bufferPos = 0;
		} else {
			// simply rewind the buffer (if necessary)
			rewind();
		}
	}

	// Returns a pointer to the beginning of the currently non-outputted
	// samples.
	// This function is provided for accessing the output samples directly.
	// Please be careful!
	//
	// When using this function to output samples, also remember to 'remove' the
	// outputted samples from the buffer by calling the
	// 'receiveSamples(numSamples)' function
	@Override
	SampleVector ptrBegin() {
		assert (buffer != null);
		return buffer.shift(bufferPos * channels);
	}

	// Adds 'numSamples' pcs of samples from the 'samples' memory position to
	// the sample buffer.
	@Override
	void putSamples(SampleVector samples) {
		Util.memcpy(ptrEnd(samples.size()), samples, samples.size() * channels);
		samplesInBuffer += samples.size();
	}

	// Output samples from beginning of the sample buffer. Copies demanded number
	// of samples to output and removes them from the sample buffer. If there
	// are less than 'numsample' samples in the buffer, returns all available.
	//
	// Returns number of samples copied.
	@Override
	int receiveSamples(SampleVector output) {
		int num;

	    num = (output.size() > samplesInBuffer) ? samplesInBuffer : output.size();

	    Util.memcpy(output, ptrBegin(), channels * num);
	    return receiveSamples(num);
	}

	// Removes samples from the beginning of the sample buffer without copying them
	// anywhere. Used to reduce the number of samples in the buffer, when accessing
	// the sample buffer with the 'ptrBegin' function.
	@Override
	int receiveSamples(int maxSamples) {
		if (maxSamples >= samplesInBuffer)
	    {
	        int temp;

	        temp = samplesInBuffer;
	        samplesInBuffer = 0;
	        return temp;
	    }

	    samplesInBuffer -= maxSamples;
	    bufferPos += maxSamples;

	    return maxSamples;
	}

	@Override
	final int numSamples() {
		return samplesInBuffer;
	}

	// Returns nonzero if the sample buffer is empty
	@Override
	final boolean isEmpty() {
		return samplesInBuffer == 0;
	}

	// Clears the sample buffer
	@Override
	void clear() {
		samplesInBuffer = 0;
	    bufferPos = 0;
	}

	/// allow trimming (downwards) amount of samples in pipeline.
	/// Returns adjusted amount of samples
	@Override
	int adjustAmountOfSamples(int numSamples) {
		if (numSamples < samplesInBuffer)
	    {
	        samplesInBuffer = numSamples;
	    }
	    return samplesInBuffer;
	}

	// Returns a pointer to the end of the used part of the sample buffer (i.e.
	// where the new samples are to be inserted). This function may be used for
	// inserting new samples into the sample buffer directly. Please be careful!
	//
	// Parameter 'slackCapacity' tells the function how much free capacity (in
	// terms of samples) there _at least_ should be, in order to the caller to
	// succesfully insert all the required samples to the buffer. When
	// necessary,
	// the function grows the buffer size to comply with this requirement.
	//
	// When using this function as means for inserting new samples, also
	// remember
	// to increase the sample count afterwards, by calling the
	// 'putSamples(numSamples)' function.
	public SampleVector ptrEnd(int slackCapacity) {
		ensureCapacity(samplesInBuffer + slackCapacity);
		return buffer.shift(samplesInBuffer * channels);
	}

	// Increases the number of samples in the buffer without copying any actual
	// samples.
	//
	// This function is used to update the number of samples in the sample
	// buffer
	// when accessing the buffer directly with 'ptrEnd' function. Please be
	// careful though!
	public void putSamples(int nSamples) {
		int req = samplesInBuffer + nSamples;
		ensureCapacity(req);
		samplesInBuffer += nSamples;
	}

	// if output location pointer 'bufferPos' isn't zero, 'rewinds' the buffer
	// and zeroes this pointer by copying samples from the 'bufferPos' pointer
	// location on to the beginning of the buffer.
	void rewind() {
		if (buffer != null && bufferPos != 0) {
			Util.memmove(buffer, ptrBegin(), channels * samplesInBuffer);
			bufferPos = 0;
		}
	}

	public void setChannels(int numChannels) {
		int usedBytes;

		assert (numChannels > 0);
		usedBytes = channels * samplesInBuffer;
		channels = (int) numChannels;
		samplesInBuffer = usedBytes / channels;
	}

}
