package sound;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

public class MidiPlayer implements MetaEventListener {

	// Midi meta event
	public static final int END_OF_TRACK_MESSAGE = 47;

	private Sequencer sequencer;
	private boolean loop;
	private boolean paused;
	
	private String midiPath;
	private final long sleepTime;

	/**
	 * Creates a new MidiPlayer object.
	 */
	public MidiPlayer() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequencer.addMetaEventListener(this);
		} catch (MidiUnavailableException ex) {
			sequencer = null;
		}
		this.sleepTime = 0;
	}
	
	public MidiPlayer(String midiPath, boolean loop){
		this(midiPath, loop, TimeUnit.SECONDS.toMillis(3));
	}
	
	public MidiPlayer(String midiPath, boolean loop, long sleepTime){
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequencer.addMetaEventListener(this);
		} catch (MidiUnavailableException ex) {
			sequencer = null;
		}
		this.midiPath = midiPath;
		this.loop = loop;
		this.sleepTime = sleepTime;
	}

	/**
	 * Loads a sequence from the file system. Returns null if an error occurs.
	 */
	public Sequence getSequence(String filename) {
		return getSequence(getClass().getResourceAsStream(filename));
	}

	/**
	 * Loads a sequence from an input stream. Returns null if an error occurs.
	 */
	public Sequence getSequence(InputStream is) {
		try {
			if (!is.markSupported()) {
				is = new BufferedInputStream(is);
			}
			Sequence s = MidiSystem.getSequence(is);
			is.close();
			return s;
		} catch (InvalidMidiDataException ex) {
			ex.printStackTrace();
			return null;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public void play(){
		Sequence sequence = getSequence(midiPath); 
		play(sequence, loop);
	}

	/**
	 * Plays a sequence, optionally looping. This method returns immediately.
	 * The sequence is not played if it is invalid.
	 */
	public void play(Sequence sequence, boolean loop) {
		if (sequencer != null && sequence != null && sequencer.isOpen()) {
			try {
				sequencer.setSequence(sequence);
				sequencer.start();
				this.loop = loop;
			} catch (InvalidMidiDataException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * This method is called by the sound system when a meta event occurs. In
	 * this case, when the end-of-track meta event is received, the sequence is
	 * restarted if looping is on.
	 */
	@Override
	public void meta(MetaMessage event) {
		if (event.getType() == END_OF_TRACK_MESSAGE) {
			if (sequencer != null && sequencer.isOpen() && loop) {
				sequencer.setTickPosition(0);
				if (sleepTime > 0) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						
					}
				}
				sequencer.start();
			}
		}
	}

	/**
	 * Stops the sequencer and resets its position to 0.
	 */
	public void stop() {
		if (sequencer != null && sequencer.isOpen()) {
			sequencer.stop();
			sequencer.setMicrosecondPosition(0);
		}
	}

	/**
	 * Closes the sequencer.
	 */
	public void close() {
		if (sequencer != null && sequencer.isOpen()) {
			sequencer.close();
		}
	}

	/**
	 * Gets the sequencer.
	 */
	public Sequencer getSequencer() {
		return sequencer;
	}

	/**
	 * Sets the paused state. Music may not immediately pause.
	 */
	public void setPaused(boolean paused) {
		if (this.paused != paused && sequencer != null && sequencer.isOpen()) {
			this.paused = paused;
			if (paused) {
				sequencer.stop();
			} else {
				sequencer.start();
			}
		}
	}

	/**
	 * Returns the paused state.
	 */
	public boolean isPaused() {
		return paused;
	}

}
