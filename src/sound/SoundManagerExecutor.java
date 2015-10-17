package sound;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import util.LoopingByteInputStream;

/**
 * The SoundManager class manages sound playback. This SoundManager used a
 * ExecutorService(which is a ThreadPoolExecutor), with each thread playing back
 * one sound at a time. This allows the SoundManager to easily limit the number
 * of simultaneous sounds being played.
 * <p>
 * Possible ideas to extend this class:
 * <ul>
 * <li>add a setMasterVolume() method, which uses Controls to set the volume for
 * each line.
 * <li>don't play a sound if more than, say, 500ms has passed since the request
 * to play
 * </ul>
 */
public class SoundManagerExecutor implements ISoundManager{

	//private final AudioFormat playbackFormat;
	//private ThreadLocal<SourceDataLine> localLine;
	//private ThreadLocal<byte[]> localBuffer;
	private Object pausedLock;
	private boolean paused;
	
	private final ExecutorService executor;

	/**
	 * Creates a new SoundManager with the specified maximum number of
	 * simultaneous sounds.
	 */
	public SoundManagerExecutor() {
		//super(Math.max(1, Math.min(maxSimultaneousSounds, getMaxSimultaneousSounds(playbackFormat))));
		//this.playbackFormat = playbackFormat;
		//localLine = new ThreadLocal<SourceDataLine>();
		//localBuffer = new ThreadLocal<byte[]>();
		pausedLock = new Object();
		// notify threads in pool it's ok to start
//		synchronized (this) {
//			notifyAll();
//			System.out.println("SoundManager notifyAll");
//		}
		executor = new ThreadPoolExecutor(2, 32, 30000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(64), new SoundThreadPoolFactory());
	}
	
	/**
	 * 有个问题没有解决：虽说使用JDK1.5的多线程任务框架，但在改写的时候发现，没有一种有效的方式对线程池里面的线程进行自定义初始化和销毁。
	 * <br/>但是这个问题如果是自己实现的线程池的话，却可以解决：即使用Worker模式在从队列getTask之前和之后分别提供钩子方法threadStarted和threadStopped
	 * 
	 * <br/><br/><b>PS</b>:注意线程池里面包含可复用的线程，利用这些可复用的线程去执行task（也是线程），即线程池中线程的run方法中调用getTask，然后调用task的run方法
	 * <br/><br/><b>自定义实现线程池方案具体详见SoundManagerThreadPool类</b>
	 *
	 */
//	private class SoundThreadPoolExecutor extends ThreadPoolExecutor {
//		private SoundThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, 
//				TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
//			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
//		}
//		
////		@Override
////		protected void beforeExecute(Thread t, Runnable r){
////			threadStarted();
////		}
//		
////		@Override
////		protected void afterExecute(Runnable r, Throwable t){
////			threadStopped();
////		}
//	}
	
	private static class SoundThreadPoolFactory implements ThreadFactory {

		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		private SoundThreadPoolFactory() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "SoundThreadPoolFactory" + "-thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}
	
	/**
	 * Gets the maximum number of simultaneous sounds with the specified
	 * AudioFormat that the default mixer can play.
	 */
	public static int getMaxSimultaneousSounds(AudioFormat playbackFormat) {
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, playbackFormat);
		Mixer mixer = AudioSystem.getMixer(null);
		return mixer.getMaxLines(lineInfo);
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
		//super.close();
		executor.shutdown();
	}

//	public void join() {
//		cleanUp();
//		super.join();
//	}

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
	
	@Override
	public Sound tryGetSound(String filename) throws UnsupportedAudioFileException {
		return getSound(getClass().getResourceAsStream(filename));
	}
	
	/**
	 * Loads a Sound from an input stream. Returns null if an error occurs.
	 */
	public Sound getSound(InputStream is) throws UnsupportedAudioFileException {
		return getSound(getAudioInputStream(is));
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
			AudioInputStream sourceAudio = AudioSystem.getAudioInputStream(is);
			return sourceAudio;
			// convert to playback format
			//return AudioSystem.getAudioInputStream(playbackFormat, source);
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
		return play(is, filter, sound.getFormat());
	}

	/**
	 * Plays a sound from an InputStream. This method returns immediately.
	 */
	@Deprecated
	public InputStream play(InputStream is) {
		return play(is, null, null);
	}

	/**
	 * Plays a sound from an InputStream with an optional sound filter. This
	 * method returns immediately.
	 */
	public InputStream play(InputStream is, SoundFilter filter, AudioFormat format) {
		if (is != null) {
			if (filter != null) {
				is = new FilteredSoundStream(is, filter);
			}
			runTask(new SoundPlayerTask(is, format));
		}
		return is;
	}
	
	private void runTask(Runnable task){
		executor.execute(task);
	}
	
	/**
	 * Signals that a PooledThread has started. Creates the Thread's line and
	 * buffer.
	 */
//	protected void threadStarted() {
//		System.out.println(Thread.currentThread().getName() + " beforeExecute");
//		// wait for the SoundManager constructor to finish
////		synchronized (this) {
////			try {
////				System.out.println("wait before");
////				wait();
////				System.out.println("wait after notifyAll");
////			} catch (InterruptedException ex) {
////			}
////		}
//
//		// use a short, 100ms (1/10th sec) buffer for filters that
//		// change in real-time
//		int bufferSize = playbackFormat.getFrameSize() * Math.round(playbackFormat.getSampleRate() / 10);
//
//		// create, open, and start the line
//		SourceDataLine line;
//		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, playbackFormat);
//		try {
//			line = (SourceDataLine) AudioSystem.getLine(lineInfo);
//			line.open(playbackFormat, bufferSize);
//		} catch (LineUnavailableException ex) {
//			// the line is unavailable - signal to end this thread
//			Thread.currentThread().interrupt();
//			return;
//		}
//
//		line.start();
//
//		// create the buffer
//		byte[] buffer = new byte[bufferSize];
//
//		// set this thread's locals
////		localLine.set(line);
////		localBuffer.set(buffer);
//	}
//
//	/**
//	 * Signals that a PooledThread has stopped. Drains and closes the Thread's
//	 * Line.
//	 */
//	protected void threadStopped() {
//		System.out.println(Thread.currentThread().getName() + " afterExecute");
//		SourceDataLine line = localLine.get();
//		if (line != null) {
//			line.drain();
//			line.close();
//		}
//	}

	/**
	 * The SoundPlayer class is a task for the PooledThreads to run. It receives
	 * the threads's Line and byte buffer from the ThreadLocal variables and
	 * plays a sound from an InputStream.
	 * <p>
	 * This class only works when called from a PooledThread.
	 */
	protected class SoundPlayerTask implements Runnable {

		private final InputStream source;
		private final AudioFormat playbackFormat;
		
		private SourceDataLine line;
		private byte[] buffer;

		public SoundPlayerTask(InputStream source, AudioFormat playbackFormat) {
			this.source = source;
			this.playbackFormat = playbackFormat;
		}
		
		private void init() {
			// use a short, 100ms (1/10th sec) buffer for filters that
			// change in real-time
			//System.out.println(playbackFormat);
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
			this.line = line;
			this.buffer = buffer;
		}

		public void run() {
			// get line and buffer from ThreadLocals
//			SourceDataLine line = (SourceDataLine) localLine.get();
//			byte[] buffer = localBuffer.get();
			init();
			
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
