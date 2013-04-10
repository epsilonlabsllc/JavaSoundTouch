package com.epsilonlabsllc.soundtouch;

public abstract class FIRFilter {
	// Number of FIR filter taps
	protected int length;
	// Number of FIR filter taps divided by 8
	protected int lengthDiv8;
	// Result divider factor in 2^k format
	protected int resultDivFactor;
	// Result divider value.
	protected int resultDivider;
	// Memory for filter coefficients
	protected SampleVector filterCoeffs;

	protected int evaluateFilterStereo(SampleVector dest, final SampleVector src) {
		int i, j, end;
		long suml, sumr;

		assert (length != 0);
		assert (src != null);
		assert (dest != null);
		assert (filterCoeffs != null);

		end = 2 * (src.size() - length);

		for (j = 0; j < end; j += 2) {
			suml = 0;
			sumr = 0;

			for (i = 0; i < length; i += 4) {
				// loop is unrolled by factor of 4 here for efficiency
				suml += src.get(2 * i + 0 + j) * filterCoeffs.get(i + 0) + // &nbsp
						src.get(2 * i + 2 + j) * filterCoeffs.get(i + 1) + // &nbsp
						src.get(2 * i + 4 + j) * filterCoeffs.get(i + 2) + // &nbsp
						src.get(2 * i + 6 + j) * filterCoeffs.get(i + 3);
				sumr += src.get(2 * i + 1 + j) * filterCoeffs.get(i + 0) + // &nbsp
						src.get(2 * i + 3 + j) * filterCoeffs.get(i + 1) + // &nbsp
						src.get(2 * i + 5 + j) * filterCoeffs.get(i + 2) + // &nbsp
						src.get(2 * i + 7 + j) * filterCoeffs.get(i + 3);
			}

			suml >>= resultDivFactor;
			sumr >>= resultDivFactor;
			// saturate to 16 bit integer limits
			suml = (suml < -32768) ? -32768 : (suml > 32767) ? 32767 : suml;
			// saturate to 16 bit integer limits
			sumr = (sumr < -32768) ? -32768 : (sumr > 32767) ? 32767 : sumr;
			dest.set(j, (int) suml);
			dest.set(j + 1, (int) sumr);
		}
		return src.size() - length;
	}

	/**
	 * Usual C-version of the filter routine for mono sound
	 * 
	 * @param dest
	 * @param src
	 * @return
	 */
	protected int evaluateFilterMono(SampleVector dest, final SampleVector src) {
		int i, j, offset, end;
		long sum;

		assert (length != 0) : "Length was zero.";

		end = src.size() - length;
		offset = 0;
		for (j = 0; j < end; j++) {
			sum = 0;
			for (i = 0; i < length; i += 4) {
				// loop is unrolled by factor of 4 here for efficiency
				sum += src.get(i + 0 + offset) * filterCoeffs.get(i + 0) + // &nbsp
						src.get(i + 1 + offset) * filterCoeffs.get(i + 1) + // &nbsp
						src.get(i + 2 + offset) * filterCoeffs.get(i + 2) + // &nbsp
						src.get(i + 3 + offset) * filterCoeffs.get(i + 3);
			}
			sum >>= resultDivFactor;
			// saturate to 16 bit integer limits
			sum = (sum < -32768) ? -32768 : (sum > 32767) ? 32767 : sum;
			dest.set(j, (int) sum);
			offset++;
		}

		return end;
	}

	/**
	 * Applies the filter to the given sequence of samples. Note : The amount of
	 * outputted samples is by value of 'filter_length' smaller than the amount
	 * of input samples.
	 * 
	 * return Number of samples copied to 'dest'.
	 * 
	 * @param dest
	 * @param src
	 * @param numChannels
	 * @return
	 */
	public abstract int evaluate(SampleVector dest, final SampleVector src, int numChannels);

	/**
	 * Set filter coefficients and length. Throws an exception if filter length isn't divisible by 8
	 * 
	 * @param coeffs
	 * @param newLength
	 * @param uResultDivFactor
	 */
	protected void setCoefficients(SampleVector coeffs, int newLength, int uResultDivFactor) {
		assert (newLength > 0);
		if (newLength % 8 != 0)
			throw new RuntimeException("FIR filter length not divisible by 8");

		lengthDiv8 = newLength / 8;
		length = lengthDiv8 * 8;
		assert (length == newLength);

		resultDivFactor = uResultDivFactor;
		resultDivider = (int) Math.pow(2.0, (int) resultDivFactor);

		filterCoeffs = new SampleVector(length);
		Util.memcpy(filterCoeffs, coeffs, length);
	}
	
	public final int getLength() {
		return length;
	}

	public static FIRFilter newInstance() {
		return null;
	}
}
