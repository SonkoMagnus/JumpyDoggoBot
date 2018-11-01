package jumpydoggo.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

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
    HashMap<UnitType, HashSet<Integer>> unitIdsByType  = new HashMap<UnitType, HashSet<Integer>>();
    
    Random rand = new Random();
    
    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame(false);
    }
    
    public Unit getWorkerFromRole(WorkerRole role) { 
    	System.out.println(workerIdsByRole);
    	if (workerIdsByRole.get(role).isEmpty()) {
    		return null;
    	} else {
    		System.out.println("blep");
    		Integer size = workerIdsByRole.get(role).size();
    		return unitsById.get(workerIdsByRole.get(role).get(rand.nextInt(size)));
    	}
    }
    
    
	@Override
	public void onStart() {
		game = mirror.getGame();
		self = game.self();
		game.setLocalSpeed(30);
		game.enableFlag(1);

		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1));
		PlannedItemPrereq pip1 = new PlannedItemPrereq(UnitType.Zerg_Drone, 9, false);
		PlannedItemPrereq cancelReq = new PlannedItemPrereq(UnitType.Zerg_Drone, 1, true);
		PlannedItem trickExt = new PlannedItem(PlannedItemType.BUILDING, UnitType.Zerg_Extractor, 16, 1 );
		
		PlannedItem overDrone = new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 16, 1);
		PlannedItemPrereq pipEx = new PlannedItemPrereq(UnitType.Zerg_Extractor, 1, true);
		overDrone.getPrereqList().add(pipEx);
		
		trickExt.getPrereqList().add(pip1);
		trickExt.setDoCancel(true);
		trickExt.getCancelPrereqList().add(cancelReq);
		plannedItems.add(trickExt);
		plannedItems.add(overDrone);
		
		for (WorkerRole wr : WorkerRole.values()) {
			workerIdsByRole.put(wr, new ArrayList<Integer>());
		}
		
	}
	
	@Override
	public void onUnitRenegade(Unit unit) {
		if (unit.getPlayer() == self) {
			unitsById.put(unit.getID(), unit);
			unitIdsByType.getOrDefault(unit.getType(), new HashSet<Integer>()).add(unit.getID());
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
		if (unit.getPlayer() == self) {
			UnitType precursor = unit.getType().whatBuilds().first;
			if (unit.getType() == UnitType.Zerg_Lurker) {
				unitIdsByType.get(UnitType.Zerg_Lurker_Egg).remove(unit.getID());
			} else if (unit.getType() == UnitType.Zerg_Guardian || unit.getType() == UnitType.Zerg_Devourer) {
				unitIdsByType.get(UnitType.Zerg_Cocoon).remove(unit.getID());
			} else {
				if (unitIdsByType.containsKey(UnitType.Zerg_Egg)) {
					unitIdsByType.get(UnitType.Zerg_Egg).remove(unit.getID());
				}
			}
		
			if (unitIdsByType.containsKey(precursor)) {
				unitIdsByType.get(precursor).remove(unit.getID());
			}
			
			unitIdsByType.putIfAbsent(unit.getType(), new HashSet<Integer>());
			unitIdsByType.get(unit.getType()).add(unit.getID());
		for (PlannedItem pi : plannedItems) {
			if (pi.plannedItemType == PlannedItemType.BUILDING) {
			if (pi.getUnitType() == unit.getType()  && pi.getStatus() == PlannedItemStatus.BUILDER_ASSIGNED) {
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
				System.out.println("Setting unit: " + pi.getUnitType() + " ID:" + unit.getID() +" to MORPHING");
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
		if (unit.getPlayer() == self) {
			unitIdsByType.get(unit.getType()).remove(unit.getID());
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
			//System.out.println("Type:" + unit.getType() + " WB:" + unit.getType().whatBuilds());
			//System.out.println(unit.getType() + " completed");
			unitIdsByType.putIfAbsent(unit.getType(), new HashSet<Integer>());
			unitIdsByType.get(unit.getType()).add(unit.getID());
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
			unitIdsByType.putIfAbsent(unit.getType(), new HashSet<Integer>());
			unitIdsByType.get(unit.getType()).add(unit.getID());
			unitsById.put(unit.getID(), unit);
		}
	}

	@Override
	public void onFrame() {
		// System.out.println(unitIdsByType);
		frameCount++;
		countAllUnits();
		StringBuilder statusMessages = new StringBuilder();
		statusMessages.append("Supply actual:" + supplyUsedActual + "\n");
		availableMinerals = self.minerals() - reservedMinerals;
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
							reserveResources(pi.getUnitType());
							pi.setStatus(PlannedItemStatus.RESOURCES_RESERVED);

						} else {
							lastImportance = pi.getImportance();
						}
					} else {
						skip = true;
					}
				}
			} else if (pi.getStatus() == PlannedItemStatus.RESOURCES_RESERVED) {
				if (pi.plannedItemType == PlannedItemType.BUILDING) {
					Unit builder = getWorkerFromRole(WorkerRole.MINERAL);
					if (builder != null) {
						TilePosition plannedPosition = getBuildTile(builder, pi.getUnitType(), self.getStartLocation());
						builder.build(pi.getUnitType(), plannedPosition);
						pi.setPlannedPosition(plannedPosition);
						pi.setBuilderId(builder.getID());
						pi.setStatus(PlannedItemStatus.BUILDER_ASSIGNED);
					}
					break;
				} else if (pi.plannedItemType == PlannedItemType.UNIT) {
					if (unitIdsByType.get(UnitType.Zerg_Larva) != null
							&& !unitIdsByType.get(UnitType.Zerg_Larva).isEmpty()) {
						Integer larvaId = unitIdsByType.get(UnitType.Zerg_Larva).iterator().next();
						Unit larva = unitsById.get(larvaId);
						pi.setUnitId(larvaId);
						pi.setStatus(PlannedItemStatus.BUILDER_ASSIGNED);
						System.out.println("larva: " + larvaId + " assigned to morph into a beautiful "+ pi.getUnitType());
						larva.morph(pi.getUnitType());
						break;
					}
				}
			}

			else if (pi.getStatus() == PlannedItemStatus.MORPHING) {
				if (pi.getDoCancel()) {
					Boolean cancelPrereqsOk = true;
					for (PlannedItemPrereq pip : pi.getCancelPrereqList()) {
						if (pip.isMorphing()) {
							if (!(unitsInProduction.getOrDefault(pip.getUnitType(), 0) >= pip.getAmount())) {
								cancelPrereqsOk = false;
								break;
							}
						} else if (!(unitCounts.getOrDefault(pip.getUnitType(), 0) >= pip.getAmount())) {
							cancelPrereqsOk = false;
							break;
						}
					}
					if (cancelPrereqsOk) {
						pi.setStatus(PlannedItemStatus.CANCEL);
					}
				}

			} else if (pi.getStatus() == PlannedItemStatus.CANCEL) {
				unitsById.get(pi.getUnitId()).cancelMorph();
				pi.setStatus(PlannedItemStatus.DONE);

			} else if (pi.getStatus() == PlannedItemStatus.DONE) {
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

		}
		game.drawTextScreen(10, 25, statusMessages.toString());
	}

	public void reserveResources(UnitType unitType) {
		reservedMinerals += unitType.mineralPrice();
		reservedGas += unitType.gasPrice();
		availableMinerals = self.minerals() - reservedMinerals;
		availableGas = self.gas() - reservedGas;
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
