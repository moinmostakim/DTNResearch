/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package movement;

import core.Coord;
import core.Settings;

/**
 *
 * @author mduddin2
 */
public class StaticMovement extends MapBasedMovement{

    private Coord location= new Coord(0,0);
    
    public StaticMovement(StaticMovement rep)
    {
        super(rep);        
        this.location = rep.location;        
    }
    
    public StaticMovement(Settings settings)
    {
        super(settings);        
    }
    
    public void setLocation(Coord l)
    {
        location = new Coord(l.getX(), l.getY());
        //System.out.println(this + " " + l);
    }
    
    public Coord getLocation()
    {
        return location;
    }
    
    @Override
    public Path getPath() {                
        return null;
    }

    @Override
    public Coord getInitialLocation() {
        return location;
    }

    @Override
    public StaticMovement replicate() {        
        return new StaticMovement(this);
    }
}
