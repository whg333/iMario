package tilegame;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import graphics.Animation;
import graphics.Sprite;
import tilegame.sprites.Fly;
import tilegame.sprites.Grub;
import tilegame.sprites.Player;
import tilegame.sprites.PowerUp;

/**
 * The ResourceManager class loads and manages tile Images and "host" Sprites
 * used in the game. Game Sprites are cloned from "host" Sprites.
 */
public class ResourceManager {

	public static final String mapPath = "/maps/";
	public static final String soundPath = "/sounds/";
	public static final String imagePath = "/images/";
	public static final String menuPath = "/images/menu/";
	
	private final GraphicsConfiguration gc;
	
	private TileMapRenderer renderer;
	private List<Image> tiles;
	private int currentMap;

	// host sprites used for cloning
	private Sprite playerSprite;
	private Sprite flySprite;
	private Sprite grubSprite;
	
	private Sprite coinSprite;
	private Sprite musicSprite;
	private Sprite goalSprite;
	
	/**
	 * Creates a new ResourceManager with the specified GraphicsConfiguration.
	 */
	public ResourceManager(GraphicsConfiguration gc) {
		this.gc = gc;
	}
	
	public void loadResources(){
		loadTileImages();
		loadCreatureSprites();
		loadPowerUpSprites();
	}

	// -----------------------------------------------------------
	// code for loading sprites and images
	// -----------------------------------------------------------

	private void loadTileImages() {
		// keep looking for tile A,B,C, etc. this makes it
		// easy to drop new tiles in the images/ directory
		tiles = new ArrayList<Image>();
		char ch = 'A';
		while (true) {
			String name = "tile_" + ch + ".png";
			InputStream input = getClass().getResourceAsStream(imagePath + name);
			if (input == null) {
				break;
			}
			tiles.add(loadImage(name));
			ch++;
		}
	}

	private void loadCreatureSprites() {

		Image[][] images = new Image[4][];

		// load left-facing images
		images[0] = new Image[] { 
			loadImage("player1.png"), 
			loadImage("player2.png"), 
			loadImage("player3.png"),
			loadImage("fly1.png"), 
			loadImage("fly2.png"), 
			loadImage("fly3.png"), 
			loadImage("grub1.png"),
			loadImage("grub2.png"),
		};

		images[1] = new Image[images[0].length];
		images[2] = new Image[images[0].length];
		images[3] = new Image[images[0].length];
		for (int i = 0; i < images[0].length; i++) {
			// right-facing images
			images[1][i] = getMirrorImage(images[0][i]);
			// left-facing "dead" images
			images[2][i] = getFlippedImage(images[0][i]);
			// right-facing "dead" images
			images[3][i] = getFlippedImage(images[1][i]);
		}

		// create creature animations
		Animation[] playerAnim = new Animation[4];
		Animation[] flyAnim = new Animation[4];
		Animation[] grubAnim = new Animation[4];
		for (int i = 0; i < 4; i++) {
			playerAnim[i] = createPlayerAnim(images[i][0], images[i][1], images[i][2]);
			flyAnim[i] = createFlyAnim(images[i][3], images[i][4], images[i][5]);
			grubAnim[i] = createGrubAnim(images[i][6], images[i][7]);
		}

		// create creature sprites
		playerSprite = new Player(playerAnim[0], playerAnim[1], playerAnim[2], playerAnim[3]);
		flySprite = new Fly(flyAnim[0], flyAnim[1], flyAnim[2], flyAnim[3]);
		grubSprite = new Grub(grubAnim[0], grubAnim[1], grubAnim[2], grubAnim[3]);
	}

	private Animation createPlayerAnim(Image player1, Image player2, Image player3) {
		Animation anim = new Animation();
		anim.addFrame(player1, 250);
		anim.addFrame(player2, 150);
		anim.addFrame(player1, 150);
		anim.addFrame(player2, 150);
		anim.addFrame(player3, 200);
		anim.addFrame(player2, 150);
		return anim;
	}

	private Animation createFlyAnim(Image img1, Image img2, Image img3) {
		Animation anim = new Animation();
		anim.addFrame(img1, 50);
		anim.addFrame(img2, 50);
		anim.addFrame(img3, 50);
		anim.addFrame(img2, 50);
		return anim;
	}

	private Animation createGrubAnim(Image img1, Image img2) {
		Animation anim = new Animation();
		anim.addFrame(img1, 250);
		anim.addFrame(img2, 250);
		return anim;
	}

	private void loadPowerUpSprites() {
		// create "goal" sprite
		Animation anim = new Animation();
		anim.addFrame(loadImage("heart1.png"), 150);
		anim.addFrame(loadImage("heart2.png"), 150);
		anim.addFrame(loadImage("heart3.png"), 150);
		anim.addFrame(loadImage("heart2.png"), 150);
		goalSprite = new PowerUp.Goal(anim);

		// create "star" sprite
		anim = new Animation();
		anim.addFrame(loadImage("star1.png"), 100);
		anim.addFrame(loadImage("star2.png"), 100);
		anim.addFrame(loadImage("star3.png"), 100);
		anim.addFrame(loadImage("star4.png"), 100);
		coinSprite = new PowerUp.Star(anim);

		// create "music" sprite
		anim = new Animation();
		anim.addFrame(loadImage("music1.png"), 150);
		anim.addFrame(loadImage("music2.png"), 150);
		anim.addFrame(loadImage("music3.png"), 150);
		anim.addFrame(loadImage("music2.png"), 150);
		musicSprite = new PowerUp.Music(anim);
	}
	
	public TileMapRenderer loadRenderer() {
		TileMapRenderer renderer = new TileMapRenderer();
		renderer.withBackground(loadImage("background.png"));
		return renderer;
	}
	
	/**
	 * Gets an image from the images/ directory.
	 */
	public Image loadImage(String name) {
		String filename = imagePath + name;
		return loadImageIcon(filename).getImage();
	}
	
	public ImageIcon loadImageIcon(String path){
		return new ImageIcon(getClass().getResource(path));
	}

	public Image getMirrorImage(Image image) {
		return getScaledImage(image, -1, 1);
	}

	public Image getFlippedImage(Image image) {
		return getScaledImage(image, 1, -1);
	}

	private Image getScaledImage(Image image, float x, float y) {
		// set up the transform
		AffineTransform transform = new AffineTransform();
		transform.scale(x, y);
		transform.translate((x - 1) * image.getWidth(null) / 2, (y - 1) * image.getHeight(null) / 2);

		// create a transparent (not translucent) image
		Image newImage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), Transparency.BITMASK);

		// draw the transformed image
		Graphics2D g = (Graphics2D) newImage.getGraphics();
		g.drawImage(image, transform, null);
		g.dispose();

		return newImage;
	}

	public TileMap loadNextMap() {
		TileMap map = null;
		while (map == null) {
			currentMap++;
			try {
				map = loadMap(mapPath + "map" + currentMap + ".txt");
			} catch (IOException ex) {
				if (currentMap == 1) {
					// no maps to load!
					return null;
				}
				currentMap = 0;
				map = null;
			}
		}
		//System.out.println(currentMap);
		return map;
	}
	
	public TileMap reloadMap() {
		try {
			return loadMap(mapPath + "map" + currentMap + ".txt");
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private TileMap loadMap(String filename) throws IOException {
		List<String> lines = new ArrayList<String>();
		int width = 0;
		int height = 0;

		// read every line in the text file into the list
		InputStream inputStream = getClass().getResourceAsStream(filename);
		if(inputStream == null){
			throw new IOException(filename + ":map Not Found!");
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		while (true) {
			String line = reader.readLine();
			// no more lines to read
			if (line == null) {
				reader.close();
				break;
			}

			// add every line except for comments
			if (!line.startsWith("#")) {
				lines.add(line);
				width = Math.max(width, line.length());
			}
		}

		// parse the lines to create a TileEngine
		height = lines.size();
		TileMap newMap = new TileMap(width, height);
		for (int y = 0; y < height; y++) {
			String line = lines.get(y);
			for (int x = 0; x < line.length(); x++) {
				char ch = line.charAt(x);

				// check if the char represents tile A, B, C etc.
				int tile = ch - 'A';
				if (tile >= 0 && tile < tiles.size()) {
					newMap.setTile(x, y, tiles.get(tile));
				}else{
					// check if the char represents a sprite
					if (ch == 'o') {
						addSprite(newMap, coinSprite, x, y);
					} else if (ch == '!') {
						addSprite(newMap, musicSprite, x, y);
					} else if (ch == '*') {
						addSprite(newMap, goalSprite, x, y);
					} else if (ch == '1') {
						addSprite(newMap, grubSprite, x, y);
					} else if (ch == '2') {
						addSprite(newMap, flySprite, x, y);
					}
				}
			}
		}

		// add the player to the map
		Sprite player = playerSprite.clone();
		player.setX(TileMapRenderer.tilesToPixels(3));
		player.setY(0);
		newMap.setPlayer(player);

		return newMap;
	}

	private void addSprite(TileMap map, Sprite hostSprite, int tileX, int tileY) {
		if (hostSprite == null) {
			return;
		}

		// clone the sprite from the "host"
		Sprite sprite = hostSprite.clone();

		// center the sprite
		sprite.setX(TileMapRenderer.tilesToPixels(tileX) + (TileMapRenderer.tilesToPixels(1) - sprite.getWidth()) / 2);

		// bottom-justify the sprite
		//往下挪一个方块再减去精灵高度，相当于让精灵底部对齐
		sprite.setY(TileMapRenderer.tilesToPixels(tileY + 1) - sprite.getHeight());

		// add it to the map
		map.addSprite(sprite);
	}

}