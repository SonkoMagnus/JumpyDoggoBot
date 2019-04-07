package jumpydoggo.main.manager;

import jumpydoggo.main.Main;
import jumpydoggo.main.map.TileInfo;
import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.WalkPosition;
import org.openbw.bwapi4j.unit.Unit;

public class UnitManager {

    protected TilePosition scoutingTarget;
    protected Unit unit;

    public UnitManager(Unit unit) {
        this.unit=unit;
    }

    public void execute() {

    }

    public void moveExcludeWalkPositions() {
        //unit.move() //shift queue
    }

    public void scout(boolean avoidthreats) {
        if (scoutingTarget != null && Main.bw.isVisible(scoutingTarget)) {
            scoutingTarget = null;
        }
        while (scoutingTarget == null) {
            for (TileInfo ti : Main.scoutHeatMap) {
                boolean avoidTile = false;
                if (avoidthreats) {
                    //THis just ensures that the unit don't pick it as target.
                    WalkPosition scTile = ti.getTile().toWalkPosition();
                    if (Main.threatMemoryMap.containsKey(ti.getTile().toWalkPosition()) || Main.activeThreatMapArray[scTile.getX()][scTile.getY()] != null) {
                        avoidTile = true;
                    }
                }

                if (!Main.scoutTargets.contains(ti.getTile())
                        && (unit.getType().isFlyer() || ti.isWalkable()) //Air units don't care about walkability
                        && !Main.bwem.getMap().getPath(unit.getPosition(), ti.getTile().toPosition()).isEmpty()
                        && !avoidTile
                        ) {
                    scoutingTarget = ti.getTile();
                    Main.scoutTargets.add(ti.getTile());
                    break;
                }
            }
        }
        //The actual scouting part
        unit.move(scoutingTarget.toPosition());
    }

    public void dieded() {
        if (scoutingTarget != null) {
            Main.scoutTargets.remove(scoutingTarget);
        }
    }


}
