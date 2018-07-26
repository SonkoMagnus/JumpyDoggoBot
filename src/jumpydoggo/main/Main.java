package jumpydoggo.main;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Race;
import bwapi.Unit;
import bwapi.UnitType;

public class Main extends DefaultBWListener {
	
    private Mirror mirror = new Mirror();

    public static Game game;

    public static Player self;
    
    public int supplyUsedActual;
   
    Random rand;
    
    /**
     * Frames passed
     */
	public static int frameCount = 0;
    /**
     * Keeps track of how many units of each unit type the player has.
     */
    public HashMap<UnitType, Integer> unitCounts = new HashMap<UnitType, Integer>();
    /**
     * Keeps track of how many units are in production
     */
    public HashMap<UnitType, Integer> unitsInProduction = new HashMap<UnitType, Integer>();
    
   // public HashMap<UnitType, Integer> targetUnitNumbers = new HashMap<UnitType, Integer>(); //Unit goals 
    
    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame(false);
    }
    
	@Override
	public void onStart() {

		rand = new Random();
		game = mirror.getGame();
		self = game.self();
		game.setLocalSpeed(30);
		System.out.println("bork bork imma pew a " + game.enemy().getRace());

	}
	
	@Override
	public void onFrame() {
		frameCount++;
		countAllUnits();
		StringBuilder statusMessages = new StringBuilder();
	    statusMessages.append("Larva count:" + unitCounts.get(UnitType.Zerg_Larva)+ "\n");
		statusMessages.append("Minerals gathered:" + self.gatheredMinerals() + "\n");
		statusMessages.append("frames:" + frameCount + "\n");
		//Not sure if meaningful statistic
		//double mineralPerFrame =  (double) self.gatheredMinerals() / (double) frameCount;
		//statusMessages.append("Minerals gathered per frame per drone:" + mineralPerFrame / unitCounts.get(UnitType.Zerg_Drone)+ "\n");
		
		for (Unit unit : self.getUnits()) {
			if (unit.getType() == UnitType.Zerg_Drone) {
				if (unit.isIdle()) {
					Unit closestMineral = null;
					// find the closest mineral
					for (Unit neutralUnit : Main.game.neutral().getUnits()) {
						if (neutralUnit.getType().isMineralField()) {
							if (closestMineral == null
									|| unit.getDistance(neutralUnit) < unit.getDistance(closestMineral)) {
								closestMineral = neutralUnit;
							}
						}
					}
					// if a mineral patch was found, send the worker to gather it
					if (closestMineral != null) {
						unit.gather(closestMineral, false);
					}
				}
				
			}
			if (unit.getType() == UnitType.Zerg_Larva && self.minerals() >= 50) {
				unit.morph(UnitType.Zerg_Drone);
			}
		}
		
		
		//DEm feedbak
		 game.drawTextScreen(10, 25, statusMessages.toString());        
		
	}

    public static void main(String[] args) {
        new Main().run();
    }
    
    public void countAllUnits() {
    	unitCounts = new HashMap<UnitType, Integer>();
        unitsInProduction = new HashMap<UnitType, Integer>();
        supplyUsedActual = 0;
            for (Unit myUnit : self.getUnits()) 
	        {
            	supplyUsedActual += myUnit.getType().supplyRequired();
	        	if(!unitCounts.containsKey(myUnit.getType())) 
	        		unitCounts.put(myUnit.getType(), 1);
	        	else
	        		unitCounts.put(myUnit.getType(), unitCounts.get(myUnit.getType())+1);
	        	
	        	if (myUnit.getType().isBuilding()) {
	        		List<UnitType> trimList = myUnit.getTrainingQueue();
	        		if (trimList.size() > 0 ) {
	        			trimList.remove(0);
	        		}
	        		for (UnitType ut : trimList) {		
	        			
	        			if(!unitsInProduction.containsKey(ut)) 
	        				unitsInProduction.put(ut, 1);
	    	        	else
	    	        		unitsInProduction.put(ut, unitsInProduction.get(ut)+1);
	        		}
	        	}
	        }
     }

}
