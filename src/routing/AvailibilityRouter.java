/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author asus
 */
public class AvailibilityRouter extends ActiveRouter {

    /**
     * Prophet router's setting namespace ({@value})
     */
    //Code need to be revisited.
    public static final String AVAVIBILITY_NS = "AvailibilityRouter";
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
    public ArrayList<ContentObject> createByOwn = new ArrayList<>();
    public ArrayList<ContentObject> replicationFromOthers = new ArrayList<>();
    public ArrayList<StateTableRow> stateTableOfObjects = new ArrayList<>();

    public ArrayList<NeighbourDetails> neighbourDetailList = new ArrayList<>();

    public Map<String, Double> retrivalTimeOfQuery = new HashMap<>();

    //public Logger logger = Logger.getLogger("AvailablityLog");
    //public FileHandler fh;
    Logger logger = new Logger();

    private int succesfullyRetrived = 0;
    private int unsuccessful = 0;
    private int totalQueryObject = 0;
    int freeSize = 0;

    ArrayList<String> removeQueires = new ArrayList<>();

    private Map<String, ContentObject> listOfObjects = new HashMap<>();

    Map<DTNHost, Map<Integer, DTNHost>> mappedReplicas = new HashMap<>();

    private Map<String, QueryContent> queryObject = new HashMap<>();

    Map<DTNHost, Integer> countMsgs = new HashMap<>();

    Map<String, DTNHost> listofDTNHostForLambdaSort = new HashMap<>();

    double t_obserb = 20000;

    int totalOcuurance = 0;

    String LogFileName;
    
    static
    {
        DTNSim.registerForReset(AvailibilityRouter.class.getCanonicalName());
        reset();
    }

    public AvailibilityRouter(Settings s) {
        super(s);
        Settings prophetSettings = new Settings(AVAVIBILITY_NS);
        noOfReplicas = prophetSettings.getInt(REPLICAS);
        freeSize = prophetSettings.getInt(maxFreeSize);
        LogFileName = prophetSettings.getSetting("LogFile");
        init();

        try {
            File g = new File("/home/moin/MyLogs/AvailibilityRouter/" + LogFileName);
            g.mkdirs();
            File f = new File(g.getAbsolutePath() + "/" + LogFileName + "replica" + noOfReplicas + ".log");
            FileOutputStream fos = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fos);
            System.setOut(ps);
        } catch (IOException ex) {
        }
//        logger.addHandler(fh);
//        SimpleFormatter formatter = new SimpleFor;matter();
//        fh.setFormatter(formatter);        
//        logger.setUseParentHandlers(false);

    }

    protected AvailibilityRouter(AvailibilityRouter r) {
        super(r);
        this.noOfReplicas = r.noOfReplicas;
        this.freeSize = r.freeSize;
        init();
    }
    
    public void init()
    {
    lastSeen = new HashMap<>();
    connectedTimesPerOccurance = new HashMap<>();
     seenofNode = new HashMap<>();
    lastDown = 0.0;
    disconnectedTime = new HashMap<>();
    stateProbability = new HashMap<>();
    contentAvailable = new HashMap<>();
    mappedMessages = new HashMap<>();
    createByOwn = new ArrayList<>();
    replicationFromOthers = new ArrayList<>();
    stateTableOfObjects = new ArrayList<>();
    neighbourDetailList = new ArrayList<>();
    retrivalTimeOfQuery = new HashMap<>();
    logger = new Logger();
    succesfullyRetrived = 0;
    unsuccessful = 0;
    totalQueryObject = 0;
    freeSize = 0;
    removeQueires = new ArrayList<>();
    listOfObjects = new HashMap<>();
    mappedReplicas = new HashMap<>();
    queryObject = new HashMap<>();
    countMsgs = new HashMap<>();
    listofDTNHostForLambdaSort = new HashMap<>();
    t_obserb = 20000;
    totalOcuurance = 0;
    //LogFileName="";
    neighbourDetailList= new ArrayList<>();
    }
    
    public static void reset()
    {
    
    //public Logger logger = Logger.getLogger("AvailablityLog");
    //public FileHandler fh;
       
    }

    @Override
    public AvailibilityRouter replicate() {
        return new AvailibilityRouter(this);
    }

    @Override
    public void changedConnection(Connection con) {

        super.changedConnection(con);
        DTNHost host = this.getHost();
        DTNHost connectedNode = con.getOtherNode(host);

        if (con.isUp()) {
             int neighbourIndex = findNeighbourFromList(connectedNode);
             if (neighbourIndex == -1) {
                NeighbourDetails neighbourDetails = new NeighbourDetails();
                neighbourDetails.neighbour = connectedNode;
                neighbourDetails.meetingTime.add(SimClock.getTime());
                neighbourDetails.meetOccurance = 1;
                neighbourDetailList.add(neighbourDetails);
            } else {              
                
                    NeighbourDetails details = neighbourDetailList.get(neighbourIndex);
                    details.meetingTime.add(SimClock.getTime());
                    details.meetOccurance = details.meetOccurance + 1;            
            }
            connectionRemained(connectedNode);
            if (SimClock.getTime() > t_obserb) {
                checkRequestResponse(connectedNode, getQueryObject());

            }
        } else if (!con.isUp()) {
            double disconnectedtime = SimClock.getTime();
            if (!neighbourDetailList.isEmpty()) {
                int neighbourIndex = findNeighbourFromList(connectedNode);
                if (neighbourIndex != -1) {
                    NeighbourDetails details = neighbourDetailList.get(neighbourIndex);
                    details.perMeetingDuration.put(details.meetOccurance, disconnectedtime - details.meetingTime.get(details.meetOccurance - 1));
                    
                }
            }
            connectionLost(connectedNode);
        }
        Collections.sort(neighbourDetailList,new Comparator<NeighbourDetails>() {

            @Override
            public int compare(NeighbourDetails o1, NeighbourDetails o2) {
           
                return o2.meetOccurance - o1.meetOccurance; 
            }

          
        });

        //System.out.println("time: " + SimClock.getTime() + " : " + getHost() + "-> NeighbourInfoList " + neighbourDetailList);

    }

    public int findNeighbourFromList(DTNHost neighbourToSearch) {
        int i = -1;
        for (i = 0; i < neighbourDetailList.size(); i++) {
            if (neighbourDetailList.get(i).neighbour.equals(neighbourToSearch)) {
                return i;
            }
        }
        return -1;
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

    public double computeOfExpectedRetriveTime(Map<Integer, DTNHost> mappedReplicas1) {
        System.out.print("ListOfReplicas " + mappedReplicas1.values() + " ");
        Map<DTNHost, Double> lambdaDTNHost0toNodes = lambdaDTNHost0toNodes();
        System.out.print("ListOfLambdas " + lambdaDTNHost0toNodes.keySet());
        double numerator = 0.0;
        double denominator = 0.0;
        String pJsValue = "";
        String lambdaValue = "";
        for (DTNHost replicas : mappedReplicas1.values()) {
            Double P_J = stateProbability.get(replicas.toString());
            pJsValue += P_J + ",";
            numerator += P_J.doubleValue();
            Double lambdaJ_0 = lambdaDTNHost0toNodes.get(replicas);
            lambdaValue += lambdaJ_0;
            denominator += lambdaJ_0;
        }
        System.out.print(" pj[" + pJsValue + "] ");
        System.out.print(" lambda [" + lambdaValue + "] ");

        if (denominator == 0.0) {
            System.out.print("Denominator is not comuted");
            return 0.0;
        }
        System.out.print("ComputedExpectedTime " + (numerator / denominator));
        System.out.println("");
        return numerator / denominator;

    }

    @Override
    public int receiveMessage(Message m, DTNHost from) {
        int ret_val = super.receiveMessage(m, from);
        if (ret_val == MessageRouter.RCV_OK && m.getTo().getAddress() == getHost().getAddress()) {
            String type = (String) m.getProperty("msgType");
            if (type != null && !type.isEmpty()) {
                if (type.equalsIgnoreCase("replica")) {
                    String objectId = (String) m.getProperty("Object");
                    double retriveTime = (double) m.getProperty("expectedRetrivedTime");
                    ContentObject contentObject
                            = new ContentObject(objectId, m.getSize(), m.getFrom(), m.getCreationTime(),
                                    SimClock.getTime(), retriveTime);

                    if (canTakeObject(contentObject)) {
                        logger.info(SimClock.getTime() + " "
                                + getHost() + " received replica for "
                                + objectId + " from "
                                + m.getFrom());

                        AvailibilityRouter fromRouter = (AvailibilityRouter) from.getRouter();

                        boolean updateStateTableRow = fromRouter.updateStateTableRow(contentObject.objectId, m.getTo(), contentObject.replicationTime);

                        listOfObjects.put(objectId, contentObject);

                        replicationFromOthers.add(contentObject);
                    }

                }
                if (type.equalsIgnoreCase("reply")) {
                    double queryTime = m.getCreationTime();
                    String objectId = (String) m.getProperty("objectId");
                    if (queryObject.containsKey(m.getId())) {
                        double receivetime = SimClock.getTime();
                        double creationTimeDouble = (double) m.getProperty("objectqueryTime");
                        double retriveTime = (double) m.getProperty("expectedRetrivedTime");

//= Double.parseDouble(creationTime);
                        double difference = receivetime - creationTimeDouble;
                        if (difference < 2 * retriveTime) {
                            succesfullyRetrived++;

                            logger.info(SimClock.getTime() + " "
                                    + getHost() + " retrived object "
                                    + objectId
                                    + " for query "
                                    + m.getId() + "from " + m.getFrom() + " querying time "
                                    + creationTimeDouble + " retrieved time "
                                    + receivetime + " difference " + difference);
                            retrivalTimeOfQuery.put(m.getId(), difference);
                        }
                        queryObject.remove(m.getId());
                    } else {

                    }
                }
            }
        }

        return ret_val;
    }

    public boolean updateStateTableRow(String objectId, DTNHost replicaNode, double replicationTime) {
        int index = getStateTableFromRouter(objectId);
        if (index != -1) {
            StateTableRow stateTableFromRouter = stateTableOfObjects.get(index);
            ReplicaState replicaState = new ReplicaState(replicaNode, replicationTime);
            stateTableFromRouter.getReplicaState().add(replicaState);
            if (stateTableFromRouter.getReplicaState().size() == this.noOfReplicas) {
                stateTableFromRouter.replicaDone = true;
            }
            //stateTableOfObjects.add(index, stateTableFromRouter);
            return true;
        }

        return false;
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
                //Integer.parseInt(split2[1]);
                //String[] split = queryObjectMaterial.split("time:");
                //String[] split1 = split[1].split("ttl:");
                //String[] split2 = split1[1].split("size:");
                //String ttlOfMessage = split2[0];
                AvailibilityRouter otherRouter = (AvailibilityRouter) connectedNode.getRouter();
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
            m.addProperty("expectedRetrivedTime", contentObject.expectedRetriveTime);
            return super.createNewMessage(m);
            //return true;
        }
        return false;

    }

    @Override
    public boolean createNewMessage(Message m) {

        if (m.getCreationTime() > t_obserb) {
            computePJ();
            Map<Integer, DTNHost> mappedReplicas1 = mappedReplicas("Probability");
            double computeOfExpectedRetriveTime = computeOfExpectedRetriveTime(mappedReplicas1);
            if (m.getId().contains("O")) {
                //Need to make it a call back function with timestamp
                ContentObject contentObject = new ContentObject(m.getId(), m.getSize(), getHost(), m.getCreationTime(), 0,
                        computeOfExpectedRetriveTime);
                createByOwn.add(contentObject);
                spreadMessageToAvailableNodesBasedOnSort(mappedReplicas1, m);
            } else {
                QueryContent queryObject = new QueryContent(m);

                getQueryObject().put(queryObject.queryID, queryObject);
                setTotalQueryObject(getTotalQueryObject() + 1);
            }
            return true;
        } else {
            return false;
        }
    }

    public ContentObject returnContentFromCreateOwn(String objectID) {
        int i = -1;
        for (i = 0; i < createByOwn.size(); i++) {
            if (createByOwn.get(i).objectId.equalsIgnoreCase(objectID)) {
                break;
            }
        }
        if (i == -1) {
            return null;
        }

        return createByOwn.get(i);
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
                ContentObject returnContentFromCreateOwn = returnContentFromCreateOwn(objectToSend.getId());
                m.addProperty("expectedRetrivedTime", returnContentFromCreateOwn.expectedRetriveTime);
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
        StateTableRow objectRow = new StateTableRow();
        objectRow.setObjectId(objectToSend.getId());
        objectRow.replicaCandidates = new ArrayList<>();
        objectRow.replicaCandidates.addAll(availableNodes);
        objectRow.replicaState = new ArrayList<>();
        stateTableOfObjects.add(objectRow);
        contentAvailable.put(objectToSend.getId(), availableNodes);
        logger.info(SimClock.getTime() + " " + getHost() + " ObjectToSend " + objectToSend.getId()
                + " " + contentAvailable.get(objectToSend.getId()));
        ////System.out.println(objectToSend.getId() + " " + contentAvailable.get(objectToSend.getId()));
    }

    public int getStateTableFromRouter(String objectID) {
        int i = 0;
        for (i = 0; i < stateTableOfObjects.size(); i++) {
            if (stateTableOfObjects.get(i).getObjectId().equalsIgnoreCase(objectID)) {
                return i;
            }
        }
        return -1;
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

    public TreeMap<String, Double> sortBylambdaConnectivity() {
        Map<DTNHost, Double> lambdafromState0toNodes = lambdaDTNHost0toNodes();
        listofDTNHostForLambdaSort.clear();
        Map<String, Double> temp = new HashMap<>();
        for (DTNHost dt : lambdafromState0toNodes.keySet()) {
            temp.put(dt.toString(), lambdafromState0toNodes.get(dt));
            listofDTNHostForLambdaSort.put(dt.toString(), dt);
        }
        ValueComparator vc = new ValueComparator(temp);
        TreeMap<String, Double> sorted_lambda = new TreeMap<>(vc);
        sorted_lambda.putAll(temp);
        //logger.info("Sorted Sequence " + sorted_lambda);
        return sorted_lambda;

    }

    public TreeMap<String, Double> sortBylambdaAndProbabilty() {
        Map<DTNHost, Double> lambdafromState0toNodes = lambdaDTNHost0toNodes();
        listofDTNHostForLambdaSort.clear();
        Map<String, Double> templambda = new HashMap<>();
        for (DTNHost dt : lambdafromState0toNodes.keySet()) {
            templambda.put(dt.toString(), (lambdafromState0toNodes.get(dt) + stateProbability.get(dt)));
            listofDTNHostForLambdaSort.put(dt.toString(), dt);
        }
        ValueComparator vc = new ValueComparator(templambda);
        TreeMap<String, Double> sorted_lambda = new TreeMap<>(vc);
        sorted_lambda.putAll(templambda);
        return sorted_lambda;

    }

    public Map<Integer, DTNHost> mappedReplicas(String sortType) {
        Map<Integer, DTNHost> mappedDTN = new HashMap<>();
        try {
            if (sortType.equalsIgnoreCase("Probability")) {
                TreeMap<String, Double> sortProbability = sortProbability();
                Map<String, DTNHost> mappedDTNHosts = new HashMap<String, DTNHost>();

                Set<String> stateProbabilityKeySet = stateProbability.keySet();
                Set<DTNHost> lambdaofconnectinghostkeySet = lambdaDTNHost0toNodes().keySet();
                Iterator<DTNHost> lambdaConnectingIterator = lambdaofconnectinghostkeySet.iterator();

                while (lambdaConnectingIterator.hasNext()) {
                    DTNHost nextDTN = lambdaConnectingIterator.next();
                    Iterator<String> stateKeySetIterator = stateProbabilityKeySet.iterator();
                    while (stateKeySetIterator.hasNext()) {
                        String nextString = stateKeySetIterator.next();
                        if (nextDTN.toString().equalsIgnoreCase(nextString)) {
                            mappedDTNHosts.put(nextString, nextDTN);
                        }

                    }
                }

                Set<String> sortedProbabilitykeySet = sortProbability.keySet();
                Iterator<String> sortedProbabiltyIterator = sortedProbabilitykeySet.iterator();

                int i = 0;
                //System.out.println("checking " + this.getNoOfReplicas());
                while (sortedProbabiltyIterator.hasNext() && i < this.getNoOfReplicas()) {
                    String probabilityState = sortedProbabiltyIterator.next();
                    if (probabilityState.equalsIgnoreCase("0")) {
                        continue;
                    } else {
                        mappedDTN.put(i, mappedDTNHosts.get(probabilityState));
                        i++;
                    }
                }
                logger.info(SimClock.getTime() + " " + getHost() + " No of Replicas " + this.getNoOfReplicas()
                        + " replicacandidate " + mappedDTN.values());
            } else if (sortType.equalsIgnoreCase("lambda")) {
                TreeMap<String, Double> sortProbability = sortBylambdaConnectivity();
                Iterator<String> sortedProbabiltyIterator = sortProbability.keySet().iterator();
                int i = 0;
                while (sortedProbabiltyIterator.hasNext() && i < this.getNoOfReplicas()) {
                    String lambdaState = sortedProbabiltyIterator.next();
                    if (lambdaState.equalsIgnoreCase("0")) {
                        continue;
                    } else {
                        mappedDTN.put(i, listofDTNHostForLambdaSort.get(lambdaState));
                        i++;
                    }
                }

                logger.info(SimClock.getTime() + " " + getHost() + " No of Replicas " + this.getNoOfReplicas()
                        + " replicacandidate " + mappedDTN.values());

            } else if (sortType.equalsIgnoreCase("lamda+probability")) {
                TreeMap<String, Double> sortProbability = sortBylambdaAndProbabilty();
                Iterator<String> sortedProbabiltyIterator = sortProbability.keySet().iterator();
                int i = 0;
                while (sortedProbabiltyIterator.hasNext() && i < this.getNoOfReplicas()) {
                    String lambdaState = sortedProbabiltyIterator.next();
                    if (lambdaState.equalsIgnoreCase("0")) {
                        continue;
                    } else {
                        mappedDTN.put(i, listofDTNHostForLambdaSort.get(lambdaState));
                        i++;
                    }
                }

                logger.info(SimClock.getTime() + " " + getHost() + " No of Replicas " + this.getNoOfReplicas()
                        + " replicacandidate " + mappedDTN.values());

            }
        } catch (Exception e) {

        }

        ////System.out.println(" " + mappedDTN.entrySet());
        return mappedDTN;
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

    public class ContentObject {

        String objectId;
        int size;
        DTNHost from;
        double creationTime, replicationTime, expectedRetriveTime;

        public ContentObject(String objectId, int size, DTNHost from, double creationTime, double replicationTime) {
            this.objectId = objectId;
            this.size = size;
            this.from = from;
            this.creationTime = creationTime;
            this.replicationTime = replicationTime;
        }

        public ContentObject(String objectId, int size, DTNHost from, double creationTime, double replicationTime, double expectedRetriveTime) {
            this.objectId = objectId;
            this.size = size;
            this.from = from;
            this.creationTime = creationTime;
            this.replicationTime = replicationTime;
            this.expectedRetriveTime = expectedRetriveTime;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ContentObject other = (ContentObject) obj;
            if (!Objects.equals(this.objectId, other.objectId)) {
                return false;
            }
            return true;
        }

        public String toString() {
            return this.objectId + " rep " + this.replicationTime + " cre " + this.creationTime + " from " + this.from.toString() + " expectedRetriveTime " + this.expectedRetriveTime;
        }

    }

    public class QueryContent {

        String queryID;
        String objectToQuery;
        int objectSize;
        DTNHost queryFrom;
        double queryCreationTime;
        double expectedRetriveTime;

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
            int i = -1;
            for (i = 0; i < createByOwn.size(); i++) {
                if (createByOwn.get(i).objectId.equalsIgnoreCase(this.objectToQuery)) {
                    break;
                }
            }
            if (i != -1) {
                this.expectedRetriveTime = createByOwn.get(i).expectedRetriveTime;
            }
        }

        @Override
        public String toString() {
            return " queryID " + this.queryID + " objToQuery " + this.objectToQuery + " objSize " + this.objectSize + " from " + this.queryFrom.toString() + " querytime " + this.queryCreationTime + " expectedRetriveTime " + this.expectedRetriveTime;

        }

    }

//    public void bruteforceSelectionSet(int k) {
//        k = 3;
//
//        Set<String> keySet = stateProbability.keySet();
//        Map<DTNHost, Double> lambdaDTNHost0toNodes = lambdaDTNHost0toNodes();
//        Set<DTNHost> keySet1 = lambdaDTNHost0toNodes.keySet();
//        Map<String, DTNHost> mappedDTN = new HashMap<String, DTNHost>();
//
//        ArrayList<DTNHost> selected = new ArrayList<>();
//
//        Iterator<String> iterator = keySet.iterator();
//
//        Iterator<String> iterator1 = keySet.iterator();
//
//        Iterator<String> iterator2 = keySet.iterator();
//
//        Iterator<DTNHost> iterator3 = keySet1.iterator();
//
//        while (iterator3.hasNext()) {
//            DTNHost nextDTN = iterator3.next();
//            Iterator<String> iterator4 = keySet.iterator();
//            while (iterator4.hasNext()) {
//                String nextString = iterator4.next();
//                if (nextDTN.toString().equalsIgnoreCase(nextString)) {
//                    mappedDTN.put(nextString, nextDTN);
//                }
//
//            }
//        }
//
//        //System.out.println("mapped DTN : " + mappedDTN.keySet());
//        while (iterator.hasNext()) {
//            String next = iterator.next();
//            double sum = 0;
//            double sum2 = 0;
//            double min = Double.MAX_VALUE;
//            double lastMin = 0.0;
//            while (iterator1.hasNext()) {
//
//                String next1 = iterator1.next();
//                if (!next.equalsIgnoreCase(next1)) {
//                    while (iterator2.hasNext()) {
//                        String next2 = iterator2.next();
//
//                        if (!next.equalsIgnoreCase(next2) && !next1.equalsIgnoreCase(next2)) {
//                            sum = stateProbability.get(next)
//                                    + stateProbability.get(next1);
//
//                            //   //System.out.println("sum " + sum);
//                            sum2 = lambdaDTNHost0toNodes.get(mappedDTN.get(next))
//                                    + lambdaDTNHost0toNodes.get(mappedDTN.get(next1));
//
//                            lastMin = (1 - sum) / sum2;
//
//                            if (lastMin < min) {
//                                selected.clear();
//                                min = lastMin;
//                                selected.add(mappedDTN.get(next));
//                                selected.add(mappedDTN.get(next1));
//                                //selected.add(mappedDTN.get(next2));
//                            }
//
//                        }
//                    }
//
//                }
//            }
//
//        }
//        //System.out.println("selected list: " + selected);
//    }
    class Logger {

        public void info(String x) {
            System.out.println(x);
        }
    }

    class StateTableRow {

        private String objectId;
        private ArrayList<DTNHost> replicaCandidates;
        private ArrayList<ReplicaState> replicaState;
        private boolean replicaDone;

        /**
         * @return the objectId
         */
        public String getObjectId() {
            return objectId;
        }

        /**
         * @return the replicaCandidates
         */
        public ArrayList<DTNHost> getReplicaCandidates() {
            return replicaCandidates;
        }

        /**
         * @param replicaCandidates the replicaCandidates to set
         */
        public void setReplicaCandidates(ArrayList<DTNHost> replicaCandidates) {
            this.replicaCandidates = replicaCandidates;
        }

        /**
         * @return the replicaState
         */
        public ArrayList<ReplicaState> getReplicaState() {
            return replicaState;
        }

        /**
         * @return the replicaDone
         */
        public boolean isReplicaDone() {
            return replicaDone;
        }

        /**
         * @param replicaDone the replicaDone to set
         */
        public void setReplicaDone(boolean replicaDone) {
            this.replicaDone = replicaDone;
        }

        /**
         * @param objectId the objectId to set
         */
        public void setObjectId(String objectId) {
            this.objectId = objectId;
        }

        @Override
        public String toString() {
            return "Obj " + objectId + " replicateNodes " + this.replicaCandidates
                    + " replicaState" + this.replicaState + " replicadonetoall "
                    + this.replicaDone + " \n";//To change body of generated methods, choose Tools | Templates.
        }

    }

    class ReplicaState {

        DTNHost replicaDoneToNode;
        double replicaTime;

        public ReplicaState(DTNHost replicaDoneToNode, double replicaTime) {
            this.replicaDoneToNode = replicaDoneToNode;
            this.replicaTime = replicaTime;
        }

        @Override
        public String toString() {
            return "replicaNode " + replicaDoneToNode.toString() + " replicatime " + replicaTime; //To change body of generated methods, choose Tools | Templates.
        }

    }

    class NeighbourDetails {

        private DTNHost neighbour;
        private List<Double> meetingTime = new ArrayList<>();
        private Map<Integer, Double> perMeetingDuration = new HashMap<>();
        private int meetOccurance;

        /**
         * @return the neighbour
         */
        public DTNHost getNeighbour() {
            return neighbour;
        }

        /**
         * @param neighbour the neighbour to set
         */
        public void setNeighbour(DTNHost neighbour) {
            this.neighbour = neighbour;
        }

        /**
         * @return the meetingTime
         */
        public List<Double> getMeetingTime() {
            return meetingTime;
        }

        /**
         * @param meetingTime the meetingTime to set
         */
        public void setMeetingTime(List<Double> meetingTime) {
            this.meetingTime = meetingTime;
        }

        /**
         * @return the perMeetingDuration
         */
        public Map<Integer, Double> getPerMeetingDuration() {
            return perMeetingDuration;
        }

        /**
         * @param perMeetingDuration the perMeetingDuration to set
         */
        public void setPerMeetingDuration(Map<Integer, Double> perMeetingDuration) {
            this.perMeetingDuration = perMeetingDuration;
        }

        /**
         * @return the meetOccurance
         */
        public int getMeetOccurance() {
            return meetOccurance;
        }

        /**
         * @param meetOccurance the meetOccurance to set
         */
        public void setMeetOccurance(int meetOccurance) {
            this.meetOccurance = meetOccurance;
        }

        @Override
        public String toString() {
            return "NeighbourDetails{" + "neighbour=" + neighbour + ", meetingTime=" + meetingTime + ", perMeetingDuration=" + perMeetingDuration + ", meetOccurance=" + meetOccurance + '}'+"\n";
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
