/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author samsung
 */
public class AvailibilityRouter extends ActiveRouter {

    Map<DTNHost, Double> lastSeen = new HashMap<>();

    public AvailibilityRouter(Settings s) {
        super(s);
    }

    protected AvailibilityRouter(AvailibilityRouter ar) {

        super(ar);
    }

    @Override
    public AvailibilityRouter replicate() {

        return new AvailibilityRouter(this);
    }

    //@Override
    /*public void changedConnection(Connection con) {
        super.changedConnection(con); //To change body of generated methods, choose Tools | Templates.
        System.out.println("Start of changed connection. ");
        DTNHost otherNode = con.getOtherNode(this.getHost());
       if (con.isUp()) {
            System.out.println("host: " + this.getHost() + " other Node: "+ otherNode);
           if (otherNode != null && lastSeen.get(otherNode)!=null) {
                double offDuration = SimClock.getTime() - lastSeen.get(otherNode);
               // System.out.println("In if ");
                System.out.println(con+" " + offDuration );
                Double simTime = SimClock.getTime();
                lastSeen.put(con.getOtherNode(this.getHost()), simTime);
            }
            else if(otherNode != null && lastSeen.get(otherNode)==null)
            {
                // First time i have encountered the otherNode
                Double time  = SimClock.getTime();
                System.out.println("Connected for first time." + time);
                 lastSeen.put(otherNode,time);
            }
        }
        if (!con.isUp()) {
            double downTime = SimClock.getTime();
            System.out.println("connection is going down" + downTime);
            DTNHost otherNode1 = con.getOtherNode(this.getHost());
            System.out.println("othernode : " + otherNode1 + " disconnecting host "+ this.getHost());

        }
        
        System.out.println("Out of the changed connection function ");

        //System.out.println(con + " " + con.isUp());
    }*/
    

}
