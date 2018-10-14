package jumpydoggo.main;

import bwapi.UnitType;

public class PlannedItem {
	
	private UnitType unitType;
	private Integer supply = 0; //If we don't set this, it means "Build it at your earliest convenience, Mr. Computer."
	private boolean isInProgress = false;
	private Integer importance = 0;
	
	public UnitType getUnitType() {
		return unitType;
	}
	public void setUnitType(UnitType unitType) {
		this.unitType = unitType;
	}
	public Integer getSupply() {
		return supply;
	}
	public void setSupply(Integer supply) {
		this.supply = supply;
	}

	public boolean isInProgress() {
		return isInProgress;
	}
	public void setInProgress(boolean isInProgress) {
		this.isInProgress = isInProgress;
	}
	public Integer getImportance() {
		return importance;
	}
	public void setImportance(Integer importance) {
		this.importance = importance;
	}
	
	public PlannedItem(UnitType unitType, Integer supply, boolean isInProgress, Integer importance) {
		super();
		this.unitType = unitType;
		this.supply = supply;
		this.isInProgress = isInProgress;
		this.setImportance(importance);
	}


}
