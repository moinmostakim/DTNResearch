/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import core.*;
import java.util.*;
import routing.*;
/**
 * Router that will deliver messages only to the final recipient.
 */
public class DirectDeliveryRouter extends ActiveRouter {

    public DirectDeliveryRouter(Settings s) {
        super(s);
        Settings prophetSettings = new Settings(AVAVIBILITY_NS);
        noOfReplicas = prophetSettings.getInt(REPLICAS);

    }

    protected DirectDeliveryRouter(DirectDeliveryRouter r) {

        super(r);
        this.noOfReplicas = r.noOfReplicas;
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

    @Override
    public int receiveMessage(Message m, DTNHost from) {
        int ret_val = super.receiveMessage(m, from); //To change body of generated methods, choose Tools | Templates.

        if (ret_val == MessageRouter.RCV_OK) {
            String type = (String) m.getProperty("msgType");
            if (type != null && !type.isEmpty()) {
                if (type.equalsIgnoreCase("Request")) {
                    Message request = (Message) m.getProperty("innermessage");
                    if (request != null) {
                        if (request.getId().contains("R_O_")) {
                            super.createNewMessage(request);
                        }
                    }
                } else if (type.equalsIgnoreCase("Reply")) {
                    String object = (String) m.getProperty("Object");
                    String query = (String) m.getProperty("query");
                    String creationTime = (String) m.getProperty("creationTime");
                    boolean containsObj = this.getListOfObjects().containsKey(object);
                    if (containsObj) {
                        double receiveTimeofObject = SimClock.getTime();
                        double queryTime = Double.parseDouble(creationTime);
                        double difference = receiveTimeofObject - queryTime;
                        retrivalTimeOfQuery.put(query, difference);
                        this.succesfullyRetrived++;
                        System.out.println("Request received in "
                                + SimClock.getTime() + " of " + object
                                + " requested at " + creationTime + " to "
                                + m.getTo() + " from node "
                                + m.getFrom() + " "
                                + containsObj);

                        removeQueires.add(query);
                    }
                }
            }
        }

        return ret_val;
    }

    public void checkRequestResponse(DTNHost connectedNode, Map<String, String> queryObject) {

        if (!removeQueires.isEmpty()) {
            for (String query : removeQueires) {
                queryObject.remove(query);

                System.out.println("" + this.getHost() + " removed " + query);
            }
            removeQueires.clear();
        }
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
                DirectDeliveryRouter otherRouter = (DirectDeliveryRouter) connectedNode.getRouter();
                boolean containsObject = otherRouter.getListOfObjects().containsKey(objectmaterial);
                if (containsObject) {
                    double diffence = SimClock.getTime() - Double.parseDouble(creationTime);
                    if (diffence <= Double.parseDouble(ttlOfMessage)) {
                        //System.out.println("ttl: " + ttlOfMessage);
                        Message m = new Message(connectedNode, this.getHost(), "R_O_" + objectmaterial, msgSize);
                        m.addProperty("Object", objectmaterial);
                        m.addProperty("Object_Size", msgSize);
                        m.addProperty("query", query);
                        m.addProperty("creationTime", creationTime);
                        m.addProperty("msgType", "Reply");
                        Message requestMessage = new Message(this.getHost(), connectedNode, "Request_From_" + m.getId(), msgSize);
                        requestMessage.addProperty("innermessage", m);
                        requestMessage.addProperty("msgType", "Request");
                        super.createNewMessage(requestMessage);
                    } else {
                        System.out.println("Request not received in correct time. Time to receive at "
                                + SimClock.getTime() + " of " + objectmaterial
                                + " requested at " + creationTime + " to "
                                + this.getHost() + " from node "
                                + otherRouter.getHost() + " "
                                + containsObject);
                        removeQueires.add(query);

                    }

                }

                //System.out.println("Request "+objectmaterial +" to "+this.getHost() +" form node " + otherRouter.getHost() + " " + containsObject);
            }
            if (!removeQueires.isEmpty()) {
                for (String query : removeQueires) {
                    queryObject.remove(query);
                    System.out.println("" + this.getHost() + " removed " + query);
                }
                removeQueires.clear();
            }

        }

    }

    public void connectionRemained(DTNHost connectedNode) {
        if (seenofNode.get(connectedNode) == null) {
            seenofNode.put(connectedNode, 1);
        } else {
            seenofNode.put(connectedNode, seenofNode.get(connectedNode) + 1);
        }
        //System.out.println("INSIDE IF");
        Double nowTime = SimClock.getTime();
        //System.out.println(connectedNode + " nowTime in connection up " + nowTime);
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
        //System.out.println("INSIDE ELSE IF");
        lastDown = nowTime;
        // System.out.println(connectedNode + " Else connection down " + lastDown);
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

    @Override
    public boolean createNewMessage(Message m) {

        if (m.getCreationTime() > t_obserb) {
            // computePJ();
            Map<Integer, DTNHost> mappedReplicas1 = new HashMap<>();
            if (m.getId().contains("O")) {

                Set<DTNHost> selectedReplicas = new HashSet<>();
                Iterator<DTNHost> iterator = seenofNode.keySet().iterator();
                ArrayList<DTNHost> dtnArrayList = new ArrayList<>();
                while (iterator.hasNext()) {
                    DTNHost next = iterator.next();
                    dtnArrayList.add(next);
                }
                if (dtnArrayList.size() > noOfReplicas) {
                    Collections.shuffle(dtnArrayList);
                    for (int i = 0; i < noOfReplicas; i++) {
                        mappedReplicas1.put(i, dtnArrayList.get(i));
                    }
                } else {

                    for (int i = 0; i < dtnArrayList.size(); i++) {
                        mappedReplicas1.put(i, dtnArrayList.get(i));
                    }
                }
                spreadMessageToAvailableNodesBasedOnSort(mappedReplicas1, m);
            } else {

                String query = m.getId();
                String object = "O" + query.substring(1) + "time:" + m.getCreationTime() + "ttl:" + 300000.0 + "size:" + m.getSize();
                getQueryObject().put(query, object);
                setTotalQueryObject(getTotalQueryObject() + 1);
                //super.createNewMessage(m);

            }
            return true;
        } else {
            return false;
        }

        //To change body of generated methods, choose Tools | Templates.
    }

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
    public Map<String, DTNHost> getListOfObjects() {
        return listOfObjects;
    }

    /**
     * @param listOfObjects the listOfObjects to set
     */
    public void setListOfObjects(Map<String, DTNHost> listOfObjects) {
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

    public void spreadMessageToAvailableNodesBasedOnSort(Map<Integer, DTNHost> mappedReplicas1, Message nextEvent) {
        //this.getHost().getRouter().deleteMessage(nextEvent.getId(), true);
        ArrayList<DTNHost> availableNodes = new ArrayList<>();
        for (int i = 0; i < mappedReplicas1.size(); i++) {
            if (mappedReplicas1.get(i) == null) {
                break;
            } else if (this.getHost().getAddress() == mappedReplicas1.get(i).getAddress()) {
                continue;
            } else {
                DTNHost peer = mappedReplicas1.get(i);
                Message m = new Message(this.getHost(), peer, "M_" + i + nextEvent.getId(), nextEvent.getSize());
                //System.out.println("m: " + peer + " i" + i);
                m.addProperty("Object", nextEvent.getId());
                super.createNewMessage(m);
                mappedMessages.put(m.getId(), m);
                availableNodes.add(peer);
                if (countMsgs.get(this.getHost()) != null) {
                    countMsgs.put(this.getHost(), countMsgs.get(this.getHost()) + 1);
                } else {
                    countMsgs.put(this.getHost(), 1);
                }
                //this.sendMessage(m.getId(), mappedReplicas1.get(i));
                //peer.messageTransferred(m.getId(), getHost());

            }
        }
        contentAvailable.put(nextEvent.getId(), availableNodes);
        //System.out.println(nextEvent.getId() + " " + contentAvailable.get(nextEvent.getId()));
    }

    public void printContentAvailable() {
        Iterator<Map.Entry<String, ArrayList<DTNHost>>> iterator = contentAvailable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ArrayList<DTNHost>> next = iterator.next();
            System.out.println("obj " + next.getKey() + " " + next.getValue());
        }
    }

    @Override
    public void update() {
        super.update();

        double time = SimClock.getTime();
        if (time > t_obserb) {

            HashMap<String, Message> deliveredMessages = this.getHost().getRouter().getDeliveredMessages();
            if (!deliveredMessages.isEmpty()) {
                Set<Map.Entry<String, Message>> entrySet = deliveredMessages.entrySet();
                Iterator<Map.Entry<String, Message>> iterator = entrySet.iterator();
                while (iterator.hasNext()) {
                    Message get = iterator.next().getValue();
                    String property = (String) get.getProperty("Object");
                    getListOfObjects().put(property, getHost());
                    getHost().getRouter().addToMessages(get, false);

                }
                deliveredMessages.clear();

            }

        }

        if (isTransferring() || !canStartTransfer()) {
            return; // can't start a new transfer
        }

        // Try only the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer
        }
    }

    @Override
    public DirectDeliveryRouter replicate() {
        return new DirectDeliveryRouter(this);
    }

    public static final String AVAVIBILITY_NS = "AvavibilityRouter";
    public static final String REPLICAS = "replicaNo";

    private int noOfReplicas;

    Map<DTNHost, Double> lastSeen = new HashMap<>();
    Map<DTNHost, Double> connectedTimesPerOccurance = new HashMap<>();
    Map<DTNHost, Integer> seenofNode = new HashMap<>();
    Double lastDown = 0.0;
    Map<DTNHost, Double> disconnectedTime = new HashMap<>();
    Map<String, Double> stateProbability = new HashMap<>();
    Map<String, ArrayList<DTNHost>> contentAvailable = new HashMap<>();
    Map<String, Message> mappedMessages = new HashMap<>();

    private int succesfullyRetrived = 0;
    private int totalQueryObject = 0;

    ArrayList<String> removeQueires = new ArrayList<>();

    private Map<String, DTNHost> listOfObjects = new HashMap<>();

    Map<DTNHost, Map<Integer, DTNHost>> mappedReplicas = new HashMap<>();

    private Map<String, String> queryObject = new HashMap<>();

    Map<DTNHost, Integer> countMsgs = new HashMap<>();

    double t_obserb = 30000;

    int totalOcuurance = 0;
    public Map<String, Double> retrivalTimeOfQuery = new HashMap<>();

}
