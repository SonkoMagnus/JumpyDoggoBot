package jumpydoggo.manager;

import bwapi.Unit;

public class UnitManager {
	
	private Unit unit;
	
	public UnitManager(Unit unit) {
		this.setUnit(unit);
	}
	
	public void operate () {
		
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

}
