package jumpydoggo.main.map;

import bwem.ChokePoint;
import bwem.typedef.CPPath;
import jumpydoggo.main.Main;
import jumpydoggo.main.math.FastIntSqrt;
import org.openbw.bwapi4j.Position;
import org.openbw.bwapi4j.WalkPosition;
import org.openbw.bwapi4j.type.Color;
import org.openbw.bwapi4j.unit.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Cartography {

    public enum Direction {N, NW, W, SW, S, SE, E, NE}
    public static Direction[] dirArray = Direction.values();

    public static HashMap<Direction, HashSet<Direction>> straightCheckPos;
    public static HashMap<Direction, HashSet<Direction>> diagForwardPos;
    public static HashMap<Direction, HashSet<Direction>> diagCheckPos;

    static {

        straightCheckPos = new HashMap<>();
        HashSet<Direction> dirs = new HashSet<>();
        dirs.add(Direction.W);
        dirs.add(Direction.E);
        straightCheckPos.put(Direction.N, dirs);
        dirs = new HashSet<>();
        dirs.add(Direction.W);
        dirs.add(Direction.E);
        straightCheckPos.put(Direction.S, dirs);
        dirs = new HashSet<>();
        dirs.add(Direction.S);
        dirs.add(Direction.N);
        straightCheckPos.put(Direction.W, dirs);
        dirs = new HashSet<>();
        dirs.add(Direction.S);
        dirs.add(Direction.N);
        straightCheckPos.put(Direction.E, dirs);

        //Straight direction positions for diagonal leaps
        diagForwardPos = new HashMap<>();
        dirs = new HashSet<>();
        dirs.add(Direction.N);
        dirs.add(Direction.W);
        diagForwardPos.put(Direction.NW, dirs);
        dirs = new HashSet<>();
        dirs.add(Direction.S);;
        dirs.add(Direction.W);
        diagForwardPos.put(Direction.SW, dirs);
        dirs = new HashSet<>();
        dirs.add(Direction.N);;
        dirs.add(Direction.E);
        diagForwardPos.put(Direction.NE, dirs);
        dirs = new HashSet<>();
        dirs.add(Direction.S);;
        dirs.add(Direction.E);
        diagForwardPos.put(Direction.SE, dirs);


        //Positions to check for additional jump point insertion for diagonal leaps
        diagCheckPos = new HashMap<>();
        dirs = new HashSet<>();
        dirs.add(Direction.S);
        dirs.add(Direction.E);
        diagCheckPos.put(Direction.NW, dirs);
        dirs = new HashSet<>();
        dirs.add(Direction.S);
        dirs.add(Direction.W);
        diagCheckPos.put(Direction.NE, dirs);
        dirs = new HashSet<>();
        dirs.add(Direction.N);
        dirs.add(Direction.E);
        diagCheckPos.put(Direction.SW, dirs);
        dirs = new HashSet<>();
        dirs.add(Direction.N);
        dirs.add(Direction.W);
        diagCheckPos.put(Direction.SE, dirs);



    }

    public static final int GROUND_FORGET_PERIOD = 10;

    public static boolean isPassableGround(WalkPosition wp) {
        if (isWalkPositionOnTheMap(wp.getX(), wp.getY()) &&
                Main.bw.isWalkable(wp) &&
                (Main.occupiedGroundArray[wp.getX()][wp.getY()] < 0 ||
                (Main.frameCount - Main.occupiedGroundArray[wp.getX()][wp.getY()]) > GROUND_FORGET_PERIOD)) {
            return true;
        }
        return false;
    }

    public static boolean isPassableGround(int x, int y) {
        if (isWalkPositionOnTheMap(x,y) &&
                Main.bw.isWalkable(x,y) &&
                (Main.occupiedGroundArray[x][y] < 0 ||
                        (Main.frameCount - Main.occupiedGroundArray[x][y]) > GROUND_FORGET_PERIOD)) {
            return true;
        }
        return false;
    }
/*
    public static boolean isWalkPositionOnTheMap(WalkPosition wp, BW bw) {
        if (wp.getX() < 0 || wp.getX() >= bw.getBWMap().mapWidth()*4 || wp.getY() < 0 || wp.getY() >= bw.getBWMap().mapHeight()*4) {
            return false;
        }
        return true;
    }
*/
    public static Set<WalkPosition> getOccupiedWalkPositionsOfUnit(Unit unit) {
        int leftWpPos = unit.getLeft() / 8;
        int rightWpPos = unit.getRight() / 8;
        int topwpPos = unit.getTop() / 8;
        int bottomwpPos = unit.getBottom() / 8;

        HashSet<WalkPosition> wps = new HashSet<>();
        for (int i = leftWpPos; i <= rightWpPos; i++) {
            for (int j = topwpPos; j <= bottomwpPos; j++) {
                wps.add(new WalkPosition(i, j));
            }
        }
        return wps;
    }

    public static Set<WalkPosition> getOccupiedWalkPositionsOfRectangle(int left, int top, int right, int bottom) {
        int leftWpPos = left / 8;
        int rightWpPos = right / 8;
        int topwpPos = top / 8;
        int bottomwpPos = bottom / 8;

        HashSet<WalkPosition> wps = new HashSet<>();
        for (int i = leftWpPos; i <= rightWpPos; i++) {
            for (int j = topwpPos; j <= bottomwpPos; j++) {
                wps.add(new WalkPosition(i, j));
            }
        }
        return wps;
    }


    public static void drawWalkPositionGrid(Collection<WalkPosition> wps, Color color) {
        if (wps != null) {
            for (WalkPosition wp : wps) {
                Main.bw.getMapDrawer().drawBoxMap(
                        wp.getX() * 8,
                        wp.getY() * 8,
                        wp.getX() * 8 + 8,
                        wp.getY() * 8 + 8,
                        color, false);
            }
        }
    }


    public static TreeSet<WalkPosition> getWalkPositionsInRange(Unit unit, final int radiusInPixels) {
        //Make a large rectangle of walkpositions
        int left = unit.getLeft() - radiusInPixels;
        int top = unit.getTop() - radiusInPixels;
        int right = unit.getRight() + radiusInPixels;
        int bottom = unit.getBottom() + radiusInPixels;

        int leftWpPos = left / 8;
        int rightWpPos = right / 8;
        int topWpPos = top / 8;
        int bottomWpPos = bottom / 8;

        int offset = unit.getRight() % 8;
        if (offset > 0) {
            rightWpPos++;
        }
        offset = unit.getBottom() % 8;
        if (offset > 0) {
            bottomWpPos++;
        }

        int radiusSq = radiusInPixels * radiusInPixels;
        TreeSet<WalkPosition> wps = new TreeSet<>(new WalkPositionComparator());
        for (int i = leftWpPos; i <= rightWpPos; i++) {
            for (int j = topWpPos; j <= bottomWpPos; j++) {
                if (i < unit.getLeft() / 8) {
                    if (j < unit.getTop() / 8) {
                        //Top left corner - compare to WP's bottom right
                        if (positionToPositionDistanceSq(unit.getLeft(), unit.getTop(), i * 8 + 8, j * 8 + 8) <= radiusSq) {
                            wps.add(new WalkPosition(i, j));
                        }
                    } else if (j > unit.getBottom() / 8) {
                        //Bottom left corner
                        if (positionToPositionDistanceSq(unit.getLeft(), unit.getBottom(), i * 8 + 8, j * 8) <= radiusSq) {
                            wps.add(new WalkPosition(i, j));
                        }
                    } else {
                        wps.add(new WalkPosition(i, j));
                    }

                } else if (i > unit.getRight() / 8) {
                    if (j < unit.getTop() / 8) {
                        //Top right corner
                        if (positionToPositionDistanceSq(unit.getRight(), unit.getTop(), i * 8, j * 8 + 8) <= radiusSq) {
                            wps.add(new WalkPosition(i, j));
                        }
                    } else if (j > unit.getBottom() / 8) {
                        //Bottom right corner
                        if (positionToPositionDistanceSq(unit.getRight(), unit.getBottom(), i * 8, j * 8) <= radiusSq) {
                            wps.add(new WalkPosition(i, j));
                        }
                    } else {
                        wps.add(new WalkPosition(i, j));
                    }

                } else {
                    wps.add(new WalkPosition(i, j));
                }
            }
        }
        return wps;
    }

    public static void mostCommonNeighbor(int x, int y) {
        int[] neighbors = new int[8];
        //Get the neighbors
        boolean hasPositive = false;
        int ind = 0;
        for (int i = -1; i<=1; i++) {
            for (int j=-1; j<=1;j++) {
                if (!(j == 0 && i == 0)) {
                    if (x+i>=0 && y+j >=0 && x+i< Main.bw.getBWMap().mapWidth()*4 && y+j < Main.bw.getBWMap().mapHeight()*4) {
                    neighbors[ind] = Main.areaDataArray[x + i][y + j];
                    if (neighbors[ind] > 0) {
                        hasPositive = true;
                    }
                    } else {
                        neighbors[ind] = -1;
                    }
                    ind++;
                }
            }
        }
        if (hasPositive) {
            Arrays.sort(neighbors);
            int maxCount = 0;
            int currCount = 0;
            int result = -1;
            for (int n = 1; n < neighbors.length; n++) {
                if (neighbors[n] != -1 && neighbors[n] != -2) {
                    if (neighbors[n] == neighbors[n - 1]) {
                        currCount++;
                    } else {
                        if (currCount > maxCount) {
                            maxCount = currCount;
                            result = neighbors[n - 1];
                        }
                        currCount = 1;
                    }

                    if (currCount > maxCount) {
                        result = neighbors[n];
                    }
                }
            }
            Main.areaDataArray[x][y] = result;
        } else {
            //Assign a new areaId
            int newAreaId = -1;
            int id = 1;
            while (newAreaId == -1) {
                if (Main.areaIds.contains(id)) {
                    id++;
                } else {
                    newAreaId = id;
                }
            }
            Main.areaDataArray[x][y] = newAreaId;
            Main.areaIds.add(newAreaId);
        }
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (!(j == 0 && i == 0)) {
                    if (x + i >= 0 && y + j >= 0 && x + i < Main.bw.getBWMap().mapWidth() * 4 && y + j < Main.bw.getBWMap().mapHeight() * 4) {
                        if (Main.areaDataArray[x + i][y + j] == -1) {
                            mostCommonNeighbor(x + i, y + j);
                        }
                    }
                }
            }
        }
    }

    public static Set<Position> getPositionsInRadiusBW(Position point, int radius) {
        int x = point.getX();
        int y = point.getY();
        HashSet<Position> positionsInRadius = new HashSet<Position>();

        for (int i = x - radius; i <= x + radius; i++) {
            for (int j = y - radius; j <= y + radius; j++) {
                Position check = new Position(i, j);
                if (point.getDistance(check) < radius) {
                    positionsInRadius.add(check);
                }

            }
        }
        return positionsInRadius;
    }


    public static Set<Position> getPositionsInRadius(Position point, int radius) {
        int x = point.getX();
        int y = point.getY();
        HashSet<Position> positionsInRadius = new HashSet<Position>();
        int radiussq = radius * radius;
        for (int i = x - radius; i <= x + radius; i++) {
            int distX = x - i;
            for (int j = y - radius; j <= y + radius; j++) {
                int distY = y - j;
                int distsq = distX * distX + distY * distY;
                if (distsq <= radiussq) {
                    Position check = new Position(i, j);
                    positionsInRadius.add(check);
                }
                ;
            }
        }
        return positionsInRadius;
    }

    public static int positionToPositionDistanceSq(int x1, int y1, int x2, int y2) {
        int distX = x1 - x2;
        int distY = y1 - y2;
        int distsq = distX * distX + distY * distY;
        return distsq;
    }

    public static void drawOccupiedTile(Unit unit) {
        Main.bw.getMapDrawer().drawBoxMap(
                unit.getLeft(),
                unit.getTop(),
                unit.getRight(),
                unit.getBottom(),
                Color.ORANGE, false);
    }


    public static Set<WalkPosition> getWalkPositionsInGridRadius(WalkPosition origin, int radius) {
        HashSet<WalkPosition> walkPositions = new HashSet<>();
        int left = origin.getX() - radius;
        int top = origin.getY() - radius;
        int right = origin.getX() + radius;
        int bottom = origin.getY() + radius;

        if (left < 0) {
            left = 0;
        }
        if (top < 0) {
            top = 0;
        }
        if (bottom > Main.bw.getBWMap().mapHeight() * 4) {
            bottom = Main.bw.getBWMap().mapHeight() * 4;
        }
        if (right > Main.bw.getBWMap().mapWidth() * 4) {
            right = Main.bw.getBWMap().mapWidth() * 4;
        }

        int steps = radius * 2 + 1;
        //Top row
        for (int i = 0; i < steps; i++) {
            walkPositions.add(new WalkPosition(left + i, top));
        }
        //Bottom row
        for (int i = 0; i < steps; i++) {
            walkPositions.add(new WalkPosition(left + i, bottom));
        }
        //Left column
        for (int i = 1; i < steps - 1; i++) {
            walkPositions.add(new WalkPosition(left, top + i));
        }
        //Right column
        for (int i = 1; i < steps - 1; i++) {
            walkPositions.add(new WalkPosition(right, top + i));
        }
        return walkPositions;
    }


    public static Set<WalkPosition> getWalkPositionsOnLine(Position p1, Position p2) {
        //Get the "big rectangle"
        int left = Math.min(p1.getX(), p2.getX());
        int top = Math.min(p1.getY(), p2.getY());
        int right = Math.max(p1.getX(), p2.getX());
        int bottom = Math.max(p1.getY(), p2.getY());

        int leftWpPos = left / 8;
        int rightWpPos = right / 8;
        int topwpPos = top / 8;
        int bottomwpPos = bottom / 8;

        HashSet<WalkPosition> wps = new HashSet<>();
        for (int i = leftWpPos; i <= rightWpPos; i++) {
            for (int j = topwpPos; j <= bottomwpPos; j++) {
                if (doIntersect(i * 8, j * 8, i * 8 + 8, j * 8, p1.getX(), p1.getY(), p2.getX(), p2.getY())   //top
                        || doIntersect(i * 8, j * 8, i * 8, j * 8 + 8, p1.getX(), p1.getY(), p2.getX(), p2.getY())   //left
                        || doIntersect(i * 8 + 8, j * 8, i * 8 + 8, j * 8 + 8, p1.getX(), p1.getY(), p2.getX(), p2.getY())   //bottom
                        || doIntersect(i * 8 + 8, j * 8, i * 8 + 8, j * 8 + 8, p1.getX(), p1.getY(), p2.getX(), p2.getY())   //right
                ) {
                    wps.add(new WalkPosition(i, j));
                }
            }
        }
        return wps;
    }

    public static boolean onSegment(int px, int py, int qx, int qy, int rx, int ry) {
        if (qx <= Math.max(px, rx) && qx >= Math.min(px, rx) &&
                qy <= Math.max(py, ry) && qy >= Math.min(py, ry))
            return true;

        return false;
    }


    public static int orientation(int qx, int qy, int px, int py, int rx, int ry) {
        int val = (qy - py) * (rx - qx) -
                (qx - px) * (ry - qy);

        if (val == 0) return 0;  // colinear

        return (val > 0) ? 1 : 2; // clock or counterclock wise
    }


    public static boolean doIntersect(int p1x, int p1y, int q1x, int q1y, int p2x, int p2y, int q2x, int q2y) {
        // Find the four orientations needed for general and
        // special cases
        int o1 = orientation(p1x, p1y, q1x, q1y, p2x, p2y);
        int o2 = orientation(p1x, p1y, q1x, q1y, q2x, q2y);
        int o3 = orientation(p2x, p2y, q2x, q2y, p1x, p1y);
        int o4 = orientation(p2x, p2y, q2x, q2y, q1x, q1y);

        // General case
        if (o1 != o2 && o3 != o4)
            return true;

        // Special Cases
        // p1, q1 and p2 are colinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1x, p1y, p2x, p2y, q1x, q1y)) return true;

        // p1, q1 and q2 are colinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1x, p1y, q2x, q2y, q1x, q1y)) return true;

        // p2, q2 and p1 are colinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2x, p2y, p1x, p1y, q2x, q2y)) return true;

        // p2, q2 and q1 are colinear and q1 lies on segment p2q2
        if (o4 == 0 && onSegment(p2x, p2y, q1x, q1y, q2x, q2y)) return true;

        return false; // Doesn't fall in any of the above cases
    }
/*
    public static boolean isUnderThreat(boolean ground, WalkPosition wp, boolean useActiveThreatMap, boolean useThreatMemory) {
        boolean underThreat = false;

        if ((useActiveThreatMap && Main.activeThreatMap.containsKey(wp))) {
            if (!Main.activeThreatMap.get(wp).getGroundThreats().isEmpty()) {
                underThreat = true;
            }
        }

        if ((useThreatMemory && Main.threatMemoryMap.containsKey(wp))) {
            if (!Main.threatMemoryMap.get(wp).getGroundThreats().isEmpty()) {
                underThreat = true;
            }
        }

        return underThreat;
    }
*/
    public static boolean isUnderThreat(boolean ground, WalkPosition wp, boolean useActiveThreatMap, boolean useThreatMemory) {
        boolean underThreat = false;
        if (isWalkPositionOnTheMap(wp.getX(), wp.getY())) {
            if (ground) {
                if ((useActiveThreatMap && Main.activeThreatMapArray[wp.getX()][wp.getY()] != null)) {
                    if (!Main.activeThreatMapArray[wp.getX()][wp.getY()].getGroundThreats().isEmpty()) {
                        underThreat = true;
                    }
                }

                if ((useThreatMemory && Main.threatMemoryMap.containsKey(wp))) {
                    if (!Main.threatMemoryMap.get(wp).getGroundThreats().isEmpty()) {
                        underThreat = true;
                    }
                }
            }
        }

        return underThreat;
    }

    public static boolean isAdjacent(WalkPosition wp1, WalkPosition wp2) {
        if (Math.abs(wp1.getX() - wp2.getX()) <= 1 && Math.abs(wp1.getY() - wp2.getY()) <= 1) {
            return true;
        }
        return
                false;
    }

    //Only non-diagonal adjacency
    public static boolean isAdjacentStrict(WalkPosition wp1, WalkPosition wp2) {
        if (wp1.getX() == wp2.getX() || wp1.getY() == wp2.getY()) {
            return true;
        }
        return false;
    }

    public static boolean hasUnthreatenedLine(boolean ground, WalkPosition start, WalkPosition end, boolean useActiveThreatMap, boolean useThreatMemory) {
        Set<WalkPosition> wpPath = getWalkPositionsOnLine(start.toPosition(), end.toPosition());
        boolean hasPath = true;
        for (WalkPosition wp : wpPath) {
            if (isUnderThreat(ground, wp, useActiveThreatMap, useThreatMemory)) {
                hasPath = false;
                break;
            }
        }
        return hasPath;
    }

    //The "Best Friend Search"
    public static Set<WalkPosition> findUnthreatenedPathBFS(WalkPosition start, WalkPosition end, boolean ground,
                                                         boolean useActiveThreatMap, boolean useThreatMemory, HashSet<WalkPosition> existingPath) {
        if (existingPath == null) {
            existingPath = new HashSet<>();
        }

        if (!useActiveThreatMap && !useThreatMemory) {
            //Why use this method then?
        }

        if (hasUnthreatenedLine(ground, start, end, useActiveThreatMap, useThreatMemory)) {
            existingPath.add(start);
            existingPath.add(end);
            return existingPath;
        } else {
            existingPath.add(start);
            WalkPosition halfway = new WalkPosition((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
            int radius = 1;
            WalkPosition target = null;
            boolean startBoxedIn = false;
            boolean endBoxedIn = false;
            boolean unWalkableBox = false;
            while (target == null && !startBoxedIn && !endBoxedIn && !unWalkableBox) {
                startBoxedIn = true;
                endBoxedIn = true;
                if (ground) {
                    unWalkableBox = true;
                }
                Collection<WalkPosition> startSearchPoints = getWalkPositionsInGridRadius(start, radius);
                Collection<WalkPosition> endSearchPoints = getWalkPositionsInGridRadius(start, radius);
                for (WalkPosition wp : startSearchPoints) {
                    if (ground && Main.bw.getBWMap().isWalkable(wp)) {
                        unWalkableBox = false;
                    }
                    if (!isUnderThreat(ground, wp, useActiveThreatMap, useThreatMemory)) {
                        startBoxedIn = false;
                        break;
                    }
                }

                for (WalkPosition wp : endSearchPoints) {
                    if (ground && Main.bw.getBWMap().isWalkable(wp)) {
                        unWalkableBox = false;
                    }
                    if (!isUnderThreat(ground, wp, useActiveThreatMap, useThreatMemory)) {
                        endBoxedIn = false;
                        break;
                    }
                }

                Collection<WalkPosition> searchPoints = getWalkPositionsInGridRadius(halfway, radius);
                for (WalkPosition wp : searchPoints) {
                    if (!wp.equals(start) && !wp.equals(end) && !existingPath.contains(wp) && !isUnderThreat(ground, wp, useActiveThreatMap, useThreatMemory)) {
                        if (ground) {
                            if (Main.bw.getBWMap().isWalkable(wp)) {
                                unWalkableBox = false;
                                target = wp;
                                break;
                            }
                        } else {
                            target = wp;
                            break;
                        }
                    }
                }
                radius++;
            }
            //No path
            if (target == null) {
                return new HashSet<>();
            }

            if (hasUnthreatenedLine(ground, start, target, useActiveThreatMap, useThreatMemory)) {
                existingPath.add(target);
            } else {
                if (!existingPath.contains(target)) {
                    existingPath.add(target);
                    existingPath.addAll(findUnthreatenedPathBFS(start, target, ground, useActiveThreatMap, useThreatMemory, existingPath));
                }
            }

            if (hasUnthreatenedLine(ground, target, end, useActiveThreatMap, useThreatMemory)) {
                existingPath.add(target);
            } else {
                existingPath.add(end);
                existingPath.addAll(findUnthreatenedPathBFS(target, end, ground, useActiveThreatMap, useThreatMemory, existingPath));
            }
        }
        existingPath.add(end);
        return existingPath;
    }

    //Method signature of the everything: JPSinfo, actualwp, startwp, endwp,  boolean ground, boolean useActiveThreatMap, boolean useThreatMemory
    public static Set<WalkPosition> findUnthreatenedPathAStar(WalkPosition start, WalkPosition end, boolean ground, boolean useActiveThreatMap, boolean useThreatMemory) {
        Set<WPInfo> path = new HashSet<>();
        Set<WalkPosition> visited = new HashSet<>();
        WPInfo endNode = null;
        PriorityQueue<WPInfo> frontier = new PriorityQueue<>(new WPInfoComparator());
        WPInfo startinfo = new WPInfo(0, start, null);
        path.add(startinfo);
        frontier.add(startinfo);
        HashMap<WalkPosition, Long> distances = new HashMap<>();
        distances.put(start, 0L);

        while (!frontier.isEmpty()) {
            WPInfo current = frontier.poll();

            if (!visited.contains(current.getWalkPosition())) {
                visited.add(current.getWalkPosition());
                if (current.getWalkPosition().equals(end)) {
                    endNode = current;
                    break;
                }
            }

            boolean noThreat = false;
            int step = 1;
            Set<WalkPosition> neighbors = new HashSet<>();
            while (!noThreat) {

                neighbors = getWalkPositionsInGridRadius(current.getWalkPosition(), step);
                neighbors = neighbors.stream().filter(n -> !isUnderThreat(ground, n, useActiveThreatMap, useThreatMemory))
                        .collect(Collectors.toSet());
                if (neighbors.size() == 0) {
                    step++;
                } else {
                    noThreat = true;
                }
            }

            for (WalkPosition wp : neighbors) {
                if (!visited.contains(wp)) {
                    int predictedDist = positionToPositionDistanceSq(end.getX(), end.getY(), wp.getX(), wp.getY()); //predicted distance
                    predictedDist = FastIntSqrt.fastSqrt(predictedDist);
                    int nCost = 1;
                    if (!isAdjacentStrict(current.getWalkPosition(), wp)) {
                        nCost++;
                    }
                    long nDist = current.getCostFromStart() + nCost;
                    //long nDist = positionToPositionDistanceSq(wp.getX(), wp.getY(), start.getX(), start.getY());
                    //nDist = FastIntSqrt.fastSqrt(predictedDist);
                    long totalDist = nDist + predictedDist;
                    if (totalDist < distances.getOrDefault(wp, Long.MAX_VALUE)) {
                        distances.put(wp, totalDist);
                        WPInfo neighborInfo = new WPInfo(totalDist, wp, current);
                        frontier.add(neighborInfo);
                    }

                }
            }
        }

        HashSet<WalkPosition> finalPath = new HashSet<>();
        boolean precc = true;
        path.add(endNode);
        WPInfo p = endNode;
        while (precc) {

            if (p.getPrecursor() != null) {
                path.add(p);
                finalPath.add(p.getWalkPosition());
                p = p.getPrecursor();
            } else {
                precc = false;
            }
        }
        return finalPath;
    }

    public static ArrayList<WalkPosition> findUnthreatenedPathJPS(WalkPosition start, WalkPosition end, boolean ground, boolean useActiveThreatMap, boolean useThreatMemory) {
        boolean foundPath = false;
        PriorityQueue<JPSInfo> straight = new PriorityQueue<>(new JPSInfoComparator());
        PriorityQueue<JPSInfo> diag = new PriorityQueue<>(new JPSInfoComparator());
        JPSInfo startJPSInfo = new JPSInfo(null, start, 0, null);

        JPSInfo endJPSInfo = null;
        Set<JPSInfo> straightJPSInfos = getJPSInfosInDirection(startJPSInfo, start, start, end, ground, useActiveThreatMap, useThreatMemory, Direction.E, Direction.W, Direction.S, Direction.N);
        straight.addAll(straightJPSInfos);

        Set<JPSInfo> diagJPSInfos = getJPSInfosInDirection(startJPSInfo, start, start, end, ground, useActiveThreatMap, useThreatMemory, Direction.NE, Direction.NW, Direction.SE, Direction.SW);
        diag.addAll(diagJPSInfos);

        ArrayList<JPSInfo> processed = new ArrayList<>();
        processed.addAll(straightJPSInfos);
        processed.addAll(diagJPSInfos);

        if (isUnderThreat(ground, start, useActiveThreatMap, useThreatMemory) || isUnderThreat(ground, end, useActiveThreatMap, useThreatMemory)
                || !Main.bw.getBWMap().isValidPosition(start)
                || !Main.bw.getBWMap().isValidPosition(end)) {
            foundPath = true;
        }

        if (ground) {
            if (!isPassableGround(start) || !isPassableGround(end)) {
                foundPath = true;
            }
        }
        while (!foundPath && (!straight.isEmpty() || !diag.isEmpty())) {
            int sImp = Integer.MAX_VALUE;
            int dImp = Integer.MAX_VALUE;
            JPSInfo jumpPoint;
            boolean straightNext = false;
            if (!straight.isEmpty()) {
                sImp = straight.peek().getImportance();
            }
            if (!diag.isEmpty()) {
                dImp = diag.peek().getImportance();
            }
            if (sImp <= dImp) {
                straightNext = true;
            }

            if (straightNext) {
                jumpPoint = straight.poll();
                Direction dir = jumpPoint.getDirection();
                //Straight path processing
                boolean straightPathProcessed = false;
                WalkPosition current = jumpPoint.getWalkPosition();
                WalkPosition ahead = getNeighborInDirection(current, jumpPoint.getDirection());
                while (!straightPathProcessed) {
                    //Terminate search if the next tile in the direction is under threat/impassable
                    if (isUnderThreat(ground, ahead, useActiveThreatMap, useThreatMemory) || !Main.bw.getBWMap().isValidPosition(ahead) || (ground && !isPassableGround(ahead))) {
                        straightPathProcessed = true;
                    }
                    if (ahead.equals(end)) {
                        straightPathProcessed = true;
                        foundPath = true;
                        endJPSInfo = new JPSInfo(null, ahead, 0, jumpPoint);
                        break;
                    }
                    //Check neighbors to the left and right
                    HashSet<Direction> checkDirs = straightCheckPos.get(jumpPoint.getDirection());
                    for (Direction checkDir : checkDirs) {
                        WalkPosition straightNeighbor = getNeighborInDirection(current, checkDir);
                        if (Main.bw.getBWMap().isValidPosition(straightNeighbor)) {
                            if (isUnderThreat(ground, straightNeighbor, useActiveThreatMap, useThreatMemory) || (ground && !isPassableGround(straightNeighbor))) {
                                WalkPosition diagWP = getNeighborInDirection(current, getJPDirections(jumpPoint.getDirection(), checkDir).iterator().next());
                                if (Main.bw.getBWMap().isValidPosition(diagWP) && !isUnderThreat(ground, diagWP, useActiveThreatMap, useThreatMemory) && isPassableGround(diagWP)) {
                                    Direction jpsDir = getJPDirections(jumpPoint.getDirection(), checkDir).iterator().next();
                                    JPSInfo jpsInfo = new JPSInfo(jpsDir, getNeighborInDirection(current, jpsDir), calcJPSImportance(diagWP, start, end, jumpPoint.getGeneration(), jpsDir), jumpPoint);
                                    if (!processed.contains(jpsInfo)) {
                                        diag.add(jpsInfo);
                                        processed.add(jpsInfo);
                                    }
                                }
                            }
                        }
                    }
                    current = ahead;
                    ahead = getNeighborInDirection(ahead, jumpPoint.getDirection());
                }
            }
            else {
                jumpPoint = diag.poll();
                if (jumpPoint.getWalkPosition().equals(end)) {
                    foundPath = true;
                    endJPSInfo = new JPSInfo(null, jumpPoint.getWalkPosition(), 0, jumpPoint);
                    break;
                } else {
                    WalkPosition diagAhead = getNeighborInDirection(jumpPoint.getWalkPosition(), jumpPoint.getDirection());
                    if (jumpPoint.getWalkPosition().equals(end)) {
                        foundPath = true;
                        endJPSInfo = new JPSInfo(null, diagAhead, 0, jumpPoint);
                        break;
                    }

                    //If the next tile in the diagonal direction isn't blocked, let's add that too
                    if (!isUnderThreat(ground, diagAhead, useActiveThreatMap, useThreatMemory) && Main.bw.getBWMap().isValidPosition(diagAhead)) {
                        JPSInfo jpsInfo = null;
                        if (Main.bw.getBWMap().isValidPosition(diagAhead)) {
                            if (ground) {
                                if (isPassableGround(diagAhead)) {
                                    jpsInfo = new JPSInfo(jumpPoint.getDirection(), diagAhead, calcJPSImportance(diagAhead, start, end, jumpPoint.getGeneration(), jumpPoint.getDirection()), jumpPoint);
                                }
                            } else {
                                jpsInfo = new JPSInfo(jumpPoint.getDirection(), diagAhead, calcJPSImportance(diagAhead, start, end, jumpPoint.getGeneration(), jumpPoint.getDirection()), jumpPoint);
                            }
                        }
                        if (jpsInfo != null) {
                            if (!processed.contains(jpsInfo)) {
                                diag.add(jpsInfo);
                                processed.add(jpsInfo);
                            }
                        }
                    }
                    //Check the 2 straight jump points in any case
                    for (Direction dir : diagForwardPos.get(jumpPoint.getDirection())) {
                        WalkPosition neighbor = getNeighborInDirection(jumpPoint.getWalkPosition(), dir);
                        Set<JPSInfo> jpsInfosInDirection;
                        if (!isUnderThreat(ground, neighbor, useActiveThreatMap, useThreatMemory))
                            if (ground) {
                                if (isPassableGround(neighbor)) {
                                    jpsInfosInDirection = getJPSInfosInDirection(jumpPoint, jumpPoint.getWalkPosition(), start, end, ground, useActiveThreatMap, useThreatMemory, dir);
                                    for (JPSInfo j : jpsInfosInDirection) {
                                        if (!processed.contains(j)) {
                                            straight.addAll(jpsInfosInDirection);
                                            processed.addAll(jpsInfosInDirection);
                                        }
                                    }
                                }
                            } else {
                                jpsInfosInDirection = getJPSInfosInDirection(jumpPoint, jumpPoint.getWalkPosition(), start, end, ground, useActiveThreatMap, useThreatMemory, dir);
                                for (JPSInfo j : jpsInfosInDirection) {
                                    if (!processed.contains(j)) {
                                        straight.addAll(jpsInfosInDirection);
                                        processed.addAll(jpsInfosInDirection);
                                    }
                                }
                            }
                    }
                    //Check the two remaining straight directions
                    for (Direction checkDir : diagCheckPos.get(jumpPoint.getDirection())) {
                        WalkPosition wp = getNeighborInDirection(diagAhead, checkDir);
                        if (Main.bw.getBWMap().isValidPosition(wp) && isUnderThreat(ground, wp, useActiveThreatMap, useThreatMemory)) {
                            Set<JPSInfo> jpsInfosInDirection = getJPSInfosInDirection(jumpPoint, wp, start, end, ground, useActiveThreatMap, useThreatMemory, getJPDirections(jumpPoint.getDirection(), checkDir));
                            for (JPSInfo j : jpsInfosInDirection) {
                                if (ground) {
                                    if (isPassableGround(j.getWalkPosition())) {
                                        if (!processed.contains(j)) {
                                            diag.add(j);
                                            processed.add(j);
                                        }
                                    }
                                } else {
                                    if (!processed.contains(j)) {
                                        diag.add(j);
                                        processed.add(j);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ArrayList<WalkPosition> patherino = new ArrayList<>();
        JPSInfo precc;

        if (endJPSInfo != null) {
            patherino.add(end);
            precc = endJPSInfo.getPrecursor();
            while (precc != null) {
                patherino.add(precc.getWalkPosition());
                precc = precc.getPrecursor();
            }
        }
        return patherino;
    }





    //Based on the original check direction, and the examined direction, returns the direction the search should go forward
    public static Set<Direction> getJPDirections(Direction jpDir, Direction checkDir) {
        HashSet<Direction> dirs = new HashSet<>();
        //Straight cases
        if (jpDir == Direction.N) {
            if (checkDir == Direction.W) {
                dirs.add(Direction.NW);
            } else if (checkDir == Direction.E) {
                dirs.add(Direction.NE);
            }
        } else if (jpDir == Direction.S) {
            if (checkDir == Direction.W) {
                dirs.add(Direction.SW);
            } else if (checkDir == Direction.E) {
                dirs.add(Direction.SE);
            }
        } else if (jpDir == Direction.E) {
            if (checkDir == Direction.S) {
                dirs.add(Direction.SE);
            } else if (checkDir == Direction.N) {
                dirs.add(Direction.NE);
            }
        } else if (jpDir == Direction.W) {
            if (checkDir == Direction.N) {
                dirs.add(Direction.NW);
            } else if (checkDir == Direction.S) {
                dirs.add(Direction.SW);
            }
            //Diagonal cases
        } else if (jpDir == Direction.NW) {
            if (checkDir == Direction.E) {
                dirs.add(Direction.NE);
            } else if (checkDir == Direction.S) {
                dirs.add(Direction.SE);
            }
        } else if (jpDir == Direction.NE) {
            if (checkDir == Direction.S) {
                dirs.add(Direction.SW);
            } else if (checkDir == Direction.W) {
                dirs.add(Direction.NW);
            }
        } else if (jpDir == Direction.SW) {
            if (checkDir == Direction.E) {
                dirs.add(Direction.SE);
            } else if (checkDir == Direction.N) {
                dirs.add(Direction.NW);
            }
        } else if (jpDir == Direction.SE) {
            if (checkDir == Direction.N) {
                dirs.add(Direction.NE);
            } else if (checkDir == Direction.W) {
                dirs.add(Direction.SW);
            }
        }
            return  dirs;
    }

    public static JPSInfo[] getJPSInfosArrayInDirection(JPSInfo precursor, WalkPosition actual, WalkPosition start, WalkPosition end, Direction... dirs) {
        JPSInfo[] jpsInfos = new JPSInfo[dirs.length];

        for (int i=0; i<dirs.length;i++) {
            jpsInfos[i] = new JPSInfo(dirs[i],
                    actual,
                    calcJPSImportance(actual, start, end, precursor.getGeneration(), dirs[i]), precursor);
        }

        return jpsInfos;
    }

    public static JPSInfo[] getJPSInfosArrayInDirection(JPSInfo jpsInfo, WalkPosition actual, WalkPosition start, WalkPosition end,  boolean ground, boolean useActiveThreatMap, boolean useThreatMemory, Direction... dirs) {
        JPSInfo[] jpsInfos = new JPSInfo[dirs.length];

        for (int i=0; i<dirs.length; i++) {
            if (!isUnderThreat(ground, actual, useActiveThreatMap, useThreatMemory)) {
                jpsInfos[i] = new JPSInfo(dirs[i],
                        actual,
                        calcJPSImportance(actual, start, end, jpsInfo.getGeneration(), dirs[i]), jpsInfo);
            }
        }
        return jpsInfos;
    }



    public static Set<JPSInfo> getJPSInfosInDirection(JPSInfo jpsInfo, WalkPosition actual, WalkPosition start, WalkPosition end,  boolean ground, boolean useActiveThreatMap, boolean useThreatMemory, Collection<Direction> dirs) {
        return getJPSInfosInDirection(jpsInfo, start, end, actual, ground, useActiveThreatMap, useActiveThreatMap, dirs.toArray(new Direction[dirs.size()]));
    }

    public static Set<JPSInfo> getJPSInfosInDirection(JPSInfo precursor, WalkPosition actual, WalkPosition start, WalkPosition end, Collection<Direction> dirs) {
        return getJPSInfosInDirection(precursor, actual, start, end, dirs.toArray(new Direction[dirs.size()]));
    }

    public static Set<JPSInfo> getJPSInfosInDirection(JPSInfo precursor, WalkPosition actual, WalkPosition start, WalkPosition end, Direction... dirs) {
        HashSet<JPSInfo> jpsInfos = new HashSet<>();

        for (Direction d : dirs) {
            jpsInfos.add(new JPSInfo(d,
                    actual,
                    calcJPSImportance(actual, start, end, precursor.getGeneration(), d), precursor));
        }
        return jpsInfos;
    }

    public static Set<JPSInfo> getJPSInfosInDirection(JPSInfo jpsInfo, WalkPosition actual, WalkPosition start, WalkPosition end,  boolean ground, boolean useActiveThreatMap, boolean useThreatMemory, Direction... dirs) {
        HashSet<JPSInfo> jpsInfos = new HashSet<>();
        for (Direction d : dirs) {
            if (!isUnderThreat(ground, actual, useActiveThreatMap, useThreatMemory)) {
                jpsInfos.add(new JPSInfo(d,
                        actual,
                        calcJPSImportance(actual, start, end, jpsInfo.getGeneration(), d), jpsInfo));
            }
        }
        return jpsInfos;
    }

    //Which direction is the end from the start
    public static Direction relativeDirection(WalkPosition start, WalkPosition end) {
        if (start.getX() < end.getX()) {
            if (start.getY() == end.getY()) {
                return Direction.E;
            } else if (start.getY() > end.getY()) {
                return Direction.NE;
            } else if (start.getY() < end.getY()) {
                return Direction.SE;
            }
        } else if (start.getX() > end.getX()) {
            if (start.getY() == end.getY()) {
                return Direction.W;
            } else if (start.getY() > end.getY()) {
                return Direction.NW;
            } else if (start.getY() < end.getY()) {
                return Direction.SW;
            }
        } else if (start.getX() == end.getX()) {
            if (start.getY() > end.getY()) {
                return Direction.N;
            } else if (start.getY() < end.getY()) {
                return Direction.S;
            }
        }
        return null;
    }

    public static int directionWeight(Direction source, Direction target) {
        int sourceIndex = 0, targetIndex = 0;
        for (int i=0; i<dirArray.length;i++) {
            if (dirArray[i] == source) {
                sourceIndex = i;
            }
            if (dirArray[i] == target) {
                targetIndex = i;
            }
        }

        //source.ordinal()

        int weight = Math.abs(sourceIndex-targetIndex);
        if (weight > 4) {
            weight = 8-weight;
        }
        return weight;
    }



    //Heuristic for calculation importance of jump points
    public static int calcJPSImportance(WalkPosition actual, WalkPosition start, WalkPosition end, int gen, Direction jpsDir) {
        Direction relativeDir = relativeDirection(actual, end);
        int endImp = positionToPositionDistanceSq(end.getX(), end.getY(), actual.getX(), actual.getY());
        return FastIntSqrt.fastSqrt(endImp) + gen + directionWeight(jpsDir, relativeDir);
    }


    public static Set<WalkPosition> getNeighborsInDirection(WalkPosition origin, Collection<Direction> dirs) {
        return getNeighborsInDirection(origin, dirs.toArray(new Direction[dirs.size()]));

    }

    public static Set<WalkPosition> getNeighborsInDirection(WalkPosition origin, Direction... dirs) {
        HashSet<WalkPosition> neighbors = new HashSet<>();
        for (Direction dir : dirs) {
            if (dir.equals(Direction.N)) {
                neighbors.add(new WalkPosition(origin.getX(), origin.getY() - 1));
            }

            if (dir.equals(Direction.S)) {
                neighbors.add(new WalkPosition(origin.getX(), origin.getY() + 1));
            }

            if (dir.equals(Direction.W)) {
                neighbors.add(new WalkPosition(origin.getX() - 1, origin.getY()));
            }

            if (dir.equals(Direction.E)) {
                neighbors.add(new WalkPosition(origin.getX() + 1, origin.getY()));
            }
            if (dir.equals(Direction.NW)) {
                neighbors.add(new WalkPosition(origin.getX() - 1, origin.getY() - 1));
            }

            if (dir.equals(Direction.NE)) {
                neighbors.add(new WalkPosition(origin.getX() + 1, origin.getY() - 1));
            }

            if (dir.equals(Direction.SW)) {
                neighbors.add(new WalkPosition(origin.getX() - 1, origin.getY() + 1));
            }

            if (dir.equals(Direction.SE)) {
                neighbors.add(new WalkPosition(origin.getX() + 1, origin.getY() + 1));
            }
        }

        return neighbors;
    }

    public static WalkPosition getNeighborInDirection(WalkPosition origin, Direction dir) {
        if (dir.equals(Direction.N)) {
            return new WalkPosition(origin.getX(), origin.getY() - 1);
        }
        if (dir.equals(Direction.S)) {
            return new WalkPosition(origin.getX(), origin.getY() + 1);
        }
        if (dir.equals(Direction.W)) {
            return new WalkPosition(origin.getX() - 1, origin.getY());
        }
        if (dir.equals(Direction.E)) {
            return new WalkPosition(origin.getX() + 1, origin.getY());
        }
        if (dir.equals(Direction.NW)) {
            return new WalkPosition(origin.getX() - 1, origin.getY() - 1);
        }

        if (dir.equals(Direction.NE)) {
            return new WalkPosition(origin.getX() + 1, origin.getY() - 1);
        }

        if (dir.equals(Direction.SW)) {
            return new WalkPosition(origin.getX() - 1, origin.getY() + 1);
        }

        if (dir.equals(Direction.SE)) {
            return new WalkPosition(origin.getX() + 1, origin.getY() + 1);
        }
        return null;
    }

    public static boolean isWalkPositionOnTheMap(int x, int y) {
        if (0 <= x
                && x < Main.bw.getBWMap().mapWidth()*4
                && 0 <= y
                && y < Main.bw.getBWMap().mapHeight()*4) {
            return true;
        }
        return false;
    }
/*
    public static boolean pathExistsFloodFillArray(WalkPosition start, WalkPosition end, boolean ground, boolean useActiveThreatMap, boolean useThreatMemory) {
        //Clearing out the threat array
        for (int x = 0; x < Main.threatArray.length; x++) {
            for (int y = 0; y < Main.threatArray[x].length; y++) {
                Main.threatArray[x][y] = 0;
            }
        }

        //Setting threatened positions to 1
        if (useActiveThreatMap) {
            for (WalkPosition wp : Main.activeThreatMap.keySet()) {
                Main.threatArray[wp.getX()][wp.getY()] = 1;
            }
        }

        if (useThreatMemory) {
            for (WalkPosition wp : Main.threatMemoryMap.keySet()) {
                Main.threatArray[wp.getX()][wp.getY()] = 1;
            }
        }

        ArrayList<WalkPosition> unchecked = new ArrayList<>();

        if ((isUnderThreat(ground, start, useActiveThreatMap, useThreatMemory) || !Main.bw.getBWMap().isValidPosition(start))) {
            return false;
        } else {
            Main.threatArray[start.getX()][start.getY()] = 1;
            unchecked.add(start);
        }

        while (!unchecked.isEmpty()) {
            ArrayList<WalkPosition> newWps = new ArrayList<>();
            for (WalkPosition uc : unchecked) {
                if (uc.equals(end)) {
                    return true;
                }
                int x = uc.getX();
                int y = uc.getY();

                x = uc.getX() + 1;
                y = uc.getY();
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0) {
                    if (end.getX() == x && end.getY() == y) {
                        return true;
                    }
                    Main.threatArray[x][y] = 1;
                    newWps.add(new WalkPosition(x, y));
                }

                x = uc.getX() - 1;
                y = uc.getY();
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0) {
                    if (end.getX() == x && end.getY() == y) {
                        return true;
                    }
                    Main.threatArray[x][y] = 1;
                    newWps.add(new WalkPosition(x, y));
                }

                x = uc.getX();
                y = uc.getY() - 1;
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0) {
                    if (end.getX() == x && end.getY() == y) {
                        return true;
                    }
                    Main.threatArray[x][y] = 1;
                    newWps.add(new WalkPosition(x, y));
                }

                x = uc.getX();
                y = uc.getY() + 1;
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0) {
                    if (end.getX() == x && end.getY() == y) {
                        return true;
                    }
                    Main.threatArray[x][y] = 1;
                    newWps.add(new WalkPosition(x, y));
                }
            }
            unchecked = newWps;
        }
        return false;
    }


    //Just returns a true/false for the path's existence
    public static boolean pathExistsFloodFillArray(WalkPosition start, Collection<WalkPosition> endPoints, boolean ground, boolean useActiveThreatMap, boolean useThreatMemory) {
        //Clearing out the threat array
        for (int x = 0; x < Main.threatArray.length; x++) {
            for (int y = 0; y < Main.threatArray[x].length; y++) {
                Main.threatArray[x][y] = 0;
            }
        }

        //Setting threatened positions to 1
        if (useActiveThreatMap) {
            for (WalkPosition wp : Main.activeThreatMap.keySet()) {
                Main.threatArray[wp.getX()][wp.getY()] = 1;
            }
        }

        if (useThreatMemory) {
            for (WalkPosition wp : Main.threatMemoryMap.keySet()) {
                Main.threatArray[wp.getX()][wp.getY()] = 1;
            }
        }

        ArrayList<WalkPosition> unchecked = new ArrayList<>();

        if ((isUnderThreat(ground, start, useActiveThreatMap, useThreatMemory) || !Main.bw.getBWMap().isValidPosition(start))) {
            return false;
        } else {
            Main.threatArray[start.getX()][start.getY()] = 1;
            unchecked.add(start);
        }

        while (!unchecked.isEmpty()) {
            ArrayList<WalkPosition> newWps = new ArrayList<>();
            for (WalkPosition uc : unchecked) {
                if (endPoints.contains(uc)) {
                    return true;
                }
                int x = uc.getX();
                int y = uc.getY();

                x = uc.getX() + 1;
                y = uc.getY();
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0) {
                    for (WalkPosition end : endPoints) {
                        if (end.getX() == x && end.getY() == y) {
                            return true;
                        }
                    }
                    Main.threatArray[x][y] = 1;
                    newWps.add(new WalkPosition(x, y));
                }

                x = uc.getX() - 1;
                y = uc.getY();
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0) {
                    for (WalkPosition end : endPoints) {
                        if (end.getX() == x && end.getY() == y) {
                            return true;
                        }
                        Main.threatArray[x][y] = 1;
                        newWps.add(new WalkPosition(x, y));
                    }
                }

                    x = uc.getX();
                    y = uc.getY() - 1;
                    if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0) {
                        for (WalkPosition end : endPoints) {
                            if (end.getX() == x && end.getY() == y) {
                                return true;
                            }
                        }
                        Main.threatArray[x][y] = 1;
                        newWps.add(new WalkPosition(x, y));
                    }

                    x = uc.getX();
                    y = uc.getY() + 1;
                    if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0) {
                        for (WalkPosition end : endPoints) {

                            if (end.getX() == x && end.getY() == y) {
                                return true;
                            }
                        }
                        Main.threatArray[x][y] = 1;
                        newWps.add(new WalkPosition(x, y));
                    }
                }
                unchecked = newWps;
            }
            return false;
    }


    //Returns the first endpoint where there is a path to
    public static WalkPosition pathExistsInArea(WalkPosition start, Collection<WalkPosition> endPoints, boolean ground, boolean useActiveThreatMap, boolean useThreatMemory, int areaId) {
        //int areaId = area.getId().intValue();
        //Clearing out the threat array
        ArrayList<WalkPosition> path = new ArrayList<>();
        for (int x = 0; x < Main.threatArray.length; x++) {
            for (int y = 0; y < Main.threatArray[x].length; y++) {
                if (Main.areaDataArray[x][y] != -1) {
                    Main.threatArray[x][y] = 0;
                }
            }
        }
        //Setting threatened positions to 1
        if (useActiveThreatMap) {
            for (WalkPosition wp : Main.activeThreatMap.keySet()) {
                Main.threatArray[wp.getX()][wp.getY()] = 1;
            }
        }
        if (useThreatMemory) {
            for (WalkPosition wp : Main.threatMemoryMap.keySet()) {
                Main.threatArray[wp.getX()][wp.getY()] = 1;
            }
        }

        ArrayList<WalkPosition> unchecked = new ArrayList<>();
        if ((isUnderThreat(ground, start, useActiveThreatMap, useThreatMemory) || !Main.bw.getBWMap().isValidPosition(start))) {
            return null;
        } else {
            Main.threatArray[start.getX()][start.getY()] = 1;
            unchecked.add(start);
        }
        while (!unchecked.isEmpty()) {
            ArrayList<WalkPosition> newWps = new ArrayList<>();
            for (WalkPosition uc : unchecked) {
                if (endPoints.contains(uc)) {
                    return uc;
                }
                int x = uc.getX();
                int y = uc.getY();
                x = uc.getX() + 1;
                y = uc.getY();
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0 && Main.areaDataArray[x][y] == areaId && isPassableGround(x, y)) {
                    for (WalkPosition end : endPoints) {
                        if (end.getX() == x && end.getY() == y) {
                            return uc;
                        }
                    }
                    Main.threatArray[x][y] = 1;
                    newWps.add(new WalkPosition(x, y));
                }
                x = uc.getX() - 1;
                y = uc.getY();
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0 && Main.areaDataArray[x][y] == areaId && isPassableGround(x, y)) {
                    for (WalkPosition end : endPoints) {
                        if (end.getX() == x && end.getY() == y) {
                            return uc;
                        }
                        Main.threatArray[x][y] = 1;
                        newWps.add(new WalkPosition(x, y));
                    }
                }
                x = uc.getX();
                y = uc.getY() - 1;
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0 && Main.areaDataArray[x][y] == areaId && isPassableGround(x, y)) {
                    for (WalkPosition end : endPoints) {
                        if (end.getX() == x && end.getY() == y) {
                            return uc;
                        }
                    }
                    Main.threatArray[x][y] = 1;
                    newWps.add(new WalkPosition(x, y));
                }
                x = uc.getX();
                y = uc.getY() + 1;
                if (isWalkPositionOnTheMap(x, y) && Main.threatArray[x][y] == 0 && Main.areaDataArray[x][y] == areaId && isPassableGround(x, y)) {
                    for (WalkPosition end : endPoints) {

                        if (end.getX() == x && end.getY() == y) {
                            return uc;
                        }
                    }
                    Main.threatArray[x][y] = 1;
                    newWps.add(new WalkPosition(x, y));
                }
            }
            unchecked = newWps;
        }
        return null;
    }
    */

    public static int findCommonAreaId(ChokePoint cp1, ChokePoint cp2) {
        for (WalkPosition wp1 : cp1.getGeometry()) {
            for (WalkPosition wp2 : cp2.getGeometry()) {
                if (Main.areaDataArray[wp1.getX()][wp1.getY()] == Main.areaDataArray[wp2.getX()][wp2.getY()]) {
                    return Main.areaDataArray[wp1.getX()][wp1.getY()];
                }
            }
        }
        return -1;
    }
/*
    //Only applies to ground pathfinding
    public static ArrayList<WalkPosition> findAnyPathMultiArea(WalkPosition start, WalkPosition end, boolean useActiveThreatMap, boolean useThreatMemory, boolean givePartialPath) {
        ArrayList<WalkPosition> summaryPath = new ArrayList<>();
        CPPath bwemPath = Main.bwem.getMap().getPath(start.toPosition(), end.toPosition());
        boolean fullPathExists = true;
        int lps = 0;
        if (bwemPath.size() == 0) {
            if (pathExistsFloodFillArray(start, end, true, useActiveThreatMap, useThreatMemory)) {
                return findUnthreatenedPathInAreaJPS(start, end, true, useActiveThreatMap, useThreatMemory, Main.areaDataArray[start.getX()][start.getY()]);
            }
        } else if (bwemPath.size() >= 1) {
            summaryPath.addAll(findAnyPathInArea(bwemPath.get(0), start, useActiveThreatMap, useThreatMemory));
            for (int cc = 1; cc< bwemPath.size(); cc++ ) {
                lps++;
                ArrayList<WalkPosition> pathInArea = findAnyPathInArea(bwemPath.get(cc), bwemPath.get(cc-1), useActiveThreatMap, useThreatMemory);
                if (pathInArea.size() >0 ) {
                    summaryPath.addAll(pathInArea);
                } else {
                    fullPathExists = false;
                    //no path!
                    break;
                }
            }
            if (!fullPathExists && !givePartialPath) {
                summaryPath = new ArrayList<>();
            } else {
                summaryPath.addAll(findAnyPathInArea(bwemPath.get(bwemPath.size() - 1), end, useActiveThreatMap, useThreatMemory));
            }
        }
        System.out.println("lööps:"+lps);
        return  summaryPath;
    }
    */

    public static ArrayList<WalkPosition> findAnyPathMultiAreaContinuous(WalkPosition start, WalkPosition end, boolean useActiveThreatMap, boolean useThreatMemory, boolean givePartialPath) {
        ArrayList<WalkPosition> summaryPath = new ArrayList<>();
        CPPath bwemPath = Main.bwem.getMap().getPath(start.toPosition(), end.toPosition());
        boolean fullPathExists = true;
        ArrayList<ArrayList<WalkPosition>> pathPieces = new ArrayList<>();
        ArrayList<int[]> originalAreaValues = new ArrayList<>();
        int level = 0; //Counter for the level we are in

        if (bwemPath.size() == 0) {
                return findUnthreatenedPathInAreaJPS2(start, end, true, useActiveThreatMap, useThreatMemory, Main.areaDataArray[start.getX()][start.getY()]);
        } else if (bwemPath.size() >= 1) {
            //Saving the areadata values
            for (ChokePoint cp : bwemPath) {
                int[] cpData = new int[cp.getGeometry().size()];
                int x = 0;
                for (WalkPosition wp : cp.getGeometry()) {
                    cpData[x++] = Main.areaDataArray[wp.getX()][wp.getY()];
                }
                originalAreaValues.add(cpData);
            }

            while (level <= bwemPath.size() && fullPathExists) {
                WalkPosition startTile = null;
                WalkPosition endTile = null;
                int areaId = -3;

                    if (level == 0) {
                        startTile = start;
                        endTile = getFirstUnthreatened(bwemPath.get(level));
                        areaId = Main.areaDataArray[start.getX()][start.getY()];

                    } else if (level == bwemPath.size()) {
                        startTile = getFirstUnthreatened(bwemPath.get(level - 1));
                        endTile = end;
                        areaId = Main.areaDataArray[end.getX()][end.getY()];
                    } else {
                        startTile = getFirstUnthreatened(bwemPath.get(level - 1));
                        endTile = getFirstUnthreatened(bwemPath.get(level));
                        if (endTile != null) {
                            areaId = Main.areaDataArray[endTile.getX()][endTile.getY()];
                        }
                    }

                if (startTile == null || endTile == null) {
                    fullPathExists = false;
                    break;
                }

                //local normalization
                int startValue = Main.areaDataArray[startTile.getX()][startTile.getY()];
                int endValue = Main.areaDataArray[endTile.getX()][endTile.getY()];

                Main.areaDataArray[startTile.getX()][startTile.getY()] = areaId;
                Main.areaDataArray[endTile.getX()][endTile.getY()] = areaId;
                boolean excludeStart = false, excludeEnd = false;



                ArrayList<WalkPosition> partialPath = findUnthreatenedPathInAreaJPS2(startTile,endTile, true, useActiveThreatMap, useThreatMemory, areaId);
                if (partialPath.size() > 0) {
                    if (pathPieces.size() <= level) {
                        pathPieces.add(partialPath);
                    } else {
                        pathPieces.set(level, partialPath);
                    }
                    level++;
                } else {
                    if (level == 0) {
                        excludeEnd = true;
                    } else {
                        excludeStart = true;
                        level--;
                    }
                }

                if (excludeStart) {
                    Main.areaDataArray[startTile.getX()][startTile.getY()] = -1;
                } else {
                    Main.areaDataArray[startTile.getX()][startTile.getY()] = startValue;
                }

                if (excludeEnd) {
                    Main.areaDataArray[endTile.getX()][endTile.getY()] = -1;
                } else {
                    Main.areaDataArray[endTile.getX()][endTile.getY()] = endValue;
                }
            }

            //Loading the original values back, after all is done
            int i = 0;
            for (ChokePoint cp : bwemPath) {
                int j = 0;
                for (WalkPosition wp : cp.getGeometry()) {
                    Main.areaDataArray[wp.getX()][wp.getY()] = originalAreaValues.get(i)[j++];
                }
                i++;
            }

        }

        if (fullPathExists) {
            for (ArrayList<WalkPosition> a : pathPieces) {
                summaryPath.addAll(a);
            }
        } else {
            if (givePartialPath) {
                for (ArrayList<WalkPosition> a : pathPieces) {
                    summaryPath.addAll(a);
                }

            }
        }
        return  summaryPath;
    }

    public static WalkPosition getFirstUnthreatened(ChokePoint cp) {
        WalkPosition tile = null;
        for (WalkPosition wp : cp.getGeometry()) {
            if (Main.areaDataArray[wp.getX()][wp.getY()] != -1) {
                tile = wp;
                break;
            }
        }
        return tile;
    }


/*
    public static ArrayList<WalkPosition> findAnyPathInArea(ChokePoint cp, WalkPosition start, boolean useActiveThreatMap, boolean useThreatMemory) {

        int startAreaId = Main.areaDataArray[start.getX()][start.getY()];
        int cpData[] = new int[cp.getGeometry().size()];
        ArrayList<WalkPosition> path = new ArrayList<>();

        int i =0;
        for (WalkPosition wp : cp.getGeometry()) {
            cpData[i++] = Main.areaDataArray[wp.getX()][wp.getY()];
            Main.areaDataArray[wp.getX()][wp.getY()] = startAreaId;
        }


        WalkPosition end = pathExistsInArea(start, cp.getGeometry(), true, useActiveThreatMap, useThreatMemory, startAreaId);
        if (end != null) {
            path = findUnthreatenedPathInAreaJPS(start, end, true, useActiveThreatMap, useThreatMemory, startAreaId);
        }

        i =0;
        for (Integer id : cpData) {
            Main.areaDataArray[cp.getGeometry().get(i).getX()][cp.getGeometry().get(i).getY()] = id;
            i++;
        }
        return  path;
    }
    */

    public static ArrayList<WalkPosition> findAnyPathInArea(ChokePoint cp1, ChokePoint cp2, boolean useActiveThreatMap, boolean useThreatMemory) {
        int areaId = findCommonAreaId(cp1, cp2);
        ArrayList<WalkPosition> path = new ArrayList<>();
        if (areaId == -1 ) {
            return path;
        }

        WalkPosition start = null;
        WalkPosition end = null;

        int cp1Data[] = new int[cp1.getGeometry().size()];
        int startAreaId =  Main.areaDataArray [cp1.getGeometry().get(0).getX()][cp1.getGeometry().get(0).getY()];
        int i =0;
        for (WalkPosition wp : cp1.getGeometry()) {
            cp1Data[i++] = Main.areaDataArray[wp.getX()][wp.getY()];
            Main.areaDataArray[wp.getX()][wp.getY()] = startAreaId;
        }

        int cp2Data[] = new int[cp2.getGeometry().size()];
        i = 0;
        for (WalkPosition wp : cp2.getGeometry()) {
            cp2Data[i++] = Main.areaDataArray[wp.getX()][wp.getY()];
            Main.areaDataArray[wp.getX()][wp.getY()] = startAreaId;
        }

        ////
        for (WalkPosition wp : cp1.getGeometry()) {
            ArrayList<WalkPosition> unthreatenedPathInAreaJPS = new ArrayList<>();
            for (WalkPosition wp2 : cp2.getGeometry()) {
                 unthreatenedPathInAreaJPS = findUnthreatenedPathInAreaJPS(wp, wp2, true, useActiveThreatMap, useThreatMemory, areaId);
                if (unthreatenedPathInAreaJPS.size() > 0) {
                    path = unthreatenedPathInAreaJPS;
                    break;
                }
            }
            if (unthreatenedPathInAreaJPS.size() > 0) {
                break;
            }
        }



        i =0;
        for (Integer id : cp1Data) {
            Main.areaDataArray[cp1.getGeometry().get(i).getX()][cp1.getGeometry().get(i).getY()] = id;
            i++;
        }

        i=0;
        for (Integer id : cp2Data) {
            Main.areaDataArray[cp2.getGeometry().get(i).getX()][cp2.getGeometry().get(i).getY()] = id;
            i++;
        }

        return  path;
        ///

        //return findUnthreatenedPathInAreaJPS(start, end, true, useActiveThreatMap, useThreatMemory, areaId);
    }


    //WIP
    public static ArrayList<WalkPosition> findUnthreatenedPathInAreaJPS(WalkPosition start, WalkPosition end, boolean ground, boolean useActiveThreatMap, boolean useThreatMemory, int areaId) {
        boolean foundPath = false;
        PriorityQueue<JPSInfo> straight = new PriorityQueue<>(new JPSInfoComparator());
        PriorityQueue<JPSInfo> diag = new PriorityQueue<>(new JPSInfoComparator());
        JPSInfo startJPSInfo = new JPSInfo(null, start, 0, null);

        JPSInfo endJPSInfo = null;
        Set<JPSInfo> straightJPSInfos = getJPSInfosInDirection(startJPSInfo, start, start, end, ground, useActiveThreatMap, useThreatMemory, Direction.E, Direction.W, Direction.S, Direction.N);
        straight.addAll(straightJPSInfos);

        Set<JPSInfo> diagJPSInfos = getJPSInfosInDirection(startJPSInfo, start, start, end, ground, useActiveThreatMap, useThreatMemory, Direction.NE, Direction.NW, Direction.SE, Direction.SW);
        diag.addAll(diagJPSInfos);

        HashSet<JPSInfo> processed = new HashSet<>();
        processed.addAll(straightJPSInfos);
        processed.addAll(diagJPSInfos);

        if (isUnderThreat(ground, start, useActiveThreatMap, useThreatMemory) || isUnderThreat(ground, end, useActiveThreatMap, useThreatMemory)
                || !Main.bw.getBWMap().isValidPosition(start)
                || !Main.bw.getBWMap().isValidPosition(end)) {
            foundPath = true;
        }

        if (ground) {
            if (!isPassableGround(start) || !isPassableGround(end)) {
                foundPath = true;
            }
        }
        while (!foundPath && (!straight.isEmpty() || !diag.isEmpty())) {
            int sImp = Integer.MAX_VALUE;
            int dImp = Integer.MAX_VALUE;
            JPSInfo jumpPoint;
            boolean straightNext = false;
            if (!straight.isEmpty()) {
                sImp = straight.peek().getImportance();
            }
            if (!diag.isEmpty()) {
                dImp = diag.peek().getImportance();
            }
            if (sImp <= dImp) {
                straightNext = true;
            }

            if (straightNext) {
                jumpPoint = straight.poll();
                Direction dir = jumpPoint.getDirection();
                //Straight path processing
                boolean straightPathProcessed = false;
                WalkPosition current = jumpPoint.getWalkPosition();
                WalkPosition ahead = getNeighborInDirection(current, jumpPoint.getDirection());
                while (!straightPathProcessed) {
                    //Terminate search if the next tile in the direction is under threat/impassable
                    if (isUnderThreat(ground, ahead, useActiveThreatMap, useThreatMemory) || !Main.bw.getBWMap().isValidPosition(ahead) || (ground && !isPassableGround(ahead)) && !isWalkPositionInArea(ahead, areaId)) {
                        straightPathProcessed = true;
                    }
                    if (ahead.equals(end)) {
                        straightPathProcessed = true;
                        foundPath = true;
                        endJPSInfo = new JPSInfo(null, ahead, 0, jumpPoint);
                        break;
                    }
                    //Check neighbors to the left and right
                    HashSet<Direction> checkDirs = straightCheckPos.get(jumpPoint.getDirection());
                    for (Direction checkDir : checkDirs) {
                        WalkPosition straightNeighbor = getNeighborInDirection(current, checkDir);
                        if (Main.bw.getBWMap().isValidPosition(straightNeighbor)) {
                            if (isUnderThreat(ground, straightNeighbor, useActiveThreatMap, useThreatMemory) || (ground && !isPassableGround(straightNeighbor)) || !isWalkPositionInArea(straightNeighbor, areaId)) {
                                WalkPosition diagWP = getNeighborInDirection(current, getJPDirections(jumpPoint.getDirection(), checkDir).iterator().next());
                                if (Main.bw.getBWMap().isValidPosition(diagWP) && !isUnderThreat(ground, diagWP, useActiveThreatMap, useThreatMemory) && isPassableGround(diagWP) && isWalkPositionInArea(diagWP, areaId)) {
                                    Direction jpsDir = getJPDirections(jumpPoint.getDirection(), checkDir).iterator().next();
                                    JPSInfo jpsInfo = new JPSInfo(jpsDir, getNeighborInDirection(current, jpsDir), calcJPSImportance(diagWP, start, end, jumpPoint.getGeneration(), jpsDir), jumpPoint);
                                    //if (!processed.contains(jpsInfo)) {
                                        diag.add(jpsInfo);
                                      //  processed.add(jpsInfo);
                                    //}
                                }
                            }
                        }
                    }
                        current = ahead;
                        ahead = getNeighborInDirection(ahead, jumpPoint.getDirection());
                }
            }
           else {
                jumpPoint = diag.poll();
                if (jumpPoint.getWalkPosition().equals(end)) {
                    foundPath = true;
                    endJPSInfo = new JPSInfo(null, jumpPoint.getWalkPosition(), 0, jumpPoint);
                    break;
                } else {
                    WalkPosition diagAhead = getNeighborInDirection(jumpPoint.getWalkPosition(), jumpPoint.getDirection());
                    if (jumpPoint.getWalkPosition().equals(end)) {
                        foundPath = true;
                        endJPSInfo = new JPSInfo(null, diagAhead, 0, jumpPoint);
                        break;
                    }

                    //If the next tile in the diagonal direction isn't blocked, let's add that too
                    if (!isUnderThreat(ground, diagAhead, useActiveThreatMap, useThreatMemory) && Main.bw.getBWMap().isValidPosition(diagAhead) && isWalkPositionInArea(diagAhead, areaId)) {
                        JPSInfo jpsInfo = null;
                        if (Main.bw.getBWMap().isValidPosition(diagAhead)) {
                            if (ground) {
                                if (isPassableGround(diagAhead)) {
                                    jpsInfo = new JPSInfo(jumpPoint.getDirection(), diagAhead, calcJPSImportance(diagAhead, start, end, jumpPoint.getGeneration(), jumpPoint.getDirection()), jumpPoint);
                                }
                            } else {
                                jpsInfo = new JPSInfo(jumpPoint.getDirection(), diagAhead, calcJPSImportance(diagAhead, start, end, jumpPoint.getGeneration(), jumpPoint.getDirection()), jumpPoint);
                            }
                        }
                        if (jpsInfo != null) {
                        //    if (!processed.contains(jpsInfo)) {
                                diag.add(jpsInfo);
                          //      processed.add(jpsInfo);
                           // }
                        }
                    }
                    //Check the 2 straight jump points in any case
                    for (Direction dir : diagForwardPos.get(jumpPoint.getDirection())) {
                        WalkPosition neighbor = getNeighborInDirection(jumpPoint.getWalkPosition(), dir);
                        Set<JPSInfo> jpsInfosInDirection;
                        if (!isUnderThreat(ground, neighbor, useActiveThreatMap, useThreatMemory) && isWalkPositionInArea(neighbor, areaId))
                            if (ground) {
                                if (isPassableGround(neighbor)) {
                                    jpsInfosInDirection = getJPSInfosInDirection(jumpPoint, jumpPoint.getWalkPosition(), start, end, ground, useActiveThreatMap, useThreatMemory, dir);
                                    for (JPSInfo j : jpsInfosInDirection) {
                                     //   if (!processed.contains(j)) {
                                            straight.addAll(jpsInfosInDirection);
                                       //     processed.addAll(jpsInfosInDirection);
                                        //}
                                    }
                                }
                            } else {
                                jpsInfosInDirection = getJPSInfosInDirection(jumpPoint, jumpPoint.getWalkPosition(), start, end, ground, useActiveThreatMap, useThreatMemory, dir);
                                for (JPSInfo j : jpsInfosInDirection) {
                                //    if (!processed.contains(j)) {
                                        straight.addAll(jpsInfosInDirection);
                                  //      processed.addAll(jpsInfosInDirection);
                                   // }
                                }
                            }
                    }
                    //Check the two remaining straight directions
                    for (Direction checkDir : diagCheckPos.get(jumpPoint.getDirection())) {
                        WalkPosition wp = getNeighborInDirection(diagAhead, checkDir);
                        if (Main.bw.getBWMap().isValidPosition(wp) && isUnderThreat(ground, wp, useActiveThreatMap, useThreatMemory)) {
                            Set<JPSInfo> jpsInfosInDirection = getJPSInfosInDirection(jumpPoint, wp, start, end, ground, useActiveThreatMap, useThreatMemory, getJPDirections(jumpPoint.getDirection(), checkDir));
                            for (JPSInfo j : jpsInfosInDirection) {
                                if (isWalkPositionInArea(j.getWalkPosition(), areaId)) {
                                    if (ground) {
                                        if (isPassableGround(j.getWalkPosition())) {
                                    //        if (!processed.contains(j)) {
                                                diag.add(j);
                                      //          processed.add(j);
                                        //    }
                                        }
                                    } else {
                                        //if (!processed.contains(j)) {
                                            diag.add(j);
                                          //  processed.add(j);
                                        //}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ArrayList<WalkPosition> patherino = new ArrayList<>();
        JPSInfo precc;

        if (endJPSInfo != null) {
            patherino.add(end);
            precc = endJPSInfo.getPrecursor();
            while (precc != null) {
                patherino.add(precc.getWalkPosition());
                precc = precc.getPrecursor();
            }
        }
        return patherino;
    }




    public static ArrayList<WalkPosition> findUnthreatenedPathInAreaJPS2(WalkPosition start, WalkPosition end, boolean ground, boolean useActiveThreatMap, boolean useThreatMemory, int areaId) {
        boolean foundPath = false;
        PriorityQueue<JPSInfo> straight = new PriorityQueue<>(new JPSInfoComparator());
        PriorityQueue<JPSInfo> diag = new PriorityQueue<>(new JPSInfoComparator());
        JPSInfo startJPSInfo = new JPSInfo(null, start, 0, null);

        JPSInfo endJPSInfo = null;
        Set<JPSInfo> straightJPSInfos = getJPSInfosInDirection(startJPSInfo, start, start, end, ground, useActiveThreatMap, useThreatMemory, Direction.E, Direction.W, Direction.S, Direction.N);
        straight.addAll(straightJPSInfos);

        Set<JPSInfo> diagJPSInfos = getJPSInfosInDirection(startJPSInfo, start, start, end, ground, useActiveThreatMap, useThreatMemory, Direction.NE, Direction.NW, Direction.SE, Direction.SW);
        diag.addAll(diagJPSInfos);

        HashSet<JPSInfo> processed = new HashSet<>();
        processed.addAll(straightJPSInfos);
        processed.addAll(diagJPSInfos);

        if (isUnderThreat(ground, start, useActiveThreatMap, useThreatMemory) || isUnderThreat(ground, end, useActiveThreatMap, useThreatMemory)
                || !Main.bw.getBWMap().isValidPosition(start)
                || !Main.bw.getBWMap().isValidPosition(end)) {
            foundPath = true;
        }

        if (ground) {
            if (!isPassableGround(start) || !isPassableGround(end)) {
                foundPath = true;
            }
        }
        while (!foundPath && (!straight.isEmpty() || !diag.isEmpty())) {
            int sImp = Integer.MAX_VALUE;
            int dImp = Integer.MAX_VALUE;
            JPSInfo jumpPoint;
            boolean straightNext = false;
            if (!straight.isEmpty()) {
                sImp = straight.peek().getImportance();
            }
            if (!diag.isEmpty()) {
                dImp = diag.peek().getImportance();
            }
            if (sImp <= dImp) {
                straightNext = true;
            }

            if (straightNext) {
                jumpPoint = straight.poll();
                Direction dir = jumpPoint.getDirection();
                //Straight path processing
                boolean straightPathProcessed = false;
                WalkPosition current = jumpPoint.getWalkPosition();
                WalkPosition ahead = getNeighborInDirection(current, jumpPoint.getDirection());
                while (!straightPathProcessed) {
                    //Terminate search if the next tile in the direction is under threat/impassable
                    if (isUnderThreat(ground, ahead, useActiveThreatMap, useThreatMemory) || !Main.bw.getBWMap().isValidPosition(ahead) || (ground && !isPassableGround(ahead)) && !isWalkPositionInArea(ahead, areaId)) {
                        straightPathProcessed = true;
                    }
                    if (ahead.equals(end)) {
                        straightPathProcessed = true;
                        foundPath = true;
                        endJPSInfo = new JPSInfo(null, ahead, 0, jumpPoint);
                        break;
                    }
                    //Check neighbors to the left and right
                    HashSet<Direction> checkDirs = straightCheckPos.get(jumpPoint.getDirection());
                    for (Direction checkDir : checkDirs) {
                        WalkPosition straightNeighbor = getNeighborInDirection(current, checkDir);
                        if (Main.bw.getBWMap().isValidPosition(straightNeighbor)) {
                            if (isUnderThreat(ground, straightNeighbor, useActiveThreatMap, useThreatMemory) || (ground && !isPassableGround(straightNeighbor)) || !isWalkPositionInArea(straightNeighbor, areaId)) {
                                WalkPosition diagWP = getNeighborInDirection(current, getJPDirections(jumpPoint.getDirection(), checkDir).iterator().next());
                                if (Main.bw.getBWMap().isValidPosition(diagWP) && !isUnderThreat(ground, diagWP, useActiveThreatMap, useThreatMemory) && isPassableGround(diagWP) && isWalkPositionInArea(diagWP, areaId)) {
                                    Direction jpsDir = getJPDirections(jumpPoint.getDirection(), checkDir).iterator().next();
                                    JPSInfo jpsInfo = new JPSInfo(jpsDir, getNeighborInDirection(current, jpsDir), calcJPSImportance(diagWP, start, end, jumpPoint.getGeneration(), jpsDir), jumpPoint);
                                    if (!processed.contains(jpsInfo)) {
                                    diag.add(jpsInfo);
                                     processed.add(jpsInfo);
                                    }
                                }
                            }
                        }
                    }
                    current = ahead;
                    ahead = getNeighborInDirection(ahead, jumpPoint.getDirection());
                }
            }
            else {
                jumpPoint = diag.poll();
                if (jumpPoint.getWalkPosition().equals(end)) {
                    foundPath = true;
                    endJPSInfo = new JPSInfo(null, jumpPoint.getWalkPosition(), 0, jumpPoint);
                    break;
                } else {
                    WalkPosition diagAhead = getNeighborInDirection(jumpPoint.getWalkPosition(), jumpPoint.getDirection());
                    if (diagAhead.equals(end)) {
                        foundPath = true;
                        endJPSInfo = new JPSInfo(null, diagAhead, 0, jumpPoint);
                        break;
                    }

                    //If the next tile in the diagonal direction isn't blocked, let's add that too
                    if (!isUnderThreat(ground, diagAhead, useActiveThreatMap, useThreatMemory) && Main.bw.getBWMap().isValidPosition(diagAhead) && isWalkPositionInArea(diagAhead, areaId)) {
                        JPSInfo jpsInfo = null;
                        if (Main.bw.getBWMap().isValidPosition(diagAhead)) {
                            if (ground) {
                                if (isPassableGround(diagAhead)) {
                                    jpsInfo = new JPSInfo(jumpPoint.getDirection(), diagAhead, calcJPSImportance(diagAhead, start, end, jumpPoint.getGeneration(), jumpPoint.getDirection()), jumpPoint);
                                }
                            } else {
                                jpsInfo = new JPSInfo(jumpPoint.getDirection(), diagAhead, calcJPSImportance(diagAhead, start, end, jumpPoint.getGeneration(), jumpPoint.getDirection()), jumpPoint);
                            }
                        }
                        if (jpsInfo != null) {
                                if (!processed.contains(jpsInfo)) {
                            diag.add(jpsInfo);
                                  processed.add(jpsInfo);
                             }
                        }
                    }
                    //Check the 2 straight jump points in any case
                    for (Direction dir : diagForwardPos.get(jumpPoint.getDirection())) {
                        WalkPosition neighbor = getNeighborInDirection(jumpPoint.getWalkPosition(), dir);
                       // Set<JPSInfo> jpsInfosInDirection;
                        if (!isUnderThreat(ground, neighbor, useActiveThreatMap, useThreatMemory) && isWalkPositionInArea(neighbor, areaId))
                            if (ground) {
                                if (isPassableGround(neighbor)) {
                                        JPSInfo[] jpsInfosArrayInDirection = getJPSInfosArrayInDirection(jumpPoint, jumpPoint.getWalkPosition(), start, end, ground, useActiveThreatMap, useThreatMemory, dir);
                                        for (int j =0; j<jpsInfosArrayInDirection.length;j++){
                                            straight.add(jpsInfosArrayInDirection[j]);
                                        }
                                }
                            } else {
                                JPSInfo[] jpsInfosArrayInDirection = getJPSInfosArrayInDirection(jumpPoint, jumpPoint.getWalkPosition(), start, end, ground, useActiveThreatMap, useThreatMemory, dir);
                                for (int j =0; j<jpsInfosArrayInDirection.length;j++) {
                                    straight.add(jpsInfosArrayInDirection[j]);
                                }
                            }
                    }
                    //Check the two remaining straight directions
                    for (Direction checkDir : diagCheckPos.get(jumpPoint.getDirection())) {
                        WalkPosition wp = getNeighborInDirection(diagAhead, checkDir);
                        if (Main.bw.getBWMap().isValidPosition(wp) && isUnderThreat(ground, wp, useActiveThreatMap, useThreatMemory)) {
                           // Set<JPSInfo> jpsInfosInDirection = getJPSInfosInDirection(jumpPoint, wp, start, end, ground, useActiveThreatMap, useThreatMemory, getJPDirections(jumpPoint.getDirection(), checkDir));
                            Direction[] dirs = (Direction[]) getJPDirections(jumpPoint.getDirection(), checkDir).toArray();
                            JPSInfo[] jpsInfosArrayInDirection = getJPSInfosArrayInDirection(jumpPoint, wp, start, end, ground, useActiveThreatMap, useThreatMemory, dirs);
                            for (int j =0; j<jpsInfosArrayInDirection.length;j++) {
                                if (isWalkPositionInArea(jpsInfosArrayInDirection[j].getWalkPosition(), areaId)) {
                                    if (ground) {
                                        if (isPassableGround(jpsInfosArrayInDirection[j].getWalkPosition())) {
                                            diag.add(jpsInfosArrayInDirection[j]);
                                        }
                                    } else {
                                        diag.add(jpsInfosArrayInDirection[j]);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ArrayList<WalkPosition> patherino = new ArrayList<>();
        JPSInfo precc;

        if (endJPSInfo != null) {
            patherino.add(end);
            precc = endJPSInfo.getPrecursor();
            while (precc != null) {
                patherino.add(precc.getWalkPosition());
                precc = precc.getPrecursor();
            }
        }
        return patherino;
    }

    public static boolean isWalkPositionInArea(WalkPosition wp, int areaId) {
        if (wp.getX() <= 0 || wp.getY() <= 0) {
            return false;
        }
        if (Main.areaDataArray[wp.getX()][wp.getY()] == areaId) {
            return true;
        }
        return false;
    }

    public static int getPathLength(ArrayList<WalkPosition> path) {
        int sumLength = 0;
        for (int i=1; i<path.size(); i++) {
            sumLength = sumLength + getDistanceFastSqrt(path.get(i), path.get(i-1));
        }
        return sumLength;
    }


    //Find the WalkPosition, where the shortest unthreatened route leads from the start position
    public static WalkPosition getApproachPoint(WalkPosition start, WalkPosition end, boolean ground, boolean useActiveThreatMap, boolean useThreatMemory, int areaId) {
        //getWalkPositionsInGridRadius
        boolean inArea = false;
        if (areaId > 0) {
            inArea = true;
        }
        int maxDist = getDistanceFastSqrt(start, end);
        WalkPosition approachPoint = null;
        int currentDist = 1;
        while (approachPoint == null && currentDist < maxDist ) {
            Set<WalkPosition> positions = getWalkPositionsInGridRadius(start, currentDist);
            int minPathLength = Integer.MAX_VALUE;
            for (WalkPosition wp : positions) {
                if (isWalkPositionOnTheMap(wp.getX(), wp.getY())
                        && !isUnderThreat(ground, wp, useActiveThreatMap, useThreatMemory)
                        && (!inArea || Main.areaDataArray[wp.getX()][wp.getY()] == areaId)) {
                    int pathLength;
                    if (inArea) {
                        pathLength = getPathLength(findUnthreatenedPathInAreaJPS(start, wp, ground, useActiveThreatMap, useThreatMemory, areaId));
                    } else {
                        pathLength = getPathLength(findUnthreatenedPathJPS(start, wp, ground, useActiveThreatMap, useThreatMemory));
                    }
                    if (pathLength < minPathLength) {
                        minPathLength = pathLength;
                        approachPoint = wp;
                    }
                }
            }
            currentDist++;
        }
        return approachPoint;
    }

    public static int getDistanceFastSqrt(WalkPosition wp1, WalkPosition wp2) {
        if (wp1 != null && wp2 != null) {
            return FastIntSqrt.fastSqrt(positionToPositionDistanceSq(wp1.getX(), wp1.getY(), wp2.getX(), wp2.getY()));
        } else {
            return 0;
        }
    }

}





