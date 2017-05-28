/**
 * Author: Aditya Pulekar
 * Author: Vishal Garg
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * LoadBalancer starts all child Managers and is the container
 * class for processing/redirecting client requests
 */

@SuppressWarnings("unchecked")
public class LoadBalancer {

    static final int replicationFactor = 5;
    static final String USERNAME = "acp8882";
    static final int portForManager = 2445;
    static final int portForLoadBalancer = 1234;
    static String[] childServers = {"domino.cs.rit.edu", "argus.cs.rit.edu", "cyclops.cs.rit.edu"};
    static String[] backupChildServers ={"tiresias.cs.rit.edu", "carya.cs.rit.edu", "argonaut.cs.rit.edu"};
    static int TOTAL = childServers.length;
    //fileToServers store list of servers where file is stored
    static HashMap<String, LinkedList<String>> fileToServers = new HashMap<String, LinkedList<String>>();
    static HashMap<String, String> fileToBackupServer = new HashMap<String, String>();
    //fileToIndex tells which index of server in server list should process next request for the File
    static HashMap<String, Integer> fileToIndex = new HashMap<String, Integer>();
    //popularityIndex tells popularity of each file
    static Map<String, Integer> popularityIndex = new HashMap<String, Integer>();
    static List<String> managersToStop = new LinkedList<String>();
    static HashMap<String, LinkedList<String>> serverToFiles = new HashMap<String, LinkedList<String>>();


    //Method to provide hash value for filename and tree level and index in that level
    int hashIt(String name, int treeLevel, int nodeNum){
        int total = 0;
        //Finding value for file name
        for(int i = 0; i < name.length(); i++){
            char temp = name.charAt(i);
            total += Character.getNumericValue(temp);
        }
        //Adding the value found to tree level and node number
        total += Math.pow(2, treeLevel) - 1 + nodeNum;
        //Giving hash value according to number of servers
        return total % TOTAL;
    }

    //Method to send file(contribution) to a particular server
    void sendFileData(String serverName, String fileName, String fileData){
        try{

            //Establish connection with server
            Socket sock = new Socket(serverName, portForManager);
            ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());

            //Send data to server
            out.writeObject("contribution");
            String[] sendData = {fileName, fileData};
            out.writeObject(sendData);
            sock.close();
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    void stopContainers(){
        try{
            for(String childServs : childServers){
                //Establish connection with server
                Socket sock = new Socket(childServs, portForManager);
                ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                //Send data to server
                out.writeObject("Stop");
                sock.close();
            }

            for(String childServs : backupChildServers){
                //Establish connection with server
                Socket sock = new Socket(childServs, portForManager);
                ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                //Send data to server
                out.writeObject("Stop");
                sock.close();
            }
            //Thread.sleep(30000);
            System.exit(0);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //Method to replicate file
    void replicate(String fileName){
        LinkedList<String> serverList = new LinkedList(fileToServers.get(fileName));
        int listSize = serverList.size();
        int level = 1;

        //Find main child server
        int rootHash = hashIt(fileName, 0, 0);
        String rootServer = childServers[rootHash];

        //Find level to replicate to
        while(listSize>=Math.pow(2, level)){
            level++;
        }

        //Replicate at each server
        try{

            //Get file data from root child server
            Socket temp = new Socket(rootServer, portForManager);
            ObjectOutputStream out = new ObjectOutputStream(temp.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(temp.getInputStream());
            out.writeObject("search");
            out.writeObject(fileName);
            String fileData = (String)in.readObject();

            //Send file to all servers at that level for tree of file
            for(int i= 0;i<Math.pow(2,level); i++){
                int tempHash = hashIt(fileName, level, i);
                String replicateServerName = childServers[tempHash];

                //Send file data to server
                sendFileData(replicateServerName, fileName, fileData);
                serverList.add(replicateServerName);
            }

            //Update LinkedList for the fileName with new servers added
            fileToServers.put(fileName, serverList);
            temp.close();
        }catch(Exception e){
            e.printStackTrace();
        }

    }


    void contributeFile(String whichOne, String fileName, String fileData){
        //Find suitable/root server
        String fileServer;
        int hashVal = hashIt(fileName, 0, 0);
        if(whichOne.equals("shadow")){
            fileServer = backupChildServers[hashVal];
        }else{
            fileServer = childServers[hashVal];
        }
        LinkedList<String> temp = new LinkedList<String>();
        temp.add(fileServer);

        //Initialize values for records on Load Balancer
        if(whichOne.equalsIgnoreCase("shadow")){
            fileToBackupServer.put(fileName, fileServer);
        }else{
            fileToServers.put(fileName, temp);
            fileToIndex.put(fileName, 0);
            popularityIndex.put(fileName, 0);
        }

        //Make mapping for files to server map
        LinkedList<String> tempList;
        if(!serverToFiles.containsKey(fileServer)){
            tempList = new LinkedList<String>();
        }else{
            tempList = serverToFiles.get(fileServer);
        }
        tempList.add(fileName);
        serverToFiles.put(fileServer, tempList);

        //Send file to server
        sendFileData(fileServer, fileName, fileData);
    }

    //Method to start Heart Beat threads
    public static void startHeartBeat(LoadBalancer lb){
        for(String server: childServers){
            lb.new HeartBeat(server).start();
        }
    }

    /**
     * @author wIsh
     * Class to check if Manager is running or not
     */
    class HeartBeat extends Thread{
        String managerToCheck;
        Socket managerSocket;

        public HeartBeat(String serverToCheck){
            this.managerToCheck = serverToCheck;
        }

        public void run(){
            System.out.println("THREAD STARTED FOR MANAGER: "+managerToCheck);
            try{
                while(true){
                    managerSocket = new Socket(managerToCheck, portForManager);
                    ObjectOutputStream out = new ObjectOutputStream(managerSocket.getOutputStream());
                    out.writeObject("h e a r t b e a t");
                    Thread.sleep(50);
                    managerSocket.close();
                }
            }catch(Exception e){
                System.out.println("Manager: "+managerToCheck+" is down!");

                //Remove server Mappings
                LinkedList<String> serverFileList = serverToFiles.get(managerToCheck);
                for(String fileName: serverFileList){
                    LinkedList<String> fileServerList = fileToServers.get(fileName);
                    fileServerList.remove(managerToCheck);
                    fileToServers.put(fileName, fileServerList);
                    fileToIndex.put(fileName, 0);
                }

                //Change to backup
                List<String> tempList = Arrays.asList(childServers);
                int index = tempList.indexOf(managerToCheck);
                String backupName = backupChildServers[index];
                childServers[index] = backupName;
                System.out.println("CHANGING "+managerToCheck+" to "+backupName);
                //Update mappings
                serverFileList = serverToFiles.get(backupName);
                for(String fileName: serverFileList){
                    LinkedList<String> fileServerList = fileToServers.get(fileName);
                    fileServerList.add(backupName);
                    fileToServers.put(fileName, fileServerList);
                }
            }
        }
    }

    /**
     * @author wIsh
     * Class to take care of incoming client requests
     */
    class ClientToMainServ extends Thread {
        Socket clientSocket;
        public ClientToMainServ(Socket client){
            clientSocket = client;
        }

        public void run(){
            try {
                //Setup connections
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                //Get tag and file name
                String tag = (String) in.readObject();
                String fileName = (String)in.readObject();

                //Check what the request was and process
                //CASE OF CONTRIBUTION REQUEST
                if(tag.equalsIgnoreCase("remove")){
                    //Remove Mappings
                    LinkedList temp = fileToServers.get(fileName);
                    String managerName = clientSocket.getInetAddress().getHostName();
                    if(!managerName.contains(".cs.rit.edu")){
                        managerName += ".cs.rit.edu";
                    }
                    System.out.println("REMOVING: "+managerName+" for file: "+fileName);
                    temp.remove(managerName);
                    fileToServers.put(fileName, temp);
                    //Resetting index so that LinkedList doesn't go out of bound on search
                    fileToIndex.put(fileName, 0);
                    //Reroute connection
                    String backupServerName = fileToBackupServer.get(fileName);
                    //out.writeObject("connect");
                    out.writeObject(backupServerName);
                    out.writeObject(portForManager);
                    Thread.sleep(4000);
                    //Create Mapping
                    temp = fileToServers.get(fileName);
                    temp.add(managerName);
                    fileToServers.put(fileName, temp);
                }else if (tag.equalsIgnoreCase("contribution")) {
                    System.out.println("\nContributing content for '" + fileName + "'.....");
                    String fileData = (String)in.readObject();

                    //For first time contribution of a file
                    if(!fileToServers.containsKey(fileName)){
                        contributeFile("", fileName, fileData);
                        contributeFile("shadow", fileName, fileData);

                    }else{

                        //If file was already there then update on all servers for file
                        LinkedList<String> temp = new LinkedList(fileToServers.get(fileName));
                        for(String fileServer: temp){
                            sendFileData(fileServer, fileName, fileData);
                        }
                        String backupServer = fileToBackupServer.get(fileName);
                        sendFileData(backupServer, fileName, fileData);
                    }

                    //THE CASE OF SEARCH REQUEST
                } else if (tag.equalsIgnoreCase("search")) {
                    if (fileName.equalsIgnoreCase("stop")) {
                        out.writeObject("OK");
                        stopContainers();
                    } else {
                        System.out.println("\nSearching content for '" + fileName + "'.....");
                        if (fileToServers.containsKey(fileName)) {

                            //Increase popularity
                            int popularity = popularityIndex.get(fileName);
                            popularityIndex.put(fileName, popularity + 1);
                            if ((popularity + 1) % replicationFactor == 0 && fileToServers.get(fileName).size() < TOTAL) {
                                replicate(fileName);
                            }

                            //Find which server should process request
                            //Creating new Linked Lists to avoid concurrent exceptions
                            LinkedList<String> serverList = new LinkedList(fileToServers.get(fileName));
                            String processorName;
                            //If container was down at this time
                            if(serverList.size()==0){
                                processorName = fileToBackupServer.get(fileName);
                            }else{
                                int nextProcessor = fileToIndex.get(fileName);
                                processorName = serverList.get(nextProcessor);

                                //Round-robin implementation
                                nextProcessor++;
                                if (nextProcessor == serverList.size()) {
                                    nextProcessor = 0;
                                }
                                fileToIndex.put(fileName, nextProcessor);
                            }


                            //Forward request to Manager server
                            out.writeObject("connect");
                            out.writeObject(processorName);
                            out.writeObject(portForManager);

                        } else {
                            out.writeObject("File not found!");
                        }
                    }
                    clientSocket.close();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void buildImage(String hostName, String pathForDockerfile){
        try {
            String command = "ssh " + USERNAME + "@" + hostName + " 'cd " + pathForDockerfile + " ; docker build -t manager_img .'";
            String[] commandArgs = new String[] {"/bin/bash", "-c", command};
            Process proc = new ProcessBuilder(commandArgs).start();
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runImage(String hostName, int port, String imageName){
        try {
            String command = "ssh " + USERNAME + "@" + hostName + " 'docker run -d -p " + port + ":1234 " + imageName + "'";
            String[] commandArgs = new String[] {"/bin/bash", "-c", command};
            Process proc = new ProcessBuilder(commandArgs).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while((line=br.readLine())!=null){
                System.out.println("Manager container ID: " + line.substring(0,12));
                managersToStop.add(line.substring(0,12));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args){
        try {
            LoadBalancer mServ = new LoadBalancer();
            ServerSocket clientServSoc = new ServerSocket(portForLoadBalancer);
            //Initialize heartbeat
            startHeartBeat(mServ);
            while(true){
                System.out.println("\nWaiting for the client to connect....");
                Socket clientConn = clientServSoc.accept();
                mServ.new ClientToMainServ(clientConn).start();
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}