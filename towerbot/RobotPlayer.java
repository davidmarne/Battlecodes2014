package towerbot;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {
	static Random rand;
	public static enum missions{
        defense, offense;
    }
	
	public static void run(RobotController rc) {
		rand = new Random();
		Direction[] directions = Direction.values();
		int mapWidth = rc.getMapWidth();
		int mapHeight = rc.getMapHeight();
		int[][] map = new int[mapWidth][mapHeight];
		MapLocation goal = new MapLocation(0,0);
		boolean first = true;
		MapLocation currentLocation = null;
		missions robotMission = missions.defense;
		
		
        while(true) {
        	
			if (rc.getType() == RobotType.HQ) {
				try {
					//Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() && rc.senseRobotCount() < 25) {
                        // on first pass, assess map
						if(rc.readBroadcast(0) == 0){
							// sense a goal location based on pastr growth
							goal = RobotUtil.sensePASTRGoal(rc);

							// Pathing Algorithm
	                        map = RobotUtil.assessMapWithDirection(rc, goal, map);

	                        // broadcast the map out for other robots to read
	                        RobotUtil.broadcastMap(rc, map);

	                        // let everyone know that the map has finished being broadcasted
	                        rc.broadcast(0, 1);

	                        //let everyone know the goal location
	                        rc.broadcast(10001, RobotUtil.mapLocToInt(goal));
						}else{
							//if there isnt a pastr being attacked and our defense is already initialized
							if(rc.readBroadcast(0) == 2){
								
								boolean lost = false;
								MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam().opponent());
								if(rc.readBroadcast(5000) == 1){
									for(MapLocation ml : pastrLocs){
										if(ml.equals(goal)){
											lost = true;
										}
									}
								}
								if(pastrLocs.length > 0 && lost == false){
									goal = pastrLocs[rand.nextInt() % pastrLocs.length];
									map = RobotUtil.assessMapWithDirection(rc, goal, new int[mapWidth][mapHeight]);
			                        //broadcast the map out for other robots to read
			                        RobotUtil.broadcastMap(rc, map);
			                        //RobotUtil.logMap(map);
			                        rc.broadcast(0, 2);
									rc.broadcast(10001, RobotUtil.mapLocToInt(goal));
									rc.broadcast(5000, 1);
								}
							}
							
							Direction toGoal = rc.getLocation().directionTo(goal);
							// spawn
                            RobotUtil.intelligentSpawn(rc, toGoal);
						}
					}
				} catch (Exception e) {
					//System.out.println("HQ Exception");
				}
			} else if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isActive()) {
						if(first){
							if(rc.readBroadcast(0) == 1 || rc.readBroadcast(0) == 2){
								
								if(rc.readBroadcast(0) == 1){
									robotMission = missions.defense;
								}else{
									robotMission = missions.offense;
								}
								map = RobotUtil.readMapFromBroadcast(rc);
								goal = RobotUtil.intToMapLoc(rc.readBroadcast(10001));
								first = false;
								if(robotMission == missions.defense){
									int numDefenders = rc.readBroadcast(10002);
									if(numDefenders < 12){
										rc.broadcast(10002, numDefenders + 1);
									}else{
										rc.broadcast(0, 2);
									}
								}
							}
						}else{
							currentLocation = rc.getLocation();
							if(robotMission == missions.defense){
								//if a pastr and noisetower havent been made/assigned to a bot
								if(rc.readBroadcast(10000) < 2){
									if(currentLocation.equals(goal)){
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
								} else { //if far away move towards goal
									int intToGoal = map[currentLocation.x][currentLocation.y] - 1;
									Direction dirToGoal = directions[intToGoal];
									RobotUtil.moveInDirection(rc, dirToGoal, "sneak");
								}
							}else{
								if(RobotUtil.mapLocToInt(goal) != rc.readBroadcast(10001)){
									map = RobotUtil.readMapFromBroadcast(rc);
									goal = RobotUtil.intToMapLoc(rc.readBroadcast(10001));
									rc.broadcast(0,2);
								}
								if(!rc.canAttackSquare(goal)){//if far away move towards goal
									int intToGoal = map[currentLocation.x][currentLocation.y] - 1;
									Direction dirToGoal = directions[intToGoal];
                                    if(rc.canMove(dirToGoal)){
                                        rc.move(dirToGoal);
                                    }
								}else{
									rc.attackSquare(goal);
								}
							}
						}
					}
				} catch (Exception e) {
					//System.out.println("Soldier Exception");
				}
			} else if (rc.getType() == RobotType.NOISETOWER) {
                try {
                    if(rc.isActive()){
                        currentLocation = rc.getLocation();
                        int maxAttack = (int)Math.sqrt(rc.getType().attackRadiusMaxSquared);
                        for (int j = 0; j < 1080; j += 45) {
                            for(int i = maxAttack; i > 2; i-=2){
                                if(rc.isActive()){
                                	double len = i ;
                                    double xVal = Math.cos(j * Math.PI / 180.0) * len;
                                    double yVal = Math.sin(j * Math.PI / 180.0) * len;

                                    MapLocation squareToAttack = currentLocation.add((int)xVal, (int)yVal);
                                    if(rc.canAttackSquare(squareToAttack)) {
                                        rc.attackSquare(squareToAttack);
                                    }
                                }
                                rc.yield();
                                rc.yield();
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
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
