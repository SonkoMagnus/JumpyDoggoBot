package jumpydoggo.main;

import bwem.BWEM;
import bwem.ChokePoint;
import bwem.area.Area;
import bwem.area.typedef.AreaId;
import bwem.typedef.CPPath;
import jumpydoggo.main.PlannedItem.PlannedItemType;
import jumpydoggo.main.manager.UnitManager;
import jumpydoggo.main.manager.ZerglingManager;
import jumpydoggo.main.map.Cartography;
import jumpydoggo.main.map.EnemyBuildingInfo;
import jumpydoggo.main.map.EnemyUnitInfo;
import jumpydoggo.main.map.MapFileWriter;
import jumpydoggo.main.map.ThreatPosition;
import jumpydoggo.main.map.TileInfo;
import jumpydoggo.main.map.TileInfoComparator;
import jumpydoggo.main.strategy.StrategyPlanner;
import org.openbw.bwapi4j.BW;
import org.openbw.bwapi4j.BWEventListener;
import org.openbw.bwapi4j.Player;
import org.openbw.bwapi4j.Position;
import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.UnitStatCalculator;
import org.openbw.bwapi4j.WalkPosition;
import org.openbw.bwapi4j.type.Color;
import org.openbw.bwapi4j.type.UnitType;
import org.openbw.bwapi4j.type.WeaponType;
import org.openbw.bwapi4j.unit.Unit;
import org.openbw.bwapi4j.unit.Weapon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


public class Main implements BWEventListener {

	//Magic number for checking threats
	public static final int THREAT_CHECK_RANGE = 768;

	public static int supplyUsedActual;

	public static int reservedMinerals;
	public static int reservedGas;

	public static int availableMinerals;
	public static int availableGas;

	public static int forcesLeft, forcesTop, forcesRight, forcesBottom;

	//public static UnitStatCalculator unitStatCalculator;
	/**
	 * Frames passed
	 */
	public static int frameCount = 0;

	public static Player self;

	public static HashMap<UnitType, Integer> unitCounts = new HashMap<>();
	public static HashMap<UnitType, Integer> unitsInProduction = new HashMap<>();


	//Threat maps

	public static ThreatPosition[][] threatMemoryMapArray;
	public static ThreatPosition[][] activeThreatMapArray;
	public HashMap<Integer, Set<WalkPosition>> threatenedWPsByIDs = new HashMap<>();
	public HashMap<Position, Integer> threatUnitPositions = new HashMap<>();

	public static PriorityQueue<PlannedItem> plannedItems = new PriorityQueue<PlannedItem>(new PlannedItemComparator()){
		@Override
		public boolean add(PlannedItem e) {
			return super.add(e);
		}
	};


	StrategyPlanner strategyPlanner = new StrategyPlanner();

	enum WorkerRole {
		MINERAL, GAS, BUILD, FIGHT, SCOUT
	}

	HashMap<WorkerRole, HashSet<Integer>> workerIdsByRole = new HashMap<>();
	HashMap<Integer, Unit> unitsById = new HashMap<Integer, Unit>();
	HashMap<UnitType, HashSet<Integer>> unitIdsByType = new HashMap<UnitType, HashSet<Integer>>();
	HashSet<Integer> availableLarvaIds = new HashSet<>();
	HashMap<Integer, UnitManager> unitManagerMap = new HashMap<>();
	HashSet<Unit> enemyUnits = new HashSet<>(); //Currently visible enemy units
	HashMap<Integer, EnemyUnitInfo> enemyUnitMemory = new HashMap<>(); //"Last seen" info

	Random rand = new Random();

	public HashMap<Integer, EnemyBuildingInfo> enemyBuildingMemory = new HashMap<>();

	public static int[][] occupiedGroundArray;
	public static int[][] areaDataArray;

	public HashMap<Integer, int[][]> coordinatesByAreaIds;


	public static BW bw;

	public Unit getWorkerFromRole(WorkerRole role) {
		if (workerIdsByRole.get(role).isEmpty()) {
			return null;
		} else {
			Integer size = workerIdsByRole.get(role).size();
			return unitsById.get(workerIdsByRole.get(role).iterator().next());
		}
	}

	public void changeWorkerRole(Integer id, WorkerRole prev, WorkerRole next) {
		workerIdsByRole.get(prev).remove(id);
		workerIdsByRole.get(next).add(id);
	}

	public void run() {

		this.bw = new BW(this);
		bw.startGame();

	}

	public static void main(String[] args) {
		Main main = new Main();
		main.run();
	}

//	public static Map map = new Map();

	public static ArrayList<TileInfo> scoutHeatMap = new ArrayList<>();
	public static HashSet<TilePosition> scoutTargets = new HashSet<>();
	public static BWEM bwem;

	Area natural; //TODO find this
	public static Set<Integer> areaIds;
	UnitStatCalculator unitStatCalculator;

	@Override
	public void onStart() {
		activeThreatMapArray = new ThreatPosition[bw.getBWMap().mapWidth()*4][bw.getBWMap().mapHeight()*4];
		threatMemoryMapArray = new ThreatPosition[bw.getBWMap().mapWidth()*4][bw.getBWMap().mapHeight()*4];

		occupiedGroundArray = new int[bw.getBWMap().mapWidth()*4][bw.getBWMap().mapHeight()*4];
		for (int x=0; x<occupiedGroundArray.length; x++ ) {
			for (int y=0; y<occupiedGroundArray[x].length; y++ ) {
				occupiedGroundArray[x][y] = -1;
			}
		}

		self = bw.getInteractionHandler().self();
		bw.getInteractionHandler().setLocalSpeed(20);
		bw.getInteractionHandler().enableFlag(1);
		unitStatCalculator = self.getUnitStatCalculator();

		plannedItems.add(new PlannedItem(PlannedItem.PlannedItemType.BUILDING, UnitType.Zerg_Spawning_Pool, 0, 1));
		PlannedItem dr1 = new PlannedItem(PlannedItem.PlannedItemType.UNIT, UnitType.Zerg_Drone, 0, 1);
		PlannedItemPrereq pip1 = new PlannedItemPrereq(UnitType.Zerg_Spawning_Pool, 1, true);
		dr1.getPrereqList().add(pip1);
		plannedItems.add(dr1);

		plannedItems.add(new PlannedItem(PlannedItem.PlannedItemType.UNIT, UnitType.Zerg_Zergling, 8, 1));
		plannedItems.add(new PlannedItem(PlannedItem.PlannedItemType.UNIT, UnitType.Zerg_Zergling, 8, 1));
		plannedItems.add(new PlannedItem(PlannedItem.PlannedItemType.UNIT, UnitType.Zerg_Zergling, 8, 1));
		for (int i=0; i<100;i++) {
			plannedItems.add(new PlannedItem(PlannedItem.PlannedItemType.UNIT, UnitType.Zerg_Zergling, 0, 1));
		}


		bwem = new BWEM(bw); // Instantiate the BWEM object.
		bwem.initialize(); // Initialize and pre-calculate internal data.
		areaIds = bwem.getMap().getAreas().stream().map(Area::getId).collect(Collectors.toSet()).stream().map(AreaId::intValue).collect(Collectors.toSet());

		areaDataArray  = new int[bw.getBWMap().mapWidth()*4][bw.getBWMap().mapHeight()*4];

		for (int x=0; x<areaDataArray.length; x++ ) {
			for (int y = 0; y < areaDataArray[x].length; y++) {
				WalkPosition wp = new WalkPosition(x, y);
				if (bwem.getMap().getArea(wp) != null) {
					areaDataArray[x][y] = bwem.getMap().getArea(wp).getId().intValue();
				} else {
					if (!bw.getBWMap().isWalkable(x, y)) {
						areaDataArray[x][y] = -2;
					} else {
						areaDataArray[x][y] = -1;
					}
				}
			}
		}

		for (int x=0; x<areaDataArray.length; x++ ) {
			for (int y = 0; y < areaDataArray[x].length; y++) {
				if (areaDataArray[x][y] == -1) {
					Cartography.mostCommonNeighbor(x,y);
				}
			}
		}

		for (WorkerRole wr : WorkerRole.values()) {
			workerIdsByRole.put(wr, new HashSet<>());
		}

		//Sectio debugiensis
		MapFileWriter.saveAreaDataArray();




		scoutHeatMap = new  ArrayList<TileInfo>();
		//Build heatmap
		for (int i=0; i< bw.getBWMap().mapWidth(); i++) {
			for (int j = 0; j < bw.getBWMap().mapHeight(); j++) {
				TileInfo ti;
				TilePosition tp = new TilePosition(i,j);
				if (bw.getStartLocations().contains(tp)) {
					ti = new TileInfo(tp, bw.isWalkable(tp.toWalkPosition()), TileInfo.TileType.BASELOCATION);
				} else {
					ti = new TileInfo(tp, bw.isWalkable(tp.toWalkPosition()), TileInfo.TileType.NORMAL);
				}
				scoutHeatMap.add(ti);
			}
		}

		countAllUnits();


	}
	public static boolean isAllSupplyUsed() {
		if (self.supplyTotal() <= supplyUsedActual ) {
			return true;
		}
		return false;
	}

	public void ageHeatMap() {
		int weight = 1;
		for (TileInfo ti : scoutHeatMap) {
			int i = ti.getImportance();
			if (bw.isVisible(ti.getTile())) {
				ti.setImportance(0);
				scoutTargets.remove(ti.getTile());
			} else {
				if (ti.getTileType() == TileInfo.TileType.BASELOCATION) {
					weight = 3;
				} else if (ti.getTileType() == TileInfo.TileType.NATURAL) {
					weight = 2;
				} else if (ti.getTileType() == TileInfo.TileType.NORMAL) {
					weight = 1;
				}
				ti.setImportance(ti.getImportance()+weight);
			}
		}
		Collections.sort(scoutHeatMap, new TileInfoComparator());

	}

	@Override
	public void onEnd(boolean isWinner) {

	}


	public boolean isUnitInThreatCheckRange(Unit unit) {
		if (unit.getLeft()+THREAT_CHECK_RANGE < forcesLeft
				|| unit.getRight()-THREAT_CHECK_RANGE > forcesRight
				|| unit.getTop() + THREAT_CHECK_RANGE < forcesTop
				|| unit.getBottom() - THREAT_CHECK_RANGE > forcesBottom) {
			return false;
		}
		return true;
	}

	TreeSet<WalkPosition> airwps = new TreeSet<>();
	TreeSet<WalkPosition> groundwps = new TreeSet<>();

	public void addToThreatMemoryArray(Unit unit) {
		if (unit.getGroundWeapon() != null && !(unit.getGroundWeapon().type() == WeaponType.None)) {
			int hrange = unitStatCalculator.weaponMaxRange(unit.getType().groundWeapon());
			groundwps = Cartography.getWalkPositionsInRange(unit, hrange);
		}

		if (unit.getAirWeapon() != null && !(unit.getAirWeapon().type() == WeaponType.None)) {
			int hrange = unitStatCalculator.weaponMaxRange(unit.getType().airWeapon());
			airwps = Cartography.getWalkPositionsInRange(unit, hrange);
		}

		for (WalkPosition wp : groundwps) {
			ThreatPosition threatPosition;
			if (threatMemoryMapArray[wp.getX()][wp.getY()] != null) {
				threatPosition = threatMemoryMapArray[wp.getX()][wp.getY()];
			} else {
				threatPosition = new ThreatPosition();
			}
			//ThreatPosition threatPosition = threatMemoryMap.getOrDefault(wp, new ThreatPosition());
			threatPosition.getGroundThreats().putIfAbsent(unit.getID(), unit.getGroundWeapon());
			threatPosition.getThreatTime().put(unit.getID(), frameCount);
			threatMemoryMapArray[wp.getX()][wp.getY()] = threatPosition;
		}

		for (WalkPosition wp : airwps) {
			ThreatPosition threatPosition;
			if (threatMemoryMapArray[wp.getX()][wp.getY()] != null) {
				threatPosition = threatMemoryMapArray[wp.getX()][wp.getY()];
			} else {
				threatPosition = new ThreatPosition();
			}
			threatPosition.getAirThreats().putIfAbsent(unit.getID(), unit.getAirWeapon());
			threatPosition.getThreatTime().putIfAbsent(unit.getID(), frameCount);
			threatMemoryMapArray[wp.getX()][wp.getY()] = threatPosition;
		}

		TreeSet<WalkPosition> allWps = groundwps;
		allWps.addAll(airwps);
		threatenedWPsByIDs.put(unit.getID(), allWps);
	}
/*
	//On unit hide
	public void addToThreatMemory(Unit unit) {
		if (unit.getGroundWeapon() != null && !(unit.getGroundWeapon().type() == WeaponType.None)) {
			int hrange = unitStatCalculator.weaponMaxRange(unit.getType().groundWeapon());
			groundwps = Cartography.getWalkPositionsInRange(unit, hrange);
		}

		if (unit.getAirWeapon() != null && !(unit.getAirWeapon().type() == WeaponType.None)) {
			int hrange = unitStatCalculator.weaponMaxRange(unit.getType().airWeapon());
			airwps = Cartography.getWalkPositionsInRange(unit, hrange);
		}

		for (WalkPosition wp : groundwps) {
			ThreatPosition threatPosition = threatMemoryMap.getOrDefault(wp, new ThreatPosition());
			threatPosition.getGroundThreats().putIfAbsent(unit.getID(), unit.getGroundWeapon());
			threatPosition.getThreatTime().put(unit.getID(), frameCount);
			threatMemoryMap.putIfAbsent(wp, threatPosition);

		}

		for (WalkPosition wp : airwps) {
			ThreatPosition threatPosition = threatMemoryMap.getOrDefault(wp, new ThreatPosition());
			threatPosition.getAirThreats().putIfAbsent(unit.getID(), unit.getAirWeapon());
			threatPosition.getThreatTime().putIfAbsent(unit.getID(), frameCount);
			threatMemoryMap.putIfAbsent(wp, threatPosition);
		}

		TreeSet<WalkPosition> allWps = groundwps;
		allWps.addAll(airwps);
		threatenedWPsByIDs.put(unit.getID(), allWps);
	}
	*/

	//On: show, destroy, morph (if visible..)
	/*
	public void removeFromThreatMemory(Integer unitID) {
		if (threatenedWPsByIDs.containsKey(unitID)) {
			for (WalkPosition wp : threatenedWPsByIDs.get(unitID)) {
				if (threatMemoryMap.containsKey(wp)) {
					ThreatPosition tp = threatMemoryMap.get(wp);
					tp.getGroundThreats().remove(unitID);
					tp.getAirThreats().remove(unitID);
					tp.getThreatTime().remove(unitID);

					if (tp.getGroundThreats().isEmpty() && tp.getAirThreats().isEmpty()) {
						threatMemoryMap.remove(wp);
					}
				}
			}
			threatenedWPsByIDs.remove(unitID);
		}
}
*/
	public void removeFromThreatMemoryArray(Integer unitID) {
		if (threatenedWPsByIDs.containsKey(unitID)) {
			for (WalkPosition wp : threatenedWPsByIDs.get(unitID)) {
				if (threatMemoryMapArray[wp.getX()][wp.getY()] != null) {
					ThreatPosition tp = threatMemoryMapArray[wp.getX()][wp.getY()];
					tp.getGroundThreats().remove(unitID);
					tp.getAirThreats().remove(unitID);
					tp.getThreatTime().remove(unitID);

					if (tp.getGroundThreats().isEmpty() && tp.getAirThreats().isEmpty()) {
						threatMemoryMapArray[wp.getX()][wp.getY()] = null;
					}
				}
			}
			threatenedWPsByIDs.remove(unitID);
		}
	}

/*
	public void maintainActiveThreatMap() {
		activeThreatMap = new HashMap<>();
		for (Unit unit : enemyUnits) {
			if (!isUnitInThreatCheckRange(unit)) {
				if (unit.getGroundWeapon() != null && !(unit.getGroundWeapon().type() == WeaponType.None)) {
					int hrange = unitStatCalculator.weaponMaxRange(unit.getType().groundWeapon());
					groundwps = Cartography.getWalkPositionsInRange(unit, hrange);
				}

				if (unit.getAirWeapon() != null && !(unit.getAirWeapon().type() == WeaponType.None)) {
					int hrange = unitStatCalculator.weaponMaxRange(unit.getType().airWeapon());
					airwps = Cartography.getWalkPositionsInRange(unit, hrange);
				}

				for (WalkPosition wp : groundwps) {
					ThreatPosition threatPosition = activeThreatMap.getOrDefault(wp, new ThreatPosition());
					if (unit.exists()) { //Unit can be killed in the meantime
						threatPosition.getGroundThreats().putIfAbsent(unit.getID(), unit.getGroundWeapon());
					}
					activeThreatMap.putIfAbsent(wp, threatPosition);
				}

				for (WalkPosition wp : airwps) {
					ThreatPosition threatPosition = activeThreatMap.getOrDefault(wp, new ThreatPosition());
					if (unit.exists()) { //Unit can be killed in the meantime
						threatPosition.getAirThreats().putIfAbsent(unit.getID(), unit.getAirWeapon());
					}
					activeThreatMap.putIfAbsent(wp, threatPosition);
				}
			}
		}
	}
*/


	public void maintainActiveThreatMapArray() {
		activeThreatMapArray = new ThreatPosition[bw.getBWMap().mapWidth()*4][bw.getBWMap().mapHeight()*4];
		for (Unit unit : enemyUnits) {
			if (!isUnitInThreatCheckRange(unit)) {
				if (unit.getGroundWeapon() != null && !(unit.getGroundWeapon().type() == WeaponType.None)) {
					int hrange = unitStatCalculator.weaponMaxRange(unit.getType().groundWeapon());
					groundwps = Cartography.getWalkPositionsInRange(unit, hrange);
				}

				if (unit.getAirWeapon() != null && !(unit.getAirWeapon().type() == WeaponType.None)) {
					int hrange = unitStatCalculator.weaponMaxRange(unit.getType().airWeapon());
					airwps = Cartography.getWalkPositionsInRange(unit, hrange);
				}

				for (WalkPosition wp : groundwps) {
					ThreatPosition threatPosition;
					if (activeThreatMapArray[wp.getX()][wp.getY()] != null) {
						threatPosition = activeThreatMapArray[wp.getX()][wp.getY()];
					} else {
						threatPosition = new ThreatPosition();
					}
					if (unit.exists()) { //Unit can be killed in the meantime
						threatPosition.getGroundThreats().putIfAbsent(unit.getID(), unit.getGroundWeapon());
					}
					activeThreatMapArray[wp.getX()][wp.getY()] = threatPosition;
				}

				for (WalkPosition wp : airwps) {
					ThreatPosition threatPosition;
					if (activeThreatMapArray[wp.getX()][wp.getY()] != null) {
						threatPosition = activeThreatMapArray[wp.getX()][wp.getY()];
					} else {
						threatPosition = new ThreatPosition();
					}
					if (unit.exists()) { //Unit can be killed in the meantime
						threatPosition.getAirThreats().putIfAbsent(unit.getID(), unit.getAirWeapon());
					}
					activeThreatMapArray[wp.getX()][wp.getY()] = threatPosition;
				}
			}
		}
	}


	int longbois = 0;
	long totalTime = 0;

	public void checkThreatMemoryPositions() {
		for (Position p :threatUnitPositions.keySet()) {
			if (bw.getBWMap().isVisible(p.getX(), p.getY())) {
				System.out.println("Position visible");
				removeFromThreatMemoryArray(threatUnitPositions.get(p));
			}
		}
	}

	//Benchmark: Should not go below 50 fps.
	public static Set<WalkPosition> debugwps = new HashSet<>();

	@Override
	public void onFrame() {
		Long start = System.currentTimeMillis();
		frameCount++;
		countAllUnits();
		ageHeatMap();
		//maintainActiveThreatMap();
		maintainActiveThreatMapArray();
		//maintainActiveThreatMap();
		checkThreatMemoryPositions();

		for (int i = 0; i<activeThreatMapArray.length; i++) {
			for (int j = 0; j<activeThreatMapArray[0].length; j++) {
				if (activeThreatMapArray[i][j] != null) {
					Cartography.drawWalkPositionGrid(Collections.singletonList(new WalkPosition(i,j)), Color.RED);
				}
			}
		}


		WalkPosition wp1 = new WalkPosition(1,1);
		WalkPosition wp2 = new WalkPosition(450,450);
		CPPath path = bwem.getMap().getPath(wp1.toPosition(), wp2.toPosition());
		int asd =1;


		ChokePoint cp1 = null;
		ChokePoint cp2 = null;
		for (ChokePoint cp : path ) {
			for (WalkPosition wp : cp.getGeometry()) {
				//bw.getMapDrawer().drawTextMap(wp.toPosition().getX(), wp.toPosition().getY(), String.valueOf(asd));
				bw.getMapDrawer().drawTextMap(wp.toPosition().getX(), wp.toPosition().getY(), String.valueOf(areaDataArray[wp.getX()][wp.getY()]));
			}
			if (asd == 1) {
				cp1 = cp;
			}
			if (asd == 2) {
				cp2 = cp;
			}
			asd++;

		}


		for (Unit i : enemyUnits) {
			bw.getMapDrawer().drawTextMap(i.getPosition(), String.valueOf(i.getID()));
		}

		WalkPosition origin = new WalkPosition(5,5);
		WalkPosition dest = new WalkPosition(450,450);

		CPPath path1 = bwem.getMap().getPath(origin.toPosition(), dest.toPosition());
		path1.isEmpty();

		WalkPosition bork = new WalkPosition(120,45);
		Collection<WalkPosition> borks = Cartography.getWalkPositionsInGridRadius(bork, 3);
		borks.addAll(Cartography.getWalkPositionsInGridRadius(bork, 2));
		borks.addAll(Cartography.getWalkPositionsInGridRadius(bork, 1));

	//	borks.addAll(Cartography.getWalkPositionsInGridRadius(dest, 1));
		for (WalkPosition x : borks) {
			ThreatPosition tp = new ThreatPosition();
			tp.getGroundThreats().put(1, new Weapon(WeaponType.Arclite_Cannon, 3));
			threatMemoryMapArray[x.getX()][x.getY()] = tp;
		}
		ThreatPosition tp = new ThreatPosition();
		tp.getGroundThreats().put(1, new Weapon(WeaponType.Arclite_Cannon, 3));
		threatMemoryMapArray[dest.getX()][dest.getY()] = tp;


		borks.add(bork);

		Cartography.drawWalkPositionGrid(borks, Color.RED);

		threatMemoryMapArray[origin.getX()][origin.getY()] = null;
		//threatMemoryMapArray[dest.getX()][dest.getY()] = null;

		debugwps.add(origin);
		debugwps.add(dest);


		ArrayList<WalkPosition> patherino = new ArrayList<>();

		WalkPosition herp = new WalkPosition(51,72);

		Area area = Main.bwem.getMap().getArea(herp);

		CPPath bwempath = bwem.getMap().getPath(origin.toPosition(), dest.toPosition());
		ChokePoint coppo = bwempath.get(bwempath.size() - 1);
		for (WalkPosition wp : coppo.getGeometry()) {
			ThreatPosition tpx = new ThreatPosition();
			tpx.getGroundThreats().put(1, new Weapon(WeaponType.Arclite_Cannon, 3));
			threatMemoryMapArray[wp.getX()][wp.getY()] = tpx;
		}


		for (int i =0; i<10; i++) {
			patherino = Cartography.findAnyPathMultiAreaContinuous(origin, dest, true, true, true);

		}

		Cartography.drawWalkPositionGrid(patherino, Color.GREEN);
		Cartography.drawWalkPositionGrid(Collections.singletonList(origin), Color.CYAN);
		bw.getMapDrawer().drawTextMap(origin.toPosition(), String.valueOf(areaDataArray[origin.getX()][origin.getY()]));



		Cartography.drawWalkPositionGrid(Collections.singletonList(dest), Color.CYAN);
		strategyPlanner.execute();


//---------------------------------


		StringBuilder statusMessages = new StringBuilder();
		statusMessages.append("Supply actual:" + supplyUsedActual + "\n");
		availableMinerals = self.minerals() - reservedMinerals;
		availableGas = self.gas() - reservedGas;

		statusMessages.append("Available minerals:" + availableMinerals + "\n");
		statusMessages.append("Available gas:" + availableGas + "\n");
		statusMessages.append("FPS:" + bw.getFPS() + "\n");
		//long avg = totalTime / frameCount;
		//statusMessages.append("Avg frame time:" + avg + "\n");
		//statusMessages.append("Enemyunits:" + enemyUnits.size() + "\n");
		//statusMessages.append("left:" + forcesLeft + " right:" + forcesRight + " top: " +forcesTop + " bottom:" + forcesBottom + "\n");

		Integer lastImportance = Integer.MIN_VALUE;
		Boolean skip = false;
		List<PlannedItem> doneItems = new ArrayList<PlannedItem>();

		for (UnitManager um : unitManagerMap.values()) {
			um.execute();
		}

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

					if (pi.getReserveOnFullSupply() == false && isAllSupplyUsed()) {
						prereqsOk = false;
					}

					if (pi.getImportance() >= lastImportance) {
						if (availableMinerals >= pi.getUnitType().mineralPrice()
								&& availableGas >= pi.getUnitType().gasPrice() && supplyUsedActual >= pi.getSupply()
								&& prereqsOk
								&& hasRequirements(pi.getUnitType())
						) {
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
						changeWorkerRole(builder.getID(), WorkerRole.MINERAL, WorkerRole.BUILD);
						pi.setStatus(PlannedItemStatus.CREATOR_ASSIGNED);
					}
					break;
				} else if (pi.plannedItemType == PlannedItemType.UNIT) {

					if (!availableLarvaIds.isEmpty())
					{
						Integer larvaId = availableLarvaIds.iterator().next();
						Unit larva = unitsById.get(larvaId);
						pi.setUnitId(larvaId);
						pi.setStatus(PlannedItemStatus.CREATOR_ASSIGNED);
						larva.morph(pi.getUnitType());
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

		Long end = System.currentTimeMillis();
		totalTime = totalTime + (end -start);
		if (end-start > 85) {
			System.out.println("FRAME LONGER THAN 85 MS, count:" + ++longbois + " enemyunits:" + enemyUnits.size());
		}

		//TESTERINO SECTION
		updateOccupiedGroundArray();



	}


	public void updateOccupiedGroundArray() {
		for (Unit unit : bw.getAllUnits()) {
			if (!unit.isFlying()) {
				for (WalkPosition wp : Cartography.getOccupiedWalkPositionsOfUnit(unit)) {
					occupiedGroundArray[wp.getX()][wp.getY()] = frameCount;
				}
			}
		}
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
	public void onUnitShow(Unit unit) { //TODO refresh type of enemybuilding (morph)
		if (unit.getPlayer() == self) {
		} else {
			if (!unit.getType().isNeutral()) {
				//Enemy
				removeFromThreatMemoryArray(unit.getID());
				if (unit.getType().isBuilding()) {
					if (enemyBuildingMemory.containsKey(unit.getID())) {
						if (!enemyBuildingMemory.get(unit.getID()).equals(unit.getTilePosition())) {
							enemyBuildingMemory.get(unit.getID()).setTilePosition(unit.getTilePosition());
						}
					} else {
						enemyBuildingMemory.put((Integer) unit.getID(), new EnemyBuildingInfo(unit.getType(), unit.getTilePosition()));
					}
				} else {
					enemyUnits.add(unit);
				}
			}
		}

	}
	//public EnemyUnitInfo(UnitType type, Weapon airWeapon, Weapon groundWeapon, int hp, int shields, int energy, Position lastPosition, int frameLastSeen) {

	@Override
	public void onUnitHide(Unit unit) {
		if (unit.getPlayer() != self && !unit.getType().isNeutral()) {
			System.out.println("Unit hidden:" + unit.getID() + "type:" + unit.getType());
			addToThreatMemoryArray(unit);
			enemyUnits.remove(unit);
			if (!unit.getType().isBuilding()) {
				if (enemyUnitMemory.containsKey(unit.getID())) {
					enemyUnitMemory.get(unit.getID()).update(unit.getType(), unit.getAirWeapon(),
							unit.getGroundWeapon(), unit.getHitPoints(),
							unit.getShields(), unit.getEnergy(), unit.getPosition(), frameCount);
				}
				enemyUnitMemory.put(unit.getID(),
						new EnemyUnitInfo(unit.getType(), unit.getAirWeapon(),
								unit.getGroundWeapon(), unit.getHitPoints(),
								unit.getShields(), unit.getEnergy(), unit.getPosition(), frameCount));
				threatUnitPositions.put(unit.getPosition(), unit.getID());
			}
		}
	}

	@Override
	public void onUnitCreate(Unit unit) {
		if (unit.getPlayer() == self) {
			unitIdsByType.putIfAbsent(unit.getType(), new HashSet<Integer>());
			unitIdsByType.get(unit.getType()).add(unit.getID());
			unitsById.put(unit.getID(), unit);

			if (unit.getType() == UnitType.Zerg_Larva) {
				availableLarvaIds.add(unit.getID());

			}
			if (unit.getType().equals(UnitType.Zerg_Zergling)) {
				ZerglingManager zm = new ZerglingManager(unit);
				zm.setRole(ZerglingManager.Role.SCOUT);
				unitManagerMap.put(unit.getID(), zm);
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
				for (HashSet<Integer> list : workerIdsByRole.values()) {
					if (list.contains(unit.getID())) {
						list.remove(unit.getID());
						break;
					}
				}
			}
			if (unitManagerMap.keySet().contains(unit.getID())) {
				unitManagerMap.get(unit.getID()).dieded();
			}

			unitManagerMap.remove(unit.getID());

		} else {
			if (!unit.getType().isNeutral()) {
				removeFromThreatMemoryArray(unit.getID());
				if (unit.getType().isBuilding()) {
					enemyBuildingMemory.remove(unit.getID());
				} else {
					enemyUnitMemory.remove(unit.getID());
					enemyUnits.remove(unit.getID());
				}
			}
		}
	}

	@Override
	public void onUnitMorph(Unit unit) {
		if (unit.getPlayer() == self) {
			UnitType precursor = unit.getType().whatBuilds().getUnitType();
			if (precursor.equals(UnitType.Zerg_Larva)) {
				availableLarvaIds.remove(unit.getID());
			}
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
					if (pi.getUnitType() == unit.getType() && pi.getStatus() == PlannedItemStatus.CREATOR_ASSIGNED) {
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

			for (HashSet<Integer> list : workerIdsByRole.values()) {
				if (list.contains(unit.getID())) {
					list.remove(unit.getID());
					break;
				}
			}
			//Assign managers
			if (unit.getType() == UnitType.Zerg_Zergling) {
				ZerglingManager zm = new ZerglingManager(unit);
				zm.setRole(ZerglingManager.Role.SCOUT);
				unitManagerMap.put(unit.getID(), zm);
			}




		}  else if (!unit.getType().isNeutral()) {
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
		forcesLeft = Integer.MAX_VALUE;
		forcesTop = Integer.MAX_VALUE;
		forcesRight = -1;
		forcesBottom = -1;

		for (Unit unit : bw.getUnits(self)) {
			if (unit.isMorphing()) {
				if (unit.getType() == UnitType.Zerg_Egg) {
					supplyUsedActual += unit.getBuildType().supplyRequired();
					unitsInProduction.put(unit.getBuildType(), unitsInProduction.getOrDefault(unit.getBuildType(), 0) + 1);
				} else {
					unitsInProduction.put(unit.getType(), unitsInProduction.getOrDefault(unit.getType(), 0) + 1);
				}

			} else {
				supplyUsedActual += unit.getType().supplyRequired();
				unitCounts.put(unit.getType(), unitCounts.getOrDefault(unit.getType(), 0) + 1);
			}
			if (unit.getLeft() < forcesLeft) {
				forcesLeft = unit.getLeft();
			}
			if (unit.getRight() > forcesRight) {
				forcesRight = unit.getRight();
			}
			if (unit.getTop() < forcesTop) {
				forcesTop = unit.getTop();
			}
			if (unit.getBottom() > forcesBottom) {
				forcesBottom = unit.getBottom();
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

	public boolean hasRequirements(UnitType unitType) {
		for (UnitType req : unitType.requiredUnits().keySet()) {
			if (unitCounts.getOrDefault(req, 0) < unitType.requiredUnits().get(req)) {
				return false;
			}
		}
		return true;
	}

}


