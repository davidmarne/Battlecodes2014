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
		MapLocation currentLocation = null;
		
        while(true) {
        	
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() && rc.senseRobotCount() < 25) {
						if(rc.readBroadcast(0) == 0){
							currentLocation = rc.getLocation();
							//sense a goal location based on pastr growth
							goal = RobotUtil.sensePASTRGoal(rc);
							System.out.println("GOAL IS: " + goal);
							//Pathing Algorithm
	                        map = RobotUtil.assessMapWithDirection(rc, goal, map);
	                        //broadcast the map out for other robots to read
	                        RobotUtil.broadcastMap(rc, map);
	                        //let everyone know that the map has finished being broadcasted
	                        rc.broadcast(0, 1);
	                        //System.out.println("ROUND: " + Clock.getRoundNum());
	                        //let everyone know the goal location
	                        rc.broadcast(10001, RobotUtil.mapLocToInt(goal));
	                        for(Direction d: directions){
	                        	//System.out.println(d + ": "+ d.ordinal());
	                        }
	                        //RobotUtil.logMap(map);
						}else{
							Direction toGoal = rc.getLocation().directionTo(goal);
							if (rc.senseObjectAtLocation(rc.getLocation().add(toGoal)) == null) {
								rc.spawn(toGoal);
							}else{
								while(true){
									Direction dir = directions[rand.nextInt() % 8];
									if(rc.senseObjectAtLocation(currentLocation.add(dir)) == null){
										rc.spawn(dir);
									}
								}
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
								goal = RobotUtil.intToMapLoc(rc.readBroadcast(10001));
								first = false;
							}
						}else{
							currentLocation = rc.getLocation();
							//if a pastr and noisetower havent been made/assigned to a bot
							if(rc.readBroadcast(10000) < 2){
								if(rc.getRobot().getID() == 644){
									System.out.println("CL "+currentLocation);
									System.out.println("Goal " + goal);
								}
								if(currentLocation.equals(goal)){
									if(rc.getRobot().getID() == 644){
										System.out.println("inside");
										}
									rc.construct(RobotType.PASTR);
									rc.broadcast(10000, 1);
								}else if(rc.readBroadcast(10000) == 1 && currentLocation.distanceSquaredTo(goal) < 4){
									rc.construct(RobotType.NOISETOWER);
									rc.broadcast(10000, 2);
								}else{
									int intToGoal = map[currentLocation.x][currentLocation.y] - 1;
									Direction dirToGoal = directions[intToGoal];
									if(rc.canMove(dirToGoal)){
										rc.move(dirToGoal);
									}
								}
							}else if(currentLocation.distanceSquaredTo(goal) > 9){//if far away move towards goal
								int intToGoal = map[currentLocation.x][currentLocation.y] - 1;
								Direction dirToGoal = directions[intToGoal];
								if(rc.canMove(dirToGoal)){
									rc.move(dirToGoal);
								}
							}else{//else move randomly
								Direction moveDirection = directions[rand.nextInt(8)];
								if (rc.canMove(moveDirection)) {
									rc.move(moveDirection);
								}
							}
						}
					}else if (rc.getType() == RobotType.NOISETOWER){
						
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
