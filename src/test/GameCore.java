package test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import graphics.ScreenManager;

/**
 * Simple abstract class used for testing. Subclasses should implement the
 * draw() method.
 */
public abstract class GameCore {

	protected static final int FONT_SIZE = 24;

	private boolean isRunning;
	
	protected String name = "我的超级玛丽";
	protected boolean isFullScreen;
	protected ScreenManager screen;

	public void setFullScreen(){
		isFullScreen = true;
	}
	
	/**
	 * Calls init() and gameLoop()
	 */
	public void run() {
		try {
			init();
			gameLoop();
		} finally {
			screen.restoreScreen();
			lazilyExit();
		}
	}
	
	public void restoreScreen(){
		isFullScreen = false;
		screen.restoreScreen();
	}

	/**
	 * Sets full screen mode and initiates and objects.
	 */
	public void init() {
		screen = new ScreenManager(name, isFullScreen, 640, 480);
		Window window = screen.getFullScreenWindow();
		window.setFont(new Font("Dialog", Font.PLAIN, FONT_SIZE));
		window.setBackground(Color.black);
		window.setForeground(Color.white);
		isRunning = true;
	}
	
	/**
	 * Runs through the game loop until stop() is called.
	 */
	public void gameLoop() {
		long startTime = System.currentTimeMillis();
		long currTime = startTime;

		while (isRunning) {
			long elapsedTime = System.currentTimeMillis() - currTime;
			currTime += elapsedTime;

			// update
			update(elapsedTime);

			// draw the screen
			Graphics2D g = screen.getGraphics();
			draw(g);
			g.dispose();
			screen.update();

			// take a nap
			try {
				Thread.sleep(20);
			} catch (InterruptedException ex) {

			}
		}
	}
	
	/**
	 * Updates the state of the game/animation based on the amount of elapsed time that has passed.
	 */
	public void update(long elapsedTime) {
		// do nothing
	}

	/**
	 * Draws to the screen. Subclasses must override this method.
	 */
	public abstract void draw(Graphics2D g);

	/**
	 * Exits the VM from a daemon thread. The daemon thread waits 2 seconds then
	 * calls System.exit(0). Since the VM should exit when only daemon threads
	 * are running, this makes sure System.exit(0) is only called if neccesary.
	 * It's neccesary if the Java Sound system is running.
	 */
	public void lazilyExit() {
		Thread thread = new Thread() {
			public void run() {
				// first, wait for the VM exit on its own.
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ex) {
				}
				// system is still running, so force an exit
				System.exit(0);
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Signals the game loop that it's time to quit
	 */
	public void stop() {
		isRunning = false;
	}
	
	public Image loadImage(String fileName) {
		System.out.println(fileName);
		return new ImageIcon(fileName).getImage();
	}
	
	public BufferedImage loadBigImage(String fileName) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return img;
	}

}
