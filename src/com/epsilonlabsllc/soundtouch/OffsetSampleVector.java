package com.epsilonlabsllc.soundtouch;

public class OffsetSampleVector extends SampleVector {
	private final int offset;
	
	public OffsetSampleVector(int[] samples, int size, int offset) {
		super(samples, size);
		this.offset = offset;
	}
	
	@Override
	public int get(int index) {
		assert (index >= 0);
		return super.get(index + this.offset);
	}
	
	@Override
	public void set(int index, int value) {
		assert (index >= 0);
		super.set(index + this.offset, value);
	}
	
	@Override
	public SampleVector shift(int shiftDistance) {
		return super.shift(shiftDistance + this.offset);
	}
}
