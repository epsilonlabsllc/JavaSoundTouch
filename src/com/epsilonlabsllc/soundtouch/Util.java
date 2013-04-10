package com.epsilonlabsllc.soundtouch;

public class Util {
	public static <T> void memcpy(SampleSet dest, SampleSet src, int num) {
		assert (dest != null);
		assert (src != null);
		assert (num >= 0);
		
		assert (src.size() >= num);
		assert (dest.size() >= src.size());
		
		for (int i = 0; i < num; i++) {
			dest.samples()[i] = src.samples()[i];
		}
	}
	
	public static void memset(SampleSet ptr, int value, int num) {
		for (int i = 0; i < num; i++) {
			ptr.samples()[i] = value;
		}
	}
	
	public static boolean testFloatEqual(double a, double b) {
		return Math.abs(a - b) < 1e-10;
	}
	
	public static final int max(int x, int y) {
		return ((x) > (y)) ? (x) : (y);
	}

	public static final double max(double x, double y) {
		return ((x) > (y)) ? (x) : (y);
	}
	
	public static final double checkLimits(double x, double mi, double ma) {
		return (((x) < (mi)) ? (mi) : (((x) > (ma)) ? (ma) : (x)));
	}
}
