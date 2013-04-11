package com.epsilonlabsllc.soundtouch;

public class AAFilter {
	private static final float PI = 3.141592655357989f;
	private static final float TWOPI = 2 * PI;

	protected FIRFilter pFIR;

	// Low-pass filter cut-off frequency, negative = invalid
	protected double cutoffFreq;

	// num of filter taps
	protected int length;
	
	/**
	 * Constructor
	 * @param len number of filter taps
	 */
	public AAFilter(int len) {
		pFIR = FIRFilter.newInstance();
		cutoffFreq = 0.5;
		setLength(len);
	}

	/**
	 * Calculates a SampleVector of coefficients for pFIR and sets them in pFIR
	 */
	protected void calculateCoeffs() {
		int i;
		double cntTemp, temp, tempCoeff, h, w;
		double fc2, wc;
		double scaleCoeff, sum;
		double[] work;
		SampleVector coeffs;

		assert (length >= 2);
		assert (length % 4 == 0);
		assert (cutoffFreq >= 0);
		assert (cutoffFreq <= 0.5);

		work = new double[length];
		coeffs = new SampleVector(length);

		fc2 = 2.0 * cutoffFreq;
		wc = PI * fc2;
		tempCoeff = TWOPI / (double) length;

		sum = 0;
		for (i = 0; i < length; i++) {
			cntTemp = (double) i - (double) (length / 2);

			temp = cntTemp * wc;
			if (temp != 0) {
				h = fc2 * Math.sin(temp) / temp; // sinc function
			} else {
				h = 1.0;
			}
			w = 0.54 + 0.46 * Math.cos(tempCoeff * cntTemp); // hamming window

			temp = w * h;
			work[i] = temp;

			// calc net sum of coefficients
			sum += temp;
		}

		// ensure the sum of coefficients is larger than zero
		assert (sum > 0);

		// ensure we've really designed a lowpass filter...
		assert (work[length / 2] > 0);
		assert (work[length / 2 + 1] > -1e-6);
		assert (work[length / 2 - 1] > -1e-6);

		// Calculate a scaling coefficient in such a way that the result can be
		// divided by 16384
		scaleCoeff = 16384.0f / sum;

		for (i = 0; i < length; i++) {
			// scale & round to nearest integer
			temp = work[i] * scaleCoeff;
			temp += (temp >= 0) ? 0.5 : -0.5;
			// ensure no overfloods
			assert (temp >= -32768 && temp <= 32767);
			coeffs.set(i, (int) temp);
		}

		// Set coefficients. Use divide factor 14 => divide result by 2^14 =
		// 16384
		pFIR.setCoefficients(coeffs, length, 14);
	}

	/**
	 * Sets new anti-alias filter cut-off edge frequency, scaled to sampling
	 * frequency (nyquist frequency = 0.5). The filter will cut off the
	 * frequencies than that.
	 * 
	 * @param fCutoff
	 */
	public void setCutoffFreq(double newCutoffFreq) {
		cutoffFreq = newCutoffFreq;
		calculateCoeffs();
	}

	/**
	 * Passes arguements and call to pFIR.evaluate()
	 * @param dest
	 * @param src
	 * @param numChannels
	 * @return
	 */
	public int evaluate(SampleVector dest, SampleVector src, int numChannels) {
		return pFIR.evaluate(dest, src, numChannels);
	}

	public int getLength() {
		return pFIR.getLength();
	}

	/**
	 * Sets number of FIR filter taps, i.e. ~filter complexity
	 * 
	 * @param length
	 */
	public void setLength(int length) {
		this.length = length;
		calculateCoeffs();
	}

}
