/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package movement;

import core.Coord;
import core.Settings;
import java.util.StringTokenizer;

/**
 *
 * @author Yusuf Sarwar
 */
public class RegularRouteMovement extends MovementModel{
    private final static String REG_ROUTE_MOVEMENT_MODEL_NS="RegularRouteMovement"; 
    private int totalRoutes, routeNo;
    private double gridScaleX, gridScaleY;
    private Coord gridOffset;
    private Route route;
    private static String routeStrings[];
    
       
    @SuppressWarnings("static-access")
    public RegularRouteMovement(RegularRouteMovement rg)
    {        
        super(rg);                
        this.gridOffset = new Coord(rg.gridOffset.getX(), rg.gridOffset.getY());
        this.gridScaleX = rg.gridScaleX;
        this.gridScaleY = rg.gridScaleY;
        this.totalRoutes = rg.totalRoutes;        
        //this.routeNo = (int)(Math.random() * totalRoutes);
        this.route = null;
    }
    
    public RegularRouteMovement(Settings settings) 
    {   
        super(settings);        
        readProperties(settings);                
    }
    
    private void readProperties(Settings settings)
    {
        settings.setNameSpace(REG_ROUTE_MOVEMENT_MODEL_NS);
        totalRoutes = Integer.parseInt(settings.getSetting("noOfRoutes"));                
        //System.out.println("No of routes " + totalRoutes);
        
        String tmp = settings.getSetting("gridOffset").trim();
        double x = Double.parseDouble(tmp.substring(0, tmp.indexOf(",")));
        double y = Double.parseDouble(tmp.substring(tmp.indexOf(",") + 1, tmp.length()));
        gridOffset = new Coord(x, y);
        
        gridScaleX = Double.parseDouble(settings.getSetting("gridScaleX"));
        gridScaleY = Double.parseDouble(settings.getSetting("gridScaleY"));            
        
        routeStrings = new String[totalRoutes];
        for(int i = 0 ; i < totalRoutes; i++)
            routeStrings[i] = settings.getSetting("route"+ i);
    }
    
    private void buildRoute()
    {
        route = new Route(gridOffset, gridScaleX, gridScaleY);        
        int i=0;
        for(i = 0 ; i < totalRoutes; i++)
            if (routeStrings[i] != null)                            
                break;            
        if (i == totalRoutes) return;
        routeNo = i;
        
        String string = routeStrings[routeNo];
        StringTokenizer stk = new StringTokenizer(string, ",");

        while(stk.hasMoreTokens())
        {
            String value = stk.nextToken().trim();                
            parseRouteFor(route, value);
        }            
        routeStrings[routeNo] = null;
        //System.out.println("Route no: " + routeNo + "=>" + route);
    }
    
    private void parseRouteFor(Route route, String value)
    {
        String tmp = value.substring(1, value.length() - 1);
        StringTokenizer stk = new StringTokenizer(tmp, " ");
        String firstToken = stk.nextToken();
        String secondToken = stk.nextToken();
        
        if (firstToken.indexOf("-") > 0)
            addPoints(route, firstToken, secondToken, false);
        else
            addPoints(route, secondToken, firstToken, true);        
    }
    
    private void addPoints(Route route, String firstToken, String secondToken, boolean reverseOrder)
    {        
        String value1 = firstToken.substring(0, firstToken.indexOf("-") );
        String value2 = firstToken.substring(firstToken.indexOf("-")+1, firstToken.length());
        String other = secondToken;

        double a = Double.parseDouble(value1);
        double b = Double.parseDouble(value2);
        double otherValue = Double.parseDouble(other);
        Coord c;
        
        if (a < b)
        {
            while(a <= b)
            {
                if (reverseOrder)                        
                    c = new Coord(otherValue, a);
                else    
                    c = new Coord(a, otherValue);
                
                route.addCoord(c, generateSpeed());
                a++;               
            }
        }
        else
        {
        //    if (route.length() > 0)
        //        a--;
            while(a >= b)
            {
                if (reverseOrder)
                    c = new Coord(otherValue, a);
                else
                    c = new Coord(a, otherValue);
                
                route.addCoord(c, generateSpeed());
                a--;
               // System.out.println(c); 
            }
        }     
    }
    
    public static void main(String args[])
    {
        new BusRouteMovement();
    }    
    
    @Override
    public Path getPath() {
        
        if (route == null)
            buildRoute();
        route.getRoute().resetWpIndex();
        return route.getRoute();
    }

    @Override
    public Coord getInitialLocation() {               
        if (route == null)
            buildRoute();
        Coord c = route.getRoute().getCoords().get(0);
        //System.out.println(c);
        return c;
    }

    @Override
    public MovementModel replicate() {
        return new RegularRouteMovement(this);
    }
}


