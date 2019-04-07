package jumpydoggo.main.manager;

import org.openbw.bwapi4j.Position;
import org.openbw.bwapi4j.unit.Unit;

public class ZerglingManager extends UnitManager{

    public enum Role{ SCOUT, FIGHT}

    private Role role;

    private Position targetPosition;
    private Position targetUnit;

    public ZerglingManager(Unit unit) {
        super(unit);
    }



    @Override
    public void execute() {

        if (this.role == Role.SCOUT) {
            if (unit.isIdle()) {
                scout(true);
            }
        } else if (this.role == Role.FIGHT) {
            if (unit.isIdle()) {
                unit.attack(targetPosition);
            }
            //KEKEkE
        }
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Position getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Position targetPosition) {
        this.targetPosition = targetPosition;
    }

    public Position getTargetUnit() {
        return targetUnit;
    }

    public void setTargetUnit(Position targetUnit) {
        this.targetUnit = targetUnit;
    }
}
