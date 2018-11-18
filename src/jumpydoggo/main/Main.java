package jumpydoggo.main;

import org.openbw.bwapi4j.*;
import org.openbw.bwapi4j.type.UnitType;
import org.openbw.bwapi4j.unit.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Random;

import jumpydoggo.main.PlannedItem.PlannedItemType;



public class Main implements BWEventListener {

	public int supplyUsedActual;

	public static int reservedMinerals;
	public static int reservedGas;

	public static int availableMinerals;
	public static int availableGas;
	/**
	 * Frames passed
	 */
	public static int frameCount = 0;

	Player self;

	public HashMap<UnitType, Integer> unitCounts = new HashMap<>();
	public HashMap<UnitType, Integer> unitsInProduction = new HashMap<>();

	PriorityQueue<PlannedItem> plannedItems = new PriorityQueue<>(new PlannedItemComparator());

	enum WorkerRole {
		MINERAL, GAS, BUILD, FIGHT, SCOUT
	}

	HashMap<WorkerRole, ArrayList<Integer>> workerIdsByRole = new HashMap<WorkerRole, ArrayList<Integer>>();
	HashMap<Integer, Unit> unitsById = new HashMap<Integer, Unit>();
	HashMap<UnitType, HashSet<Integer>> unitIdsByType = new HashMap<UnitType, HashSet<Integer>>();

	Random rand = new Random();

	private BW bw;

	public Unit getWorkerFromRole(WorkerRole role) {
		if (workerIdsByRole.get(role).isEmpty()) {
			return null;
		} else {
			Integer size = workerIdsByRole.get(role).size();
			return unitsById.get(workerIdsByRole.get(role).get(rand.nextInt(size)));
		}
	}

	public void run() {
		this.bw = new BW(this);
		this.bw.startGame();

	}

	public static void main(String[] args) {
		Main main = new Main();
		main.run();
	}



	@Override
	public void onStart() {
		self = bw.getInteractionHandler().self();
		bw.getInteractionHandler().setLocalSpeed(30);
		bw.getInteractionHandler().enableUserInput();

		plannedItems.add(new PlannedItem(PlannedItemType.BUILDING, UnitType.Zerg_Spawning_Pool, 0, 1));
		PlannedItem dr1 = new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1);
		PlannedItemPrereq pip1 = new PlannedItemPrereq(UnitType.Zerg_Spawning_Pool, 1, true);
		dr1.getPrereqList().add(pip1);
		plannedItems.add(dr1);

		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Zergling, 8, 1));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Zergling, 10, 1));
		plannedItems.add(new PlannedItem(PlannedItemType.UNIT, UnitType.Zerg_Zergling, 12, 1));
		for (WorkerRole wr : WorkerRole.values()) {
			workerIdsByRole.put(wr, new ArrayList<Integer>());
		}

	}

	@Override
	public void onEnd(boolean isWinner) {

	}

	@Override
	public void onFrame() {
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
								&& prereqsOk && self.hasUnitTypeRequirement(pi.getUnitType())) {
							// Unit requirement logic doesn't actually work
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

					for (Unit neutralUnit : bw.getMinerals()) {
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


		bw.getMapDrawer().drawTextScreen(10, 25, statusMessages.toString());

	}

	@Override
	public void onSendText(String text) {

	}

	@Override
	public void onReceiveText(Player player, String text) {

	}

	@Override
	public void onPlayerLeft(Player player) {

	}

	@Override
	public void onNukeDetect(Position target) {

	}

	@Override
	public void onUnitDiscover(Unit unit) {

	}

	@Override
	public void onUnitEvade(Unit unit) {

	}

	@Override
	public void onUnitShow(Unit unit) {

	}

	@Override
	public void onUnitHide(Unit unit) {

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
	public void onUnitMorph(Unit unit) {
		if (unit.getPlayer() == self) {
			UnitType precursor = unit.getType().whatBuilds().getUnitType();
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
					if (pi.getUnitType() == unit.getType() && pi.getStatus() == PlannedItemStatus.BUILDER_ASSIGNED) {
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
	public void onSaveGame(String gameName) {

	}

	@Override
	public void onUnitComplete(Unit unit) {
		if (unit.getPlayer() == self) {
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
						}
					}
				}
			}
			if (unit.getType() == UnitType.Zerg_Drone) {
				workerIdsByRole.get(WorkerRole.MINERAL).add(unit.getID());
			}
		}
	}

	public void countAllUnits() {
		unitCounts = new HashMap<UnitType, Integer>();
		unitsInProduction = new HashMap<UnitType, Integer>();
		supplyUsedActual = 0;
		for (Unit unit : bw.getAllUnits()) {
			//System.out.println("unit" + unit);
		}
		System.out.println(bw.getUnits(self).size());
		System.out.println(self);

		for (Unit myUnit : bw.getUnits(self)) {
			if (myUnit.isMorphing()) {
				if (myUnit.getType() == UnitType.Zerg_Egg) {
					supplyUsedActual += myUnit.getBuildType().supplyRequired();
					unitsInProduction.put(myUnit.getBuildType(), unitsInProduction.getOrDefault(myUnit.getBuildType(), 0) + 1);
				} else {
					unitsInProduction.put(myUnit.getType(), unitsInProduction.getOrDefault(myUnit.getType(), 0) + 1);
				}

			} else {
				supplyUsedActual += myUnit.getType().supplyRequired();
				unitCounts.put(myUnit.getType(), unitCounts.getOrDefault(myUnit.getType(), 0) + 1);
			}
		}
	}

	public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
		TilePosition ret = null;
		int maxDist = 3;
		int stopDist = 40;

		// Refinery, Assimilator, Extractor
		if (buildingType.isRefinery()) {

			for (Unit n : bw.neutral().getUnits()) {
				if ((n.getType() == UnitType.Resource_Vespene_Geyser)
						&& (Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist)
						&& (Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist) && n.isVisible())
					return n.getTilePosition();
			}
		}

		while ((maxDist < stopDist) && (ret == null)) {
			for (int i = aroundTile.getX() - maxDist; i <= aroundTile.getX() + maxDist; i++) {
				for (int j = aroundTile.getY() - maxDist; j <= aroundTile.getY() + maxDist; j++) {
					if (bw.canBuildHere(new TilePosition(i, j), buildingType, builder, false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : bw.getAllUnits()) {
							if (u.getID() == builder.getID())
								continue;
							if ((Math.abs(u.getTilePosition().getX() - i) < 4)
									&& (Math.abs(u.getTilePosition().getY() - j) < 4))
								unitsInWay = true;
						}
						if (!unitsInWay) {
							return new TilePosition(i, j);
						}
						// creep for Zerg
						if (buildingType.requiresCreep()) {
							boolean creepMissing = false;
							for (int k = i; k <= i + buildingType.tileWidth(); k++) {
								for (int l = j; l <= j + buildingType.tileHeight(); l++) {
									if (!bw.hasCreep(k, l))
										creepMissing = true;
									break;
								}
							}
							if (creepMissing)
								continue;
						}
					}
				}
			}
			maxDist += 2;
		}

		if (ret == null)
			bw.printf("Unable to find suitable build position for " + buildingType.toString());
		return ret;
	}

	public void reserveResources(UnitType unitType) {
		reservedMinerals += unitType.mineralPrice();
		reservedGas += unitType.gasPrice();
		availableMinerals = self.minerals() - reservedMinerals;
		availableGas = self.gas() - reservedGas;
	}

}
	

