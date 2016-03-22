/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import core.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import routing.*;

/**
 * Router that will deliver messages only to the final recipient.
 */
public class DirectDeliveryRouter extends ActiveRouter {

    public ArrayList<ContentObject> createByOwn = new ArrayList<>();
    public ArrayList<ContentObject> replicationFromOthers = new ArrayList<>();
    //Logger logger = Logger.getLogger("AvailablityLog");
    FileHandler fh;
    String LogFileName;
    int freeSize = 0;
    Logger logger = new Logger();
    public static final String maxFreeSize = "maxFreeSize";

    public DirectDeliveryRouter(Settings s) {
        super(s);
        Settings prophetSettings = new Settings(AVAVIBILITY_NS);
        noOfReplicas = prophetSettings.getInt(REPLICAS);
        freeSize = prophetSettings.getInt(maxFreeSize);
        LogFileName = prophetSettings.getSetting("LogFile");

        try {
                   File g = new File("/home/moin/MyLogs/DirectDeliveryRouter/" + LogFileName);
            g.mkdirs();
            File f = new File(g.getAbsolutePath() + "/" + LogFileName + "replica" + noOfReplicas + ".log");
            FileOutputStream fos = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fos);
            System.setOut(ps);
         } catch (IOException ex) {
         }
//        logger.addHandler(fh);
//        SimpleFormatter formatter = new SimpleFormatter();
//        fh.setFormatter(formatter);
//        logger.setUseParentHandlers(false);
//        

    }

    protected DirectDeliveryRouter(DirectDeliveryRouter r) {

        super(r);
        this.noOfReplicas = r.noOfReplicas;
        this.freeSize = r.freeSize;
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
        int ret_val = super.receiveMessage(m, from);
        if (ret_val == MessageRouter.RCV_OK && m.getTo().getAddress() == getHost().getAddress()) {
            String type = (String) m.getProperty("msgType");
            if (type != null && !type.isEmpty()) {
                if (type.equalsIgnoreCase("replica")) {
                    String objectId = (String) m.getProperty("Object");
                    ContentObject contentObject
                            = new ContentObject(objectId, m.getSize(), m.getFrom(), m.getCreationTime(),
                                    SimClock.getTime());

                    if (canTakeObject(contentObject)) {
                        logger.info(SimClock.getTime() + " "
                                + getHost() + " received replica for "
                                + objectId + " from "
                                + m.getFrom());
                        
                        listOfObjects.put(objectId, contentObject);
                        replicationFromOthers.add(contentObject);
                    }

                }
                if (type.equalsIgnoreCase("reply")) {
                    double queryTime = m.getCreationTime();
                    String objectId = (String) m.getProperty("objectId");
                    if (queryObject.containsKey(m.getId())) {
                        succesfullyRetrived++;
                        double receivetime = SimClock.getTime();
                        double creationTimeDouble = (double) m.getProperty("objectqueryTime");
                        //= Double.parseDouble(creationTime);
                        double difference = receivetime - creationTimeDouble;
                        logger.info(SimClock.getTime() + " "
                                + getHost() + " retrived object "
                                + objectId
                                + " for query "
                                + m.getId() + "from " + m.getFrom() + " querying time "
                                + creationTimeDouble + " retrieved time "
                                + receivetime + " difference " + difference);
                        
                        retrivalTimeOfQuery.put(m.getId(), difference);
                        queryObject.remove(m.getId());
                    } else {

                    }
                }
            }
        }

        return ret_val;

    }

    public void checkRequestResponse(DTNHost connectedNode, Map<String, QueryContent> queryObject) {
        
        if (!queryObject.isEmpty()) {
            Set<Map.Entry<String, QueryContent>> entrySet = queryObject.entrySet();
            Iterator<Map.Entry<String, QueryContent>> iterator = entrySet.iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, QueryContent> next = iterator.next();
                String query = next.getKey();
                QueryContent queryObjectMaterial = next.getValue();
                String objectmaterial = queryObjectMaterial.objectToQuery;//split[0];
                double creationTime = queryObjectMaterial.queryCreationTime; //split1[0];
                int msgSize = queryObjectMaterial.objectSize;
                DirectDeliveryRouter otherRouter = (DirectDeliveryRouter) connectedNode.getRouter();
                boolean containsObject = otherRouter.getListOfObjects().containsKey(objectmaterial);
                if (containsObject) {
                    otherRouter.initiateObjectTransfer(query, objectmaterial, getHost(), creationTime);
                }
            }

        }

    }
    
    protected boolean initiateObjectTransfer(String queryId, String objectId, DTNHost to, double queryCreationTime) {

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
            return super.createNewMessage(m);
            //return true;
        }
        return false;

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
                
                ContentObject contentObject = new ContentObject(m.getId(), m.getSize(), getHost(), m.getCreationTime(), 0);
                createByOwn.add(contentObject);
                logger.info(SimClock.getTime() +" "+getHost() + " No of Replicas " + this.getNoOfReplicas()
                        + " replicacandidate " + mappedReplicas1.values());
                spreadMessageToAvailableNodesBasedOnShuffle(mappedReplicas1, m);
            } else {

                QueryContent queryObject = new QueryContent(m);                
                getQueryObject().put(queryObject.queryID, queryObject);
                setTotalQueryObject(getTotalQueryObject() + 1);


            }
            return true;
        } else {
            return false;
        }

        //To change body of generated methods, choose Tools | Templates.
    }

    public class ContentObject {

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

        public String toString() {
            return this.objectId + " rep " + this.replicationTime + " cre " + this.creationTime + " from " + this.from.toString();
        }

    }

    public class QueryContent {

        String queryID;
        String objectToQuery;
        int objectSize;
        DTNHost queryFrom;
        double queryCreationTime;

        public QueryContent(String queryID, String objectToQuery, DTNHost queryFrom, double queryCreationTime, int objectSize) {
            this.queryID = queryID;
            this.objectToQuery = objectToQuery;
            this.queryFrom = queryFrom;
            this.queryCreationTime = queryCreationTime;
            this.objectSize = objectSize;
        }

        public QueryContent(Message m) {
            String query = m.getId();
            String[] split = query.split("-");
            String queryN0 = split[0];
            String objectID = split[1];
//                String object = "O" + objectID + "time:" + m.getCreationTime() + "ttl:" + 4320.0 + "size:" + m.getSize();

            this.queryID = m.getId();
            this.objectToQuery = "O" + objectID;
            queryCreationTime = m.getCreationTime();
            queryFrom = m.getFrom();
            this.objectSize = m.getSize();

        }

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
    public Map<String, QueryContent> getQueryObject() {
        return queryObject;
    }

    /**
     * @param queryObject the queryObject to set
     */
    public void setQueryObject(Map<String, QueryContent> queryObject) {
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

    public void spreadMessageToAvailableNodesBasedOnShuffle(Map<Integer, DTNHost> mappedReplicas1, Message objectToSend) {
        //this.getHost().getRouter().deleteMessage(nextEvent.getId(), true);
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
                //this.sendMessage(m.getId(), mappedReplicas1.get(i));
                //peer.messageTransferred(m.getId(), getHost());

            }
        }
        contentAvailable.put(objectToSend.getId(), availableNodes);
       logger.info(SimClock.getTime()+" "+getHost() + " ObjectToSend " + objectToSend.getId()
                + " " + contentAvailable.get(objectToSend.getId()));
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
    
    class Logger
    {
        public void info(String x )
        {
            System.out.println(x);
        }
    }

    public static final String AVAVIBILITY_NS = "DirectDeliveryRouter";
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

    private Map<String, ContentObject> listOfObjects = new HashMap<>();

    Map<DTNHost, Map<Integer, DTNHost>> mappedReplicas = new HashMap<>();

    private Map<String, QueryContent> queryObject = new HashMap<>();

    Map<DTNHost, Integer> countMsgs = new HashMap<>();

    double t_obserb = 20000;

    int totalOcuurance = 0;
    public Map<String, Double> retrivalTimeOfQuery = new HashMap<>();

}
