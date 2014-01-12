package broski;

import battlecode.common.*;

import java.util.Random;

import battlecode.common.*;

import java.util.Random;
import java.util.ArrayList;

public class RobotPlayer{

    public static RobotController rc;
    static Direction allDirections[] = Direction.values();
    static Random randall = new Random();
    static int directionalLooks[] = new int[]{0,1,-1,2,-2};
    static MapLocation currLoc;

    public static void run(RobotController rcin){
        rc = rcin;
        randall.setSeed(rc.getRobot().getID());
        while(true){
            try{
                if(rc.getType()==RobotType.HQ){//if I'm a headquarters
                    runHeadquarters();
                }else if(rc.getType()==RobotType.SOLDIER){

                    aStarNode fin = aStarSearch(rc.getLocation(), new MapLocation(28,30));
                    ArrayList<aStarNode> path = new ArrayList<aStarNode>();
                    while(fin != null){
                        path.add(fin);
                        fin = fin.parent;
                    }
                    System.out.println("PATH:");
                    for(int i = path.size() -1 ; i >= 0; i-- ){
                        System.out.println(path.get(i).curr);
                    }
                    for(int i = path.size() -2 ; i >= 0; i-- ){
                        rc.move(rc.getLocation().directionTo(path.get(i).curr));

//                    }

                }
                rc.yield();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static aStarNode aStarSearch(MapLocation start, MapLocation goal){

        aStarNode startN = new aStarNode(null, start, goal);

        ArrayList<aStarNode> open = new ArrayList<aStarNode>();
        ArrayList<aStarNode> closed = new ArrayList<aStarNode>();

        aStarNode q = null;
        open.add(startN);


        while(!open.isEmpty()){
            int index = 0;
            double max = Double.MAX_VALUE;

            for(int i = 0; i < open.size(); i++){
                if(open.get(i).f < max){
                    max = open.get(i).f;
                    q = open.get(i);
                    index = i;
                }
            }
            open.remove(index);
            closed.add(q);

            if(q.curr.equals(goal)){
                return q;
            }


            ArrayList<aStarNode> successors = new ArrayList<aStarNode>();
            int j = 0;
            for(Direction d: allDirections){
                if(d != Direction.OMNI && d != Direction.NONE && getMapLoc(q.curr.add(d)) != 2){
                    successors.add(new aStarNode(q, q.curr.add(d), goal));
                }
            }

            for(aStarNode s: successors){


                boolean flag = false;

                for(int i = 0; i < closed.size(); i++){
                    if(closed.get(i).curr.equals(s.curr)){
                        flag = true;
                    }
                }

                if(flag == false){
                    for(int i = 0; i < open.size(); i++){
                        if(s.curr.equals(open.get(i))){
                                flag = true;
                        }
                    }

                    if(flag == false){
                        open.add(s);
                    }

                }

            }
        }
        return new aStarNode(null, null, null);
    }


    private static void duo() throws GameActionException {

        currLoc = rc.getLocation();
        int id = rc.getRobot().getID();
        int leader;
        int follower;

        leader = rc.readBroadcast(101);
        follower = rc.readBroadcast(102);

        if(leader == 0){
            rc.broadcast(101, id);
            leader = id;
        }else if(follower == 0 && id != leader){
            rc.broadcast(102, id);
            follower = id;
        }


        if(id == leader){

            double max = 0.0;
            MapLocation cowLoc = new MapLocation(0,0);
            double[][] cowGrowth = rc.senseCowGrowth();

            for(int i = 0; i < cowGrowth.length; i++){
                for(int j = 0; j < cowGrowth[i].length; j++){
                    //if the growth is the greatest that is within 100 squared units from the current location
                    if (cowGrowth[i][j] > max && currLoc.distanceSquaredTo(new MapLocation(i,j)) < 100){
                        max = cowGrowth[i][j];
                        cowLoc = new MapLocation(i, j);
                    }
                }
            }

            //broadcast location of cows we are headed towards
            //and head towards the cows
            if(cowLoc.equals(currLoc)){

            }
            rc.broadcast(103,locToInt(cowLoc));
            if(rc.canMove(currLoc.directionTo(cowLoc))){
                rc.move(currLoc.directionTo(cowLoc));
            }else{
                while(true){
                    Direction randDir = allDirections[(int)(Math.random()*8)];
                    if(rc.canMove(randDir)){
                        rc.move(randDir);
                        break;
                    }
                }
            }

        }else if(id == follower){

            MapLocation mapLoc = intToLoc(rc.readBroadcast(103));
            if(rc.canMove(currLoc.directionTo(mapLoc))){
                rc.move(currLoc.directionTo(mapLoc));
            }else{
                while(true){
                    Direction randDir = allDirections[(int)(Math.random()*8)];
                    if(rc.canMove(randDir)){
                        rc.move(randDir);
                        break;
                    }
                }
            }

        }else{
            while(true){
                Direction randDir = allDirections[(int)(Math.random()*8)];
                if(rc.canMove(randDir)){
                    rc.move(randDir);
                    break;
                }
            }
        }

    }


    private static int locToInt(MapLocation m){
        return (m.x*100 + m.y);
    }

    private static MapLocation intToLoc(int i){
        return new MapLocation(i/100,i%100);
    }



    private static void runHeadquarters() throws GameActionException {
        Direction spawnDir = Direction.NORTH;
        if(rc.isActive()&&rc.canMove(spawnDir)&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
            rc.spawn(Direction.NORTH);
        }
    }
}