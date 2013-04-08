package com.epsilonlabsllc.soundtouch;
/// A linear samplerate transposer class that uses integer arithmetics.
/// for the transposing.
public class RateTransposerInteger extends RateTransposer{
	/// fixed-point interpolation routine precision
	private static final int SCALE 65536;
	private int iSlopeCount;
	private int sPrevSampleL;
	private int sPrevSampleR;

	// Constructor
	public RateTransposerInteger(){
		// Notice: use local function calling syntax for sake of clarity, 
		// to indicate the fact that C++ constructor can't call virtual functions.
		resetRegisters();
		setRate(1.0f);
	}
	
	public void resetRegisters(){
		iSlopeCount = 0;
		sPrevSampleL = 0;
		sPrevSampleR = 0;
	}
	
	// Transposes the sample rate of the given samples using linear interpolation. 
	// 'Mono' version of the routine. Returns the number of samples returned in 
	// the "dest" buffer
	public int transposeMono(byte[] dest, byte[] src, int nSamples){
		int i, used;
		byte temp, vol1; //LONG_SAMPLETYPE

		if (nSamples == 0) return 0;  // no samples, no work

		used = 0;    
		i = 0;
		
		//TODO: pickup point

		// Process the last sample saved from the previous call first...
		while (iSlopeCount <= SCALE) {
			vol1 = (LONG_SAMPLETYPE)(SCALE - iSlopeCount);
			temp = vol1 * sPrevSampleL + iSlopeCount * src[0];
			dest[i] = (SAMPLETYPE)(temp / SCALE);
			i++;
			iSlopeCount += iRate;
		}
		// now always (iSlopeCount > SCALE)
		iSlopeCount -= SCALE;

		while (1)
		{
			while (iSlopeCount > SCALE) 
			{
				iSlopeCount -= SCALE;
				used ++;
				if (used >= nSamples - 1) goto end;
			}
			vol1 = (LONG_SAMPLETYPE)(SCALE - iSlopeCount);
			temp = src[used] * vol1 + iSlopeCount * src[used + 1];
			dest[i] = (SAMPLETYPE)(temp / SCALE);

			i++;
			iSlopeCount += iRate;
		}
		end:
			// Store the last sample for the next round
			sPrevSampleL = src[nSamples - 1];

		return i;
	}
}
