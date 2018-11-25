package jumpydoggo.main.map;

import java.util.Comparator;

public class TileInfoComparator implements Comparator<TileInfo> {

    @Override
    public int compare(TileInfo x, TileInfo y) {
        {
            if (x.getImportance() > y.getImportance())
            {
                return -1;
            }

            else if (x.getImportance() < y.getImportance())
            {
                return 1;
            }
            return 0;
        }
    }

}
