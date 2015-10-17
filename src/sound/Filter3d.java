package sound;

import graphics.Sprite;

/**
 * The Filter3d class is a SoundFilter that creates a 3d sound effect. The sound
 * is filtered so that it is quiter the farther away the sound source is from
 * the listener.
 * <p>
 * Possible ideas to extend this class:
 * <ul>
 * <li>pan the sound to the left and right speakers
 * </ul>
 * 
 * @see FilteredSoundStream
 */
public class Filter3d extends SoundFilter {

	/** number of samples to shift when changing the volume. */
	private static final int NUM_SHIFTING_SAMPLES = 500;

	private Sprite source;
	private Sprite listener;
	private int maxDistance;
	private float lastVolume;

	/**
	 * Creates a new Filter3d object with the specified source and listener
	 * Sprites. The Sprite's position can be changed while this filter is
	 * running.
	 * <p>
	 * The maxDistance parameter is the maximum distance that the sound can be
	 * heard.
	 */
	public Filter3d(Sprite source, Sprite listener, int maxDistance) {
		this.source = source;
		this.listener = listener;
		this.maxDistance = maxDistance;
		this.lastVolume = 0.0f;
	}

	/**
	 * Filters the sound so that it gets more quiet with distance.
	 */
	public void filter(byte[] samples, int offset, int length) {

		if (source == null || listener == null) {
			// nothing to filter - return
			return;
		}

		// calculate the listener's distance from the sound source
		float dx = (source.getX() - listener.getX());
		float dy = (source.getY() - listener.getY());
		float distance = (float) Math.sqrt(dx * dx + dy * dy);

		// set volume from 0 (no sound) to 1
		float newVolume = (maxDistance - distance) / maxDistance;
		if (newVolume <= 0) {
			newVolume = 0;
		}

		// set the volume of the sample
		int shift = 0;
		for (int i = offset; i < offset + length; i += 2) {//why used i += 2?

			float volume = newVolume;

			// shift from the last volume to the new volume
			if (shift < NUM_SHIFTING_SAMPLES) {
				volume = lastVolume + (newVolume - lastVolume) * shift / NUM_SHIFTING_SAMPLES;
				shift++;
			}

			// change the volume of the sample
			short oldSample = getSample(samples, i);
			short newSample = (short) (oldSample * volume);
			setSample(samples, i, newSample);
		}

		lastVolume = newVolume;
	}

}
