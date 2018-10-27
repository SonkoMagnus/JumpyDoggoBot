package jumpydoggo.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import jumpydoggo.main.PlannedItem.PlannedItemType;

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
    enum WorkerRole { MINERAL, GAS, BUILD, FIGHT, SCOUT}    
    HashMap<WorkerRole, ArrayList<Integer>> workerIdsByRole = new HashMap<WorkerRole, ArrayList<Integer>>();
    HashMap<Integer, Unit> unitsById = new HashMap<Integer, Unit>();
    
    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame(false);
    }
    
	@Override
	public void onStart() {
		game = mirror.getGame();
		self = game.self();
		game.setLocalSpeed(30);
		game.enableFlag(1);

		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1, PlannedItemStatus.PLANNED));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1, PlannedItemStatus.PLANNED));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1, PlannedItemStatus.PLANNED));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1, PlannedItemStatus.PLANNED));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1, PlannedItemStatus.PLANNED));
		for (WorkerRole wr : WorkerRole.values()) {
			workerIdsByRole.put(wr, new ArrayList<Integer>());
		}
		
	}
	
	@Override
	public void onUnitRenegade(Unit unit) {
		if (unit.getPlayer() == self) {
			if (unit.getType() == UnitType.Zerg_Extractor) {
				for (PlannedItem pi : plannedItems) {
					if (pi.getUnitType() == UnitType.Zerg_Extractor
							&& unit.getTilePosition().equals(pi.getPlannedPosition())) {
						reservedMinerals -= pi.getUnitType().mineralPrice();
						pi.setStatus(PlannedItemStatus.MORPHING);
						pi.setUnitId(unit.getID());
					}
				}
			}
		}
	}
	
	
	
	@Override
	public void onUnitMorph(Unit unit) {
		System.out.println(unit.getType() + " with id: " + unit.getID() + " morphed");
		if (unit.getPlayer() == self) {
			System.out.println(unit.getType() + ": DEBÜG BÜILD TYPE:" + unit.getBuildType());
		for (PlannedItem pi : plannedItems) {
			if (pi.plannedItemType == PlannedItemType.BUILDING) {
			if (pi.getUnitType() == unit.getType()  && pi.getStatus() == PlannedItemStatus.WORKER_ASSIGNED) {
				reservedMinerals -= pi.getUnitType().mineralPrice();
				reservedGas -= pi.getUnitType().gasPrice();
				pi.setStatus(PlannedItemStatus.MORPHING);
				pi.setUnitId(unit.getID());
			}
			} else if (pi.plannedItemType == PlannedItemType.UNIT && pi.getUnitId() != null 
					&& unit.getID() == pi.getUnitId() && unit.getBuildType() == pi.getUnitType()) {
				reservedMinerals -= pi.getUnitType().mineralPrice();
				reservedGas -= pi.getUnitType().gasPrice();
				pi.setStatus(PlannedItemStatus.MORPHING);
				pi.setUnitId(unit.getID());
			}
		}
		
		for (ArrayList<Integer> list : workerIdsByRole.values()) {
			if (list.contains(unit.getID())) {
				list.remove(list.indexOf(unit.getID()));
				break;
			}
		}
		}
	}
	
	
	
	@Override
	public void onUnitDestroy(Unit unit) {
		System.out.println(unit.getType() + " with id: " + unit.getID() + " destroyed");
		if (unit.getPlayer() == self) {
			unitsById.remove(unit.getID());		
			for (PlannedItem pi : plannedItems) {
				if (pi.getBuilderId() != null && pi.getBuilderId() == unit.getID()) {
					pi.setStatus(PlannedItemStatus.PLANNED);
				}
			}
			if (unit.getType() == UnitType.Zerg_Drone) {
				for (ArrayList<Integer> list : workerIdsByRole.values()) {
					if (list.contains(unit.getID())) {
						list.remove(list.indexOf(unit.getID()));						
						break;
					}
				}
			}
			
		}
	}
	
	@Override
	public void onUnitComplete(Unit unit) {
		if (unit.getPlayer() == self) {
			for (PlannedItem pi : plannedItems) {
				if (pi.getStatus() == PlannedItemStatus.MORPHING) {

					if (pi.plannedItemType == PlannedItemType.BUILDING) {
						if (pi.getBuilderId() != null && pi.getBuilderId() == unit.getID()) {
							pi.setStatus(PlannedItemStatus.DONE);
						}
						// For Extractors
						if (pi.getUnitType() == UnitType.Zerg_Extractor
								&& unit.getTilePosition().equals(pi.getPlannedPosition())) {
							pi.setStatus(PlannedItemStatus.DONE);
						}
					} else if (pi.plannedItemType == PlannedItemType.UNIT) {
						if (unit.getID() == pi.getUnitId()) {
							pi.setStatus(PlannedItemStatus.DONE);
							System.out.println("Setting unit: " + pi.getUnitType() + " to DONE");
						}
					}
				}
			}

			if (unit.getType() == UnitType.Zerg_Drone) {
				workerIdsByRole.get(WorkerRole.MINERAL).add(unit.getID());
			}
		}
	}
	
	@Override
	public void onUnitCreate(Unit unit) {
		
		if (unit.getPlayer() == self) {
			unitsById.put(unit.getID(), unit);
			System.out.println(unit.getType() + " created, addded to unitsById");

		}
	}
	
	@Override
	public void onFrame() {
		frameCount++;
		countAllUnits();
		StringBuilder statusMessages = new StringBuilder();
		statusMessages.append("Supply actual:" + supplyUsedActual + "\n");
		availableMinerals = self.minerals()-reservedMinerals;
		availableGas = self.gas() - reservedGas;
		
		statusMessages.append("Available minerals:" + availableMinerals + "\n");
		statusMessages.append("Available gas:" + availableGas + "\n");
			
		
		Integer lastImportance = Integer.MIN_VALUE;
		Boolean skip = false;
		List<PlannedItem> doneItems = new ArrayList<PlannedItem>();
		
		for (PlannedItem pi : plannedItems) {
			if (pi.getStatus() == PlannedItemStatus.PLANNED) {
				if (!skip) {

					Boolean prereqsOk = true;
					for (PlannedItemPrereq pip : pi.getPrereqList()) {
						if (pip.isMorphing()) {
							if (!(unitsInProduction.getOrDefault(pip.getUnitType(), 0) >= pip.getAmount())) {
								prereqsOk = false;
								break;
							}
						} else if (!(unitCounts.getOrDefault(pip.getUnitType(), 0) >= pip.getAmount())) {
							prereqsOk = false;
							break;
						}
					}

					if (pi.getImportance() >= lastImportance) {
						if (availableMinerals >= pi.getUnitType().mineralPrice()
								&& availableGas >= pi.getUnitType().gasPrice() && supplyUsedActual >= pi.getSupply()
								&& prereqsOk) {
							reservedMinerals += pi.getUnitType().mineralPrice();
							reservedGas += pi.getUnitType().gasPrice();
							availableMinerals = self.minerals() - reservedMinerals;
							availableGas = self.gas() - reservedGas;
							//System.out.println("Prereqs&resources are ok, reserving"); //TODO check larvability
							if (pi.plannedItemType == PlannedItemType.BUILDING) {
								for (Unit unit : self.getUnits()) {
									if (unit.getType() == UnitType.Zerg_Drone && !unit.isMorphing()) {
										TilePosition plannedPosition = getBuildTile(unit, pi.getUnitType(),
												self.getStartLocation());
										unit.build(pi.getUnitType(), plannedPosition);
										pi.setPlannedPosition(plannedPosition);
										pi.setBuilderId(unit.getID());
										pi.setStatus(PlannedItemStatus.WORKER_ASSIGNED); // Dude got this
										System.out.println(pi.getUnitType() + " : worker assigned" + pi.getStatus());
										break; // This is important, one dude is enough!

									}
								}
							} else if (pi.plannedItemType == PlannedItemType.UNIT ){
								for (Unit unit : self.getUnits()) {
								if (unit.getType() == UnitType.Zerg_Larva) {
									pi.setUnitId(unit.getID());
									unit.morph(pi.getUnitType());
									System.out.println("Larva with id:" + unit.getID() + " selected to morph into " + pi.getUnitType());
									break;
								}
								}
							}

						} else {
							lastImportance = pi.getImportance();
						}
					} else {
						skip = true;
					}
				}

			} else if (pi.getStatus() == PlannedItemStatus.CANCEL) {
				unitsById.get(pi.getUnitId()).cancelMorph();
				pi.setStatus(PlannedItemStatus.DONE);
							
			} else  if (pi.getStatus() == PlannedItemStatus.DONE) {
			 	doneItems.add(pi);
		 }
		}
		
		plannedItems.removeAll(doneItems);
		
		
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
			if (myUnit.isMorphing()) {
				if (myUnit.getType() == UnitType.Zerg_Egg) {
					supplyUsedActual += myUnit.getBuildType().supplyRequired();
					unitsInProduction.put(myUnit.getBuildType(), unitsInProduction.getOrDefault(myUnit.getBuildType(), 0)+1);
				} else {
				unitsInProduction.put(myUnit.getType(), unitsInProduction.getOrDefault(myUnit.getType(), 0)+1);
				}
				
			} else {
			supplyUsedActual += myUnit.getType().supplyRequired();
			unitCounts.put(myUnit.getType(), unitCounts.getOrDefault(myUnit.getType(), 0)+1);
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
						( Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist ) &&
    					n.isVisible()
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
