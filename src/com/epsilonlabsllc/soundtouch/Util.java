package com.epsilonlabsllc.soundtouch;

public class Util {
	public static <T> void memcpy(SampleVector dest, SampleVector src, int num) {
		assert (dest != null);
		assert (src != null);
		assert (num >= 0);
		
		assert (src.size() >= num);
		assert (dest.size() >= src.size());
		
		for (int i = 0; i < num; i++) {
			dest.set(i, src.get(i));
		}
	}
	
	public static void memset(SampleVector ptr, int value, int num) {
		for (int i = 0; i < num; i++) {
			ptr.set(i, value);
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
