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

	public void resetRegisters() {
		iSlopeCount = 0;
		sPrevSampleL = 0;
		sPrevSampleR = 0;
	}

	@Override
	protected int transposeMono(SampleSet dest, SampleSet src) {
		int i, used;
		long temp, vol1; // these are large samples

		if (src.size() == 0)
			return 0; // no samples, no work

		used = 0;
		i = 0;

		// Process the last sample saved from the previous call first...
		while (iSlopeCount <= SCALE) {
			vol1 = SCALE - iSlopeCount;
			temp = vol1 * sPrevSampleL + iSlopeCount * src.samples()[0];
			dest.samples()[i] = (int) (temp / SCALE);
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
			temp = src.samples()[used] * vol1 + iSlopeCount * src.samples()[used + 1];
			dest.samples()[i] = (int) (temp / SCALE);

			i++;
			iSlopeCount += iRate;
		}

		// Store the last sample for the next round
		sPrevSampleL = (int) src.samples()[src.size() - 1];

		return i;
	}

	@Override
	protected int transposeStereo(SampleSet dest, SampleSet src) {
		int srcPos, i, used;
		double temp, vol1;

		if (src.size() == 0)
			return 0; // no samples, no work

		used = 0;
		i = 0;

		// Process the last sample saved from the sPrevSampleLious call first...
		while (iSlopeCount <= SCALE) {
			vol1 = (SCALE - iSlopeCount);
			temp = vol1 * sPrevSampleL + iSlopeCount * src.samples()[0];
			dest.samples()[2 * i] = (int) (temp / SCALE);
			temp = vol1 * sPrevSampleR + iSlopeCount * src.samples()[1];
			dest.samples()[2 * i + 1] = (int) (temp / SCALE);
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
			temp = src.samples()[srcPos] * vol1 + iSlopeCount * src.samples()[srcPos + 2];
			dest.samples()[2 * i] = (int) (temp / SCALE);
			temp = src.samples()[srcPos + 1] * vol1 + iSlopeCount * src.samples()[srcPos + 3];
			dest.samples()[2 * i + 1] = (int) (temp / SCALE);

			i++;
			iSlopeCount += iRate;
		}

		// Store the last sample for the next round
		sPrevSampleL = (int) src.samples()[2 * src.size() - 2];
		sPrevSampleR = (int) src.samples()[2 * src.size() - 1];

		return i;
	}

	@Override
	public void setRate(float newRate) {
		iRate = (int) (newRate * SCALE + 0.5f);
		super.setRate(newRate);
	}
}
