package main.jumpydoggo;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;

public class Main extends DefaultBWListener {
	
    private Mirror mirror = new Mirror();

    public static Game game;

    public static Player self;
    
    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame(false);
    }
    
    @Override
    public void onStart() {
    	System.out.println("starteded");
    	
    }
    
    public static void main(String[] args) {
        new Main().run();
    }

}
