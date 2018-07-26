package jumpydoggo.main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import jumpydoggo.manager.UnitManager;

public class Main extends DefaultBWListener {
	
    private Mirror mirror = new Mirror();

    public static Game game;

    public static Player self;
    
    public int supplyUsedActual;
    public static Integer availableMinerals;
    public static Integer availableGas;
    
    private static Set<TilePosition> plannedPositions = new HashSet<TilePosition>();
    public static HashMap<Integer, UnitManager> unitManagers = new HashMap<Integer, UnitManager>();
    public HashMap<UnitType, HashSet<Integer>> unitManagerIDs = new HashMap<UnitType, HashSet<Integer>>(); //IDs by unit type, for quick access
   
    
    public void assignUnitManager(Unit unit) {
    	if (unit.getType() == UnitType.Zerg_Drone) {
    		//TODO workermanager
    	}  else {
    		unitManagers.put(unit.getID(), new UnitManager(unit));
    	}
    	unitManagerIDs.putIfAbsent(unit.getType(), new HashSet<Integer>());
    	unitManagerIDs.get(unit.getType()).add(unit.getID());
    }
    
    
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
    public void onUnitCreate(Unit unit) {
    	System.out.println("DEBUG:" + unit.getType() + " created (onUnitCreate)");
    }
    
    @Override
    public void onUnitMorph(Unit unit) {
    	System.out.println(unit.getType());
    
    }
    
	@Override
	public void onStart() {

		rand = new Random();
		game = mirror.getGame();
		self = game.self();
		game.setLocalSpeed(30);
     	game.enableFlag(1);
		System.out.println("bork bork imma pew a " + game.enemy().getRace());

	}
	
	@Override
	public void onUnitDestroy(Unit unit) {
		System.out.println(unit.getType() + " destroyed");
	}
	
	@Override
	public void onFrame() {
		frameCount++;
		countAllUnits();
		StringBuilder statusMessages = new StringBuilder();
	    statusMessages.append("Larva count:" + unitCounts.get(UnitType.Zerg_Larva)+ "\n");
		statusMessages.append("Minerals gathered:" + self.gatheredMinerals() + "\n");
		statusMessages.append("frames:" + frameCount + "\n");
		statusMessages.append("st:" + self.supplyTotal()	 + "\n");
		
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
				//unit.morph(UnitType.Zerg_Drone);
			}
			
			game.drawTextMap(unit.getX(), unit.getY(), Integer.toString(unit.getID()));
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
    
    //Default buildtile, not accounting for simcity 
    public static TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
    	TilePosition ret = null;
    	int maxDist = 3;
    	int stopDist = 40;
    	
    	if (aroundTile == null) {
    		aroundTile = self.getStartLocation();
    	}
    	
    	// Refinery, Assimilator, Extractor
    	if (buildingType.isRefinery()) {
    		for (Unit n : game.neutral().getUnits()) {
    			if ((n.getType() == UnitType.Resource_Vespene_Geyser) &&
    					( Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist ) &&
    					( Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist )
    					) return n.getTilePosition();
    		}
    	}
    	while ((maxDist < stopDist) && (ret == null)) {
    		for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
    			for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
    				TilePosition targetTile = new TilePosition(i,j);
    				boolean canBuild = true;
    				for (TilePosition tp : getTilesForBuilding(targetTile.toPosition(), buildingType)){
    					if (plannedPositions.contains(tp)) {
    						canBuild = false;
    					}
    				}

    				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false) && canBuild) {
    					// units that are blocking the tile
    					boolean unitsInWay = false;
    					for (Unit u : game.getAllUnits()) {
    						if (u.getID() == builder.getID()) continue;
    						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
    					}
    					if (!unitsInWay) {
    						ret = new TilePosition(i, j);
    					    for (int th=0; th<buildingType.tileHeight();th++) {
        					    for (int tw=0; tw<buildingType.tileWidth();tw++) {    	
        					    	TilePosition occupied = new TilePosition(i+tw, j+th);
        					    	if (!plannedPositions.contains(occupied)) plannedPositions.add(occupied);
        					    }
    					    }			
    						return ret;
    					}
    					// creep for Zerg
    					if (buildingType.requiresCreep()) {
    						boolean creepMissing = false;
    						for (int k=i; k<=i+buildingType.tileWidth(); k++) {
    							for (int l=j; l<=j+buildingType.tileHeight(); l++) {
    								if (!game.hasCreep(k, l)) creepMissing = true;
    								break;
    							}
    						}
    						if (creepMissing) continue;
    					}
    				}
    			}
    		}
    		maxDist += 1;
    	}
    	if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
    	return ret;
    }
    
    //Starting from the given position, returns which building tiles will the building occupy.
    public static HashSet<TilePosition> getTilesForBuilding(Position pos, UnitType type) {
    	int x = (pos.getX() / 32)*32 ;
    	int y = (pos.getY() / 32)*32;
    	int h = type.tileHeight();
    	int w = type.tileWidth();
    	HashSet<TilePosition> buildingTiles = new HashSet<TilePosition>();
    	
    	for (int i=0; i<h;i++) {
    		for (int j=0;j<w;j++) {
    			TilePosition tp = new TilePosition((x+i*32)/32, (y+j*32)/32);
    			buildingTiles.add(tp);
    		}
    	}
    	return buildingTiles;
    }
    

}
