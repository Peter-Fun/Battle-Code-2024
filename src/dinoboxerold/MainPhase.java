package dinoboxerold;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import battlecode.common.*;

public class MainPhase {
    
    public static void runMainPhase(RobotController rc) throws GameActionException {
    

        // Buy global upgrade (prioritize capturing)
        if(rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
            rc.buyGlobal(GlobalUpgrade.ACTION);
        } 
        else if(rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
            rc.buyGlobal(GlobalUpgrade.CAPTURING);
        }

        //attack enemies, prioritizing enemies that have your flag
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] nearbyFriendlies = rc.senseNearbyRobots(-1, rc.getTeam());
        MapLocation[] nearbyFriendliesSpots = new MapLocation[nearbyFriendlies.length];
        for(RobotInfo robot : nearbyEnemies) {
            if(robot.hasFlag()) {
                Pathfind.moveTowards(rc, robot.getLocation(), true);
                if(rc.canAttack(robot.getLocation())) rc.attack(robot.getLocation());
            }
        }
        for(RobotInfo robot : nearbyEnemies) {
            if(rc.canAttack(robot.getLocation())) {
                rc.attack(robot.getLocation());
            }
        }
        //try to heal friendly robots
        for(int i = 0; i < nearbyFriendlies.length; i++) {
            RobotInfo robot = nearbyFriendlies[i];
            if(rc.canHeal(robot.getLocation())) rc.heal(robot.getLocation());
            nearbyFriendliesSpots[i] = robot.getLocation();
            
        }

        if(!rc.hasFlag()) {
            //move towards the closest enemy flag (including broadcast locations)
            ArrayList<MapLocation> flagLocs = new ArrayList<>();
            FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
            for(FlagInfo flag : enemyFlags) flagLocs.add(flag.getLocation());
            if(flagLocs.size() == 0) {
                MapLocation[] broadcastLocs = rc.senseBroadcastFlagLocations();
                for(MapLocation flagLoc : broadcastLocs) flagLocs.add(flagLoc);
            }

            MapLocation closestFlag = findClosestLocation(rc.getLocation(), flagLocs);
            boolean taken = false; // move to closest flag if not taken already
            if(closestFlag != null) {
                for (MapLocation spot : nearbyFriendliesSpots){
                    if (spot.equals(closestFlag)){
                        taken = true;
                        break;
                    }
                }
                if (!taken){
                    if ((rc.getLocation().distanceSquaredTo(closestFlag) <= 36 || nearbyEnemies.length > 5) && rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())){
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                    }
                    Pathfind.moveTowards(rc, closestFlag, true);

                    if(rc.canPickupFlag(closestFlag)) rc.pickupFlag(closestFlag);
                }
                else{
                    if (rc.getLocation().distanceSquaredTo(closestFlag) >= 20){
                        Pathfind.moveTowards(rc, closestFlag, true);
                    }
                    else{
                        Pathfind.explore(rc);
                    }
                }
            }
            else {
                //if there are no dropped enemy flags, explore randomly
                Pathfind.explore(rc);
            }
        }
        else {
            //if we have the flag, move towards the closest ally spawn zone
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation closestSpawn = findClosestLocation(rc.getLocation(), Arrays.asList(spawnLocs));
            Pathfind.bugNavOne(rc, closestSpawn);
        }
    }

    public static MapLocation findClosestLocation(MapLocation me, List<MapLocation> otherLocs) {
        MapLocation closest = null;
        int minDist = Integer.MAX_VALUE;
        for(MapLocation loc : otherLocs) {
            int dist = me.distanceSquaredTo(loc);
            if(dist < minDist) {
                minDist = dist;
                closest = loc;
            }
        }
        return closest;
    }
}
