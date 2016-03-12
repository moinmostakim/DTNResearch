/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.*;
import java.util.*;
import routing.*;

/**
 *
 * @author asus
 */
public class AvavibilityRouter extends ActiveRouter {

    /**
     * Prophet router's setting namespace ({@value})
     */
    public static final String AVAVIBILITY_NS = "AvavibilityRouter";
    public static final String REPLICAS = "replicaNo";
    public static final String maxFreeSize = "maxFreeSize";

    private int noOfReplicas;

    Map<DTNHost, Double> lastSeen = new HashMap<>();
    Map<DTNHost, Double> connectedTimesPerOccurance = new HashMap<>();
    Map<DTNHost, Integer> seenofNode = new HashMap<>();
    Double lastDown = 0.0;
    Map<DTNHost, Double> disconnectedTime = new HashMap<>();
    Map<String, Double> stateProbability = new HashMap<>();
    Map<String, ArrayList<DTNHost>> contentAvailable = new HashMap<>();
    Map<String, Message> mappedMessages = new HashMap<>();
    public Map<String, Double> retrivalTimeOfQuery = new HashMap<>();

    private int succesfullyRetrived = 0;
    private int unsuccessful = 0;
    private int totalQueryObject = 0;
    int freeSize = 0;

    ArrayList<String> removeQueires = new ArrayList<>();

    private Map<String, ContentObject> listOfObjects = new HashMap<>();

    Map<DTNHost, Map<Integer, DTNHost>> mappedReplicas = new HashMap<>();

    private Map<String, String> queryObject = new HashMap<>();

    Map<DTNHost, Integer> countMsgs = new HashMap<>();

    double t_obserb = 20000;

    int totalOcuurance = 0;

    public AvavibilityRouter(Settings s) {
        super(s);
        Settings prophetSettings = new Settings(AVAVIBILITY_NS);
        noOfReplicas = prophetSettings.getInt(REPLICAS);
        freeSize = prophetSettings.getInt(maxFreeSize);
    }

    protected AvavibilityRouter(AvavibilityRouter r) {
        super(r);
        this.noOfReplicas = r.noOfReplicas;
        this.freeSize = r.freeSize;

    }

    @Override
    public AvavibilityRouter replicate() {
        return new AvavibilityRouter(this);
    }

    @Override
    public void changedConnection(Connection con) {

        super.changedConnection(con);
        DTNHost host = this.getHost();
        DTNHost connectedNode = con.getOtherNode(host);

        if (con.isUp()) {
            connectionRemained(connectedNode);
            if (SimClock.getTime() > t_obserb) {
                checkRequestResponse(connectedNode, getQueryObject());

            }
        } else if (!con.isUp()) {
            connectionLost(connectedNode);
        }

    }

    @Override
    protected void transferDone(Connection con) {
        super.transferDone(con);
        Message message = con.getMessage();
    }

    public boolean canTakeObject(ContentObject co) {
        int totalObjectSizeInListOfObjects = totalObjectSizeInListOfObjects();
        if (co.size + totalObjectSizeInListOfObjects < freeSize) {
            return true;
        } else {
            return false;
        }
    }

    public int totalObjectSizeInListOfObjects() {
        int size = 0;
        Iterator<Map.Entry<String, ContentObject>> iteratorOfList = listOfObjects.entrySet().iterator();
        while (iteratorOfList.hasNext()) {
            Map.Entry<String, ContentObject> iteratorObject = iteratorOfList.next();
            ContentObject iteratedContentObject = iteratorObject.getValue();
            size += iteratedContentObject.size;
        }
        return size;
    }

    @Override
    public int receiveMessage(Message m, DTNHost from) {
        int ret_val = super.receiveMessage(m, from); //To change body of generated methods, choose Tools | Templates.

        if (ret_val == MessageRouter.RCV_OK && m.getTo().getAddress() == getHost().getAddress()) {
            String type = (String) m.getProperty("msgType");
            if (type != null && !type.isEmpty()) {
                if (type.equalsIgnoreCase("replica")) {
                    String objectId = (String) m.getProperty("Object");
                    ContentObject contentObject
                            = new ContentObject(objectId, m.getSize(), m.getFrom(), m.getCreationTime(), SimClock.getTime());

                    //System.out.println(SimClock.getTime() + " " + getHost() + " received replica for " + objectId + " from " + m.getFrom());
                    if (canTakeObject(contentObject)) {
                        listOfObjects.put(objectId, contentObject);
                    }
                    
                }
                if (type.equalsIgnoreCase("reply")) {
                    double queryTime = m.getCreationTime();
                    String objectId = (String) m.getProperty("objectId");
                    if (queryObject.containsKey(m.getId())) {
                        succesfullyRetrived++;
                        double receivetime = SimClock.getTime();
                        String creationTime = (String) m.getProperty("objectqueryTime");
                        double creationTimeDouble = Double.parseDouble(creationTime);
                        //System.out.println(receivetime + " Object retrieved for " + m.getId() + " object " + objectId);
                        double difference = receivetime - creationTimeDouble;
                        retrivalTimeOfQuery.put(m.getId(), difference);
                        queryObject.remove(m.getId());
                    } else {

                    }
                }
            }
        }

        return ret_val;
    }

    public void checkRequestResponse(DTNHost connectedNode, Map<String, String> queryObject) {

        if (!queryObject.isEmpty()) {
            Set<Map.Entry<String, String>> entrySet = queryObject.entrySet();
            Iterator<Map.Entry<String, String>> iterator = entrySet.iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, String> next = iterator.next();
                String query = next.getKey();
                String objectmaterialwithtime = next.getValue();
                String[] split = objectmaterialwithtime.split("time:");
                String objectmaterial = split[0];
                String[] split1 = split[1].split("ttl:");
                String creationTime = split1[0];
                String[] split2 = split1[1].split("size:");
                String ttlOfMessage = split2[0];
                int msgSize = Integer.parseInt(split2[1]);
                AvavibilityRouter otherRouter = (AvavibilityRouter) connectedNode.getRouter();
                boolean containsObject = otherRouter.getListOfObjects().containsKey(objectmaterial);
                if (containsObject) {
                    otherRouter.initiateObjectTransfer(query, objectmaterial, getHost(), creationTime);
                }
            }

        }

    }

    protected boolean initiateObjectTransfer(String queryId, String objectId, DTNHost to, String queryCreationTime) {

        if (!listOfObjects.containsKey(objectId)) {
            return false;
        }
        //System.out.println(SimClock.getTime() + " " + getHost() + " responding query " + queryId + " for " + objectId);
        ContentObject contentObject = listOfObjects.get(objectId);
        if (contentObject != null) {
            Message m = new Message(getHost(), to, queryId, contentObject.size);
            m.addProperty("msgType", "reply");
            m.addProperty("objectId", contentObject.objectId);
            m.addProperty("objectqueryTime", queryCreationTime);
            super.createNewMessage(m);
            return true;
        }
        return false;

    }

    @Override
    public boolean createNewMessage(Message m) {

        if (m.getCreationTime() > t_obserb) {
            computePJ();
            Map<Integer, DTNHost> mappedReplicas1 = mappedReplicas();
            if (m.getId().contains("O")) {
                //System.out.println(SimClock.getTime() + " object " + m.getId() + " created");
                spreadMessageToAvailableNodesBasedOnSort(mappedReplicas1, m);
            } else {
                String query = m.getId();
                String[] split = query.split("-");
                String queryN0 = split[0];
                String objectID = split[1];
                String object = "O" + objectID + "time:" + m.getCreationTime() + "ttl:" + 4320.0 + "size:" + m.getSize();
                getQueryObject().put(query, object);
                setTotalQueryObject(getTotalQueryObject() + 1);
                //System.out.println(SimClock.getTime() + " query " + query + " created object " + "O" + objectID);
            }
            return true;
        } else {
            return false;
        }
    }

    public void spreadMessageToAvailableNodesBasedOnSort(Map<Integer, DTNHost> mappedReplicas1, Message objectToSend) {

        ArrayList<DTNHost> availableNodes = new ArrayList<>();
        for (int i = 0; i < mappedReplicas1.size(); i++) {
            if (mappedReplicas1.get(i) == null) {
                break;
            } else if (this.getHost().getAddress() == mappedReplicas1.get(i).getAddress()) {
                continue;
            } else {
                DTNHost peer = mappedReplicas1.get(i);
                Message m = new Message(this.getHost(), peer, "M_" + i + objectToSend.getId(), objectToSend.getSize());
                m.addProperty("msgType", "replica");
                m.addProperty("Object", objectToSend.getId());
                m.addProperty("Replica", m);
                super.createNewMessage(m);
                mappedMessages.put(m.getId(), m);
                availableNodes.add(peer);
                if (countMsgs.get(this.getHost()) != null) {
                    countMsgs.put(this.getHost(), countMsgs.get(this.getHost()) + 1);
                } else {
                    countMsgs.put(this.getHost(), 1);
                }
            }
        }
        contentAvailable.put(objectToSend.getId(), availableNodes);
        ////System.out.println(objectToSend.getId() + " " + contentAvailable.get(objectToSend.getId()));
    }

    public void printContentAvailable() {
        Iterator<Map.Entry<String, ArrayList<DTNHost>>> iterator = contentAvailable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ArrayList<DTNHost>> next = iterator.next();
            //System.out.println("obj " + next.getKey() + " " + next.getValue());
        }
    }

    @Override
    public void update() {

        super.update(); //To change body of generated methods, choose Tools | Templates.

        if (isTransferring() || !canStartTransfer()) {
            return; // can't start a new transfer
        }
        // Try only the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer
        }

    }

    public Double connectedTime(DTNHost connectedNode) {
        Double get = connectedTimesPerOccurance.get(connectedNode);
        ////System.out.println("Connection with " + connectedNode + " and connected time" + get);
        return get;
    }

    public Double disConnectedTime(DTNHost connectedNode) {
        Double get = disconnectedTime.get(connectedNode);
        ////System.out.println("Connection with " + connectedNode + " and disconnected time" + get);
        return get;
    }

    public void printPJ() {
        Set<String> keySet = stateProbability.keySet();
        double sum = 0.0;
        for (String h : keySet) {
            //System.out.println("state " + h + " value " + stateProbability.get(h));
            sum += stateProbability.get(h);
        }
        //System.out.println("sum: " + sum);
    }

    public void connectionRemained(DTNHost connectedNode) {
        if (seenofNode.get(connectedNode) == null) {
            seenofNode.put(connectedNode, 1);
        } else {
            seenofNode.put(connectedNode, seenofNode.get(connectedNode) + 1);
        }
        ////System.out.println("INSIDE IF");
        Double nowTime = SimClock.getTime();
        ////System.out.println(connectedNode + " nowTime in connection up " + nowTime);
        lastSeen.put(connectedNode, nowTime);
        Double disConnectedTime = nowTime - lastDown;
        if (disconnectedTime.get(connectedNode) != null) {
            disconnectedTime.put(connectedNode, disconnectedTime.get(connectedNode)
                    + disConnectedTime);
        } else {
            disconnectedTime.put(connectedNode, disConnectedTime);
        }

    }

    public void connectionLost(DTNHost connectedNode) {
        Double nowTime = SimClock.getTime();
        ////System.out.println("INSIDE ELSE IF");
        lastDown = nowTime;
        // //System.out.println(connectedNode + " Else connection down " + lastDown);
        Double connectedTime = 0.0;
        if (lastSeen.get(connectedNode) != null) {
            connectedTime = nowTime - lastSeen.get(connectedNode);
            if (connectedTimesPerOccurance.get(connectedNode) != null) {
                connectedTimesPerOccurance.put(connectedNode, connectedTimesPerOccurance.get(connectedNode)
                        + connectedTime);
            } else {
                connectedTimesPerOccurance.put(connectedNode, connectedTime);
            }
        }
    }

    public void printSeenOccurance() {
        Iterator<Map.Entry<DTNHost, Integer>> iterator = seenofNode.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<DTNHost, Integer> next = iterator.next();
            //System.out.println("Host" + this.getHost() + " " + next.getKey() + " " + next.getValue());
        }
    }

    public Map<DTNHost, Double> lambdaDTNHostto0() {
        Map<DTNHost, Double> lamdaNodeto0State = new HashMap<>();
        Set<DTNHost> keySet = disconnectedTime.keySet();

        if (keySet.isEmpty()) {
            return null;
        } else {
            for (DTNHost connectedNode : keySet) {
                Double disconnectedTime = disConnectedTime(connectedNode);
                Integer get = seenofNode.get(connectedNode);
                double computedLambda = 1.0 * get / disconnectedTime;
                lamdaNodeto0State.put(connectedNode, computedLambda);
            }
        }
        return lamdaNodeto0State;

    }

    public Map<DTNHost, Double> lambdaDTNHost0toNodes() {
        Map<DTNHost, Double> lamda0StatetoNodes = new HashMap<>();
        Set<DTNHost> keySet = connectedTimesPerOccurance.keySet();

        if (keySet.isEmpty()) {
            return null;
        } else {
            for (DTNHost connectedNode : keySet) {
                Double connectedTime = connectedTime(connectedNode);
                Integer get = seenofNode.get(connectedNode);
                double computedLambda = 1.0 * get / connectedTime;
                lamda0StatetoNodes.put(connectedNode, computedLambda);
            }
        }
        return lamda0StatetoNodes;

    }

    public void computePJ() {

        Map<DTNHost, Double> lambdaDTNHostto0 = lambdaDTNHostto0();
        Map<DTNHost, Double> lambdaDTNHost0toNodes = lambdaDTNHost0toNodes();

        double sumOfRatio = 0.0;

        Set<DTNHost> keySet0toN;

        if (lambdaDTNHost0toNodes == null && lambdaDTNHost0toNodes == null) {
            return;
        } else if (lambdaDTNHost0toNodes == null) {
            keySet0toN = lambdaDTNHostto0.keySet();
        } else {
            keySet0toN = lambdaDTNHost0toNodes.keySet();
        }

        if (keySet0toN != null) {

            for (DTNHost h : keySet0toN) {
                sumOfRatio += (lambdaDTNHostto0.get(h) / lambdaDTNHost0toNodes.get(h));
            }
            ////System.out.println("sum of Ratio:  " + sumOfRatio);

            Double P0 = 1.0 / (1.0 + sumOfRatio);
            stateProbability.put("0", P0);

            for (DTNHost h : keySet0toN) {

                double P_h = (lambdaDTNHostto0.get(h) * P0) / lambdaDTNHost0toNodes.get(h);
                stateProbability.put(h.toString(), P_h);
            }

        }
    }

    public TreeMap<String, Double> sortProbability() {

        Map<String, Double> temp = new HashMap<>();
        temp.putAll(stateProbability);
        ValueComparator vc = new ValueComparator(temp);
        TreeMap<String, Double> sorted_probability = new TreeMap<String, Double>(vc);
        sorted_probability.putAll(temp);
        ////System.out.println("results " + sorted_probability);
        return sorted_probability;

    }

    public Map<Integer, DTNHost> mappedReplicas() {
        Map<Integer, DTNHost> mappedDTN = new HashMap<>();
        try {
            TreeMap<String, Double> sortProbability = sortProbability();
            Map<String, DTNHost> mappedDTNHosts = new HashMap<String, DTNHost>();

            Set<String> keySet = stateProbability.keySet();
            Set<DTNHost> keySet1 = lambdaDTNHost0toNodes().keySet();
            Iterator<DTNHost> iterator3 = keySet1.iterator();

            while (iterator3.hasNext()) {
                DTNHost nextDTN = iterator3.next();
                Iterator<String> iterator4 = keySet.iterator();
                while (iterator4.hasNext()) {
                    String nextString = iterator4.next();
                    if (nextDTN.toString().equalsIgnoreCase(nextString)) {
                        mappedDTNHosts.put(nextString, nextDTN);
                    }

                }
            }

            Set<String> keySet4 = sortProbability.keySet();
            Iterator<String> iterator = keySet4.iterator();

            int i = 0;
            ////System.out.println("checking " + this.getNoOfReplicas());
            while (iterator.hasNext() && i < this.getNoOfReplicas()) {
                String next = iterator.next();
                if (next.equalsIgnoreCase("0")) {
                    continue;
                } else {
                    mappedDTN.put(i, mappedDTNHosts.get(next));
                    i++;
                }
            }
        } catch (Exception e) {

        }

        ////System.out.println(" " + mappedDTN.entrySet());
        return mappedDTN;
    }

    public void bruteforceSelectionSet(int k) {
        k = 3;

        Set<String> keySet = stateProbability.keySet();
        Map<DTNHost, Double> lambdaDTNHost0toNodes = lambdaDTNHost0toNodes();
        Set<DTNHost> keySet1 = lambdaDTNHost0toNodes.keySet();
        Map<String, DTNHost> mappedDTN = new HashMap<String, DTNHost>();

        ArrayList<DTNHost> selected = new ArrayList<>();

        Iterator<String> iterator = keySet.iterator();

        Iterator<String> iterator1 = keySet.iterator();

        Iterator<String> iterator2 = keySet.iterator();

        Iterator<DTNHost> iterator3 = keySet1.iterator();

        while (iterator3.hasNext()) {
            DTNHost nextDTN = iterator3.next();
            Iterator<String> iterator4 = keySet.iterator();
            while (iterator4.hasNext()) {
                String nextString = iterator4.next();
                if (nextDTN.toString().equalsIgnoreCase(nextString)) {
                    mappedDTN.put(nextString, nextDTN);
                }

            }
        }

        //System.out.println("mapped DTN : " + mappedDTN.keySet());
        while (iterator.hasNext()) {
            String next = iterator.next();
            double sum = 0;
            double sum2 = 0;
            double min = Double.MAX_VALUE;
            double lastMin = 0.0;
            while (iterator1.hasNext()) {

                String next1 = iterator1.next();
                if (!next.equalsIgnoreCase(next1)) {
                    while (iterator2.hasNext()) {
                        String next2 = iterator2.next();

                        if (!next.equalsIgnoreCase(next2) && !next1.equalsIgnoreCase(next2)) {
                            sum = stateProbability.get(next)
                                    + stateProbability.get(next1);

                            //   //System.out.println("sum " + sum);
                            sum2 = lambdaDTNHost0toNodes.get(mappedDTN.get(next))
                                    + lambdaDTNHost0toNodes.get(mappedDTN.get(next1));

                            lastMin = (1 - sum) / sum2;

                            if (lastMin < min) {
                                selected.clear();
                                min = lastMin;
                                selected.add(mappedDTN.get(next));
                                selected.add(mappedDTN.get(next1));
                                //selected.add(mappedDTN.get(next2));
                            }

                        }
                    }

                }
            }

        }
        //System.out.println("selected list: " + selected);
    }

    /**
     * @return the succesfullyRetrived
     */
    public int getSuccesfullyRetrived() {
        return succesfullyRetrived;
    }

    /**
     * @param succesfullyRetrived the succesfullyRetrived to set
     */
    public void setSuccesfullyRetrived(int succesfullyRetrived) {
        this.succesfullyRetrived = succesfullyRetrived;
    }

    /**
     * @return the totalQueryObject
     */
    public int getTotalQueryObject() {
        return totalQueryObject;
    }

    /**
     * @param totalQueryObject the totalQueryObject to set
     */
    public void setTotalQueryObject(int totalQueryObject) {
        this.totalQueryObject = totalQueryObject;
    }

    /**
     * @return the listOfObjects
     */
    public Map<String, ContentObject> getListOfObjects() {
        return listOfObjects;
    }

    /**
     * @param listOfObjects the listOfObjects to set
     */
    public void setListOfObjects(Map<String, ContentObject> listOfObjects) {
        this.listOfObjects = listOfObjects;
    }

    /**
     * @return the queryObject
     */
    public Map<String, String> getQueryObject() {
        return queryObject;
    }

    /**
     * @param queryObject the queryObject to set
     */
    public void setQueryObject(Map<String, String> queryObject) {
        this.queryObject = queryObject;
    }

    /**
     * @return the noOfReplicas
     */
    public int getNoOfReplicas() {
        return noOfReplicas;
    }

    /**
     * @param noOfReplicas the noOfReplicas to set
     */
    public void setNoOfReplicas(int noOfReplicas) {
        this.noOfReplicas = noOfReplicas;
    }

    private class ContentObject {

        String objectId;
        int size;
        DTNHost from;
        double creationTime, replicationTime;

        public ContentObject(String objectId, int size, DTNHost from, double creationTime, double replicationTime) {
            this.objectId = objectId;
            this.size = size;
            this.from = from;
            this.creationTime = creationTime;
            this.replicationTime = replicationTime;
        }

    }
}

class ValueComparator implements Comparator<String> {

    Map<String, Double> base;

    public ValueComparator(Map<String, Double> base) {
        this.base = base;
    }

    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }

}
