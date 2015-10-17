package tilegame;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Iterator;

import javax.sound.midi.Sequencer;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import graphics.NullRepaintManager;
import graphics.Sprite;
import input.GameAction;
import input.InputManager;
import sound.EchoFilter;
import sound.ISoundManager;
import sound.MidiPlayer;
import sound.Sound;
import sound.SoundManagerExecutor;
import test.GameCore;
import tilegame.sprites.Creature;
import tilegame.sprites.Player;
import tilegame.sprites.PowerUp;

/**
 * GameManager manages all parts of the game.
 */
public class GameManager extends GameCore implements ActionListener {

	private static final int DRUM_TRACK = 1;

	private static final float GRAVITY = 0.002f;

	private Point pointCache = new Point();
	
	private Sound prizeSound;
	private Sound boopSound;
	private Sound jumpSound;
	private ISoundManager soundManager;
	private MidiPlayer midiPlayer;
	
	private InputManager inputManager;
	private ResourceManager resourceManager;
	private TileMapRenderer renderer;
	private TileMap map;

	private GameAction moveLeft;
	private GameAction moveRight;
	private GameAction jump;
	
	private GameAction pause;
	private GameAction config;
	private GameAction exit;
	
	@Override
	public void init() {
		super.init();
		initRepaintManager();
		initInput();
		initResources();
		initButton();
		initSounds();
		initMusic();
	}
	
	/**
	 * set up input manager
	 */
	private void initInput() {
		moveLeft = new GameAction("moveLeft");
		moveRight = new GameAction("moveRight");
		jump = new GameAction("jump", GameAction.DETECT_INITAL_PRESS_ONLY);
		
		pause = new GameAction("pause", GameAction.DETECT_INITAL_PRESS_ONLY);
		config = new GameAction("config");
		exit = new GameAction("exit", GameAction.DETECT_INITAL_PRESS_ONLY);

		inputManager = new InputManager(screen.getFullScreenWindow());
		//inputManager.setCursor(InputManager.INVISIBLE_CURSOR);

		inputManager.mapToKey(moveLeft, KeyEvent.VK_LEFT);
		inputManager.mapToKey(moveRight, KeyEvent.VK_RIGHT);
		inputManager.mapToKey(jump, KeyEvent.VK_UP);
		
		inputManager.mapToKey(pause, KeyEvent.VK_P);
		inputManager.mapToKey(exit, KeyEvent.VK_ESCAPE);
	}
	
	private boolean paused;
	
	private JButton playButton;
	private JButton configButton;
	private JButton quitButton;
	private JButton pauseButton;
	private JPanel playButtonSpace;
	
	private void initRepaintManager() {
		// make sure Swing components don't paint themselves
		NullRepaintManager.install();
	}
	
	private void initButton() {
		// create buttons
		quitButton = createButton("quit", "退出");
		playButton = createButton("play", "继续");
		pauseButton = createButton("pause", "暂停");
		configButton = createButton("config", "全屏切换");

		// create the space where the play/pause buttons go.
		playButtonSpace = new JPanel();
		playButtonSpace.setOpaque(false);
		playButtonSpace.add(pauseButton);

		JFrame frame = screen.getFullScreenWindow();
		Container contentPane = frame.getContentPane();

		// make sure the content pane is transparent
		if (contentPane instanceof JComponent) {
			((JComponent) contentPane).setOpaque(false);
		}

		// add components to the screen's content pane
		contentPane.setLayout(new FlowLayout(FlowLayout.LEFT));
		contentPane.add(playButtonSpace);
		contentPane.add(configButton);
		contentPane.add(quitButton);

		// explicitly layout components (needed on some systems)
		frame.validate();
	}
	
	/**
	 * Creates a Swing JButton. The image used for the button is located at
	 * "../images/menu/" + name + ".png". The image is modified to create a
	 * "default" look (translucent) and a "pressed" look (moved down and to the
	 * right).
	 * <p>
	 * The button doesn't use Swing's look-and-feel and instead just uses the
	 * image.
	 */
	public JButton createButton(String name, String toolTip) {
		// create the button
		JButton button = new JButton();
		button.addActionListener(this);
		button.setIgnoreRepaint(true);
		button.setFocusable(false);
		button.setToolTipText(toolTip);
		button.setBorder(null);
		button.setContentAreaFilled(false);
		
		// get the cursor for this button
		Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		button.setCursor(cursor);
		
		// load the image
		String menuImagePath = ResourceManager.menuPath + name + ".png";
		ImageIcon iconRollover = resourceManager.loadImageIcon(menuImagePath);
		int w = iconRollover.getIconWidth();
		int h = iconRollover.getIconHeight();

		// make translucent default image
		Image image = screen.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
		Graphics2D g = (Graphics2D) image.getGraphics();
		Composite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f);
		g.setComposite(alpha);
		g.drawImage(iconRollover.getImage(), 0, 0, null);
		g.dispose();
		ImageIcon iconDefault = new ImageIcon(image);

		// make a pressed image
		image = screen.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
		g = (Graphics2D) image.getGraphics();
		g.drawImage(iconRollover.getImage(), 2, 2, null);
		g.dispose();
		ImageIcon iconPressed = new ImageIcon(image);
		
		button.setIcon(iconDefault);
		button.setRolloverIcon(iconRollover);
		button.setPressedIcon(iconPressed);

		return button;
	}
	
	/**
	 * Called by the AWT event dispatch thread when a button is pressed.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == quitButton) {
			// fire the "exit" gameAction
			exit.tap();
		} else if (src == configButton) {
			// doesn't do anything (for now)
			config.tap();
		} else if (src == playButton || src == pauseButton) {
			// fire the "pause" gameAction
			pause.tap();
		}
	}
	
	/**
	 * Sets the paused state.
	 */
	private void setPaused(boolean p) {
		if (paused != p) {
			paused = p;
			midiPlayer.setPaused(paused);
			inputManager.resetAllGameActions();
		}
		
		playButtonSpace.removeAll();
		if (isPaused()) {
			playButtonSpace.add(playButton);
		} else {
			playButtonSpace.add(pauseButton);
		}
	}
	
	/**
	 * Tests whether the game is paused or not.
	 */
	public boolean isPaused() {
		return paused;
	}
	
	public boolean isFullScreen() {
		return isFullScreen;
	}
	
	private void initResources() {
		// start resource manager
		resourceManager = new ResourceManager(screen.getFullScreenWindow().getGraphicsConfiguration());
		// load resources
		resourceManager.loadResources();
		
		// load renderer with background
		renderer = resourceManager.loadRenderer();

		// load first map
		map = resourceManager.loadNextMap();
	}
	
	/**
	 * load sounds
	 */
	private void initSounds() {
		//soundManager = new SoundManagerThreadPool(ISoundManager.PLAYBACK_FORMAT);
		soundManager = new SoundManagerExecutor();
		prizeSound = soundManager.getSound(ResourceManager.soundPath + "prize.wav");
		
		try {
			boopSound = soundManager.tryGetSound(ResourceManager.soundPath + "crush.wav");
		} catch (UnsupportedAudioFileException e) {
			boopSound = soundManager.getSound(ResourceManager.soundPath + "boop2.wav");
		} catch (IOException ioe) {
			boopSound = soundManager.getSound(ResourceManager.soundPath + "boop2.wav");
		}
		
		try {
			jumpSound = soundManager.tryGetSound(ResourceManager.soundPath + "jump.wav");
		} catch (UnsupportedAudioFileException e) {
			jumpSound = Sound.NONE_SOUND;
		} catch (IOException ioe) {
			jumpSound = Sound.NONE_SOUND;
		}
	}
	
//	public static void main(String[] args) {
//		SoundManagerExecutor soundManager = new SoundManagerExecutor();
////		SoundManagerThreadPool soundManager = new SoundManagerThreadPool(ISoundManager.PLAYBACK_FORMAT);
////		AudioFormat sourceFormat = AudioSystem.getAudioInputStream(game.getClass().getResourceAsStream(ResourceManager.soundPath + "crush.wav")).getFormat();
////		AudioFormat targetFormat = AudioSystem.getAudioInputStream(game.getClass().getResourceAsStream(ResourceManager.soundPath + "prize.wav")).getFormat();
////		System.out.println(AudioSystem.isConversionSupported(sourceFormat, targetFormat));
//		Sound boopSound = soundManager.getSound(ResourceManager.soundPath + "boop2.wav");
//		soundManager.play(boopSound);
////		Sound crushSound = soundManager.getSound2(ResourceManager.soundPath + "crush.wav");
////		soundManager.play(crushSound);
//		//soundManager.close();
//	}
	
	private void initMusic() {
		midiPlayer = new MidiPlayer(ResourceManager.soundPath + "music.mid", true);
		midiPlayer.play();
		//toggleDrumPlayback();
	}
	
	/**
	 * Turns on/off drum playback in the midi music (track 1).
	 */
	private void toggleDrumPlayback() {
		Sequencer sequencer = midiPlayer.getSequencer();
		if (sequencer != null) {
			sequencer.setTrackMute(DRUM_TRACK, !sequencer.getTrackMute(DRUM_TRACK));
		}
	}
	
	@Override
	public void draw(Graphics2D g) {
		renderer.draw(g, map, screen);
		
		// the layered pane contains things like popups (tooltips,
		// popup menus) and the content pane.
		JFrame frame = screen.getFullScreenWindow();
		frame.getLayeredPane().paintComponents(g);
	}

	/**
	 * Gets the current map.
	 */
	@Deprecated
	public TileMap getMap() {
		return map;
	}

	/**
	 * Updates Animation, position, and velocity of all Sprites in the current map.
	 */
	@Override
	public void update(long elapsedTime) {
		if (isPlayerDead()) {
			reloadMap();
			return;
		}

		checkSystemInput();
		if (!isPaused()) {
			checkGameInput();
			updatePlayer(elapsedTime);
			updateOtherSprites(elapsedTime);
		}
	}
	
	private boolean isPlayerDead() {
		return ((Creature) map.getPlayer()).isDead();
	}
	
	/**
	 * player is dead! start map over
	 */
	private void reloadMap() {
		map = resourceManager.reloadMap();
	}
	
	/**
	 * get keyboard/mouse input
	 * @param elapsedTime
	 */
	private void checkSystemInput() {
		if (pause.isPressed()) {
			setPaused(!isPaused());
		}
		if(config.isPressed()){
			setFullScreen(!isFullScreen());
		}
		if (exit.isPressed()) {
			stop();
		}
	}
	
	private void setFullScreen(boolean f) {
		if (isFullScreen != f) {
			isFullScreen = f;
			screen.setFullScreen(isFullScreen);
		}
	}
	
	private void checkGameInput(){
		Player player = (Player) map.getPlayer();
		if (player.isAlive()) {
			float velocityX = 0;
			if (moveLeft.isPressed()) {
				velocityX -= player.getMaxSpeed();
			}
			if (moveRight.isPressed()) {
				velocityX += player.getMaxSpeed();
			}
			if (jump.isPressed()) {
				if(player.jump(false)){
					soundManager.play(jumpSound);
				}
			}
			player.setVelocityX(velocityX);
		}
	}
	
	private void updatePlayer(long elapsedTime) {
		Creature player = (Creature) map.getPlayer();
		updateCreature(player, elapsedTime);
		player.update(elapsedTime);
	}
	
	private void updateOtherSprites(long elapsedTime) {
		Iterator<Sprite> i = map.getSprites();
		while (i.hasNext()) {
			Sprite sprite = i.next();
			if (sprite instanceof Creature) {
				Creature creature = (Creature) sprite;
				if (creature.isDead()) {
					i.remove();
				} else {
					updateCreature(creature, elapsedTime);
				}
			}
			// normal update
			sprite.update(elapsedTime);
		}
	}
	
	/**
	 * Closes any resurces used by the GameManager.
	 */
	@Override
	public void stop() {
		super.stop();
		midiPlayer.close();
		soundManager.close();
	}

	/**
	 * Updates the creature, applying gravity for creatures that aren't flying, and checks collisions.
	 */
	private void updateCreature(Creature creature, long elapsedTime) {
		applyGravity(creature, elapsedTime);
		detectCollisionHorizontally(creature, elapsedTime);
		detectCollisionVertically(creature, elapsedTime);
	}
	
	private void applyGravity(Creature creature, long elapsedTime) {
		if (!creature.isFlying()) {
			creature.setVelocityY(creature.getVelocityY() + GRAVITY * elapsedTime);
		}
	}
	
	private void detectCollisionHorizontally(Creature creature, long elapsedTime) {
		// change x
		float dx = creature.getVelocityX();
		float oldX = creature.getX();
		float newX = oldX + dx * elapsedTime;
		Point tile = getTileCollision(creature, newX, creature.getY());
		if (tile == null) {
			creature.setX(newX);
		} else {
			// line up with the tile boundary
			if (dx > 0) {
				creature.setX(TileMapRenderer.tilesToPixels(tile.x) - creature.getWidth());
			} else if (dx < 0) {
				creature.setX(TileMapRenderer.tilesToPixels(tile.x + 1));
			}
			creature.collideHorizontal();
		}
		if (creature instanceof Player) {
			checkPlayerCollision((Player) creature, false);
		}
	}
	
	private void detectCollisionVertically(Creature creature, long elapsedTime) {
		// change y
		float dy = creature.getVelocityY();
		float oldY = creature.getY();
		float newY = oldY + dy * elapsedTime;
		Point tile = getTileCollision(creature, creature.getX(), newY);
		if (tile == null) {
			creature.setY(newY);
		} else {
			// line up with the tile boundary
			if (dy > 0) {
				creature.setY(TileMapRenderer.tilesToPixels(tile.y) - creature.getHeight());
			} else if (dy < 0) {
				creature.setY(TileMapRenderer.tilesToPixels(tile.y + 1));
			}
			creature.collideVertical();
		}
		if (creature instanceof Player) {
			boolean canKill = (oldY < creature.getY());
			checkPlayerCollision((Player) creature, canKill);
		}
	}
	
	/**
	 * Gets the tile that a Sprites collides with. Only the Sprite's X or Y
	 * should be changed, not both. Returns null if no collision is detected.
	 */
	private Point getTileCollision(Sprite sprite, float newX, float newY) {
		float fromX = Math.min(sprite.getX(), newX);
		float fromY = Math.min(sprite.getY(), newY);
		float toX = Math.max(sprite.getX(), newX);
		float toY = Math.max(sprite.getY(), newY);

		// get the tile locations
		int fromTileX = TileMapRenderer.pixelsToTiles(fromX);
		int fromTileY = TileMapRenderer.pixelsToTiles(fromY);
		int toTileX = TileMapRenderer.pixelsToTiles(toX + sprite.getWidth() - 1);
		int toTileY = TileMapRenderer.pixelsToTiles(toY + sprite.getHeight() - 1);

		// check each tile for a collision
		for (int x = fromTileX; x <= toTileX; x++) {
			for (int y = fromTileY; y <= toTileY; y++) {
				if (x < 0 || x >= map.getWidth() || map.getTile(x, y) != null) {
					// collision found, return the tile
					pointCache.setLocation(x, y);
					return pointCache;
				}
			}
		}

		// no collision found
		return null;
	}

	/**
	 * Checks for Player collision with other Sprites. If canKill is true,
	 * collisions with Creatures will kill them.
	 */
	private void checkPlayerCollision(Player player, boolean canKill) {
		if (!player.isAlive()) {
			return;
		}

		// check for player collision with other sprites
		Sprite collisionSprite = getSpriteCollision(player);
		if (collisionSprite instanceof PowerUp) {
			acquirePowerUp((PowerUp) collisionSprite);
		} else if (collisionSprite instanceof Creature) {
			Creature badGuy = (Creature) collisionSprite;
			if (canKill) {
				// kill the badguy and make player bounce
				soundManager.play(boopSound);
				badGuy.setState(Creature.STATE_DYING);
				player.setY(badGuy.getY() - player.getHeight());
				player.jump(true);
			} else {
				// player dies!
				//soundManager.play(boopSound);
				player.setState(Creature.STATE_DYING);
			}
		}
	}
	
	/**
	 * Gets the Sprite that collides with the specified Sprite, or null if no
	 * Sprite collides with the specified Sprite.
	 */
	private Sprite getSpriteCollision(Sprite sprite) {
		// run through the list of Sprites
		Iterator<Sprite> i = map.getSprites();
		while (i.hasNext()) {
			Sprite otherSprite = i.next();
			if (isCollision(sprite, otherSprite)) {
				// collision found, return the Sprite
				return otherSprite;
			}
		}
		// no collision found
		return null;
	}
	
	/**
	 * Checks if two Sprites collide with one another. Returns false if the two
	 * Sprites are the same. Returns false if one of the Sprites is a Creature
	 * that is not alive.
	 */
	private boolean isCollision(Sprite s1, Sprite s2) {
		// if the Sprites are the same, return false
		if (s1 == s2) {
			return false;
		}

		// if one of the Sprites is a dead Creature, return false
		if (s1 instanceof Creature && !((Creature) s1).isAlive()) {
			return false;
		}
		if (s2 instanceof Creature && !((Creature) s2).isAlive()) {
			return false;
		}

		// get the pixel location of the Sprites
		int s1x = Math.round(s1.getX());
		int s1y = Math.round(s1.getY());
		int s2x = Math.round(s2.getX());
		int s2y = Math.round(s2.getY());

		// check if the two sprites' boundaries intersect
		return s1x < s2x + s2.getWidth() && s2x < s1x + s1.getWidth() 
				&& s1y < s2y + s2.getHeight() && s2y < s1y + s1.getHeight();
	}

	/**
	 * Gives the player the speicifed power up and removes it from the map.
	 */
	private void acquirePowerUp(PowerUp powerUp) {
		// remove it from the map
		map.removeSprite(powerUp);

		if (powerUp instanceof PowerUp.Star) {
			// do something here, like give the player points
			soundManager.play(prizeSound);
		} else if (powerUp instanceof PowerUp.Music) {
			// change the music
			soundManager.play(prizeSound);
			toggleDrumPlayback();
		} else if (powerUp instanceof PowerUp.Goal) {
			// advance to next map
			soundManager.play(prizeSound, new EchoFilter(2000, .7f), false);
			map = resourceManager.loadNextMap();
		}
	}
}