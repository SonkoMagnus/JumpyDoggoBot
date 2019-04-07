package jumpydoggo.main.map;

import java.util.Comparator;

public class WPInfoComparator implements Comparator<WPInfo> {

    @Override
    public int compare(WPInfo o1, WPInfo o2) {
        if (o1.getCostFromStart() > o2.getCostFromStart()) {
            return 1;
        } else if (o1.getCostFromStart() < o2.getCostFromStart()) {
            return -1;
        }
        return 0;
    }
}
