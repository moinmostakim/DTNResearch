/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.ConnectionListener;
import core.DTNHost;
import core.SimClock;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import routing.AvailibilityRouter;
import routing.DirectDeliveryRouter;
import routing.MessageRouter;

/**
 *
 * @author samsung
 */
public class SuccsessOfAvailibility extends Report implements ConnectionListener {

    Map<DTNHost, AvailibilityRouter> hostMapped;
    Map<DTNHost, DirectDeliveryRouter> hostMapped2;

    String router = "";
    String header = "Host" + " \t " + "Total Query" + " \t " + "Success";

    public SuccsessOfAvailibility() {

        init();
        write(header);
    }

    @Override
    protected void init() {
        super.init();
        router = getSettings().getSetting("router");
        System.out.println("router" + router);
        if (router.equalsIgnoreCase("AvavibilityRouter")) {
            hostMapped = new HashMap<>();
        } else {
            hostMapped2 = new HashMap<>();
        }

    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {

        if (!router.isEmpty() && router.equalsIgnoreCase("AvavibilityRouter")) {
            hostMapped.put(host1, (AvailibilityRouter) host1.getRouter());
            hostMapped.put(host2, (AvailibilityRouter) host2.getRouter());
        } else {
            hostMapped2.put(host1, (DirectDeliveryRouter) host1.getRouter());
            hostMapped2.put(host2, (DirectDeliveryRouter) host2.getRouter());

        }

    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void done() {

        if (!router.isEmpty() && router.equalsIgnoreCase("AvavibilityRouter")) {

            Map<DTNHost, AvailibilityRouter> tempMap = new HashMap<>();
            tempMap.putAll(hostMapped);
            ValueComparator vc = new ValueComparator(tempMap);
            TreeMap<DTNHost, AvailibilityRouter> treeMap = new TreeMap<>(vc);
            treeMap.putAll(tempMap);
            Set<Map.Entry<DTNHost, AvailibilityRouter>> entrySet = treeMap.entrySet();
            Iterator<Map.Entry<DTNHost, AvailibilityRouter>> iterator = entrySet.iterator();
            int totalsum = 0;
            int totalSuccess = 0;
            double totalLatency = 0;
            while (iterator.hasNext()) {
                Map.Entry<DTNHost, AvailibilityRouter> next = iterator.next();
                DTNHost host = next.getKey();
                AvailibilityRouter hostRouter = next.getValue();
                double averageLatencyOfQueryReceived = averageLatencyOfQueryReceived(hostRouter);
                write(host + " \t " + hostRouter.getTotalQueryObject()
                        + " \t " + hostRouter.getSuccesfullyRetrived()
                        + " \t " + averageLatencyOfQueryReceived);
                totalLatency += averageLatencyOfQueryReceived;
                totalsum += hostRouter.getTotalQueryObject();
                totalSuccess += hostRouter.getSuccesfullyRetrived();
            }
            AvailibilityRouter router = hostMapped.entrySet().iterator().next().getValue();
            int bufferSize = router.getBufferSize();
            int noOfReplicas = router.getNoOfReplicas();
            write(hostMapped.size() + " \t " + totalsum + " \t " + totalSuccess + " \t " + (totalLatency / hostMapped.size()) + " \t " + SimClock.getTime() + " \t " + bufferSize + " \t " + noOfReplicas);
            super.done(); //To change body of generated methods, choose Tools | Templates.
        } else {
            Map<DTNHost, DirectDeliveryRouter> tempMap = new HashMap<>();
            tempMap.putAll(hostMapped2);
            ValueComparator1 vc = new ValueComparator1(tempMap);
            TreeMap<DTNHost, DirectDeliveryRouter> treeMap = new TreeMap<>(vc);
            treeMap.putAll(tempMap);
            Set<Map.Entry<DTNHost, DirectDeliveryRouter>> entrySet = treeMap.entrySet();

            //Set<Map.Entry<DTNHost, DirectDeliveryRouter>> entrySet = hostMapped2.entrySet();
            Iterator<Map.Entry<DTNHost, DirectDeliveryRouter>> iterator = entrySet.iterator();
            int totalsum = 0;
            int totalSuccess = 0;
            double totalLatency = 0;

            while (iterator.hasNext()) {
                Map.Entry<DTNHost, DirectDeliveryRouter> next = iterator.next();
                DTNHost host = next.getKey();
                DirectDeliveryRouter hostRouter = next.getValue();
                double averageLatencyOfQueryReceived = averageLatencyOfQueryReceived(hostRouter);
                write(host + " \t " + hostRouter.getTotalQueryObject()
                        + " \t " + hostRouter.getSuccesfullyRetrived()
                        + " \t " + averageLatencyOfQueryReceived);
                totalLatency += averageLatencyOfQueryReceived;
                totalsum += hostRouter.getTotalQueryObject();
                totalSuccess += hostRouter.getSuccesfullyRetrived();

            }
            DirectDeliveryRouter router = hostMapped2.entrySet().iterator().next().getValue();
            int bufferSize = router.getBufferSize();
            int noOfReplicas = router.getNoOfReplicas();
            write(hostMapped2.size() + " \t " +
                    totalsum + " \t " + totalSuccess 
                    + " \t " + (totalLatency / hostMapped2.size()) 
                    + " \t " + SimClock.getTime() + " \t " + bufferSize + " \t " 
                    + noOfReplicas);
            super.done();
        }
    }

    public double averageLatencyOfQueryReceived(AvailibilityRouter hostRouter) {
        double result = 0.0;

        Set<String> keySet = hostRouter.retrivalTimeOfQuery.keySet();
        int size = keySet.size();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String s = iterator.next();
            result += hostRouter.retrivalTimeOfQuery.get(s);
        }
        
        if(size !=0)
            result /= size;
        else
            result /=1.0;

        return result;
    }

    public double averageLatencyOfQueryReceived(DirectDeliveryRouter hostRouter) {
        double result = 0.0;

        Set<String> keySet = hostRouter.retrivalTimeOfQuery.keySet();
        int size = keySet.size();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String s = iterator.next();
            result += hostRouter.retrivalTimeOfQuery.get(s);
        }

        if(size!=0)
            result /= size;
        else
            result /=1;

        return result;
    }

}

class ValueComparator implements Comparator<DTNHost> {

    Map<DTNHost, AvailibilityRouter> base;

    public ValueComparator(Map<DTNHost, AvailibilityRouter> base) {
        this.base = base;
    }

    public int compare(DTNHost a, DTNHost b) {
        if (a.getAddress() <= b.getAddress()) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }

}

class ValueComparator1 implements Comparator<DTNHost> {

    Map<DTNHost, DirectDeliveryRouter> base;

    public ValueComparator1(Map<DTNHost, DirectDeliveryRouter> base) {
        this.base = base;
    }

    public int compare(DTNHost a, DTNHost b) {
        if (a.getAddress() <= b.getAddress()) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }

}
