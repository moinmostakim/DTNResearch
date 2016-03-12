/* 
 * Copyright 2007 TKK/Netlab
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;
import java.util.List;

import core.Settings;
import core.SimClock;
import movement.distribution.ParetoDistribution;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;

/**
 * Map based movement model that uses Dijkstra's algorithm to find shortest
 * paths between two random map nodes and Points Of Interest
 */
public class HumanMovement extends MapBasedMovement {
    
    /** the Dijkstra shortest path finder */
    private DijkstraPathFinder pathFinder;    
    private SimMap map;    
    private MapNode home;    
    private double radius;
    private String homeCenter;
    private MapNode evacuationCenter;
    private double stayProb;
    private boolean isReturn, stayHome;
    private double returnTime;
    
    public HumanMovement(Settings settings) {
        super(settings);
                
        this.homeCenter = settings.getSetting("homeCenter");
        this.radius = Double.parseDouble(settings.getSetting("radius"));
        this.stayProb = Double.parseDouble(settings.getSetting("stayProb"));
        this.map = getMap();       
        this.evacuationCenter = null;
        this.pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
        this.isReturn = this.stayHome = false;     
    }
	
	/**
	 * Copyconstructor.
	 * @param mbm The ShortestPathMapBasedMovement prototype to base 
	 * the new object to 
	 */
	protected HumanMovement(HumanMovement rem) {
            
            super(rem);
            this.homeCenter = rem.homeCenter;
            this.radius = rem.radius;
            this.pathFinder = rem.pathFinder;	
            this.stayProb = rem.stayProb;
            this.map = rem.map;                            
            this.home = getHome();   
            this.evacuationCenter = getTheNearestEvacuationCenter(home);
            this.isReturn = rem.isReturn;            
	}
	
        
    private MapNode getHome()
    {        
        MapNode currCenter;        
        currCenter = DisasterMovement.getCenters().get(homeCenter).get(rng.nextInt(DisasterMovement.getCenters().get(homeCenter).size()));
        return currCenter;
    }
        
    @Override
    public Path getPath() {
        Path p = new Path(generateSpeed());
        MapNode to = null;

        if (stayHome)
        {
            do{
                to = map.getNodes().get(rng.nextInt(map.getNodes().size()));
            }while(to.getLocation().distance(home.getLocation()) > radius);
        }
        else
        {
            if(isReturn && SimClock.getTime() > returnTime)
            {
                to = home;
                isReturn = false;
                stayHome = true;                                
            }
            else
            if (!isReturn)
            {        
                //System.out.println("Going to Evacuation center: " + evacuationCenter.getLocation());
                to = evacuationCenter;
                isReturn = true;
            }
            else
                return null;
        }
        
        List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode, to);   

        if(nodePath == null)
            return null;
        
        // this assertion should never fire if the map is checked in read phase
        assert nodePath.size() > 0 : "No path from " + lastMapNode + " to " +
                to + ". The simulation map isn't fully connected";

        for (MapNode node : nodePath) { // create a Path from the shortest path
                p.addWaypoint(node.getLocation());
        }

        lastMapNode = to;

        return p;
    }	

    private MapNode getTheNearestEvacuationCenter(MapNode h)
    {
        List<MapNode> centers = DisasterMovement.getCenters().get("evacuationcenter");
        double minDistace  = -1.0;
        MapNode closestCenter = null;
        
        for(MapNode n : centers)
        {
            double distance = n.getLocation().distance(home.getLocation());
            if (minDistace < 0 || minDistace > distance)
            {
                minDistace = distance;
                closestCenter = n;
            }
        }
                
        if (rng.nextDouble() < stayProb)
            stayHome = true;

        /* Determine the return time from the evacuation center */
        returnTime = new ParetoDistribution(24*60*60, 48*60*60, "pareto", rng).nextValue();
                
        return closestCenter;
    }
    @Override
    public Coord getInitialLocation() 
    {        
        lastMapNode = home;
        return home.getLocation().clone();
    }
    
	@Override
	public HumanMovement replicate() {
		return new HumanMovement(this);
	}
}
