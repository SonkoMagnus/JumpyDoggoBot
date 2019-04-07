package jumpydoggo.main.map;

import org.openbw.bwapi4j.WalkPosition;

public class JPSInfo  {

    private Cartography.Direction direction;
    private WalkPosition walkPosition;
    private int importance;
    private JPSInfo precursor;
    private int generation = 0;

    public JPSInfo(Cartography.Direction direction, WalkPosition walkPosition, int importance, JPSInfo precursor) {
        this.direction = direction;
        this.walkPosition = walkPosition;
        this.importance = importance;
        this.precursor = precursor;
        if (precursor != null) {
            this.generation = precursor.getGeneration()+1;
        }
    }


    public Cartography.Direction getDirection() {
        return direction;
    }

    public void setDirection(Cartography.Direction direction) {
        this.direction = direction;
    }

    public WalkPosition getWalkPosition() {
        return walkPosition;
    }

    public void setWalkPosition(WalkPosition walkPosition) {
        this.walkPosition = walkPosition;
    }


    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }


    @Override
    public boolean equals(Object j) {
        if (((JPSInfo)j).getWalkPosition().getX() == this.getWalkPosition().getX() &&
                (((JPSInfo)j).getWalkPosition().getY() == this.getWalkPosition().getY() &&
                ((JPSInfo)j).getDirection().equals(this.direction))) {
            return  true;
        }
        return  false;
    }

    public JPSInfo getPrecursor() {
        return precursor;
    }

    public void setPrecursor(JPSInfo precursor) {
        this.precursor = precursor;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }
}
