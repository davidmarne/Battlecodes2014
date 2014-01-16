package towerbot;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {
	static Random rand;
	
	public static void run(RobotController rc) {
		rand = new Random();
		Direction[] directions = Direction.values();
		int mapWidth = rc.getMapWidth();
		int[][] map = new int[mapWidth][mapWidth];
		MapLocation goal = new MapLocation(0,0);
		boolean first = true;
		MapLocation currentLocation;
		
        while(true) {
        	
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() && rc.senseRobotCount() < 25) {
						if(rc.readBroadcast(0) == 0){
							goal = RobotUtil.sensePASTRGoal(rc);
							System.out.println("GOAL IS: " + goal);
	                        map = RobotUtil.assessMapWithDirection(rc, goal, map);
	                        RobotUtil.broadcastMap(rc, map);
	                        rc.broadcast(0, 1);
	                        System.out.println("ROUND: " + Clock.getRoundNum());
	                        for(Direction d: directions){
	                        	System.out.println(d + ": "+ d.ordinal());
	                        }
	                        RobotUtil.logMap(map);
						}else{
							Direction toGoal = rc.getLocation().directionTo(goal);
							if (rc.senseObjectAtLocation(rc.getLocation().add(toGoal)) == null) {
								rc.spawn(toGoal);
							}
						}
					}
				} catch (Exception e) {
					System.out.println("HQ Exception");
				}
			}
			
			if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isActive()) {
						if(first){
							if(rc.readBroadcast(0) == 1){
								map = RobotUtil.readMapFromBroadcast(rc);
								first = false;
							}
						}else{
							currentLocation = rc.getLocation();
							
							if(rc.readBroadcast(10000) < 2){
								if(rc.getLocation().equals(goal)){
									rc.construct(RobotType.PASTR);
									rc.broadcast(10000, 1);
								}else if(rc.readBroadcast(10000) == 1 && rc.getLocation().distanceSquaredTo(goal) < 4){
									rc.construct(RobotType.NOISETOWER);
									rc.broadcast(10000, 2);
								}
							}
							
							int intToGoal = map[currentLocation.x][currentLocation.y] - 1;
							Direction dirToGoal = directions[intToGoal];
							if(rc.canMove(dirToGoal)){
								rc.move(dirToGoal);
							}
						}
					}
				} catch (Exception e) {
					//System.out.println("Soldier Exception");
				}
			}
			
			rc.yield();
		}

//        public static MapLocation findMaxCowLocation() {
//            double[][] cowCoords = rc.senseCowGrowth();
//            int xLoc = 0;
//            int yLoc = 0;
//            // if the team is A - top left team
//
//
//            return new MapLocation(xLoc, yLoc);
//        }
	}
}
