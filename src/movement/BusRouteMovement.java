/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package movement;

import core.Coord;
import core.Settings;
import java.io.BufferedWriter;
import java.util.StringTokenizer;


/**
 *
 * @author mduddin2
 */
public class BusRouteMovement {
    private static final String BUS_ROUTE_MOVEMENT_SETTING_FILE = "busroutemovement.txt";
    private String constantString;
    private int routeNo, nodeNo;
    private double gridScaleX, gridScaleY;
    private Coord gridOffset;
    private Route routes[];
    private Settings busRouteMovementSettings;    
    private int gridDimensionX, gridDimensionY;
    private double simEndTime;
    
    public BusRouteMovement() 
    {        
        BufferedWriter fout = null; 
        double time;
        int nodeCount;
        
            busRouteMovementSettings = new Settings();
            Settings.init(BUS_ROUTE_MOVEMENT_SETTING_FILE);
            readProperties();
            buildRoutes();                    
            System.out.println("Routes are built");            
            
            /*
        try {                        
            
            fout = new BufferedWriter(new FileWriter("movement_trace.txt", true));                                
            fout.append(constantString + "\n");
            Iterator<Coord>[] routesEnums = null; //(Iterator<Coord>[])Array.newInstance(Iterator.class, routeNo);
            for(int k = 0; k < routeNo; k++ )
                routesEnums[k] = routes[k].getRoute().getCoords().iterator();          
            time = 0;
            
            nodeCount = 0;
            while(time <= simEndTime)
            {       
                for (nodeCount = 0; nodeCount < nodeNo; nodeCount++)
                //for(int k = 0; k < routeNo; k++)
                {
                    int k = nodeCount % routeNo;
                    if (routesEnums[k].hasNext() == false)
                        routesEnums[k] = routes[k].getRoute().getCoords().iterator();
                    Coord c = (Coord)routesEnums[k].next();
                    fout.append(time + " p" + nodeCount + " " + c.getX() + " " + c.getY() +"\n");
                    //System.out.println(" " + (i * 10) + " m36 " + c.getX() + " " + c.getY() +"\n");                    
                }
                time += 100.0;
            }
            fout.close();            
            
        } catch (IOException ex) {
            Logger.getLogger(BusRouteMovement.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fout.close();
            } catch (IOException ex) {
                Logger.getLogger(BusRouteMovement.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
             */ 
    }

    private void readProperties()
    {
        constantString = busRouteMovementSettings.getSetting("constantString");
        routeNo = Integer.parseInt(busRouteMovementSettings.getSetting("noOfRoutes"));        
        nodeNo = Integer.parseInt(busRouteMovementSettings.getSetting("noOfNodes"));        
        //System.out.println("No of routes " + routeNo);
        
        String tmp = busRouteMovementSettings.getSetting("gridOffset").trim();
        double x = Double.parseDouble(tmp.substring(0, tmp.indexOf(",")));
        double y = Double.parseDouble(tmp.substring(tmp.indexOf(",") + 1, tmp.length()));
        gridOffset = new Coord(x, y);
        
        gridScaleX = Double.parseDouble(busRouteMovementSettings.getSetting("gridScaleX"));
        gridScaleY = Double.parseDouble(busRouteMovementSettings.getSetting("gridScaleY"));
        routes = new Route[routeNo];
        
        gridDimensionX = Integer.parseInt(busRouteMovementSettings.getSetting("gridDimensionX"));
        gridDimensionY = Integer.parseInt(busRouteMovementSettings.getSetting("gridDimensionY"));
        
        gridDimensionX++;
        gridDimensionY++;
        
        simEndTime = Double.parseDouble(busRouteMovementSettings.getSetting("simEndTime"));
    }
    
    private void buildRoutes()
    {
        double cornerX, cornerY;
        cornerX = (gridOffset.getX() + (gridDimensionX + 1) * gridScaleX);
        cornerY = (gridOffset.getY() + (gridDimensionY + 1) * gridScaleY);      
       
        String lineString="";
        lineString = "0.0 0.0, ";
        lineString += cornerX + " 0.0, ";
        lineString += cornerX + " " + cornerY +", ";
        lineString += "0.0 " + cornerY + ", ";
        lineString += "0.0 0.0";
        
        lineString = "LINESTRING (" + lineString + ")";
        
        //System.out.println(lineString);
        
        lineString = "";
        
        lineString = "0.0 0.0, ";
        lineString += gridOffset.getX() + ", " + gridOffset.getY();
        lineString = "LINESTRING (" + lineString + ")";
        //System.out.println(lineString);
        
        lineString = "";
        for (int y = 0; y < gridDimensionY; y++)
        {
            for (int x = 0; x < gridDimensionX; x++)
            {
                if (lineString.equals(""))            
                    lineString = "LINESTRING (" + (gridOffset.getX() + x * gridScaleX) +  " " + (gridOffset.getY() + y * gridScaleY);                    
                else
                    lineString = lineString + ", " + (gridOffset.getX() + x * gridScaleX) +  " " + (gridOffset.getY() + y * gridScaleY);
            }
            lineString = lineString + ")";
            System.out.println(lineString);
            lineString = "";
        }
        
        lineString="";
        for (int x = 0; x < gridDimensionX; x++)
        {
            for (int y = 0; y < gridDimensionY; y++)
            {
                if (lineString.equals(""))            
                    lineString = "LINESTRING (" + (gridOffset.getX() + x * gridScaleX) +  " " + (gridOffset.getY() + y * gridScaleY);                    
                else
                    lineString = lineString + ", " + (gridOffset.getX() + x * gridScaleX) +  " " + (gridOffset.getY() + y * gridScaleY);
            }
            lineString = lineString + ")";
            System.out.println(lineString);
            lineString = "";
        }
        
        /*for (int i = 0; i < routeNo; i++)
        {
            routes[i] = new Route(gridOffset, gridScaleX, gridScaleY);
            String string = busRouteMovementSettings.getSetting("route"+ i);
            StringTokenizer stk = new StringTokenizer(string, ",");
            
            while(stk.hasMoreTokens())
            {
                String value = stk.nextToken().trim();                
                parseRouteFor(routes[i], value);
            }            
            
            System.out.println("" + routes[i]);
        }
        */
        
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
            //if (route.length() > 0)
            //    a++;
            while(a <= b)
            {
                if (reverseOrder)                        
                    c = new Coord(otherValue, a);
                else    
                    c = new Coord(a, otherValue);
                
                route.addCoord(c, 100.0);
                a++;
               // System.out.println(c); 
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
                
                route.addCoord(c, 100.0);
                a--;
               // System.out.println(c); 
            }
        }     
    }
    
    public static void main(String args[])
    {
        new BusRouteMovement();
    }    
}
