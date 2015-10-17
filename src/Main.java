import tilegame.GameManager;

public class Main {

	public static void main(String[] args) {
		GameManager game = new GameManager();
		if(args.length > 0 && args[0].equals("-f")){
			game.setFullScreen();
		}
		game.run();
	}
	
}
