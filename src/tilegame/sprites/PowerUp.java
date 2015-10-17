package tilegame.sprites;

import java.lang.reflect.Constructor;

import graphics.Animation;
import graphics.Sprite;

/**
 * A PowerUp class is a Sprite that the player can pick up.
 */
public class PowerUp extends Sprite {

	private PowerUp(Animation anim) {
		super(anim);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PowerUp clone() {
		// use reflection to create the correct subclass
		Constructor<PowerUp> constructor = (Constructor<PowerUp>) (getClass().getConstructors()[0]);
		try {
			return constructor.newInstance(new Object[] { (Animation) anim.clone() });
		} catch (Exception ex) {
			// should never happen
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * A Star PowerUp. Gives the player points.
	 */
	public static class Star extends PowerUp {
		public Star(Animation anim) {
			super(anim);
		}
	}

	/**
	 * A Music PowerUp. Changes the game music.
	 */
	public static class Music extends PowerUp {
		public Music(Animation anim) {
			super(anim);
		}
	}

	/**
	 * A Goal PowerUp. Advances to the next map.
	 */
	public static class Goal extends PowerUp {
		public Goal(Animation anim) {
			super(anim);
		}
	}

}
