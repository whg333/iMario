package sound;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

public interface ISoundManager {
	
	// uncompressed, 44100Hz, 16-bit, mono, signed, little-endian
	public static final AudioFormat PLAYBACK_FORMAT = new AudioFormat(44100, 16, 1, true, false);
	
	/**
	 * Loads a Sound from the file system. Returns null if an error occurs.
	 * @param filename
	 * @return
	 */
	Sound getSound(String filename);
	
	Sound tryGetSound(String filename) throws UnsupportedAudioFileException, IOException;
	
	/**
	 * Plays a sound. This method returns immediately.
	 * @param sound
	 * @return
	 */
	InputStream play(Sound sound);
	
	/**
	 * Plays a sound with an optional SoundFilter, and optionally looping. This
	 * method returns immediately.
	 * @param sound
	 * @param filter
	 * @param loop
	 * @return
	 */
	InputStream play(Sound sound, SoundFilter filter, boolean loop);
	
	/**
	 * close javax.sound.sampled.Mixer and clear/shutdown sound player thread pool
	 */
	void close();
	
}
