/* 
 * Copyright 2007 TKK/Netlab
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;
import core.DTNHost;
import java.util.List;

import core.Settings;
import java.util.ArrayList;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;
//import routing.MulticopyContactRouter;

/**
 * Map based movement model that uses Dijkstra's algorithm to find shortest
 * paths between two random map nodes and Points Of Interest
 */
public class PolicePatrolMovement extends MapBasedMovement {
    
    /** the Dijkstra shortest path finder */
    private DijkstraPathFinder pathFinder;
    
    private SimMap map;
    private MapNode home;
    private List<MapNode> targets;    
    
    private int nextTarget;
    /**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public PolicePatrolMovement(Settings settings) {
            super(settings);
            
            this.map = getMap();
            this.pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());            
	}
	
	/**
	 * Copyconstructor.
	 * @param mbm The ShortestPathMapBasedMovement prototype to base 
	 * the new object to 
	 */
	protected PolicePatrolMovement(PolicePatrolMovement rem) {
            
            super(rem);
            this.pathFinder = rem.pathFinder;	
            this.map = rem.map;                            
            home = getHome();
            this.targets = getTargets();
	}
	
    private List<MapNode> getTargets()
    {        
        List <MapNode> trgs = new ArrayList<MapNode>();
        List<MapNode> hhs = DisasterMovement.getCenters().get("neighborhood");
        int N = 2 + rng.nextInt(hhs.size()-2);
        
        for(int i = 0; i < N; i++) // iterate over 4 zones, 2,3,4,5
        {            
            trgs.add(hhs.get(rng.nextInt(hhs.size())));
        }
        
        trgs.add(home);
        nextTarget = 0;
        return trgs;
    }
        
    private MapNode getHome()
    {            
        List<MapNode> ps = DisasterMovement.getCenters().get("policestation");       
        return ps.get(rng.nextInt(ps.size()));
    }
    
    
	@Override
	public Path getPath() {
            Path p = new Path(generateSpeed());
            MapNode to = null;
            
            if (lastMapNode.equals(home) || rng.nextDouble() < 0.5)
            {
                to = targets.get(nextTarget);
                nextTarget = (nextTarget + 1) % targets.size();
                if (nextTarget == 0)
                {
                    targets = getTargets();
                }
            }
            else
            {
                do
                {                   
                    to = map.getNodes().get(rng.nextInt(map.getNodes().size()));
                }while(to.getLocation().distance(lastMapNode.getLocation()) > 200);
            }
            
            List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode, to);
            
            if (nodePath == null)
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
    
    @Override
    public Coord getInitialLocation() 
    {        
        lastMapNode = home;       
        return home.getLocation().clone();
    }
    
	@Override
	public PolicePatrolMovement replicate() {
		return new PolicePatrolMovement(this);
	}    
}
