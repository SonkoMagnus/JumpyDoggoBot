package jumpydoggo.main.map;

import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.type.UnitType;

public class EnemyBuildingInfo {

    private UnitType type;
    private TilePosition tilePosition;

    public EnemyBuildingInfo(UnitType type, TilePosition tilePosition) {
        this.type = type;
        this.tilePosition = tilePosition;
    }

    public UnitType getType() {
        return type;
    }

    public void setType(UnitType type) {
        this.type = type;
    }

    public TilePosition getTilePosition() {
        return tilePosition;
    }

    public void setTilePosition(TilePosition tilePosition) {
        this.tilePosition = tilePosition;
    }
}
