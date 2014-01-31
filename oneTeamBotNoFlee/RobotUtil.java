package oneTeamBotNoFlee;

import battlecode.common.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;


public class RobotUtil {
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
	static int groupAttackLocation = 15;
	static int[] numberInjuredInGroup = {22,23,24,25,26}; 
	static int groupUpdate = 20;
	static int groupLeaderPicked = 21;
	static int totalHealthOfInjured = 33;
	static Direction allDirections[] = Direction.values();
	static Random rand = new Random();
	public static enum missions{
		defense, offense;
	}
	static int InjuredAttack = 34;
	static boolean injured = false;

	public static boolean micro(RobotController rc) throws GameActionException{

		boolean result = false;
		Robot[] enemiesNear = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam().opponent());
		Robot[] teammatesNear = rc.senseNearbyGameObjects(Robot.class, 35, rc.getTeam());
		
		if(enemiesNear.length > 0){
			if(enemiesNear.length <= teammatesNear.length - 2){
				if(rc.readBroadcast(groupAttackLocation) != -1){//if its group has a target
					MapLocation groupAttackSpot = intToMapLoc(rc.readBroadcast(groupAttackLocation));
					//if you can sense the spot, and theres still a robot attack, else tell everyone its gone and try and attack any other bots around
					if(rc.canSenseSquare(groupAttackSpot)){
						GameObject objAtLoc = rc.senseObjectAtLocation(groupAttackSpot);
						if(objAtLoc != null && objAtLoc.getTeam() != rc.getTeam()){//if there is still a robot at the spot
							if(rc.canAttackSquare(groupAttackSpot)){//if we can attack do so
								rc.attackSquare(groupAttackSpot);
								result = true;
							}else{//else if there is an enemy attackable do so, or else move towards group attack spot
								boolean flag = false;
								for(Robot r: enemiesNear){

									MapLocation temp = rc.senseLocationOf(r);
									if(rc.canAttackSquare(temp)){
										if(rc.senseRobotInfo(r).type != RobotType.HQ){
											rc.attackSquare(temp);
											flag = true;
										}
									}
								}
								if(!flag){
									moveInDirection(rc, bugPathNextSquare(rc, rc.getLocation(), groupAttackSpot));
									result = true;
								}
							}
						}else{//else there is no object so tell group and try to attack others close
							rc.broadcast(groupAttackLocation, -1);
							for(Robot r: enemiesNear){
								MapLocation temp = rc.senseLocationOf(r);
								if(rc.canAttackSquare(temp)){
									if(rc.senseRobotInfo(r).type != RobotType.HQ){
										rc.attackSquare(temp);
										rc.broadcast(groupAttackLocation, mapLocToInt(rc.senseLocationOf(r)));
										result = true;
									}
								}
							}
						}
					}else{//else you are not close to the spot if you can hit a robot do so, or else move towards group attck location
						boolean flag = false;
						for(Robot r: enemiesNear){
							MapLocation temp = rc.senseLocationOf(r);
							if(rc.canAttackSquare(temp)){
								if(rc.senseRobotInfo(r).type != RobotType.HQ){
									rc.attackSquare(temp);
									result = true;
								}
							}
						}
					}
				}else if(enemiesNear.length > 0){//there is no group attack loc so try and attack those close. if you can tell the others his loc
					for(Robot r: enemiesNear){
						MapLocation attackSpot = rc.senseLocationOf(r);
						if(rc.canAttackSquare(attackSpot)){
							if(rc.senseRobotInfo(r).type != RobotType.HQ){
								rc.attackSquare(attackSpot);
								result = true;
								rc.broadcast(groupAttackLocation, mapLocToInt(attackSpot));
							}
						}
					}
				}
			}else{
				
				if(rc.readBroadcast(groupAttackLocation) != -1){
					MapLocation groupAttackSpot = intToMapLoc(rc.readBroadcast(groupAttackLocation));
					//if you can sense the spot, and theres still a robot attack, else tell everyone its gone and try and attack any other bots around
					if(rc.canSenseSquare(groupAttackSpot)){
						GameObject objAtLoc = rc.senseObjectAtLocation(groupAttackSpot);
						if(objAtLoc != null && objAtLoc.getTeam() != rc.getTeam()){//if there is still a robot at the spot
							if(rc.canAttackSquare(groupAttackSpot)){//if we can attack do so
								rc.attackSquare(groupAttackSpot);
								result = true;
							}else{//else if there is an enemy attackable do so, or else move towards group attack spot
								boolean flag = false;
								for(Robot r: enemiesNear){

									MapLocation temp = rc.senseLocationOf(r);
									if(rc.canAttackSquare(temp)){
										if(rc.senseRobotInfo(r).type != RobotType.HQ){
											rc.attackSquare(temp);
											flag = true;
										}
									}
								}
								if(!flag){
									moveInDirection(rc, bugPathNextSquare(rc, rc.getLocation(), groupAttackSpot));
									result = true;
								}
							}
						}else{//else there is no object so tell group and try to attack others close
							rc.broadcast(groupAttackLocation, -1);
							for(Robot r: enemiesNear){
								MapLocation temp = rc.senseLocationOf(r);
								if(rc.canAttackSquare(temp)){
									if(rc.senseRobotInfo(r).type != RobotType.HQ){
										rc.attackSquare(temp);
										rc.broadcast(groupAttackLocation, mapLocToInt(rc.senseLocationOf(r)));
										result = true;
									}
								}
							}
						}
					}
				}
				if(result == false){
					for(Robot r : enemiesNear){
						MapLocation attackSpot = rc.senseLocationOf(r);
						if(rc.canAttackSquare(attackSpot)){
							if(rc.senseRobotInfo(r).type != RobotType.HQ){
								rc.attackSquare(attackSpot);
								result = true;
								rc.broadcast(groupAttackLocation, mapLocToInt(attackSpot));
							}
						}
					}
				}
				if(result == false){
					int counterx = 0;
					int countery = 0;
					for(Robot opp: enemiesNear){
						MapLocation oppLoc = rc.senseLocationOf(opp);
						counterx += oppLoc.x;
						countery += oppLoc.y;
					}
					MapLocation avg = new MapLocation(counterx / enemiesNear.length, countery / enemiesNear.length);

					Direction dirToGoal = rc.getLocation().directionTo(avg).opposite();
					moveInDirection(rc, dirToGoal);
				}
			}
		}

		return result;
}


public static void logMap(double[][] map) {
	int mapWidth = map.length;
	int mapHeight = map[0].length;
	System.out.print("\n");
	for (int i = 0; i < mapHeight; i++) {
		for (int j = 0; j < mapWidth; j++) {
			System.out.print((int)map[j][i] + " ");
		}
		System.out.print("\n");
	}
}

public static void logMap(int[][] map) {
	int mapWidth = map.length;
	int mapHeight = map[0].length;
	System.out.print("\n");
	for (int i = 0; i < mapHeight; i++) {
		for (int j = 0; j < mapWidth; j++) {
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

public static void intelligentSpawn(RobotController rc, Direction goalDirection) throws GameActionException{
	int[] directionPriority = {0, 1, -1, 2, -2, 3, -3, 4};
	if(rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS){
		for(int direction:directionPriority){
			int trialDir = (goalDirection.ordinal() + direction + 8) % 8;
			if(rc.canMove(allDirections[trialDir])){
				rc.spawn(allDirections[trialDir]);
				rc.broadcast(startGroup, rc.readBroadcast(startGroup) + 1);
				break;
			}
		}
	}
}

public static void moveInDirection(RobotController rc, Direction goalDirection) throws GameActionException {
	int[] directionPriority = {0, 1, -1, 2, -2, 3, -3, 4};
	if(rc.isActive()){
		for(int direction:directionPriority){
			int trialDir = (goalDirection.ordinal() + direction + 8) % 8;
			if(rc.canMove(allDirections[trialDir])){
				if(rc.getLocation().distanceSquaredTo(intToMapLoc(rc.readBroadcast(DefenseGoalLocation))) < 50){
					rc.sneak(allDirections[trialDir]);
				} else {
					rc.move(allDirections[trialDir]);
				}
				break;
			}
		}
	}
}

public static int[][] assessMapWithDirection(RobotController rc, MapLocation goal, int[][] map) throws GameActionException {
	ArrayDeque<MapLocation> queue = new ArrayDeque<MapLocation>();
	ArrayDeque<MapLocation> enemyQueue = new ArrayDeque<MapLocation>();
	int mapWidth = map.length;
	int mapHeight = map[0].length;
	int currentX;
	int currentY;
	MapLocation currentLocation;
	map[goal.x][goal.y] = 9;
	MapLocation temp = rc.senseHQLocation();
	map[temp.x][temp.y] = 9;
	MapLocation enemyHq = rc.senseEnemyHQLocation();
	map[enemyHq.x][enemyHq.y] = 9;

	enemyQueue.add(enemyHq);
	while(!enemyQueue.isEmpty()){
		currentLocation = enemyQueue.poll();
		currentX = currentLocation.x;
		currentY = currentLocation.y;

		for(Direction dir : allDirections){
			temp = currentLocation.add(dir);
			if(temp.x != -1 && temp.y != -1 && temp.x < mapWidth && temp.y < mapHeight){
				if(map[temp.x][temp.y] != 9 && enemyHq.distanceSquaredTo(temp) < 20){
					map[temp.x][temp.y] = 9;
					enemyQueue.add(temp);
				}
			}
		}
	}

	// we want map locations in the queue
	queue.add(goal);
	while(!queue.isEmpty()) {
		intelligentSpawn(rc, rc.getLocation().directionTo(goal));
		if(rc.readBroadcast(startGroup) > 1){
			rc.broadcast(startGroupGO, 1);
		}else{
			rc.broadcast(startGroupGO, 0);
		}

		currentLocation = queue.poll();
		currentX = currentLocation.x;
		currentY = currentLocation.y;

		// check the northern square
		if(currentY != 0 && map[currentX][currentY-1] == 0 && rc.senseTerrainTile(new MapLocation(currentX, currentY-1)).ordinal() != 2) {
			map[currentX][currentY-1] = 5;
			queue.add(new MapLocation(currentX, currentY-1));

		}
		// check the north eastern square
		if(currentY != 0 && currentX != mapWidth-1 && map[currentX+1][currentY-1] == 0 && rc.senseTerrainTile(new MapLocation(currentX+1, currentY-1)).ordinal() != 2) {
			map[currentX+1][currentY-1] = 6;
			queue.add(new MapLocation(currentX+1, currentY-1));

		}
		// check the eastern square
		if(currentX != mapWidth-1 && map[currentX+1][currentY] == 0 && rc.senseTerrainTile(new MapLocation(currentX+1, currentY)).ordinal() != 2) {
			map[currentX+1][currentY] = 7;
			queue.add(new MapLocation(currentX+1, currentY));
		}
		// check the south eastern square
		if(currentX != mapWidth-1 && currentY != mapHeight-1 && map[currentX+1][currentY+1] == 0 && rc.senseTerrainTile(new MapLocation(currentX+1, currentY+1)).ordinal() != 2) {
			map[currentX+1][currentY+1] = 8;
			queue.add(new MapLocation(currentX+1, currentY+1));
		}
		// check the southern square
		if(currentY != mapHeight-1 && map[currentX][currentY+1] == 0 && rc.senseTerrainTile(new MapLocation(currentX, currentY+1)).ordinal() != 2) {
			map[currentX][currentY+1] = 1;
			queue.add(new MapLocation(currentX, currentY+1));
		}
		// check the south western square
		if(currentX != 0 && currentY != mapHeight-1 && map[currentX-1][currentY+1] == 0 && rc.senseTerrainTile(new MapLocation(currentX-1, currentY+1)).ordinal() != 2) {
			map[currentX-1][currentY+1] = 2;
			queue.add(new MapLocation(currentX-1, currentY+1));
		}
		// check the western square
		if(currentX != 0 && map[currentX-1][currentY] == 0 && rc.senseTerrainTile(new MapLocation(currentX-1, currentY)).ordinal() != 2) {
			map[currentX-1][currentY] = 3;
			queue.add(new MapLocation(currentX-1, currentY));
		}
		// check the north western square
		if(currentX != 0 && currentY != 0 && map[currentX-1][currentY-1] == 0 && rc.senseTerrainTile(new MapLocation(currentX-1, currentY-1)).ordinal() != 2) {
			map[currentX-1][currentY-1] = 4;
			queue.add(new MapLocation(currentX-1, currentY-1));
		}	


	}
	return map;
}

private static int shortestDistance;
private static boolean onWall = false;
private static Direction currentDirection = null;

/**
 * Method assess the bots current location and returns the next direction according to our bug path, or null
 */
public static Direction bugPathNextSquare(RobotController rc, MapLocation currentLocation, MapLocation destination) throws GameActionException {
	if (currentDirection == null) {
		currentDirection = rc.getLocation().directionTo(destination);
		shortestDistance = rc.getLocation().distanceSquaredTo(destination);
	}

	int terrain;
	MapLocation targetLocation;
	int currentDistance = currentLocation.distanceSquaredTo(destination);
	/*
        if(rc.getRobot().getID() == 105){
            System.out.println("1 " + shortestDistance + ", currentDistance:" + currentDistance);
        }
	 */

	rc.setIndicatorString(2, "onWall: " + onWall);
	rc.setIndicatorString(1, destination.toString());
	// we are at out destination
	if(currentLocation.x == destination.x && currentLocation.y == destination.y) {
		if(rc.getRobot().getID() == 105){
			//System.out.println("2 " + shortestDistance + ", currentDistance:" + currentDistance);
		}
		rc.setIndicatorString(0, "null");
		return null;
	}
	Direction dir = currentLocation.directionTo(destination);
	targetLocation = currentLocation.add(dir);
	terrain = rc.senseTerrainTile(targetLocation).ordinal();

	// if we can move in the direction of the destination
	if(!onWall && terrain != 2 && terrain != 3) {
		if(rc.getRobot().getID() == 105){
			//System.out.println("3 " + shortestDistance + ", currentDistance:" + currentDistance);
		}
		shortestDistance = currentDistance;
		rc.setIndicatorString(0, dir.toString());
		currentDirection = dir;
		return dir;
		// we can not move towards the destination because there is a wall
	} else {
		onWall = true;
		// if we ever get closer to our destination, and the square towards is open, leave wall
		if (currentDistance <= shortestDistance && terrain != 2 && terrain != 3) {
			if(rc.getRobot().getID() == 105){
				//System.out.println("4 " + shortestDistance + ", currentDistance:" + currentDistance);
			}
			onWall = false;
			rc.setIndicatorString(0, dir.toString());
			currentDirection = dir;
			return dir;
		}

		dir = currentDirection;
		// while the targetLocation is a wall, rotate and update to new target location
		// this deals with if we need to rotate left
		int counter = 0;
		targetLocation = rc.getLocation().add(currentDirection);
		terrain = rc.senseTerrainTile(targetLocation).ordinal();
		while (terrain == 2 || terrain == 3) {
			counter += 1;
			dir = dir.rotateLeft();
			targetLocation = currentLocation.add(dir);
			terrain = rc.senseTerrainTile(targetLocation).ordinal();
		}
		if(rc.getRobot().getID() == 105){
			//System.out.println(counter);
		}
		if (counter > 0) {
			if(rc.getRobot().getID() == 105){
				//System.out.println("5 " + shortestDistance + ", currentDistance:" + currentDistance);
			}
			rc.setIndicatorString(0, dir.toString());
			currentDirection = dir;
			return dir;
		}
		// if the wall on our right disappears
		// this deals with if we need to rotate right
		MapLocation wallLocation = currentLocation.add(dir.rotateRight().rotateRight());
		terrain = rc.senseTerrainTile(wallLocation).ordinal();
		if(terrain != 2 && terrain != 3) {
			if(rc.getRobot().getID() == 105){
				//System.out.println("6 " + shortestDistance + ", currentDistance:" + currentDistance);
			}
			dir = dir.rotateRight().rotateRight();
			rc.setIndicatorString(0, dir.toString());
			currentDirection = dir;
			return dir;
		} else {
			if(rc.getRobot().getID() == 105){
				//System.out.println("7 " + shortestDistance + ", currentDistance:" + currentDistance);
			}
			rc.setIndicatorString(0, dir.toString());
			currentDirection = dir;
			return dir;
		}
	}
}

public static MapLocation sensePASTRGoal3(RobotController rc, int mapWidth, int mapHeight){

	int avg = mapWidth + mapHeight;
	double[][] map = rc.senseCowGrowth();
	mapWidth--;
	mapHeight--;
	double largestCowGrowth = -1;
	MapLocation maxLoc = new MapLocation(-1,-1);
	int closestGoalDistance = 999;
	double totalCows;

	for(int i = 0; i <  avg; i++){
		int currentWidth = rand.nextInt() % mapWidth;
		int currentHeight = rand.nextInt() % mapHeight;
		MapLocation potentialGoal = new MapLocation(currentWidth, currentHeight);

		if(rc.senseTerrainTile(potentialGoal).ordinal() != 2 && rc.senseTerrainTile(potentialGoal).ordinal() != 3 && potentialGoal.distanceSquaredTo(rc.senseEnemyHQLocation()) > 100){
			totalCows = 0;
			for(int j = -4; j <= 4; j++){
				for(int k = -4; k <= 4; k++){
					int x = currentWidth+j;
					int y = currentHeight+k;
					// make sure that we are only accessing squares within the bounds of our map array
					if(x >= 0 && y >= 0 && x < mapWidth && y < mapHeight){
						totalCows += map[x][y];
					}
				}
			}
			if(totalCows > largestCowGrowth) {
				largestCowGrowth = totalCows;
				closestGoalDistance = potentialGoal.distanceSquaredTo(rc.senseHQLocation());
				maxLoc = potentialGoal;
			}

			// if the new sample has the same amount of cows as the previous best location
			// then chose the location that is closest to our HQ
			if (totalCows == largestCowGrowth) {
				if (closestGoalDistance > potentialGoal.distanceSquaredTo(rc.senseHQLocation())) {
					closestGoalDistance = potentialGoal.distanceSquaredTo(rc.senseHQLocation());
					maxLoc = potentialGoal;
				}
			}
		}
	}

	return maxLoc;
}

public static void broadcastMap(RobotController rc, int[][] map, int offset) throws GameActionException{
	int mapWidth = map.length;
	int mapHeight = map[0].length;
	for(int i = 0; i < mapWidth; i++){
		for(int j = 0; j < mapHeight; j++){
			MapLocation ml = new MapLocation(i,j);
			rc.broadcast(mapLocToInt(ml) + offset, map[i][j]);
		}
	}
}

public static MapLocation getPastrToMakeGoal(RobotController rc, int[] channels, MapLocation ourPASTR) throws GameActionException{
	MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam().opponent());

	MapLocation closestPASTR = null;
	int smallestDistance = 9999;
	boolean newPASTRLocation;
	// loop through all enemy PASTRS - we need to find the one closest to our PASTR
	for(MapLocation pastr: pastrLocs){
		//if(pastr.distanceSquaredTo(rc.senseEnemyHQLocation()) > 40){
		newPASTRLocation = true;
		// loop through all channels
		for(int channel: channels){
			// don't calculate a whole map if one already exists for it.
			if(rc.readBroadcast(channel) == mapLocToInt(pastr)){
				newPASTRLocation = false;
			}
		}
		if(newPASTRLocation){
			if(pastr.distanceSquaredTo(ourPASTR) < smallestDistance) {
				closestPASTR = pastr;
				smallestDistance = pastr.distanceSquaredTo(ourPASTR);
			}
		}
		//}
	}
	return closestPASTR;
}

//need to iron out lastOffstet ish
public static int getNewGoalPastr(RobotController rc, int lastOffset, int[] channels) throws GameActionException{
	ArrayList<Integer> populatedChannels = new ArrayList<Integer>();
	for(int channel : channels){
		if(channel != lastOffset && rc.readBroadcast(channel) != -1){
			populatedChannels.add(channel);
		}
	}

	if(populatedChannels.size() > 0){
		int max = Integer.MAX_VALUE;
		int result = -1;
		for(int channel: populatedChannels){
			int current = RobotUtil.intToMapLoc(rc.readBroadcast(DefenseGoalLocation)).distanceSquaredTo(RobotUtil.intToMapLoc(rc.readBroadcast(channel)));
			if(current < max){
				max = current;
				result = channel;
			}
		}
		return result;
	} else {
		return -1;
	}
}
}
