package oneTeamBot;

import battlecode.common.*;

import java.util.Random;

import oneTeamBot.RobotUtil;

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
	static int pastrLocation = 14;
	static int[] groupAttackLocation = {15,16,17,18,19};
	static int[] numberInjuredInGroup = {22,23,24,25,26}; 
	static int groupLeaderPicked = 21;
	static int reinforcementsNeeded = 27;
	static int reinforcementsSent = 28;
	static int offenseInitialized = 29;
	static int noNewPastrToAttack = 30;
	static int pastrNeedsReinforcements = 31;
	static int mapBeingAssessed = 32;
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
        boolean alreadyAttacked;
        boolean pastrHurt = false;
        while(true) {
			if (rc.getType() == RobotType.HQ) {
				try {
                    if(rc.isActive()) {
                        // on the first pass
                        if (first) {
                            first = false;
                            // broadcast this out first and then check for -1 instead of 0, because if the map location
                            // is at (0, 0), then we will never make it out of the loop
                            rc.broadcast(DefenseGoalLocation, -1);
                            rc.broadcast(OffenseCurrentGoalOffset, -1);
                            rc.broadcast(pastrLocation, -1);
                            //group attack locations must also be init'ed -1 so they 0,0 can be a location
                            for(int i : groupAttackLocation){
                                rc.broadcast(i, -1);
                            }
                            for(int i : OffenseGoalLocations){
                            	rc.broadcast(i, -1);
                            }
                        }

                        // first priority - attack nearby enemy robots
                        Robot[] enemiesNear = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());

                        if(enemiesNear.length != 0) {
                            alreadyAttacked = false;
                            // attack enemies that are within the attack radius
                            for(Robot r: enemiesNear){
                                if(rc.isActive() && rc.canAttackSquare(rc.senseLocationOf(r))){
                                    rc.attackSquare(rc.senseLocationOf(r));
                                    alreadyAttacked = true;
                                    break;
                                }
                            }

                            // attack enemies that are within splash dmg radius
                            if (!alreadyAttacked) {
                                for(Robot r: enemiesNear){
                                    MapLocation enemyLocation = rc.senseLocationOf(r);
                                    MapLocation attackLocation = enemyLocation.add(enemyLocation.directionTo(rc.getLocation()));
                                    if(rc.isActive() && rc.canAttackSquare(attackLocation)){
                                        rc.attackSquare(attackLocation);
                                        break;
                                    }
                                }
                            }
                        }

					    // Second priority - Check if a robot is spawnable and spawn one if it is
						if(rc.senseRobotCount() < 25){
							RobotUtil.intelligentSpawn(rc, rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
						}

						//if there are 5 new spawnees at the hq send em out
						if(rc.readBroadcast(startGroup) > 1){
							rc.broadcast(startGroupGO, 1);
						}else{
							rc.broadcast(startGroupGO, 0);
						}
						
						//if goal destroyed pick a new goal
						if(rc.readBroadcast(OffenseGoalDestroyed) == 1){
							int oldOffset = rc.readBroadcast(OffenseCurrentGoalOffset);
							int n = RobotUtil.getNewGoalPastr(rc, oldOffset, OffenseGoalLocations);
							if(n != -1){
								rc.broadcast(OffenseCurrentGoalOffset, n);
								rc.broadcast(noNewPastrToAttack, 0);
							}
						}

                        // third priority - single run - calculate a good spot four OUR PASTR
						if(rc.readBroadcast(DefenseGoalLocation) == -1){
							//sense a goal location based on PASTR growth  
							while(true){
                                ourPASTR = RobotUtil.sensePASTRGoal3(rc, mapWidth, mapHeight);

                                // keep calculating goals until one returns that is not the initial one
                                if(ourPASTR.x != -1 && ourPASTR.y != -1) {
									break;
								}
							}
							rc.broadcast(DefenseGoalLocation, RobotUtil.mapLocToInt(ourPASTR));
							rc.broadcast(mapBeingAssessed, 1);
							//Pathing Algorithm
	                        map = RobotUtil.assessMapWithDirection(rc, ourPASTR, map);
	                        RobotUtil.logMap(map);
	                        //broadcast the map out for other robots to read
	                        RobotUtil.broadcastMap(rc, map, DefenseChannelOffset);
							rc.broadcast(mapBeingAssessed, 0);
						}else{
							//if a new map can be computed do so
							for(int channel: OffenseGoalLocations){
								if(rc.readBroadcast(channel) == -1){
									goal = RobotUtil.getPastrToMakeGoal(rc, OffenseGoalLocations, ourPASTR);
									if(goal != null){
										rc.broadcast(channel, RobotUtil.mapLocToInt(goal));
										map = RobotUtil.assessMapWithDirection(rc, goal, new int[mapWidth][mapHeight]);
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
							if((rc.readBroadcast(offenseInitialized) == 0 || rc.readBroadcast(OffenseGoalDestroyed) == 1) && first){
								if(rc.readBroadcast(DefenseGoalLocation) >= 0 ){
									//all bots go to our pastr to rally
									goal = RobotUtil.intToMapLoc(rc.readBroadcast(DefenseGoalLocation));
									robotMission = missions.defense;
									first = false;
								}
							}else if(rc.readBroadcast(offenseInitialized) == 1 && first){
								goal = RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)));
								robotMission = missions.offense;
								first = false;	
							}
							//when a group of five or more has been gathered we can finally go to goal
							if(rc.readBroadcast(startGroupGO) == 1){
								rc.broadcast(startGroup, rc.readBroadcast(startGroup) - 1);
								second = false;
							}else{
								
							}
						}else{
							//int startBC = Clock.getBytecodeNum();
							boolean result;
							if(robotMission == missions.defense){
								result = RobotUtil.micro(rc, 0);
							}else{
								result = RobotUtil.micro(rc, 1);
							}
							
							currentLocation = rc.getLocation();
							
							if(!result){
								if(robotMission == missions.defense){/*
									if(rc.readBroadcast(reinforcementsSent) > 0){
										goal = RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)));
										System.out.println("Retrieved " + goal);
										robotMission = missions.offense;
										rc.broadcast(reinforcementsSent, rc.readBroadcast(reinforcementsSent) - 1);
										
									}else*/ 
									//startBC = Clock.getBytecodeNum();
									if(rc.readBroadcast(sendToAttack) > 0){
										goal = RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)));
										System.out.println("Retrieved " + goal);
										robotMission = missions.offense;
										//rc.broadcast(sendToAttack, rc.readBroadcast(sendToAttack) - 1);
										/*
										if(rc.readBroadcast(groupLeaderPicked) == 1){
											leaderOfGroup = true;
											rc.broadcast(groupLeaderPicked, 0);
										}*/
									}else{
									    //if a pastr and noisetower havent been made/assigned to a bot
										if(rc.readBroadcast(buildingProgress) < 1){
											if(currentLocation.equals(goal)){
												rc.construct(RobotType.NOISETOWER);
												rc.broadcast(buildingProgress, 1);
											}
										}else{//if the old pastr or tower has been destroyed 
											if((currentLocation.equals(RobotUtil.intToMapLoc(rc.readBroadcast(pastrLocation))) || rc.readBroadcast(pastrLocation) == -1 && currentLocation.distanceSquaredTo(goal) < 1.5)){
												rc.construct(RobotType.PASTR);
												if(rc.readBroadcast(pastrLocation) == -1){
													rc.broadcast(pastrLocation, RobotUtil.mapLocToInt(currentLocation));
												}
											}else if(currentLocation.equals(goal)){
												rc.construct(RobotType.NOISETOWER);
											}
										}
										//move towards goal
										if(rc.readBroadcast(mapBeingAssessed) == 1){
											RobotUtil.moveInDirection(rc, RobotUtil.bugPathNextSquare(rc, rc.getLocation(), goal), "sneak");
										}else{
											int intToGoal = rc.readBroadcast(RobotUtil.mapLocToInt(new MapLocation(currentLocation.x,currentLocation.y)) + DefenseChannelOffset) - 1;
											Direction dirToGoal = directions[intToGoal];
											RobotUtil.moveInDirection(rc, dirToGoal, "sneak");
										}
									}
									//System.out.println("Defense ByteCodes: " + (Clock.getBytecodeNum() - startBC));
								}else{
									//if many are injured in group ask for reinforcements
									/*
									if(leaderOfGroup){
										if(rc.readBroadcast(numberInjuredInGroup[1]) > 4){
											rc.broadcast(reinforcementsNeeded, 1);
										}
									}
									*/
									//startBC = Clock.getBytecodeNum();
									if(rc.readBroadcast(pastrNeedsReinforcements) > 0){
										robotMission = missions.defense;
										goal = RobotUtil.intToMapLoc(rc.readBroadcast(DefenseGoalLocation));
										rc.broadcast(pastrNeedsReinforcements, rc.readBroadcast(pastrNeedsReinforcements) - 1);
									}else if(rc.readBroadcast(noNewPastrToAttack) == 1){
										int intToGoal = rc.readBroadcast(RobotUtil.mapLocToInt(new MapLocation(currentLocation.x,currentLocation.y)) + DefenseChannelOffset) - 1;
										Direction dirToGoal = directions[intToGoal];
										RobotUtil.moveInDirection(rc, dirToGoal, "sneak");
									}else{
										//if the goal pastr is gone tell the hq and go back to our pastr
										goal = RobotUtil.intToMapLoc(rc.readBroadcast(rc.readBroadcast(OffenseCurrentGoalOffset)));
										if(rc.canSenseSquare(goal)){
											if(rc.senseObjectAtLocation(goal) == null){
												rc.broadcast(rc.readBroadcast(OffenseCurrentGoalOffset), -1);
												rc.broadcast(OffenseGoalDestroyed, 1);
												goal = RobotUtil.intToMapLoc(rc.readBroadcast(DefenseGoalLocation));
												rc.broadcast(noNewPastrToAttack, 1);
												//robotMission = missions.defense;
											}
										}
										//move towards goal
										int intToGoal = rc.readBroadcast(RobotUtil.mapLocToInt(new MapLocation(currentLocation.x,currentLocation.y)) + (rc.readBroadcast(OffenseCurrentGoalOffset)*10000)) - 1;
										Direction dirToGoal = directions[intToGoal];
										RobotUtil.moveInDirection(rc, dirToGoal, "move");
									}
									//System.out.println("Offense ByteCodes: " + (Clock.getBytecodeNum() - startBC));
								}
							}
							//System.out.println("MICRO BYTECODES: " + (Clock.getBytecodeNum() - startBC));
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
            		if(pastrHurt){
            			if(rc.getHealth() > 90){
            				pastrHurt = false;
            			}
            		}else if(rc.getHealth() < 50){//get more guards
            			pastrHurt = true;
            			rc.broadcast(pastrNeedsReinforcements, 6);
            		}else if(rc.readBroadcast(offenseInitialized) == 0 ){
            			int oldOffset = rc.readBroadcast(OffenseCurrentGoalOffset);
						int n = RobotUtil.getNewGoalPastr(rc, oldOffset, OffenseGoalLocations);
						if(n != -1){
							rc.broadcast(OffenseCurrentGoalOffset, n);
							System.out.println("Channel: " + n);
							System.out.println("GOTO " + RobotUtil.intToMapLoc(rc.readBroadcast(n)));
							rc.broadcast(sendToAttack, 5);
							rc.broadcast(groupLeaderPicked, 1);
							rc.broadcast(offenseInitialized, 1);
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
            		}else if(rc.readBroadcast(offenseInitialized) == 1 && teammatesNear.length > 8){
            			if(rc.readBroadcast(OffenseCurrentGoalOffset) != -1){
            				rc.broadcast(sendToAttack, 4);
            			}
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
            		
            	}catch (Exception e) {
					e.printStackTrace();
				}
            }
			
			rc.yield();
		}
        
	}
}
