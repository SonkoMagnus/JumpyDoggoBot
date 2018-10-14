package jumpydoggo.main;

import java.util.HashMap;
import java.util.PriorityQueue;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class Main extends DefaultBWListener {
	
    private Mirror mirror = new Mirror();

    public static Game game;

    public static Player self;
    
    public int supplyUsedActual;
    
    public static int reservedMinerals;
    public static int reservedGas;
    
    public static int availableMinerals;
    public static int availableGas;
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
    
    PriorityQueue<PlannedItem> plannedItems = new PriorityQueue<>(new PlannedItemComparator());
    
    
    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame(false);
    }
    
	@Override
	public void onStart() {
		game = mirror.getGame();
		self = game.self();
		game.setLocalSpeed(30);
		
		//4 Pool example
		plannedItems.add(new PlannedItem(UnitType.Zerg_Spawning_Pool, 0, false, 1));
		plannedItems.add(new PlannedItem(UnitType.Zerg_Creep_Colony, 0, false, 0));
		
		System.out.println(plannedItems.peek().getUnitType());
	}
	
	@Override
	public void onUnitMorph(Unit unit) {
		for (PlannedItem pi : plannedItems) {
			if (pi.getUnitType() == unit.getType()  && pi.isInProgress()) {
				reservedMinerals -= pi.getUnitType().mineralPrice();
			}
		}
	}
	
	@Override
	public void onFrame() {
		frameCount++;
		countAllUnits();
		StringBuilder statusMessages = new StringBuilder();
		//statusMessages.append("Minerals gathered:" + self.gatheredMinerals() + "\n");
		//statusMessages.append("frames:" + frameCount + "\n");
		
		availableMinerals = self.minerals()-reservedMinerals;
		availableGas = self.gas() - reservedGas;
		
		statusMessages.append("Available minerals:" + availableMinerals + "\n");
		statusMessages.append("Available gas:" + availableGas + "\n");
				
		for (PlannedItem pi : plannedItems) {
			if (!pi.isInProgress()) {
				if (availableMinerals >= pi.getUnitType().mineralPrice() && availableGas >= pi.getUnitType().gasPrice()
						&& supplyUsedActual >= pi.getSupply()) {
					reservedMinerals += pi.getUnitType().mineralPrice();
					reservedGas += pi.getUnitType().gasPrice();
					availableMinerals = self.minerals()-reservedMinerals;
					availableGas = self.gas() - reservedGas;
					for (Unit unit : self.getUnits()) {
						if (unit.getType() == UnitType.Zerg_Drone && !unit.isMorphing()) {
							unit.build(pi.getUnitType(), getBuildTile(unit, pi.getUnitType(), self.getStartLocation()));
							pi.setInProgress(true); // Dude got this
							break; // This is important, one dude is enough!
						}
					}
				} else {
					break;
				}
			}
		}
		
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
			//	unit.morph(UnitType.Zerg_Drone);
			}
			game.drawTextMap(unit.getPosition(), Integer.toString(unit.getID()) + "," + unit.getType());
		}
		 game.drawTextScreen(10, 25, statusMessages.toString());        
		
	}

    public static void main(String[] args) {
        new Main().run();
    }
    
	public void countAllUnits() {
		unitCounts = new HashMap<UnitType, Integer>();
		unitsInProduction = new HashMap<UnitType, Integer>();
		supplyUsedActual = 0;
		for (Unit myUnit : self.getUnits()) {
			supplyUsedActual += myUnit.getType().supplyRequired();
			unitCounts.put(myUnit.getType(), unitCounts.getOrDefault(myUnit.getType(), 1));
			if (myUnit.getType() == UnitType.Zerg_Egg) {
				supplyUsedActual += myUnit.getBuildType().supplyRequired();
				unitsInProduction.put(myUnit.getType(), unitsInProduction.getOrDefault(myUnit.getType(), 1));
			}
		}
	}
	
	// Returns a suitable TilePosition to build a given building type near
	// specified TilePosition aroundTile, or null if not found. (builder parameter is our worker)
	public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
		TilePosition ret = null;
		int maxDist = 3;
		int stopDist = 40;

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
					if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : game.getAllUnits()) {
							if (u.getID() == builder.getID()) continue;
							if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
						}
						if (!unitsInWay) {
							return new TilePosition(i, j);
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
			maxDist += 2;
		}

		if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
		return ret;
	}
     

}
