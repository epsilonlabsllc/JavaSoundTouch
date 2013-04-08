package com.epsilonlabsllc.soundtouch;

public class SampleSet {
	private float[] samples;
	private int size;
	
	public SampleSet(float[] samples) {
		this.samples = samples;
	}
	
	public void setSamples(float[] samples) {
		this.samples = samples;
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	
	public float[] samples() {
		return this.samples;
	}
	
	public int size() {
		return this.size;
	}
}
