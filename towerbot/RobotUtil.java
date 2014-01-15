package towerbot;


import battlecode.common.*;

import java.util.ArrayList;

/**
 * Created by dominicfrost on 1/12/14.
 */
public class RobotUtil {

    public static void logMap(int[][] map) {
        int mapWidth = map.length;
        int mapHeight = map[0].length;
        System.out.print("\n");
        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapHeight; j++) {
                System.out.print(map[j][i] + " ");
            }
            System.out.print("\n");
        }
    }

    public static int mapLocToInt(MapLocation m){
        return (m.x*100 + m.y);
    }

    public static MapLocation intToMapLoc(int i){
        return new MapLocation(i/100,i%100);
    }

    public static int[][] assessMap(RobotController rc, int[][] map) {
        int mapWidth = rc.getMapWidth();
        int ogMapWidth = mapWidth;
        int mapHeight = rc.getMapHeight();
        for (int i = 0; i < mapHeight + 2; i++) {
            for (int j = 0; j < mapWidth + 2; j++) {
                int tile;
                if(i == 0 || j == 0 || i == ogMapWidth + 1 || j == mapHeight + 1) { // pad the map to easily find corners
                    tile = 2;
                    map[i][j] = tile;
                    map[ogMapWidth + 1 - i][mapHeight + 1 - j] = tile;
                } else {
                    tile = rc.senseTerrainTile(new MapLocation(i - 1, j - 1)).ordinal();
                    map[i][j] = tile > 2 ? 2 : tile;
                    map[ogMapWidth + 1 - i][mapHeight + 1 - j] = tile > 2 ? 2 : tile;
                }
                map[i][j] = tile > 2 ? 2 : tile;
                map[ogMapWidth + 1 - i][mapHeight + 1 - j] = tile > 2 ? 2 : tile;
            }
            mapWidth--;
        }
        return map;
    }

    public static ArrayList<Direction> bugPath(MapLocation start, MapLocation destination, int[][] map) throws GameActionException {
        Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        ArrayList<Direction> path = new ArrayList<Direction>();
        MapLocation currentLocation = start;
        MapLocation targetLocation;
        int currentDistance = currentLocation.distanceSquaredTo(destination);
        int shortestDistance = currentDistance;
        System.out.println("Start loc: (" + start.x + ", " + start.y + ")");
        System.out.println("Destination loc: (" + destination.x + ", " + destination.y + ")");
        int roundNum = Clock.getRoundNum();;
        while(true) {
            if (Clock.getRoundNum() - roundNum > 100) {return path;}
            // we are at out destination
            if(currentLocation.x == destination.x && currentLocation.y == destination.y) {
                break;
            }
            Direction dir = currentLocation.directionTo(destination);
            targetLocation = currentLocation.add(dir);
//            System.out.println("VAL: " + map[targetLocation.x][targetLocation.y] + "TargetLoc: " + targetLocation);
            // if we can move in the direction of the destination
            if(map[targetLocation.x+1][targetLocation.y+1] != 2 && map[targetLocation.x+1][targetLocation.y+1] != 3) {
                path.add(dir);
//                System.out.println("1: " + currentLocation + " -> " + dir + " -> " + targetLocation);
                currentLocation = targetLocation;
                currentDistance = currentLocation.distanceSquaredTo(destination);
                shortestDistance = currentDistance;

            // we can not move towards the destination because there is a wall
            } else {
                // location to the destination should not be a wall
                MapLocation temp = currentLocation.add(currentLocation.directionTo(destination));
                // we are not closer and the direction towards the destination is a wall
                while(currentDistance >= shortestDistance || (map[temp.x+1][temp.y+1] == 2 || map[temp.x+1][temp.y+1] == 3)) {
                    if (Clock.getRoundNum() - roundNum > 100) {return path;}
                    // while the targetLocation is a wall, rotate and update to new target location
                    // this deals with if we need to rotate left
                    int flag = 0;
                    while (map[targetLocation.x+1][targetLocation.y+1] == 2 || map[targetLocation.x+1][targetLocation.y+1] == 3) {
                        if (Clock.getRoundNum() - roundNum > 100) {return path;}
                        flag += 1;
                        dir = dir.rotateLeft();
//                        System.out.println("Rotate Left, DIR is now: " + dir);
                        targetLocation = currentLocation.add(dir);
                    }
                    // if the wall on our right disappears
                    // this deals with if we need to rotate right
                    MapLocation wallLocation = currentLocation.add(dir.rotateRight().rotateRight());
                    if (flag == 1) {
                        path.add(dir);
                        currentLocation = targetLocation;
                        currentDistance = currentLocation.distanceSquaredTo(destination);
                        break;
                    }
                    if(map[wallLocation.x+1][wallLocation.y+1] != 2 && map[wallLocation.x+1][wallLocation.y+1] != 3) {
                        dir = dir.rotateRight().rotateRight();
//                        System.out.println("Rotate Right x2, DIR is now: " + dir);
                        targetLocation = currentLocation.add(dir);
                    } else {
                        targetLocation = currentLocation.add(dir);
                    }
                    path.add(dir);
//                    System.out.println("2: " + currentLocation + " -> " + dir + " -> " + targetLocation);
                    currentLocation = targetLocation;
                    targetLocation = currentLocation.add(dir);
                    currentDistance = currentLocation.distanceSquaredTo(destination);
                    temp = currentLocation.add(currentLocation.directionTo(destination));
                }
            }
        }
        return path;
    }
}
