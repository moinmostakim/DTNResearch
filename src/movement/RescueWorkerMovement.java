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
public class RescueWorkerMovement extends MapBasedMovement {
    
    /** the Dijkstra shortest path finder */
    private DijkstraPathFinder pathFinder;    
    private SimMap map;    
    private MapNode home;    
    private String reportingCenter;
    private String servicingCenter;
    private boolean isReturn;
    
    public RescueWorkerMovement(Settings settings) {
        super(settings);
                
        this.reportingCenter = settings.getSetting("reportingCenter");
        this.servicingCenter = settings.getSetting("servicingCenter");
        this.map = getMap();       
        this.pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
        this.isReturn = false;     
    }
	
	/**
	 * Copyconstructor.
	 * @param mbm The ShortestPathMapBasedMovement prototype to base 
	 * the new object to 
	 */
	protected RescueWorkerMovement(RescueWorkerMovement rem) {
            
            super(rem);
            this.reportingCenter = rem.reportingCenter;
            this.servicingCenter = rem.servicingCenter;
            this.pathFinder = rem.pathFinder;	
            this.map = rem.map;                            
            this.home = getHome();            
            this.isReturn = rem.isReturn;
	}
	
        
    private MapNode getHome()
    {        
        MapNode currCenter;        
        currCenter = DisasterMovement.getCenters().get(reportingCenter).get(rng.nextInt(DisasterMovement.getCenters().get(reportingCenter).size()));
        return currCenter;
    }
    
    
    @Override
    public Path getPath() {
        Path p = new Path(generateSpeed());
        MapNode to = null;       

        //System.out.println("home " + homePoints);
        
        if (lastMapNode.equals(home) || rng.nextDouble() < 0.6)
        { 
            do{
                to = DisasterMovement.getCenters().get(servicingCenter).get(rng.nextInt(DisasterMovement.getCenters().get(servicingCenter).size()));
            }while(to.equals(lastMapNode)); // || to.getLocation().distance(home.getLocation()) > 500);
        }
        else
            to = home;
        
        //System.out.println("Moving from " + lastMapNode + " to " + to + " " + isReturn);

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
	public RescueWorkerMovement replicate() {
		return new RescueWorkerMovement(this);
	}
}
