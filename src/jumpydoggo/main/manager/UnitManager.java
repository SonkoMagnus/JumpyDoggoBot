package jumpydoggo.main.manager;

import jumpydoggo.main.Main;
import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.unit.Unit;

public class UnitManager {

    protected TilePosition scoutingTarget;
    protected Unit unit;

    public UnitManager(Unit unit) {
        this.unit=unit;
    }

    public void execute() {

    }

    public void dieded() {
        if (scoutingTarget != null) {
            Main.scoutTargets.remove(scoutingTarget);
        }
    }
}
