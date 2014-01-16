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
    static ArrayList<MapLocation> cornersAvailableToMake = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> cornersAvailableToGuard = new ArrayList<MapLocation>();
    static ArrayList<Direction> path = new ArrayList<Direction>();
    static int numPASTRs = 0;
    static int numAlreadySentToMake = 0;
    static int numAlreadySentToGuard = 0;
    public static enum orderTypes{
        makePasture, guardPasture;
    }
    static orderTypes orderType;
    static int channelPASTRS = 1000;
    static int channelSentPASTRs = 4000;
    static int channelSentGuard = 5000;
    


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
                                cornersAvailableToMake.add(RobotUtil.intToMapLoc(rc.readBroadcast(i + 1)));
                            }
                            start = true;
                        }
                    } else {//Initialization is finished, execute strategies here
                        if (rc.isActive()) {
                        	//updates available pasture locations
                        	if(numAlreadySentToMake < rc.readBroadcast(channelSentPASTRs)){
                        		for(int i = numAlreadySentToMake + 1; i <= rc.readBroadcast(channelSentPASTRs); i++){
                        			MapLocation sent = RobotUtil.intToMapLoc(rc.readBroadcast(channelSentPASTRs + i ));
                        			for(int j = 0; j < cornersAvailableToMake.size(); j++){
                        				if(sent.equals(cornersAvailableToMake.get(j))){
                        					cornersAvailableToGuard.add(cornersAvailableToMake.get(j));
                        					cornersAvailableToMake.remove(j);
                        					break;
                        				}
                        			}

								}
                        	}
                        	numAlreadySentToMake = rc.readBroadcast(channelSentPASTRs);
                        	
                        	if(numAlreadySentToGuard < rc.readBroadcast(channelSentGuard)){
                        		for(int i = numAlreadySentToGuard + 1; i <= rc.readBroadcast(channelSentGuard); i++){
                        			MapLocation sent = RobotUtil.intToMapLoc(rc.readBroadcast(channelSentGuard + i ));
                        			for(int j = 0; j < cornersAvailableToGuard.size(); j++){
                        				if(sent.equals(cornersAvailableToGuard.get(j))){
                        					cornersAvailableToGuard.remove(j);
                        					break;
                        				}
									}

								}
                        	}
                        	numAlreadySentToGuard = rc.readBroadcast(channelSentGuard);
                        	
                            //make a makePasture bot
                            if(cornersAvailableToMake.size() > 0 && !hasOrders) {
                            	//get a random corner and delete it from cornersAvailableToMake, find a path
                                corner = cornersAvailableToMake.remove((rc.getRobot().getID() * rand.nextInt(cornersAvailableToMake.size())) % cornersAvailableToMake.size());
                                path = RobotUtil.bugPath(rc.getLocation(), corner, map);
                                System.out.println(rc.getRobot().getID() + " is being sent to "+corner + " to make a PASTR");
                                //number of bots already sent to make a corner is incremented 
                                //and the corner is broadcasted so others know its already gonna be made a pastr
                                numAlreadySentToMake++;
                                rc.broadcast(channelSentPASTRs, numAlreadySentToMake);
                                rc.broadcast(channelSentPASTRs + numAlreadySentToMake, RobotUtil.mapLocToInt(corner));
                                hasOrders = true;
                                orderType = orderTypes.makePasture;;
                            }else if(cornersAvailableToGuard.size() > 0 && !hasOrders){//make a guard bot
                                guardLocation = cornersAvailableToGuard.remove(0);
                                path = RobotUtil.bugPath(rc.getLocation(), guardLocation, map);
                                System.out.println(rc.getRobot().getID() + " is being sent to "+ guardLocation + " to guard a PASTR");
                                //number of bots already sent to guard a corner is incremented 
                                //and the corner is broadcasted so others know its already gonna be guarded
                                numAlreadySentToGuard++;
                                rc.broadcast(channelSentGuard, numAlreadySentToGuard);
                                rc.broadcast(channelSentGuard + numAlreadySentToGuard, RobotUtil.mapLocToInt(guardLocation));
                                hasOrders = true;
                                orderType = orderTypes.guardPasture;
                            }
                            //
                            if(hasOrders) {
                            	
                                if (orderType == orderTypes.makePasture) {
                                	
                                    if (rc.getLocation().x != corner.x || rc.getLocation().y != corner.y) {
                                        Direction dir = path.get(0);
                                        
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
                                    if (rc.getLocation().distanceSquaredTo(guardLocation) > 9) {
                                        Direction dir = path.get(0);
                                        if (rc.canMove(dir)) {
                                            path.remove(0);
                                            rc.setIndicatorString(0, "" + dir);
                                            rc.setIndicatorString(1, "" + rc.getLocation());
                                            rc.move(dir);
                                        }
                                    } else {
                                    	//while(true){
                                    		int caseNum = rand.nextInt() % 8;
                                        	if(caseNum == 0){
                                        		MapLocation target = new MapLocation(guardLocation.x + 2, guardLocation.y +2);
                                        		if(rc.canAttackSquare(target) && !target.equals(rc.getLocation())){
                                        			rc.attackSquare(target);
                                        		}
                                        	}else if(caseNum == 1){
                                        		MapLocation target = new MapLocation(guardLocation.x + 2, guardLocation.y +1);
                                        		if(rc.canAttackSquare(target) && !target.equals(rc.getLocation())){
                                        			rc.attackSquare(target);
                                        		}
                                        	}else if(caseNum == 2){
                                        		MapLocation target = new MapLocation(guardLocation.x + 2, guardLocation.y);
                                        		if(rc.canAttackSquare(target) && !target.equals(rc.getLocation())){
                                        			rc.attackSquare(target);
                                        		}
                                        	}else if(caseNum == 3){
                                        		MapLocation target = new MapLocation(guardLocation.x + 1, guardLocation.y);
                                        		if(rc.canAttackSquare(target) && !target.equals(rc.getLocation())){
                                        			rc.attackSquare(target);
                                        		}
                                        	}else if(caseNum == 4){
                                        		MapLocation target = new MapLocation(guardLocation.x + 1, guardLocation.y + 2);
                                        		if(rc.canAttackSquare(target) && !target.equals(rc.getLocation())){
                                        			rc.attackSquare(target);
                                        		}
                                        	}else if(caseNum == 5){
                                        		MapLocation target = new MapLocation(guardLocation.x , guardLocation.y );
                                        		if(rc.canAttackSquare(target) && !target.equals(rc.getLocation())){
                                        			rc.attackSquare(target);
                                        		}
                                        	}else if(caseNum == 6){
                                        		MapLocation target = new MapLocation(guardLocation.x , guardLocation.y + 2);
                                        		if(rc.canAttackSquare(target) && !target.equals(rc.getLocation())){
                                        			rc.attackSquare(target);
                                        		}
                                        	}else{
                                        		MapLocation target = new MapLocation(guardLocation.x , guardLocation.y + 1);
                                        		if(rc.canAttackSquare(target) && !target.equals(rc.getLocation())){
                                        			rc.attackSquare(target);
                                        		}
                                        	}
                                    	//}
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
