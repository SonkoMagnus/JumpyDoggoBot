package jumpydoggo.main.map;

import java.util.Comparator;

public class JPSInfoComparator implements Comparator<JPSInfo> {

    @Override
    public int compare(JPSInfo o1, JPSInfo o2) {
        if (o1.getImportance() > o2.getImportance()) {
            return 1;
        } else if (o1.getImportance() < o2.getImportance()) {
            return -1;
        }
        return 0;

    }
}
