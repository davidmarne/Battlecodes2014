package safebot;

import battlecode.common.*;

import java.util.Random;
import java.util.ArrayList;

public class RobotPlayer {
    static int mapWidth;
    static int mapHeight;
	static Random rand;
    static RobotController rc;
    static double cowCoords[][];
    static int map[][];
    static MapLocation maxCows = null;
    static Boolean firstRun = true;
    static ArrayList<MapLocation> corners= new ArrayList<MapLocation>();


    public static void run(RobotController rcIn) {
        rc = rcIn;
		rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        Boolean hasOrders = false;
        Boolean start = false;
        MapLocation corner = null;
        while(true) {
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() && rc.senseRobotCount() < 25) {
						Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) == null) {
							rc.spawn(toEnemy);
						}
					}

                    if(firstRun) {
                        rc.broadcast(0, 0);
                        firstRun = false;
                        drawMap();
                        locateCorners();
//                        System.out.println("write: " + corners.size());
                        rc.broadcast(0, corners.size());
                        for(int i = 0; i < corners.size(); i++) {
                            rc.broadcast(i + 1, mapLocToInt(corners.get(i)));
                        }
                    }
//                    rc.setIndicatorString(1, "" + rc.senseTerrainTile(new MapLocation(10, 10)).ordinal());
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
			
			if (rc.getType() == RobotType.SOLDIER) {
				try {
                    // wait for HQ to broadcast positions of corners, then start
                    if (!start) {
//                        System.out.println( "read: " + rc.readBroadcast(0));
                        if(rc.readBroadcast(0) != 0) {
                            for(int i = 0; i < rc.readBroadcast(0); i++) {
                                corners.add(intToMapLoc(rc.readBroadcast(i + 1)));
                            }
                            start = true;
                        }
                    } else {
                        if (rc.isActive()) {
                            if(corners.size() > 0 && !hasOrders) {
//                                System.out.println((rc.getRobot().getID()*rand.nextInt(corners.size()))%corners.size());
                                corner = corners.remove((rc.getRobot().getID()*rand.nextInt(corners.size()))%(corners.size() - 3));
                                hasOrders = true;
                            }
                            if(hasOrders) {
                                Direction dir = rc.getLocation().directionTo(corner);
                                if(rc.getLocation().x != corner.x || rc.getLocation().y != corner.y) {
                                    if (rc.canMove(dir)) {
                                        rc.move(dir);
                                    }
                                } else {
                                    corners.add(corner);
                                    hasOrders = false;
                                    rc.construct(RobotType.PASTR);
                                }
                            }
                        }
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			rc.yield();
		}
	}

    public static MapLocation findMaxCowLocation() {
        cowCoords = rc.senseCowGrowth();
        int xLoc = 0;
        int yLoc = 0;
        // if the team is A - top left team
        if(rc.getTeam() == Team.A) {
            int iLength = cowCoords.length;
            int jLength = cowCoords[0].length;
            double maxCows = 0.0;
            for(int i = 0; i < iLength; i++) {
                for(int j = 0; j < jLength; j++) {
                    if(maxCows < cowCoords[i][j]) {
                        maxCows = cowCoords[i][j];
                        xLoc = i;
                        yLoc = j;
                    }
                }
            }
        } else {
            int iLength = cowCoords.length;
            int jLength = cowCoords[0].length;
            double maxCows = 0.0;
            for(int i = iLength; i >=0 ; i--) {
                for(int j = jLength; j >= 0 ; j--) {
                    if(maxCows < cowCoords[i][j]) {
                        maxCows = cowCoords[i][j];
                        xLoc = i;
                        yLoc = j;
                    }
                }
            }
        }

        return new MapLocation(xLoc, yLoc);
    }

    public static void drawMap() {
        mapWidth = rc.getMapWidth();
        int ogMapWidth = mapWidth;
        mapHeight = rc.getMapHeight();
        map = new int[mapWidth + 2][mapHeight + 2];
        for (int i = 0; i < mapHeight + 2; i++) {
            for (int j = 0; j < mapWidth + 2; j++) {
                int tile;
                if(i == 0 || j == 0 || i == ogMapWidth + 1 || j == mapHeight + 1) { // pad the map to easily find corners
                    tile = 2;
                } else {
                    tile = rc.senseTerrainTile(new MapLocation(j - 1, i - 1)).ordinal();
                }
                map[i][j] = tile > 2 ? 2 : tile;
                map[ogMapWidth + 1 - i][mapHeight + 1 - j] = tile > 2 ? 2 : tile;
            }
            mapWidth--;
        }
    }

    public static void locateCorners() {
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        //start at (1, 1)
        for (int i = 1; i < mapWidth + 1; i++) {
            for (int j = 1; j < mapHeight + 1; j++) {
                // place a pasture at the location, P, for best coverage of the corner
                // x x x x x
                // x - - - -
                // x - P - -
                // x - - - -
                // x - - - -
                if (map[i][j] != 2) {
                    if (map[i-1][j-1] == 2 && map[i][j-1] == 2 && map[i-1][j] == 2) {           // top left corner
                        corners.add(new MapLocation((j - 1) + 1, (i - 1) + 1));
                        map[i+2][j+2] = 3;
//                        System.out.println("adding corner from 1: " + "(" + (j - 1 + 2) + ", " + (i - 1 + 2) + ")");
                    } else if (map[i+1][j-1] == 2 && map[i][j-1] == 2 && map[i+1][j] == 2) {    // top right corner
                        corners.add(new MapLocation((j - 1) + 1, (i - 1) - 1));
                        map[i-2][j+2] = 3;
//                        System.out.println("adding corner from 2: " + "(" + (j - 1 + 2) + ", " + (i - 1 - 2) + ")");
                    } else if (map[i+1][j+1] == 2 && map[i][j+1] == 2 && map[i+1][j] == 2) {    // bottom right corner
                        corners.add(new MapLocation((j - 1) - 1, (i - 1) - 1));
                        map[i-2][j-2] = 3;
//                        System.out.println("adding corner from 3: " + "(" + (j - 1 - 2) + ", " + (i - 1 - 2) + ")");
                    } else if (map[i-1][j+1] == 2 && map[i][j+1] == 2 && map[i-1][j] == 2) {    // bottom left corner
                        corners.add(new MapLocation((j - 1) - 1, (i - 1) + 1));
                        map[i+2][j-2] = 3;
//                        System.out.println("adding corner from 4: " + "(" + (j - 1 - 2) + ", " + (i - 1 + 2) + ")");
                    }
                }
            }
        }
//        logMap();
    }

    public static void logMap() {
        System.out.print("\n");
        for (int i = 1; i < mapWidth + 1; i++) {
            for (int j = 1; j < mapHeight + 1; j++) {
                System.out.print(map[i][j] + " ");
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
}
