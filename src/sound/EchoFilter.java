package sound;

/**
 * The EchoFilter class is a SoundFilter that emulates an echo.
 * 
 * @see FilteredSoundStream
 */
public class EchoFilter extends SoundFilter {

	private short[] delayBuffer;
	private int delayBufferPos;
	private float decay;

	/**
	 * Creates an EchoFilter with the specified number of delay samples and the
	 * specified decay rate.
	 * <p>
	 * The number of delay samples specifies how long before the echo is
	 * initially heard. For a 1 second echo with mono, 44100Hz sound, use 44100
	 * delay samples.
	 * <p>
	 * The decay value is how much the echo has decayed from the source. A decay
	 * value of .5 means the echo heard is half as loud as the source.
	 */
	public EchoFilter(int numDelaySamples, float decay) {
		delayBuffer = new short[numDelaySamples];
		this.decay = decay;
	}
	
	/**
	 * Clears this EchoFilter's internal delay buffer.
	 */
	@Override
	public void reset() {
		for (int i = 0; i < delayBuffer.length; i++) {
			delayBuffer[i] = 0;
		}
		delayBufferPos = 0;
	}

	/**
	 * Gets the remaining size, in bytes, of samples that this filter can echo
	 * after the sound is done playing. Ensures that the sound will have decayed
	 * to below 1% of maximum volume (amplitude).
	 */
	@Override
	public int getRemainingSize() {
		float finalDecay = 0.01f;
		// derived from Math.pow(decay,x) <= finalDecay
		int numRemainingBuffers = (int) Math.ceil(Math.log(finalDecay) / Math.log(decay));
		int bufferSize = delayBuffer.length * 2;

		return bufferSize * numRemainingBuffers;
	}

	/**
	 * Filters the sound samples to add an echo. The samples played are added to
	 * the sound in the delay buffer multipied by the decay rate. The result is
	 * then stored in the delay buffer, so multiple echoes are heard.
	 */
	@Override
	public void filter(byte[] samples, int offset, int length) {

		for (int i = offset; i < offset + length; i += 2) {
			// update the sample
			short oldSample = getSample(samples, i);
			short newSample = (short) (oldSample + decay * delayBuffer[delayBufferPos]);
			setSample(samples, i, newSample);

			// update the delay buffer
			delayBuffer[delayBufferPos] = newSample;
			delayBufferPos++;
			if (delayBufferPos == delayBuffer.length) {
				delayBufferPos = 0;
			}
		}
	}

}
