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
    static int cornersMap[][];
    static MapLocation maxCows = null;
    static Boolean firstRun = true;
    static ArrayList<MapLocation> corners= new ArrayList<MapLocation>();
    static ArrayList<Direction> path = new ArrayList<Direction>();


    public static void run(RobotController rcIn) {
        rc = rcIn;
		rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        Boolean hasOrders = false;
        Boolean start = false;
        MapLocation corner = null;
        map = new int[rc.getMapWidth()][rc.getMapHeight()];
        cornersMap = new int [rc.getMapWidth() + 2][rc.getMapHeight() + 2];
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
                        RobotUtil.assessMap(rc, map, cornersMap);
                        // you can't move through a HQ... duh
                        cornersMap[rc.senseHQLocation().x][rc.senseHQLocation().y] = 2;
                        cornersMap[rc.senseEnemyHQLocation().x][rc.senseEnemyHQLocation().y] = 2;

                        locateCorners();
//                        RobotUtil.logMap(cornersMap);
                        rc.broadcast(0, corners.size());
                        for(int i = 0; i < corners.size(); i++) {
                            rc.broadcast(i + 1, RobotUtil.mapLocToInt(corners.get(i)));
                        }
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
			
			if (rc.getType() == RobotType.SOLDIER) {
				try {
                    if(firstRun) {
                        firstRun = false;
                        RobotUtil.assessMap(rc, map, cornersMap);
                        cornersMap[rc.senseHQLocation().x+1][rc.senseHQLocation().y+1] = 2;
                        cornersMap[rc.senseEnemyHQLocation().x+1][rc.senseEnemyHQLocation().y+1] = 2;
                    }
                    // wait for HQ to broadcast positions of corners, then start
                    if (!start) {
                        if(rc.readBroadcast(0) != 0) {
                            for(int i = 0; i < rc.readBroadcast(0); i++) {
                                corners.add(RobotUtil.intToMapLoc(rc.readBroadcast(i + 1)));
                            }
                            start = true;
                        }
                    } else {
                        if (rc.isActive()) {
                            if(corners.size() > 0 && !hasOrders) {
                                corner = corners.get((rc.getRobot().getID() * rand.nextInt(corners.size())) % (corners.size() - 3));
                                path = RobotUtil.bugPath(rc.getLocation(), corner, cornersMap);
                                hasOrders = true;
                            }
                            if(hasOrders) {
                                if(rc.getLocation().x != corner.x || rc.getLocation().y != corner.y) {
                                    Direction dir = path.get(0);
                                    if (rc.canMove(dir)) {
                                        path.remove(0);
                                        rc.move(dir);
                                    }
                                } else {
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
                if (cornersMap[i][j] != 2) {
                    if (cornersMap[i-1][j-1] == 2 && cornersMap[i][j-1] == 2 && cornersMap[i-1][j] == 2) {           // top left corner
                        corners.add(new MapLocation(i + 1 - 1, j + 1 - 1));
                        cornersMap[i+1][j+1] = 4;
//                        System.out.println("adding corner from 1: " + "(" + (i + 1) + ", " + (j + 1) + ")");
                    } else if (cornersMap[i+1][j-1] == 2 && cornersMap[i][j-1] == 2 && cornersMap[i+1][j] == 2) {    // top right corner
                        corners.add(new MapLocation(i - 1 - 1, j + 1 - 1));
                        cornersMap[i-1][j+1] = 4;
//                        System.out.println("adding corner from 2: " + "(" + (i - 1) + ", " + (j + 1) + ")");
                    } else if (cornersMap[i+1][j+1] == 2 && cornersMap[i][j+1] == 2 && cornersMap[i+1][j] == 2) {    // bottom right corner
                        corners.add(new MapLocation(i - 1 - 1, j - 1 - 1));
                        cornersMap[i-1][j-1] = 4;
//                        System.out.println("adding corner from 3: " + "(" + (i - 1) + ", " + (j - 1) + ")");
                    } else if (cornersMap[i-1][j+1] == 2 && cornersMap[i][j+1] == 2 && cornersMap[i-1][j] == 2) {    // bottom left corner
                        corners.add(new MapLocation(i + 1 - 1, j - 1 - 1));
                        cornersMap[i+1][j-1] = 4;
//                        System.out.println("adding corner from 4: " + "(" + (i + 1) + ", " + (j - 1) + ")");
                    }
                }
            }
        }
//        logMap();
    }
}
