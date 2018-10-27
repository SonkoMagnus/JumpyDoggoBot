package jumpydoggo.main;

import bwapi.UnitType;

public class PlannedItemPrereq {
	
	private UnitType unitType;
	private Integer amount;
	private Boolean morphing;
	
	public UnitType getUnitType() {
		return unitType;
	}
	public void setUnitType(UnitType unitType) {
		this.unitType = unitType;
	}
	public Integer getAmount() {
		return amount;
	}
	public void setAmount(Integer amount) {
		this.amount = amount;
	}
	public Boolean isMorphing() {
		return morphing;
	}
	public void setMorphing(Boolean morphing) {
		this.morphing = morphing;
	}
	

}
