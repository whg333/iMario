package sound;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import util.LoopingByteInputStream;
import util.ThreadPool;

/**
 * The SoundManager class manages sound playback. This SoundManager is a
 * ThreadPool, with each thread playing back one sound at a time. This allows
 * the SoundManager to easily limit the number of simultaneous sounds being
 * played.
 * <p>
 * Possible ideas to extend this class:
 * <ul>
 * <li>add a setMasterVolume() method, which uses Controls to set the volume for
 * each line.
 * <li>don't play a sound if more than, say, 500ms has passed since the request
 * to play
 * </ul>
 */
public class SoundManagerThreadPool extends ThreadPool implements ISoundManager{

	private AudioFormat playbackFormat;
	private ThreadLocal<SourceDataLine> localLine;
	private ThreadLocal<byte[]> localBuffer;
	private Object pausedLock;
	private boolean paused;

	/**
	 * Creates a new SoundManager using the maximum number of simultaneous
	 * sounds.
	 */
	public SoundManagerThreadPool(AudioFormat playbackFormat) {
		this(playbackFormat, getMaxSimultaneousSounds(playbackFormat));
	}

	/**
	 * Creates a new SoundManager with the specified maximum number of
	 * simultaneous sounds.
	 */
	public SoundManagerThreadPool(AudioFormat playbackFormat, int maxSimultaneousSounds) {
		super(Math.min(maxSimultaneousSounds, getMaxSimultaneousSounds(playbackFormat)));
		this.playbackFormat = playbackFormat;
		localLine = new ThreadLocal<SourceDataLine>();
		localBuffer = new ThreadLocal<byte[]>();
		pausedLock = new Object();
		// notify threads in pool it's ok to start
		synchronized (this) {
			notifyAll();
		}
	}

	/**
	 * Gets the maximum number of simultaneous sounds with the specified
	 * AudioFormat that the default mixer can play.
	 */
	public static int getMaxSimultaneousSounds(AudioFormat playbackFormat) {
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, playbackFormat);
		Mixer mixer = AudioSystem.getMixer(null);
		int maxLines = mixer.getMaxLines(lineInfo);
		if (maxLines == AudioSystem.NOT_SPECIFIED) {
	        maxLines = 32;
	    }
	    return maxLines;
	}

	/**
	 * Does any clean up before closing.
	 */
	protected void cleanUp() {
		// signal to unpause
		setPaused(false);

		// close the mixer (stops any running sounds)
		Mixer mixer = AudioSystem.getMixer(null);
		if (mixer.isOpen()) {
			mixer.close();
		}
	}

	@Override
	public void close() {
		cleanUp();
		super.close();
	}

	public void join() {
		cleanUp();
		super.join();
	}

	/**
	 * Sets the paused state. Sounds may not pause immediately.
	 */
	public void setPaused(boolean paused) {
		if (this.paused != paused) {
			synchronized (pausedLock) {
				this.paused = paused;
				if (!paused) {
					// restart sounds
					pausedLock.notifyAll();
				}
			}
		}
	}

	/**
	 * Returns the paused state.
	 */
	public boolean isPaused() {
		return paused;
	}

	@Override
	public Sound getSound(String filename) {
		try {
			return getSound(getAudioInputStream(filename));
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
			return Sound.NONE_SOUND;
		}
	}

	/**
	 * Loads a Sound from an input stream. Returns null if an error occurs.
	 */
	public Sound getSound(InputStream is) throws UnsupportedAudioFileException {
		return getSound(getAudioInputStream(is));
	}
	
	@Override
	public Sound tryGetSound(String filename) throws UnsupportedAudioFileException, IOException {
		return getSound(getAudioInputStream(filename), getClass().getResourceAsStream(filename));
	}
	
	private Sound getSound(AudioInputStream audioStream, InputStream sis) throws UnsupportedAudioFileException, IOException {
		if (audioStream == null) {
			return null;
		}

		// get the number of bytes to read
		long frameLength = audioStream.getFrameLength();
		//System.out.println("frameLength:"+frameLength+",FormatFrameSize:"+audioStream.getFormat().getFrameSize());
		if(frameLength < 0){
			AudioInputStream source = null;
			source = AudioSystem.getAudioInputStream(sis);
			//System.out.println("srcFormat:"+source.getFormat()+"\ntargetFormat:"+ISoundManager.PLAYBACK_FORMAT+"\nconvertFormat:"+audioStream.getFormat());
			//当发现wav格式和目标wav格式不符合时，做了转换后其AudioInputStream.getFrameLength()将返回负数（-1）
			//这就会导致NegativeArraySizeException，因为创建的byte数组的长度不可以为负数
			//这里兼容一下，找到原来的格式，原来的格式中FrameLength是知道的，只是在转换失败后变成负数了，此处做一下相应的byte长度转换
			frameLength = source.getFrameLength() * 4;
		}
		int length = (int) (frameLength * audioStream.getFormat().getFrameSize());

		// read the entire stream
		byte[] samples = new byte[length];
		DataInputStream is = new DataInputStream(audioStream);
		is.readFully(samples);
		is.close();

		// return the samples
		return new Sound(samples, audioStream.getFormat());
	}

	/**
	 * Loads a Sound from an AudioInputStream.
	 */
	public Sound getSound(AudioInputStream audioStream) {
		if (audioStream == null) {
			return null;
		}

		// get the number of bytes to read
		int length = (int) (audioStream.getFrameLength() * audioStream.getFormat().getFrameSize());

		// read the entire stream
		byte[] samples = new byte[length];
		DataInputStream is = new DataInputStream(audioStream);
		try {
			is.readFully(samples);
			is.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// return the samples
		return new Sound(samples, audioStream.getFormat());
	}

	/**
	 * Creates an AudioInputStream from a sound from the file system.
	 */
	public AudioInputStream getAudioInputStream(String filename) throws UnsupportedAudioFileException {
		return getAudioInputStream(getClass().getResourceAsStream(filename));
	}

	/**
	 * Creates an AudioInputStream from a sound from an input stream
	 */
	public AudioInputStream getAudioInputStream(InputStream is) throws UnsupportedAudioFileException {
		try {
			if (!is.markSupported()) {
				is = new BufferedInputStream(is);
			}
			// open the source stream
			AudioInputStream source = AudioSystem.getAudioInputStream(is);
			// convert to playback format
			return AudioSystem.getAudioInputStream(playbackFormat, source);
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public InputStream play(Sound sound) {
		return play(sound, null, false);
	}

	@Override
	public InputStream play(Sound sound, SoundFilter filter, boolean loop) {
		if(sound == null || !sound.isCanPlay()){
			return null;
		}
		InputStream is;
		if (loop) {
			is = new LoopingByteInputStream(sound.getSamples());
		} else {
			is = new ByteArrayInputStream(sound.getSamples());
		}
		return play(is, filter);
	}

	/**
	 * Plays a sound from an InputStream. This method returns immediately.
	 */
	@Deprecated
	public InputStream play(InputStream is) {
		return play(is, null);
	}

	/**
	 * Plays a sound from an InputStream with an optional sound filter. This
	 * method returns immediately.
	 */
	public InputStream play(InputStream is, SoundFilter filter) {
		if (is != null) {
			if (filter != null) {
				is = new FilteredSoundStream(is, filter);
			}
			runTask(new SoundPlayer(is));
		}
		return is;
	}

	/**
	 * Signals that a PooledThread has started. Creates the Thread's line and
	 * buffer.
	 */
	@Override
	protected void threadStarted() {
		//System.out.println(Thread.currentThread().getName() + " beforeExecute");
		// wait for the SoundManager constructor to finish
		synchronized (this) {
			try {
				wait();
			} catch (InterruptedException ex) {
			}
		}

		// use a short, 100ms (1/10th sec) buffer for filters that
		// change in real-time
		int bufferSize = playbackFormat.getFrameSize() * Math.round(playbackFormat.getSampleRate() / 10);

		// create, open, and start the line
		SourceDataLine line;
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, playbackFormat);
		try {
			line = (SourceDataLine) AudioSystem.getLine(lineInfo);
			line.open(playbackFormat, bufferSize);
		} catch (LineUnavailableException ex) {
			// the line is unavailable - signal to end this thread
			Thread.currentThread().interrupt();
			return;
		}

		line.start();

		// create the buffer
		byte[] buffer = new byte[bufferSize];

		// set this thread's locals
		localLine.set(line);
		localBuffer.set(buffer);
	}

	/**
	 * Signals that a PooledThread has stopped. Drains and closes the Thread's
	 * Line.
	 */
	@Override
	protected void threadStopped() {
		//System.out.println(Thread.currentThread().getName() + " afterExecute");
		SourceDataLine line = localLine.get();
		if (line != null) {
			line.drain();
			line.close();
		}
	}

	/**
	 * The SoundPlayer class is a task for the PooledThreads to run. It receives
	 * the threads's Line and byte buffer from the ThreadLocal variables and
	 * plays a sound from an InputStream.
	 * <p>
	 * This class only works when called from a PooledThread.
	 */
	protected class SoundPlayer implements Runnable {

		private InputStream source;

		public SoundPlayer(InputStream source) {
			this.source = source;
		}

		public void run() {
			// get line and buffer from ThreadLocals
			SourceDataLine line = (SourceDataLine) localLine.get();
			byte[] buffer = localBuffer.get();
			if (line == null || buffer == null) {
				// the line is unavailable
				return;
			}

			// copy data to the line
			try {
				int numBytesRead = 0;
				while (numBytesRead != -1) {
					// if paused, wait until unpaused
					synchronized (pausedLock) {
						if (paused) {
							try {
								pausedLock.wait();
							} catch (InterruptedException ex) {
								return;
							}
						}
					}
					// copy data
					numBytesRead = source.read(buffer, 0, buffer.length);
					if (numBytesRead != -1) {
						line.write(buffer, 0, numBytesRead);
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		}
	}

}
