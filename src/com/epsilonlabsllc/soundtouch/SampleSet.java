package com.epsilonlabsllc.soundtouch;

public class SampleSet {
	private int[] samples;
	private int size;
	
	public SampleSet(int[] samples) {
		this.samples = samples;
		this.size = samples.length;
	}
	
	public void setSamples(int[] samples) {
		this.samples = samples;
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	
	public int[] samples() {
		return this.samples;
	}
	
	public int size() {
		return this.size;
	}
}
