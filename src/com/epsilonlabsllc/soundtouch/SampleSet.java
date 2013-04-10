package com.epsilonlabsllc.soundtouch;

public class SampleSet {
	private final int[] samples;
	private int size;
	
	public SampleSet(int size) {
		this.samples = new int[size];
		this.size = size;
	}
	
	public SampleSet(int[] samples) {
		this.samples = samples;
		this.size = samples.length;
	}
	
	public SampleSet(int[] samples, int size) {
		this.samples = samples;
		this.size = size;
	}
	
	public SampleSet setSamples(int[] samples) {
		return new SampleSet(samples);
	}
	
	public SampleSet setSize(int size) {
		this.size = size;
		return this;
	}
	
	public int[] samples() {
		return this.samples;
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
	public SampleSet shift(int shiftDistance) {
		int[] newSamples = new int[samples.length - size];
		for (int i = 0; i < newSamples.length; i++) {
			newSamples[i] = samples[i + shiftDistance];
		}
		
		return new SampleSet(newSamples);
	}
}
