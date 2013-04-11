package com.epsilonlabsllc.soundtouch;

/// A linear samplerate transposer class that uses integer arithmetics.
/// for the transposing.
public class RateTransposerInteger extends RateTransposer {
	// / fixed-point interpolation routine precision
	private static final int SCALE = 65536;
	private int iSlopeCount;
	private int sPrevSampleL;
	private int sPrevSampleR;

	// Constructor
	public RateTransposerInteger() {
		// Notice: use local function calling syntax for sake of clarity,
		// to indicate the fact that C++ constructor can't call virtual
		// functions.
		resetRegisters();
		setRate(1.0f);
	}

	/**
	 * sets iSlopeCount, sPrevSampleL and sPrevSampleR to 0
	 */
	public void resetRegisters() {
		iSlopeCount = 0;
		sPrevSampleL = 0;
		sPrevSampleR = 0;
	}

	/**
	 * transposes a mono SampleVetor from src to dest
	 */
	@Override
	protected int transposeMono(SampleVector dest, SampleVector src) {
		int i, used;
		long temp, vol1; // these are large samples

		if (src.size() == 0)
			return 0; // no samples, no work

		used = 0;
		i = 0;

		// Process the last sample saved from the previous call first...
		while (iSlopeCount <= SCALE) {
			vol1 = SCALE - iSlopeCount;
			temp = vol1 * sPrevSampleL + iSlopeCount * src.get(0);
			dest.set(i, (int) (temp / SCALE));
			i++;
			iSlopeCount += iRate;
		}
		// now always (iSlopeCount > SCALE)
		iSlopeCount -= SCALE;

		boolean done = false;
		while (!done) {
			while (iSlopeCount > SCALE) {
				iSlopeCount -= SCALE;
				used++;
				if (used >= src.size() - 1) {
					done = true;
					break;
				}
			}
			vol1 = SCALE - iSlopeCount;
			temp = src.get(used) * vol1 + iSlopeCount * src.get(used + 1);
			dest.set(i, (int) (temp / SCALE));

			i++;
			iSlopeCount += iRate;
		}

		// Store the last sample for the next round
		sPrevSampleL = (int) src.get(src.size() - 1);

		return i;
	}

	/**
	 * transposes a stereo SampleVector from src to dest
	 */
	@Override
	protected int transposeStereo(SampleVector dest, SampleVector src) {
		int srcPos, i, used;
		double temp, vol1;

		if (src.size() == 0)
			return 0; // no samples, no work

		used = 0;
		i = 0;

		// Process the last sample saved from the sPrevSampleLious call first...
		while (iSlopeCount <= SCALE) {
			vol1 = (SCALE - iSlopeCount);
			temp = vol1 * sPrevSampleL + iSlopeCount * src.get(0);
			dest.set(2 * i, (int) (temp / SCALE));
			temp = vol1 * sPrevSampleR + iSlopeCount * src.get(1);
			dest.set(2 * i + 1, (int) (temp / SCALE));
			i++;
			iSlopeCount += iRate;
		}
		// now always (iSlopeCount > SCALE)
		iSlopeCount -= SCALE;

		boolean done = false;
		while (!done) {
			while (iSlopeCount > SCALE) {
				iSlopeCount -= SCALE;
				used++;
				if (used >= src.size() - 1) {
					done = true;
					break;
				}
			}
			srcPos = 2 * used;
			vol1 = (SCALE - iSlopeCount);
			temp = src.get(srcPos) * vol1 + iSlopeCount * src.get(srcPos + 2);
			dest.set(2 * i, (int) (temp / SCALE));
			temp = src.get(srcPos + 1) * vol1 + iSlopeCount * src.get(srcPos + 3);
			dest.set(2 * i + 1, (int) (temp / SCALE));

			i++;
			iSlopeCount += iRate;
		}

		// Store the last sample for the next round
		sPrevSampleL = (int) src.get(2 * src.size() - 2);
		sPrevSampleR = (int) src.get(2 * src.size() - 1);

		return i;
	}

	@Override
	public void setRate(float newRate) {
		iRate = (int) (newRate * SCALE + 0.5f);
		super.setRate(newRate);
	}
}
