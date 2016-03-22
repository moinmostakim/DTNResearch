/* 
 * Copyright 2007 TKK/Netlab
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;
import core.DTNHost;
import core.DTNSim;
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

public class InCenterMovement extends MapBasedMovement {
    
    public static final String POI_NS = "PointsOfInterest";
    
    /** Points Of Interest file path -prefix id ({@value})*/
    public static final String POI_FILE_S = "poiFile";

    /** the Dijkstra shortest path finder */
    private DijkstraPathFinder pathFinder;
    
    private SimMap map;
    private List<MapNode> centers;
    private List<MapNode> targets;
    private int nrOfTargets;
    
    public static int noOfCenters;
    
    private MapNode home;
    
    private boolean isReturn;
    private Coord offset;
    private double scaleX, scaleY;        
    
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public InCenterMovement(Settings settings) {
            super(settings);
            
            this.map = getMap();
            this.pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());        
            centers = new ArrayList<MapNode>();
            
           // settings.setNameSpace(MulticopyContactRouter.contact_NS);
            nrOfTargets = Integer.parseInt(settings.getSetting("nrOfTargets"));
                        
            //centers.add(map.getNodeByCoord(new Coord(1200,1200+600)));
            //centers.add(map.getNodeByCoord(new Coord(1200,2200+600)));
            //centers.add(map.getNodeByCoord(new Coord(2200,2200+600)));
            //centers.add(map.getNodeByCoord(new Coord(2200,1200+600)));
                        
            this.isReturn = false;     
	}
	
	/**
	 * Copyconstructor.
	 * @param mbm The ShortestPathMapBasedMovement prototype to base 
	 * the new object to 
	 */
	protected InCenterMovement(InCenterMovement rem) {
            
            super(rem);
            this.pathFinder = rem.pathFinder;	
            this.map = rem.map;                
            this.centers = rem.centers;            
            
            nrOfTargets = rem.nrOfTargets;
            
            home = getHome();
            this.offset = rem.offset;
            this.scaleX = rem.scaleX;
            this.scaleY = rem.scaleY;
	}
        
        static
        {
            DTNSim.registerForReset(InCenterMovement.class.getCanonicalName());
            reset();
        }
        public static void reset()
        {
            noOfCenters = 0;
        } 
	
        /*
    private  void getCenters()
    {       
        centers = new ArrayList<MapNode>();
        for(int i=0; i < 50; i++) // 50 possible centers
        {
            MapNode currCenter;            
            do{
                int index  = (int)(rng.nextDouble() * map.getNodes().size());
                currCenter = map.getNodes().get(index);                
                if (centers.contains(currCenter)) continue;
                
            }while( getOkMapNodeTypes()!= null && !currCenter.isType(getOkMapNodeTypes()));
            
            centers.add(currCenter);        
        }
    }
    */
        
    private MapNode getHome()
    {
        //if (noOfCenters < 4)
        //{
        //    noOfCenters++;
        //    return centers.get(noOfCenters-1);
        //}
        
        boolean tooClose = false;            
        MapNode currCenter;
        do{
            int index  = (int)(rng.nextDouble() * map.getNodes().size());
            currCenter = map.getNodes().get(index);                               
            tooClose = false;
            
            for(MapNode c : centers)
            {
                if (currCenter.getLocation().distance(c.getLocation()) < 500)
                {
                    tooClose = true;
                    break;
                }
            }                            
        }while(tooClose == true || (getOkMapNodeTypes()!= null && !currCenter.isType(getOkMapNodeTypes())));
                
        centers.add(currCenter);
        noOfCenters++;
               
        return currCenter;
    }
    
    public void adjustHome(List<DTNHost> hosts)
    {
        int index = noOfCenters - 1;                        
        DTNHost h = hosts.get(12 + index);  // Static node starts from here
        
        MovementModel mm = h.getMovement();
        
        //System.out.println("Target host: " + h + " " + mm);
        
        if (mm instanceof StaticMovement)
        {
            Coord c = centers.get(index).getLocation();
            ((StaticMovement)mm).setLocation(c);
            h.setLocation(c);
        }
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
                if (targets == null)
                    setTargets();
                index = (int)(targets.size() * rng.nextDouble());                            
                to = targets.get(index);

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
    
       void setTargets()
       {
           if (targets != null) return;
            
            targets = new ArrayList<MapNode>();
            int noOfTargets = Math.min(nrOfTargets, centers.size() - 1) ; //(int) (5 + 10.0 * rng.nextDouble());
            int index = 0;
            MapNode cand = null;
            
            for (int i = 0 ; i < noOfTargets; i++)
            {
                do
                { 
                    index = rng.nextInt(centers.size()); 
                    cand = centers.get(index);                                                           
                } while (home.equals(cand) || targets.contains(cand) || home.getLocation().distance(cand.getLocation()) > 3000);
                
                targets.add(cand);                        
            }                           
       }
       
    @Override
    public Coord getInitialLocation() 
    {        
        lastMapNode = home;       
        return home.getLocation().clone();
    }
    
	@Override
	public InCenterMovement replicate() {
		return new InCenterMovement(this);
	}
        
        public List<MapNode> getCenters()
        {
            return centers;
        }
}
