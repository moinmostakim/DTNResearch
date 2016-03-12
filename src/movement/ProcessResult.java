/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package movement;

//import core.Tuple;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 * @author mduddin2
 */
public class ProcessResult {

    private final int MAX = 401;
    ContactList interContacts[][];
    InterContactList interContactDelays[];
    
    private static final int noOfNodes = 401;
    
    private double sum;
    private int count;
    
    public ProcessResult(String fileName) throws Exception
    {               
        System.out.println("File "+ fileName);
        
        interContacts = new ContactList[MAX][MAX];
        interContactDelays = new InterContactList[noOfNodes];
        
        for(int i =0; i < MAX; i++)
        {            
            if (i < noOfNodes)
            interContactDelays[i] = new InterContactList(i);
            
            for(int j= 0; j < MAX; j++)
            {
                interContacts[i][j] = new ContactList(i, j);                
            }
        }
        
        
        BufferedReader input = new BufferedReader(new FileReader(fileName));        
        //BufferedReader input = new BufferedReader(new FileReader("C:\\Users\\Yusuf Sarwar\\Documents\\research\\DTN\\One-Move\\exp\\log.meeting.short.txt"));
        //BufferedReader input = new BufferedReader(new FileReader("/home/csgrad/mduddin2/research/DTN/Simulation/One-Move/exp/log.meeting.short.txt"));
        String line = null;
        while((line = input.readLine()) != null)
        {
            StringTokenizer stk = new StringTokenizer(line, " ");
            
            String ts = stk.nextToken();            
            String nodeA = stk.nextToken();            
            String nodeB = stk.nextToken();
                        
            double time = Double.parseDouble(ts);
            int a = Integer.parseInt(nodeA);
            int b = Integer.parseInt(nodeB);
            
            //System.out.println(ts + " " + " " + nodeA + " " + nodeB);
            
            if (time > 172800)
                break;
            process(a, b, time);
            
            //if (a < noOfNodes)
            //processInterContact(a, b, time);
            //if (count++  >= 2000)
            //    break;
        }
        
        /*
        for (int i=0; i<MAX; i++)
            for(int j=0; j<MAX; j++)
            {
              if (interContacts[i][j].list.size() > 0)
              {
                  System.out.println(interContacts[i][j]);        
                  for(double v : interContacts[i][j].list)
                      System.out.println("IC " + v);
              }
              
            }
        */
        
        //printNeighbor();
        //outputReliefCenterToRHousehold();
        //outputInterContactDelay();        
    }   
    
    public void printNeighbor()
    {        
        int count = 0;
        for(int i=0 ; i < MAX; i++)
        {
            count = 0;
            for(int j=0; j < MAX; j++)                
                if (interContacts[i][j].count >= 10)
                {                    
                    count++;                    
                }
            System.out.println("COUNT " + i + " " + count);
        }        
    }
    
    void outputReliefCenterToRHousehold()
    {
        double avg, sum;
        int count = 0;
        avg = sum = 0;
        
        for(int s = 85; s <= 94; s++)
        {
            for(int rl = 70; rl <= 74; rl++)
                if (interContacts[s][rl].list.size() > 0)
                {
                    for(Double v : interContacts[s][rl].list)
                    {
                        sum+= v;
                        count++;
                        System.out.println("IC " + count + "  " + sum/count);
                    }
                }            
            
            for(int rl = 79; rl <= 83; rl++)
                if (interContacts[s][rl].list.size() > 0)
                {                    
                    for(Double v : interContacts[s][rl].list)
                    {
                        sum += v;
                        count++;
                        System.out.println("IC " + count + " " + sum/count);
                    }
                }            
        }            
    }
    
    void outputInterContactDelay(){        
        for(int i = 0; i < interContactDelays.length; i++)
            interContactDelays[i].print();        
    }
    
    public void process(int nodeA, int nodeB, double time)
    {
        ContactList cl = interContacts[nodeA][nodeB];
        
        if(cl.lastMeeting > 0)
        {
            cl.add(time - cl.lastMeeting, cl.lastMeeting);
        }
        
        if (nodeA >= 85 && nodeA <= 94)
        {
            if ((nodeB >=70 && nodeB <= 74) || (nodeB >= 79 && nodeB <= 83))
            {    
                double value = time - cl.lastMeeting;
                sum += value;
                count++;
                //System.out.println("IC " + (time/3600.0) + " " + sum/count);
            }
        }
        
        System.out.println("IC " + (time - cl.lastMeeting));
        
        cl.lastMeeting = time;
    }
    
    void processInterContact(int nodeA, int nodeB, double currTime)
    {
        for(int other = 0; other < MAX; other++)
        {
            if (interContacts[nodeA][other].lastMeeting > 0)
            {
                //interContactDelays[nodeA].contacts[other][nodeB].list.add(currTime - interContacts[nodeA][other].lastMeeting);
                interContactDelays[nodeA].add(other, nodeB, currTime - interContacts[nodeA][other].lastMeeting);
            }
        }
    }
    
    private class InterContactList
    {
        //ContactList contacts[][];
        int contactCount[][];
        int nodeId;
        static final int thresholdCount = 5;
        
        public InterContactList(int n) 
        {
            nodeId = n;
            contactCount = new int[MAX][MAX];
            /*
            contacts = new ContactList[MAX][MAX];
            for(int i=0; i < MAX; i++)
                for(int j=0; j< MAX; j++)
                    contacts[i][j] = new ContactList(i, j);
            */ 
        }
        
        public void add(int nodeA, int nodeB, double time)
        {
            double meetingRatio = 1.0 * interContacts[nodeId][nodeA].count / interContacts[nodeId][nodeB].count;
            
            if (meetingRatio > 1.0)
                meetingRatio = 1.0 / meetingRatio;
            
            if(meetingRatio > 0.75)
            {
                //if (contacts[nodeA][nodeB].list.size() < thresholdCount)
                //    contacts[nodeA][nodeB].list.add(time);
                //else
                contactCount[nodeA][nodeB]++;
                System.out.println("IC " + time);
            }
        }
        
        public void print()
        {
            boolean neighbors[] = new boolean[MAX];
            
            int count = 0;
            for(int i=0 ; i < MAX; i++)
                for(int j=0; j < MAX; j++)
                    if (contactCount[i][j] >= thresholdCount)
                    {
                        if (!neighbors[i]){
                            count++;                        
                            neighbors[i] = !neighbors[i];
                        }
                        if (!neighbors[j])
                        {
                            count++;
                            neighbors[j] = !neighbors[j];
                        }
                    }
            
            System.out.println("COUNT " + nodeId + " " + count);
        }
         
        public String toString()
        {
            String str = null;
            //for(int i=0 ; i < MAX; i++)
            //    for(int j=0; j < MAX; j++)
                    //if (contacts[i][j].list.size() > thresholdCount)
                    //    str += contacts[i][j].toString();
            return str;
        }
    }
    
    public class ContactList
    {
        int nodeA, nodeB;
        List<Double> list;
        double lastMeeting;
        int count;
        
        public ContactList(int i, int j) {
            nodeA = i;
            nodeB = j;
            
            list = new ArrayList<Double>();
            lastMeeting = -1;
            count = 0;
        }               
        
        public void add(double d, double time)
        {
            list.add(d);
            lastMeeting = time;
            count++;
        }
        
        public String toString()
        {
            return "LM " + nodeA + " " + nodeB + " " + lastMeeting +  " Gaps " + list;
        }
    }
    
    public static void main(String[] args)
    {
        try{
            new ProcessResult(args[0]);
            //new ProcessResult("/home/sarwar/windocs/research/DTN/One-Move/exp/neigh/log.meeting-rwp-trans-10.txt");
        
        }catch(Exception ex)
        {
            System.out.println("Halted");
            ex.printStackTrace();
        }        
    }
}
