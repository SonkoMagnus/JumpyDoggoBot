package jumpydoggo.main.map;

import org.openbw.bwapi4j.WalkPosition;

import java.util.Comparator;

public class WalkPositionComparator implements Comparator<WalkPosition> {


    @Override
    public int compare(WalkPosition o1, WalkPosition o2) {

        if (o1.getX() == o2.getX() && o1.getY() == o2.getY()) {
            return 0;
        } else if (o1.getX() > o1.getX()){
            return 1;
        } else {
            return -1;
        }
    }
}
