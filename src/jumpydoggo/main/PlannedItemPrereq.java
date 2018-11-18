package jumpydoggo.main;

import org.openbw.bwapi4j.type.UnitType;

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

    public PlannedItemPrereq(UnitType unitType, Integer amount, Boolean morphing) {
        super();
        this.unitType = unitType;
        this.amount = amount;
        this.morphing = morphing;
    }


}
