package jumpydoggo.main.strategy;

import jumpydoggo.main.Main;
import jumpydoggo.main.PlannedItem;
import org.openbw.bwapi4j.type.UnitType;

import static jumpydoggo.main.Main.isAllSupplyUsed;
import static jumpydoggo.main.Main.plannedItems;
import static jumpydoggo.main.Main.supplyUsedActual;
import static jumpydoggo.main.Main.unitsInProduction;

public class StrategyPlanner {

    public void extractorTrickGeneralized() {

    }

    public void addOverlordsOnFullSupply(){
        if (isAllSupplyUsed()
                && unitsInProduction.getOrDefault(UnitType.Zerg_Overlord, 0) == 0
                && supplyUsedActual <= 400) {
            PlannedItem overlord = new PlannedItem(PlannedItem.PlannedItemType.UNIT, UnitType.Zerg_Overlord, Main.supplyUsedActual, 1);
            overlord.setReserveOnFullSupply(true);
            boolean isAny = plannedItems.stream().anyMatch(p -> p.getUnitType().equals(UnitType.Zerg_Overlord)
                    && p.plannedItemType.equals(PlannedItem.PlannedItemType.UNIT)
                    && p.getSupply().equals(overlord.getSupply()) && p.getImportance() == 1);
            if (!isAny) {
                plannedItems.add(overlord);
            }
        }
    }

    public void execute() {
        addOverlordsOnFullSupply();
        addMoarLings();
    }

    //lel
    public void addMoarLings() {
        boolean moarlings = plannedItems.stream().anyMatch(p -> p.getUnitType().equals(UnitType.Zerg_Zergling)
                && p.plannedItemType.equals(PlannedItem.PlannedItemType.UNIT));
        if (!moarlings) {
            plannedItems.add(new PlannedItem(PlannedItem.PlannedItemType.UNIT, UnitType.Zerg_Zergling, 0, 1));
        }
    }

}
