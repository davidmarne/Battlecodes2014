package bytecodetestplayer;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {
	static Random rand;
	
	public static void run(RobotController rc) {
		while(true) {
			if (rc.getType() == RobotType.HQ) {
                int start = 0;
				try {
                    int[] arr = new int[10];

                    start = Clock.getBytecodesLeft();
                    // code to test here

                    int r = arr[15];



				} catch (Exception e) {
//					e.printStackTrace();
                    System.out.println((start - Clock.getBytecodesLeft()));
				}
			}
			rc.yield();
		}
	}
}
