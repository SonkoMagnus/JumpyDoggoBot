package jumpydoggo.main.map;

import org.openbw.bwapi4j.WalkPosition;

public class WPInfo {

    private long costFromStart;
    private WalkPosition walkPosition;
    private WPInfo precursor;

    public WPInfo(long costFromStart, WalkPosition walkPosition, WPInfo precursor) {
        this.costFromStart = costFromStart;
        this.walkPosition = walkPosition;
        this.precursor = precursor;
    }

    public WPInfo(long costFromStart, WalkPosition walkPosition) {
        this.costFromStart = costFromStart;
        this.walkPosition = walkPosition;
    }


    public WalkPosition getWalkPosition() {
        return walkPosition;
    }

    public void setWalkPosition(WalkPosition walkPosition) {
        this.walkPosition = walkPosition;
    }

    public long getCostFromStart() {
        return costFromStart;
    }

    public void setCostFromStart(long costFromStart) {
        this.costFromStart = costFromStart;
    }

    public WPInfo getPrecursor() {
        return precursor;
    }

    public void setPrecursor(WPInfo precursor) {
        this.precursor = precursor;
    }
}
