package team071;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;

public class RobotPlayer {
	static Random rand;
	public static enum missions{
        defense, offense;
    }
	
	static int DefenseChannelOffset = 10000;
	static int DefenseGoalLocation = 64999;
	static int[] OffenseGoalLocations = {2,3,4,5};
	static int OffenseGoalDestroyed = 7;
	static int OffenseCurrentGoalOffset = 8;
	static int buildingProgress = 9;
	static int numDefendingGoal = 10;
	static int startGroup = 11;
	static int startGroupGO = 12;
	static int sendToAttack = 13;
	static int towerLocation = 14;
	static int[] groupAttackLocation = {15,16,17,18,19};
	static int[] numberInjuredInGroup = {22,23,24,25,26}; 
	static int groupUpdate = 20;
	static int groupLeaderPicked = 21;
	static int reinforcementsNeeded = 27;
	static int reinforcementsSent = 28;
	//every robotID + 50 is the robots healing boolean
	
	public static void run(RobotController rc) {
		rand = new Random();
		Direction[] directions = Direction.values();
		int mapWidth = rc.getMapWidth();
		int mapHeight = rc.getMapHeight();
		int[][] map = new int[mapWidth][mapHeight];
		MapLocation goal = new MapLocation(0,0);
        MapLocation ourPASTR = new MapLocation(0,0);
		boolean first = true;
		boolean second = true;
		MapLocation currentLocation;
		missions robotMission = missions.defense;
		int groupNum = 0;
		boolean leaderOfGroup = false;

        while(true) {
        	
			if (rc.getType() == RobotType.HQ) {
				try {
                    if (first) {
                        first = false;
                        // broadcast this out first and then check for -1 instead of 0, becuase if the map location
                        // is at (0, 0), then we will never make it out of the loop
                        rc.broadcast(DefenseGoalLocation, -1);
                        //group attack locations must also be init'ed -1 so they 0,0 can be a location 
                        for(int i : groupAttackLocation){
                        	rc.broadcast(i, -1);
                        }
                    }
					Robot[] enemiesNear = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
					for(Robot r: enemiesNear){
						if(rc.isActive() && rc.canAttackSquare(rc.senseLocationOf(r)) && rc.senseRobotInfo(r).type != RobotType.HQ){
							rc.attackSquare(rc.senseLocationOf(r));
							break;
						}
					}
					
					//Check if a robot is spawnable and spawn one if it is
					if (rc.isActive() ) {
						if(rc.senseRobotCount() < 25){
							RobotUtil.intelligentSpawn(rc, rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
						}
						//if there are 5 new spawnees at the hq send em out
						if(rc.readBroadcast(startGroup) > 3){
							rc.broadcast(startGroupGO, 1);
						}else{
							rc.broadcast(startGroupGO, 0);
						}
						
						if(rc.readBroadcast(DefenseGoalLocation) == -1){
							//sense a goal location based on PASTR growth
							while(true){
                                ourPASTR = RobotUtil.sensePASTRGoal3(rc, mapWidth, mapHeight);

                                // keep calculating goals until one returns that is not the initial one
                                if(ourPASTR.x != -1 && ourPASTR.y != -1) {
									break;
								}
							}
							//Pathing Algorithm
	                        map = RobotUtil.assessMapWithDirection(rc, ourPASTR, map);
	                        //broadcast the map out for other robots to read
	                        RobotUtil.broadcastMap(rc, map, DefenseChannelOffset);
	                        //let everyone know the goal location
	                        rc.broadcast(DefenseGoalLocation, RobotUtil.mapLocToInt(ourPASTR));
						}else{
							//if a new map can be computed do so
							for(int channel: OffenseGoalLocations){
								if(rc.readBroadcast(channel) == 0){
									goal = RobotUtil.getPastrToMakeGoal(rc, OffenseGoalLocations, ourPASTR);
									if(goal != null){
										map = RobotUtil.assessMapWithDirection(rc, goal, new int[mapWidth][mapHeight]);
										rc.broadcast(channel, RobotUtil.mapLocToInt(goal));
										RobotUtil.broadcastMap(rc, map, channel*10000);
										System.out.println(goal+ " has been added to " +channel);
										break;
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isActive()) {
						if(first || second){
							//get map to our pastr if possible
							if(rc.readBroadcast(DefenseGoalLocation) >= 0 && first){
								//all bots go to our pastr to rally
								goal = RobotUtil.intToMapLoc(rc.readBroadcast(DefenseGoalLocation));
								robotMission = missions.defense;
								first = false;
							}
							//when a group of five or more has been gathered we can finally go to goal
							if(rc.readBroadcast(startGroupGO) == 1){
								rc.broadcast(startGroup, rc.readBroadcast(startGroup) - 1);
								second = false;
							}
						}else{
							
							if(RobotUtil.micro(rc, groupNum) == true){
								rc.yield();
								continue;
							}
							
							currentLocation = rc.getLocation();
							
							if(robotMission == missions.defense){
								if(rc.readBroadcast(reinforcementsSent) > 0){
									goal = RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)));
									System.out.println("Retrieved " + goal);
									robotMission = missions.offense;
									groupNum = rc.readBroadcast(groupUpdate);
									rc.broadcast(reinforcementsSent, rc.readBroadcast(reinforcementsSent) - 1);
									
								}else if(rc.readBroadcast(sendToAttack) > 0){
									goal = RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)));
									System.out.println("Retrieved " + goal);
									robotMission = missions.offense;
									rc.broadcast(sendToAttack, rc.readBroadcast(sendToAttack) - 1);
									groupNum = rc.readBroadcast(groupUpdate);
									if(rc.readBroadcast(groupLeaderPicked) == 1){
										leaderOfGroup = true;
										rc.broadcast(groupLeaderPicked, 0);
									}
								}else{
								    //if a pastr and noisetower havent been made/assigned to a bot
									if(rc.readBroadcast(buildingProgress) < 2){
										if(currentLocation.equals(goal)){
											rc.construct(RobotType.NOISETOWER);
											rc.broadcast(towerLocation, RobotUtil.mapLocToInt(currentLocation));
											rc.broadcast(buildingProgress, 1);
										}else if(rc.readBroadcast(buildingProgress) == 1 && currentLocation.distanceSquaredTo(goal) < 4){
											rc.construct(RobotType.PASTR);
											rc.broadcast(buildingProgress, 2);
										}
									}else{//if the old pastr or tower has been destroyed 
										if(currentLocation.equals(goal)){
											rc.construct(RobotType.PASTR);
										}else if(currentLocation.equals(RobotUtil.intToMapLoc(rc.readBroadcast(towerLocation)))){
											rc.construct(RobotType.NOISETOWER);
										}
									}
									//move towards goal
									int intToGoal = rc.readBroadcast(RobotUtil.mapLocToInt(new MapLocation(currentLocation.x,currentLocation.y)) + DefenseChannelOffset) - 1;
									Direction dirToGoal = directions[intToGoal];
									RobotUtil.moveInDirection(rc, dirToGoal, "sneak");
								}
							}else{
								//if many are injured in group ask for reinforcements
								if(leaderOfGroup){
									if(rc.readBroadcast(numberInjuredInGroup[groupNum]) > 4){
										rc.broadcast(reinforcementsNeeded, groupNum);
									}
								}
								//if the goal pastr is gone tell the hq and go back to our pastr
								if(rc.canSenseSquare(goal)){
									if(rc.senseObjectAtLocation(goal) == null){
										rc.broadcast(groupNum -1, 0);
										goal = RobotUtil.intToMapLoc(rc.readBroadcast(DefenseGoalLocation));
										groupNum = 0;
										//map = RobotUtil.readMapFromBroadcast(rc, DefenseChannelOffset);
										//wasOffense = true;
										robotMission = missions.defense;
									}
								}
								//move towards goal
								int intToGoal = rc.readBroadcast(RobotUtil.mapLocToInt(new MapLocation(currentLocation.x,currentLocation.y)) + ((groupNum -1)*10000)) - 1;
								Direction dirToGoal = directions[intToGoal];
								RobotUtil.moveInDirection(rc, dirToGoal, "move");
							}
						}
					}
				} catch (Exception e) {
					//e.printStackTrace();
				}
			} else if (rc.getType() == RobotType.NOISETOWER) {
				try {
                    if(rc.isActive()){
                        currentLocation = rc.getLocation();
                        int maxAttack = (int)Math.sqrt(rc.getType().attackRadiusMaxSquared);
                        for (int j = 0; j < 1080; j += 45) {
                            for(int i = maxAttack; i > 2; i-=1){
                                if(rc.isActive()){
                                    double xVal = Math.cos(j * Math.PI / 180.0) * i;
                                    double yVal = Math.sin(j * Math.PI / 180.0) * i;

                                    MapLocation squareToAttack = currentLocation.add((int)xVal, (int)yVal);
                                    if(rc.canAttackSquare(squareToAttack) &&
                                            squareToAttack.x < rc.getMapWidth() + 3 && squareToAttack.x > -3 &&
                                            squareToAttack.y < rc.getMapHeight() + 3 && squareToAttack.y > -3) {
                                        rc.attackSquare(squareToAttack);
                                        rc.yield();
                                        rc.yield();
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else if(rc.getType() == RobotType.PASTR){
            	try{
	            	Robot[] teammatesNear = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam());
	            	//System.out.println(teammatesNear.length);
	            	if(rc.readBroadcast(reinforcementsNeeded) > 0){
	            		if(teammatesNear.length > 9){
	            			rc.broadcast(reinforcementsSent, 3);
	            			rc.broadcast(groupUpdate, rc.readBroadcast(reinforcementsNeeded));
	            			rc.broadcast(OffenseCurrentGoalOffset, rc.readBroadcast(reinforcementsNeeded));
	            		}
	            	}else if(teammatesNear.length > 10){
	            		int oldOffset = rc.readBroadcast(OffenseCurrentGoalOffset);
						int n = RobotUtil.getNewGoalPastr(rc, oldOffset, OffenseGoalLocations);
						if(n != -1){
							rc.broadcast(groupUpdate, Arrays.binarySearch(OffenseGoalLocations, n) + 1);
							rc.broadcast(OffenseCurrentGoalOffset, n);
							System.out.println("GOTO " + RobotUtil.intToMapLoc(rc.readBroadcast(n)));
							rc.broadcast(sendToAttack, 6);
							rc.broadcast(groupLeaderPicked, 1);
							//gives time for e
							rc.yield();
							rc.yield();
							rc.yield();
							rc.yield();
							rc.yield();
							rc.yield();
							rc.yield();
							rc.yield();
							rc.yield();
							rc.yield();
							rc.yield();
							rc.yield();
						}
	            	}
            	}catch (Exception e) {
					e.printStackTrace();
				}
            }
			
			rc.yield();
		}
        
	}
}
