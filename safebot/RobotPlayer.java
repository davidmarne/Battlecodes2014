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
    static ArrayList<Direction> path = new ArrayList<Direction>();
    static int numPASTRs = 0;
    static int guardedPASTRs = 0;
    public static enum orderTypes{
        makePasture, guardPasture;
    }
    static orderTypes orderType;
    static int channelPASTRS = 1000;
    static int channelPASTRMaker = 2000;
    static int channelPASTRGuarder = 2001;
    


    public static void run(RobotController rcIn) {
        rc = rcIn;
		rand = new Random();
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        Boolean hasOrders = false;
        Boolean start = false;
        MapLocation corner = null;
        MapLocation guardLocation = null;
        map = new int [rc.getMapWidth() + 2][rc.getMapHeight() + 2];
        while(true) {
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() && rc.senseRobotCount() < 25) {
						Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) == null) {
							rc.spawn(toEnemy);
						}

                        if(rc.readBroadcast(100) > numPASTRs) {
                            int numOfNewPASTRs = rc.readBroadcast(100);
                            for(int i = numPASTRs + 1; i <= numOfNewPASTRs; i++){
                                int newPASTRLocInt = rc.readBroadcast(100+i);
                                MapLocation newPASTRLoc = RobotUtil.intToMapLoc(newPASTRLocInt);
                                map[newPASTRLoc.x+1][newPASTRLoc.y+1] = 2;
                            }
                            numPASTRs = numOfNewPASTRs;
                            System.out.println("NUM PASTRS: " + numPASTRs);
                        }
                        
					}

                    if(firstRun) {
                        rc.broadcast(0, 0);
                        firstRun = false;
                        RobotUtil.assessMap(rc, map);
                        // you can't move through a HQ... duh
                        map[rc.senseHQLocation().x+1][rc.senseHQLocation().y+1] = 2;
                        map[rc.senseEnemyHQLocation().x+1][rc.senseEnemyHQLocation().y+1] = 2;

                        locateCorners();
                        RobotUtil.logMap(map);
                        for(int i = 0; i < corners.size(); i++) {
                            rc.broadcast(i + 1, RobotUtil.mapLocToInt(corners.get(i)));
                        }
                        rc.broadcast(0, corners.size());
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
            } else if (rc.getType() == RobotType.SOLDIER) {
				try {
                    //if its the first run initialize the map
                    if(firstRun) {
                        firstRun = false;
                        RobotUtil.assessMap(rc, map);
                        map[rc.senseHQLocation().x+1][rc.senseHQLocation().y+1] = 2;
                        map[rc.senseEnemyHQLocation().x+1][rc.senseEnemyHQLocation().y+1] = 2;
                    }
                    // read location of newest PASTR and update map
                    if(rc.readBroadcast(100) > numPASTRs) {
                        int numOfNewPASTRs = rc.readBroadcast(100);
                        for(int i = numPASTRs + 1; i <= numOfNewPASTRs; i++){
                            int newPASTRLocInt = rc.readBroadcast(100+i);
                            MapLocation newPASTRLoc = RobotUtil.intToMapLoc(newPASTRLocInt);
                            map[newPASTRLoc.x+1][newPASTRLoc.y+1] = 2;
                        }
                        numPASTRs = numOfNewPASTRs;
                    }
                    // wait for HQ to broadcast positions of corners, then start
                    if (!start) {
                        if(rc.readBroadcast(0) != 0) {
                            for(int i = 0; i < rc.readBroadcast(0); i++) {
                                corners.add(RobotUtil.intToMapLoc(rc.readBroadcast(i + 1)));
                            }
                            start = true;
                        }
                    } else {//Initialization is finished, execute strategies here
                        if (rc.isActive()) {
                            //make a makePasture bot
                            if(corners.size() > 0 && !hasOrders && rc.readBroadcast(2000) < corners.size()) {
                                corner = corners.get((rc.getRobot().getID() * rand.nextInt(corners.size())) % corners.size());
                                path = RobotUtil.bugPath(rc.getLocation(), corner, map);
                                hasOrders = true;
                                if(rc.getRobot().getID() == 1031){
                                	System.out.println("1031 Goal is "+corner + " path is of length " +path.size());
                                }
                                orderType = orderTypes.makePasture;
                                rc.broadcast(2000, rc.readBroadcast(2000) + 1);
                            }else if(rc.readBroadcast(100) > rc.readBroadcast(2001) && !hasOrders){//make a guard bot
                                guardLocation = RobotUtil.intToMapLoc(rc.readBroadcast(100+guardedPASTRs));
                                guardedPASTRs++;
                                path = RobotUtil.bugPath(rc.getLocation(), guardLocation, map);
                                hasOrders = true;
                                System.out.println("Guard Created");
                                orderType = orderTypes.guardPasture;
                                rc.broadcast(2001, rc.readBroadcast(2001) + 1);
                            }
                            //
                            if(hasOrders) {
                            	if(rc.getRobot().getID() == 1031){
                            		System.out.println(orderType);
                            	}
                                if (orderType == orderTypes.makePasture) {
                                	if(rc.getRobot().getID() == 1031){
                                		System.out.println(corner);
                                	}
                                    if (rc.getLocation().x != corner.x || rc.getLocation().y != corner.y) {
                                        Direction dir = path.get(0);
                                        if(rc.getRobot().getID() == 1031){
                                        	System.out.println("1031 "+ rc.getLocation().add(dir));
                                        }
                                        if (rc.canMove(dir)) {
                                            path.remove(0);
                                            rc.setIndicatorString(0, "" + dir);
                                            rc.setIndicatorString(1, "" + rc.getLocation());
                                            
                                            rc.move(dir);
                                        }
                                    } else {
                                        hasOrders = false;
                                        int totalPASTRs = rc.readBroadcast(100) + 1;
                                        rc.broadcast(100, totalPASTRs);
                                        rc.broadcast(100 + totalPASTRs, RobotUtil.mapLocToInt(rc.getLocation()));
                                        firstRun = true;
                                        rc.construct(RobotType.PASTR);
                                    }
                                }else if(orderType == orderTypes.guardPasture){
                                    if (rc.getLocation().distanceSquaredTo(guardLocation) < 4) {
                                    	System.out.println(rc.getRobot().getID() + " made it to its guard location");
                                        Direction dir = path.get(0);
                                        if (rc.canMove(dir)) {
                                            path.remove(0);
                                            rc.setIndicatorString(0, "" + dir);
                                            rc.setIndicatorString(1, "" + rc.getLocation());
                                            rc.move(dir);
                                        }
                                    } else {
                                        int caseNum = rand.nextInt() % 4;
                                        if(caseNum == 0){
                                        	MapLocation target = new MapLocation(guardLocation.x + 2, guardLocation.y +2);
                                        	if(rc.canAttackSquare(target)){
                                        		rc.attackSquare(target);
                                        	}
                                        }else if(caseNum == 1){
                                        	MapLocation target = new MapLocation(guardLocation.x, guardLocation.y +2);
                                        	if(rc.canAttackSquare(target)){
                                        		rc.attackSquare(target);
                                        	}
                                        }else if(caseNum == 2){
                                        	MapLocation target = new MapLocation(guardLocation.x + 2, guardLocation.y);
                                        	if(rc.canAttackSquare(target)){
                                        		rc.attackSquare(target);
                                        	}
                                        }else{
                                        	MapLocation target = new MapLocation(guardLocation.x , guardLocation.y);
                                        	if(rc.canAttackSquare(target)){
                                        		rc.attackSquare(target);
                                        	}
                                        }
                                    }
                                }
                            }
                        }
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
            }else if(rc.getType() == RobotType.PASTR){

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
                if (map[i][j] != 2) {
                    if (map[i-1][j-1] == 2 && map[i][j-1] == 2 && map[i-1][j] == 2) {           // top left corner
                        // don't place a corner outside of the map, or on the border
                        if(i + 1 <= mapWidth && j + 1 <= mapHeight) {
                            corners.add(new MapLocation(i + 1 - 1, j + 1 - 1));
                            map[i+1][j+1] = 4;
//                            System.out.println("adding corner from 1: " + "(" + (i + 1) + ", " + (j + 1) + ")");
                        }
                    } else if (map[i+1][j-1] == 2 && map[i][j-1] == 2 && map[i+1][j] == 2) {    // top right corner
                        // don't place a corner outside of the map, or on the border
                        if(i - 1 <= mapWidth && j + 1 <= mapHeight) {
                            corners.add(new MapLocation(i - 1 - 1, j + 1 - 1));
                            map[i-1][j+1] = 4;
//                            System.out.println("adding corner from 2: " + "(" + (i - 1) + ", " + (j + 1) + ")");
                        }
                    } else if (map[i+1][j+1] == 2 && map[i][j+1] == 2 && map[i+1][j] == 2) {    // bottom right corner
                        // don't place a corner outside of the map, or on the border
                        if(i - 1 <= mapWidth && j - 1 <= mapHeight) {
                            corners.add(new MapLocation(i - 1 - 1, j - 1 - 1));
                            map[i-1][j-1] = 4;
//                            System.out.println("adding corner from 3: " + "(" + (i - 1) + ", " + (j - 1) + ")");
                        }

                    } else if (map[i-1][j+1] == 2 && map[i][j+1] == 2 && map[i-1][j] == 2) {    // bottom left corner
                        // don't place a corner outside of the map, or on the border
                        if(i + 1 <= mapWidth && j - 1 <= mapHeight) {
                            corners.add(new MapLocation(i + 1 - 1, j - 1 - 1));
                            map[i+1][j-1] = 4;
//                            System.out.println("adding corner from 4: " + "(" + (i + 1) + ", " + (j - 1) + ")");
                        }
                    }
                }
            }
        }
//        logMap();
    }
}
