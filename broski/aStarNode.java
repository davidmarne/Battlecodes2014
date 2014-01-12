package broski;

import battlecode.common.MapLocation;

/**
 * Created by davemarne on 1/10/14.
 */
public class aStarNode {
    aStarNode parent;
    MapLocation curr;
    double f;
    double g;
    double h;

    public aStarNode(aStarNode parent, MapLocation curr, MapLocation goal){
        this.parent = parent;
        this.curr = curr;


        this.h = curr.distanceSquaredTo(goal);
        if(parent != null){
            this.g = parent.g + 1;
            this.f = this.g + this.h ;
        }else{
            this.g = 0;
            this.f = this.g + this.h;
        }
    }
}
