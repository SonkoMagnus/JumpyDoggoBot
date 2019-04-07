package jumpydoggo.main.tests;

import jumpydoggo.main.map.Cartography;
import org.junit.Assert;
import org.junit.Test;
import org.openbw.bwapi4j.WalkPosition;

import java.util.HashSet;

public class CartographyTests {

    @Test
    public void calcJPSTest() {
        WalkPosition origin = new WalkPosition(10, 10);
        WalkPosition destination = new WalkPosition(20, 20);

        WalkPosition wp1 = new WalkPosition(10,11);
        WalkPosition wp2 = new WalkPosition(21,21);
        int wp1Imp = Cartography.calcJPSImportance(origin, destination, wp1, 1, Cartography.relativeDirection(wp1, destination));
        int wp2Imp = Cartography.calcJPSImportance(origin, destination, wp2, 1, Cartography.relativeDirection(wp1, destination));
        Assert.assertTrue(wp1Imp < wp2Imp);

    }

    @Test
    public void checkDirTest() {


        HashSet<Cartography.Direction> dirS = Cartography.straightCheckPos.get(Cartography.Direction.S);
        Assert.assertTrue(dirS.contains(Cartography.Direction.W) && dirS.contains(Cartography.Direction.E));

    }


    @Test
    public void checkDirWeightTest() {
        Assert.assertTrue(Cartography.directionWeight(Cartography.Direction.N, Cartography.Direction.S) == 4);
        Assert.assertTrue(Cartography.directionWeight(Cartography.Direction.N, Cartography.Direction.SE) == 3);
        Assert.assertTrue(Cartography.directionWeight(Cartography.Direction.N, Cartography.Direction.SW) == 3);
    }


}



