package jumpydoggo.main.map;

import org.openbw.bwapi4j.TilePosition;

public class TileInfo {



    public enum TileType {NORMAL, BASELOCATION, NATURAL}

    private TileType tileType;
    private TilePosition tile;
    private int importance = 0;

    private boolean isWalkable;

    public TileInfo(TilePosition tile, boolean isWalkable) {
        this.tile = tile;
        this.isWalkable = isWalkable;
    }

    public TileInfo(TilePosition tile, boolean isWalkable, TileType tileType) {
        this.tile = tile;
        this.isWalkable = isWalkable;
        this.tileType = tileType;

    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public boolean isWalkable() {
        return isWalkable;
    }

    public void setWalkable(boolean walkable) {
        isWalkable = walkable;
    }

    public TilePosition getTile() {
        return tile;
    }

    public void setTile(TilePosition tile) {
        this.tile = tile;
    }

    public TileType getTileType() {
        return tileType;
    }

    public void setTileType(TileType tileType) {
        this.tileType = tileType;
    }
}
