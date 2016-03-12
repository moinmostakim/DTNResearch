/* 
 * Copyright 2007 TKK/Netlab
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;
import java.util.List;

import core.Settings;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;

/**
 * Map based movement model that uses Dijkstra's algorithm to find shortest
 * paths between two random map nodes and Points Of Interest
 */
public class RandomEventMovement extends MapBasedMovement {
    
    /** the Dijkstra shortest path finder */
    private DijkstraPathFinder pathFinder;    
    private SimMap map;
    private String homeCenterType;
    private MapNode home;    
    private boolean isReturn;
    
    public RandomEventMovement(Settings settings) {
        super(settings);
                
        this.map = getMap();
        this.homeCenterType = settings.getSetting("homeCenterType");
        this.pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
        this.isReturn = false;     
    }
	
	/**
	 * Copyconstructor.
	 * @param mbm The ShortestPathMapBasedMovement prototype to base 
	 * the new object to 
	 */
	protected RandomEventMovement(RandomEventMovement rem) {
            
            super(rem);
            this.pathFinder = rem.pathFinder;	
            this.map = rem.map;                
            this.homeCenterType = rem.homeCenterType;
            this.home = getHome();            
            this.isReturn = rem.isReturn;
	}
	
        
    private MapNode getHome()
    {
        MapNode currCenter;        
        currCenter = DisasterMovement.getCenters().get(homeCenterType).get(rng.nextInt(DisasterMovement.getCenters().get(homeCenterType).size()));
        return currCenter;
    }
    
    
    @Override
    public Path getPath() {
        Path p = new Path(generateSpeed());
        MapNode to = null;
        int index = 0;

        //System.out.println("home " + homePoints);
        if (isReturn)
        {   
            to = home;
            isReturn = false;
        }
        else
        {   
            do{
                index = (int)(rng.nextDouble() * map.getNodes().size());                            
                to = map.getNodes().get(index);                
            }while( getOkMapNodeTypes()!= null && (!to.isType(getOkMapNodeTypes()) || to.equals(home) || to.getLocation().distance(home.getLocation()) > 2000));

            isReturn = true;                        
        }

        //System.out.println("Moving from " + lastMapNode + " to " + to + " " + isReturn);

        List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode, to);

        // this assertion should never fire if the map is checked in read phase
        assert nodePath.size() > 0 : "No path from " + lastMapNode + " to " +
                to + ". The simulation map isn't fully connected";

        for (MapNode node : nodePath) { // create a Path from the shortest path
                p.addWaypoint(node.getLocation());
        }

        lastMapNode = to;

        return p;
    }	

    @Override
    public Coord getInitialLocation() 
    {        
        lastMapNode = home;
        return home.getLocation().clone();
    }
    
	@Override
	public RandomEventMovement replicate() {
		return new RandomEventMovement(this);
	}
}
