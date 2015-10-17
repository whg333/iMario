package tilegame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.Iterator;

import graphics.ScreenManager;
import graphics.Sprite;
import tilegame.sprites.Creature;

/**
 * The TileMapRenderer class draws a TileMap on the screen. It draws all tiles,
 * sprites, and an optional background image centered around the position of the
 * player.
 * 
 * <p>
 * If the width of background image is smaller the width of the tile map, the
 * background image will appear to move slowly, creating a parallax background
 * effect.
 * 
 * <p>
 * Also, three static methods are provided to convert pixels to tile positions,
 * and vice-versa.
 * 
 * <p>
 * This TileMapRender uses a tile size of 64.
 */
public class TileMapRenderer {

	private static final int TILE_SIZE = 64;
	// the size(value is 6) in bits of the tile
	// Math.pow(2, TILE_SIZE_BITS) == TILE_SIZE
	private static final int TILE_SIZE_BITS = (int)(Math.log(TILE_SIZE)/Math.log(2));
	
	private Image background;

	/**
	 * Converts a pixel position to a tile position.
	 */
	public static int pixelsToTiles(float pixels) {
		return pixelsToTiles(Math.round(pixels));
	}

	/**
	 * Converts a pixel position to a tile position.
	 */
	public static int pixelsToTiles(int pixels) {
		// use shifting to get correct values for negative pixels
		return pixels >> TILE_SIZE_BITS;

		// or, for tile sizes that aren't a power of two,
		// use the floor function:
		// return (int)Math.floor((float)pixels / TILE_SIZE);
	}

	/**
	 * Converts a tile position to a pixel position.
	 */
	public static int tilesToPixels(int numTiles) {
		// no real reason to use shifting here.
		// it's slighty faster, but doesn't add up to much on modern processors.
		return numTiles << TILE_SIZE_BITS;

		// use this if the tile size isn't a power of 2:
		// return numTiles * TILE_SIZE;
	}

	/**
	 * Sets the background to draw.
	 */
	public void withBackground(Image background) {
		this.background = background;
	}

	/**
	 * Draws the specified TileMap.
	 */
	public void draw(Graphics2D g, TileMap map, ScreenManager screen) {
		Sprite player = map.getPlayer();
		int mapWidth = tilesToPixels(map.getWidth());
		int mapHeight = tilesToPixels(map.getHeight());

		int screenWidth = screen.getWidth();
		int screenHeight = screen.getHeight();
		
		// get the scrolling position of the map based on player's position
		int offsetX = screenWidth / 2 - Math.round(player.getX()) - TILE_SIZE;
		offsetX = Math.min(offsetX, 0);
		offsetX = Math.max(offsetX, screenWidth - mapWidth);

		// get the y offset to draw all sprites and tiles
		int offsetY = screenHeight / 2 - Math.round(player.getY()) - player.getHeight();
		offsetY = Math.min(offsetY, 0);
		offsetY = Math.max(offsetY, screenHeight - mapHeight);
		
//		System.out.println("player.getX():"+player.getX()+",map.getWidth():"+map.getWidth()+",mapWidth:"+mapWidth+",map.getHeight():"
//				+map.getHeight()+",mapHeight:"+mapHeight+",screenWidth:"+screenWidth+",screenHeight:"+screenHeight+",offsetX:"+offsetX+",offsetY:"+offsetY);

		drawBackground(g, mapWidth, screenWidth, screenHeight, offsetX);
		drawVisibleTiles(g, map, screenWidth, screenHeight, offsetX, offsetY);
		drawPlayer(g, player, offsetX, offsetY);
		drawOtherSprites(g, map, screenWidth, screenHeight, offsetX, offsetY);
	}
	
	private void drawBackground(Graphics2D g, int mapWidth, int screenWidth, int screenHeight, int offsetX) {
		// draw black background, if needed
		if (background == null || screenHeight > background.getHeight(null)) {
			g.setColor(Color.black);
			g.fillRect(0, 0, screenWidth, screenHeight);
		}

		// draw parallax background image
		if (background != null) {
			// 该backgroundX公式 = offsetX * 背景 / 地图，相当于做了个比例映射
			int backgroundX = offsetX * (screenWidth - background.getWidth(null)) / (screenWidth - mapWidth);
			int backgroundY = screenHeight - background.getHeight(null);

			g.drawImage(background, backgroundX, backgroundY, null);
		}
	}
	
	private void drawVisibleTiles(Graphics2D g, TileMap map, int screenWidth, int screenHeight, 
			int offsetX, int offsetY) {
		
		// draw the visible tiles
		int firstTileX = pixelsToTiles(-offsetX);
		int lastTileX = firstTileX + pixelsToTiles(screenWidth) + 1;
		for (int y = 0; y < screenHeight; y++) {
			for (int x = firstTileX; x <= lastTileX; x++) {
				Image image = map.getTile(x, y);
				if (image != null) {
					g.drawImage(image, tilesToPixels(x) + offsetX, tilesToPixels(y) + offsetY, null);
				}
			}
		}
	}
	
	private void drawPlayer(Graphics2D g, Sprite player, int offsetX, int offsetY) {
		// draw player
		g.drawImage(player.getImage(), Math.round(player.getX()) + offsetX, Math.round(player.getY()) + offsetY, null);
	}
	
	private void drawOtherSprites(Graphics2D g, TileMap map, int screenWidth, int screenHeight, 
			int offsetX, int offsetY) {
		
		// draw sprites
		int firstTileX = pixelsToTiles(-offsetX);
		int lastTileX = firstTileX + pixelsToTiles(screenWidth) + 1;
		int firstTileY = pixelsToTiles(-offsetY);
		int lastTileY = firstTileY + pixelsToTiles(screenHeight) + 1;
		Iterator<Sprite> i = map.getSprites();
		while (i.hasNext()) {
			Sprite sprite = i.next();
			int spriteX = Math.round(sprite.getX());
			int spriteY = Math.round(sprite.getY());
			
			//don't draw the sprite which it's not on screen
			if (firstTileX > pixelsToTiles(spriteX + sprite.getWidth()) || lastTileX <= pixelsToTiles(spriteX)
					|| firstTileY > pixelsToTiles(spriteY + sprite.getHeight()) || lastTileY <= pixelsToTiles(spriteY)) {
				continue;
			}
			
			g.drawImage(sprite.getImage(), spriteX + offsetX, spriteY + offsetY, null);
			// wake up the creature when it's on screen
			if (sprite instanceof Creature) {
				((Creature) sprite).wakeUp();
			}
		}
	}

}
