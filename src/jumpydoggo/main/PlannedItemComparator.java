package jumpydoggo.main;

import java.util.Comparator;

public class PlannedItemComparator implements Comparator<PlannedItem> {

	@Override
	public int compare(PlannedItem x, PlannedItem y) {
		if (x.getImportance() >= y.getImportance()) {
			return -1;
		}

		else if (x.getImportance() < y.getImportance()) {
			return 1;
		}
		return 0;
	}
}
