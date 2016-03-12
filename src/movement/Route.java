/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package movement;

import core.Coord;

/**
 *
 * @author Yusuf Sarwar
 */
public class Route
{
    private Path path;
    private Coord offset;
    private double gridScaleX, gridScaleY;
    
    public Route(Coord offset, double gridScaleX, double gridScaleY)
    {
        this.offset = offset;
        this.gridScaleX = gridScaleX;
        this.gridScaleY = gridScaleY;
        this.path = new Path();
    }
    
    public void addCoord(Coord coord, double speed)
    {   
        double x, y;
        x = offset.getX() + coord.getX() * gridScaleX;
        y = offset.getY() + coord.getY() * gridScaleY;
        
        path.addWaypoint(new Coord(x, y), speed);                
        //points.add(coord);
    }
    
    public void add(Coord coord, double speed)
    {
        path.addWaypoint(coord, speed);
    }
    
    public Path getRoute()
    {
        return path;
    }
    
    @Override
    public String toString()
    {       
        return path.toString();        
    }
    
    public int length()
    {
        return path.getCoords().size();
    }   
}

