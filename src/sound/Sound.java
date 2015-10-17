package sound;

import javax.sound.sampled.AudioFormat;

/**
    The Sound class is a container for sound samples. The sound
    samples are format-agnostic and are stored as a byte array.
*/
public class Sound {
	
	public static final Sound NONE_SOUND = new Sound();
	
	private Sound(){
		samples = new byte[0];
		format = null;
	}

    private final byte[] samples;
    private final AudioFormat format;

    /**
        Create a new Sound object with the specified byte array.
        The array is not copied.
    */
    public Sound(byte[] samples, AudioFormat format) {
        this.samples = samples;
        this.format = format;
    }

	/**
        Returns this Sound's objects samples as a byte array.
    */
    public byte[] getSamples() {
        return samples;
    }
    
    public boolean isCanPlay(){
    	return samples.length > 0;
    }
    
    public AudioFormat getFormat() {
		return format;
	}

}
