package com.epsilonlabsllc.soundtouch;

public class SampleVector {
	private final int[] samples;
	private final int size;
	
	public SampleVector(int size) {
		this.samples = new int[size];
		this.size = size;
	}
	
	public SampleVector(int[] samples) {
		this.samples = samples;
		this.size = samples.length;
	}
	
	public SampleVector(int[] samples, int size) {
		this.samples = samples;
		this.size = size;
	}
	
	public int get(int index) {
		return this.samples[index];
	}
	
	public void set(int index, int value) {
		this.samples[index] = value;
	}
	
	public SampleVector size(int newSize) {
		return new SampleVector(this.samples, newSize);
	}
	
	public int size() {
		return this.size;
	}
	
	/**
	 * The equivalent of advancing a pointer e.g.<pre>
	 * <code>int *ptr;
	 * int *newPtr = ptr + 5;</code></pre>
	 * @param shiftDistance
	 * @return
	 */
	public SampleVector shift(int shiftDistance) {
		return new OffsetSampleVector(this.samples, this.size, shiftDistance);
	}
}
