package jumpydoggo.main;

import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.type.TechType;
import org.openbw.bwapi4j.type.UnitType;
import org.openbw.bwapi4j.type.UpgradeType;

import java.util.ArrayList;
import java.util.List;

public class PlannedItem {

    public Boolean getReserveOnFullSupply() {
        return reserveOnFullSupply;
    }

    public void setReserveOnFullSupply(Boolean reserveOnFullSupply) {
        this.reserveOnFullSupply = reserveOnFullSupply;
    }

    public enum PlannedItemType {
        BUILDING, UNIT, TECH, UPGRADE
    }

    public PlannedItemType plannedItemType;
    private UnitType unitType;
    private TechType techType;
    private UpgradeType upgradeType;
    private Integer supply = 0;  //If we don't set this, it means "Build it at your earliest convenience, Mr. Computer."
    private Integer importance = 0;
    private Integer builderId;
    private Integer unitId;
    private TilePosition plannedPosition;
    private Boolean reserveOnFullSupply = false;

    private List<PlannedItemPrereq> prereqList = new ArrayList<PlannedItemPrereq>();
    private Boolean doCancel = false;
    private List<PlannedItemPrereq> cancelPrereqList = new ArrayList<PlannedItemPrereq>();
    //unit, amount, morphing?

    private PlannedItemStatus status;

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
    public Integer getImportance() {
        return importance;
    }
    public void setImportance(Integer importance) {
        this.importance = importance;
    }
    public PlannedItemStatus getStatus() {
        return status;
    }
    public void setStatus(PlannedItemStatus status) {
        this.status = status;
    }

    public PlannedItem(PlannedItemType plannedItemType, UnitType unitType, Integer supply, Integer importance, PlannedItemStatus status) {
        super();
        this.plannedItemType = plannedItemType;
        this.unitType = unitType;
        this.supply = supply;
        this.importance = importance;
        this.status = status;
    }

    public PlannedItem(PlannedItemType plannedItemType, UnitType unitType, Integer supply, Integer importance) {
        super();
        this.plannedItemType = plannedItemType;
        this.unitType = unitType;
        this.supply = supply;
        this.importance = importance;
        this.status = PlannedItemStatus.PLANNED;
    }


    public PlannedItem(PlannedItemType plannedItemType, UnitType unitType, Integer supply, Integer importance, PlannedItemStatus status, TilePosition plannedPosition) {
        super();
        this.plannedItemType = plannedItemType;
        this.unitType = unitType;
        this.supply = supply;
        this.importance = importance;
        this.status = status;
        this.plannedPosition = plannedPosition;
    }

    public Integer getBuilderId() {
        return builderId;
    }
    public void setBuilderId(Integer builderId) {
        this.builderId = builderId;
    }
    public TilePosition getPlannedPosition() {
        return plannedPosition;
    }
    public void setPlannedPosition(TilePosition plannedPosition) {
        this.plannedPosition = plannedPosition;
    }
    public Integer getUnitId() {
        return unitId;
    }
    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }
    public List<PlannedItemPrereq> getPrereqList() {
        return prereqList;
    }
    public void setPrereqList(List<PlannedItemPrereq> prereqList) {
        this.prereqList = prereqList;
    }
    public List<PlannedItemPrereq> getCancelPrereqList() {
        return cancelPrereqList;
    }
    public void setCancelPrereqList(List<PlannedItemPrereq> cancelPrereqList) {
        this.cancelPrereqList = cancelPrereqList;
    }
    public Boolean getDoCancel() {
        return doCancel;
    }
    public void setDoCancel(Boolean doCancel) {
        this.doCancel = doCancel;
    }
    public TechType getTechType() {
        return techType;
    }
    public void setTechType(TechType techType) {
        this.techType = techType;
    }
    public UpgradeType getUpgradeType() {
        return upgradeType;
    }
    public void setUpgradeType(UpgradeType upgradeType) {
        this.upgradeType = upgradeType;
    }

}
