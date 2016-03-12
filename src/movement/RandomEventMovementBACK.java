/* 
 * Copyright 2007 TKK/Netlab
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;
import java.util.List;

import core.Settings;
import core.SettingsError;
import input.WKTReader;
import java.io.File;
import java.io.IOException;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;

/**
 * Map based movement model that uses Dijkstra's algorithm to find shortest
 * paths between two random map nodes and Points Of Interest
 */
public class RandomEventMovementBACK extends MapBasedMovement {
    
    public static final String POI_NS = "PointsOfInterest";
    
    /** Points Of Interest file path -prefix id ({@value})*/
	public static final String POI_FILE_S = "poiFile";

	/** the Dijkstra shortest path finder */
	private DijkstraPathFinder pathFinder;
    
    private MapNode home;

    private SimMap map;
    private boolean isReturn;
    private Coord offset;
    private double scaleX, scaleY;        
    private MapNode[] centers;
    
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public RandomEventMovementBACK(Settings settings) {
		super(settings);
		this.map = getMap();
        this.pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());        
        settings.setNameSpace("RegularRouteMovement");
        
        offset = new Coord(400, 400);
        scaleX = settings.getDouble("gridScaleX");
        scaleY = settings.getDouble("gridScaleY");
        
        this.home = getHome();
        
        this.isReturn = false;        
	}
	
	/**
	 * Copyconstructor.
	 * @param mbm The ShortestPathMapBasedMovement prototype to base 
	 * the new object to 
	 */
	protected RandomEventMovementBACK(RandomEventMovementBACK rem) {
		super(rem);
		this.pathFinder = rem.pathFinder;	     
            this.map = rem.map;
            this.home = getHome();        
            this.isReturn = rem.isReturn;     
            this.offset = rem.offset;
            this.scaleX = rem.scaleX;
            this.scaleY = rem.scaleY;
	}
     private void getCenters()
    {
        Settings fileSettings = new Settings(POI_NS);
		WKTReader reader = new WKTReader();
		List<Coord> pts = null;
        
		File poiFile = null;
		
		try {
			poiFile = new File(fileSettings.getSetting(POI_FILE_S + 1)); 
			pts = reader.readPoints(poiFile);
		}
        catch(IOException ioex)
        {
            throw new SettingsError("couldn't read the POIS file at " + this.getClass().getName() + " MSG " + ioex.getMessage());
        }
        
        centers = new MapNode[pts.size()];
        for(int i=0; i < pts.size(); i++)
        {
            Coord c = pts.get(i);
            c.setLocation(scaleX * c.getX(), scaleY * c.getY());
            c.translate(offset.getX(), offset.getY());            
            centers[i] = map.getNodeByCoord(c);
        }
    }
     
    private MapNode getHome()
    {
     
        MapNode c = centers[(int)(rng.nextDouble() * centers.length)];                
        return map.getNodeByCoord(c.getLocation());
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
            //index = (int)(rng.nextDouble() * homePoints.size());
            // System.out.println("Index " + index);
            //from = map.getNodeByCoord(homePoints.get(index));           
            
            do{
                index = (int)(rng.nextDouble() * map.getNodes().size());                            
                to = map.getNodes().get(index);                
            }while( getOkMapNodeTypes()!= null && !to.isType(getOkMapNodeTypes()));
            
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
	public RandomEventMovementBACK replicate() {
		return new RandomEventMovementBACK(this);
	}
}
