/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package movement;

import core.Coord;
import core.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import movement.map.MapNode;
import movement.map.SimMap;

/**
 *
 * @author mduddin2
 */
public class ReliefCenterMovement extends MapBasedMovement{

    private SimMap map;
    private MapNode center;
    private String centerType;
    private static int neighborhoodIndex = 0;
            
    public ReliefCenterMovement(ReliefCenterMovement rep)
    {
        super(rep);  
        this.map = rep.map;
        this.centerType = rep.centerType;
        this.center = findNodeAsCenter();
    }
    
    public ReliefCenterMovement(Settings settings)
    {
        super(settings);
        this.map = getMap();        
        this.centerType = settings.getSetting("centerType");        
    }
    
    private MapNode findNodeAsCenter()
    {        
        Map<String, List<MapNode>> allCenters = DisasterMovement.getCenters();
        
        if (allCenters.get(centerType) == null)
        {
            allCenters.put(centerType, new ArrayList<MapNode>());
        }
        
        List<MapNode> centers = allCenters.get(centerType);
        MapNode candidate = null;
                
        if (neighborhoodIndex == allCenters.get("neighborhood").size())
            neighborhoodIndex = 0;
        
        MapNode neighborhood = allCenters.get("neighborhood").get(neighborhoodIndex++);
        
        boolean again = false;
        do
        {
            again = false;
            candidate = map.getNodes().get(rng.nextInt(map.getNodes().size()));                                              
            
            again = neighborhood.getLocation().distance(candidate.getLocation()) > 500;
            
        }while((getOkMapNodeTypes()!= null && !candidate.isType(getOkMapNodeTypes())) || centers.contains(candidate) || again);
        
        centers.add(candidate);
        return candidate;               
    }  
    
    @Override
    public Path getPath() {                
        return null;
    }

    @Override
    public Coord getInitialLocation() {
        return center.getLocation();
    }
    
    @Override
    public ReliefCenterMovement replicate() {        
        return new ReliefCenterMovement(this);
    }
}
