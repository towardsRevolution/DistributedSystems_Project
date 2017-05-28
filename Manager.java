/**
 * Author: Aditya Pulekar
 * Author: Vishal Garg
 */
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

//Make sure that the child server is listening on physical port 2500.

public class Manager implements Runnable{
    static String loadBalancer = "yes.cs.rit.edu";
    static int loadBalancerport = 1234;
    static ServerSocket managerServSoc;
    static int PORT = 2445;
    static int port_forContainer = 3101;
    Socket clientSocket;
    static final String USERNAME = "acp8882";
    ObjectInputStream in_client;
    ObjectOutputStream out_client;
    ObjectInputStream in_fromContainer;
    ObjectOutputStream out_toContainer;
    static Map<String,Integer> fileToPortsMapping;
    static List<String> containersToStop = new LinkedList<String>();

    public Manager(Socket s){
        clientSocket = s;
    }

    public static void buildImage(String pathForDockerfile){
        try {
            //Eg: ssh acp8882@carya.cs.rit.edu 'cd ~/Courses/Wikipedia_Project/forContainer ; docker build -t img_for_container .'
            String command = "ssh " + USERNAME + "@" + InetAddress.getLocalHost().toString().split("/")[0]+".cs.rit.edu" + " 'cd " + pathForDockerfile + " ; docker build -t img_for_container .'";
            //System.out.println(command);
            String[] commandArgs = new String[] {"/bin/bash", "-c", command};
            Process proc = new ProcessBuilder(commandArgs).start();
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runImage() {
        try {
            String command = "ssh " + USERNAME + "@" + InetAddress.getLocalHost().toString().split("/")[0]+".cs.rit.edu" + " docker run -d -p " + port_forContainer + ":2500 img_for_container";
            String[] commandArgs = new String[]{"/bin/bash", "-c", command};
            Process proc = new ProcessBuilder(commandArgs).start();
            //proc.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while((line=br.readLine())!=null){
                containersToStop.add(line.substring(0,12));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopContainers(){
        try {
            for (String IDs : containersToStop) {
                String[] commandArgs = new String[]{"/bin/bash", "-c", "docker stop " + IDs};
                Process proc = new ProcessBuilder(commandArgs).start();
                proc.waitFor();
            }
            System.out.println("Containers stopped! System exiting...");
            System.exit(0);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    //Method to send file info to container with specified port number
    public void communicateWithContainer(String[] contribution, int port_forContainer){
        try {
            //So, Manager will be communicating with the same host but on a different virtual port on which the container will be listening.
            System.out.println("Host--> " + InetAddress.getLocalHost().toString().split("/")[0]+".cs.rit.edu");
            System.out.println("Communicating to container port --> " + port_forContainer);
            Socket containerSoc = new Socket(InetAddress.getLocalHost().toString().split("/")[0]+".cs.rit.edu", port_forContainer);
            out_toContainer = new ObjectOutputStream(containerSoc.getOutputStream());
            //Note that we are sending the contributed file below.
            out_toContainer.writeObject("ADD");
            out_toContainer.writeObject(contribution);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startContainer(String why, String fileData[]){

        System.out.println("\nCreating container for the "+why+" request....");
        runImage();
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("CONTAINER CREATED!");
        fileToPortsMapping.put(fileData[0],port_forContainer);
        System.out.println("FileToPort map: \n" + fileToPortsMapping + "\n");
        communicateWithContainer(fileData, port_forContainer);
        new HeartBeat(port_forContainer, fileData[0]).start();
        port_forContainer++;
    }

    /**
     * @author wIsh
     * Class to check if Containers are running
     */
    class HeartBeat extends Thread{
        int portToCheck;
        Socket containerSocket;
        String fileName;
        public HeartBeat(int port, String fileName){
            portToCheck = port;
            this.fileName = fileName;
        }

        public void run(){
            try{
                String hostName = InetAddress.getLocalHost().toString().split("/")[0]+".cs.rit.edu";
                while(true){
                    //Send heartbeat to container
                    //If it cannot connect that means server is down and goes to catch block
                    containerSocket = new Socket(hostName, portToCheck);
                    ObjectOutputStream out = new ObjectOutputStream(containerSocket.getOutputStream());
                    out.writeObject("h e a r t b e a t");
                    Thread.sleep(50);
                    containerSocket.close();
                }

            }catch(Exception e){
                System.out.println("Container with port: " + portToCheck+" is down!");
                fileToPortsMapping.remove(fileName);
                try {
                    //Communicate with Load Balancer
                    Socket loadBalan = new Socket(loadBalancer, loadBalancerport);
                    //Tell to remove fileName to server mapping
                    ObjectOutputStream out = new ObjectOutputStream(loadBalan.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(loadBalan.getInputStream());
                    out.writeObject("remove");
                    out.writeObject(fileName);
                    //System.out.println("SENT DATA TO LOAD BALANCER");
                    //Get file again from backup
                    String backupServerName = (String)in.readObject();
                    int backupServerPort = (int)in.readObject();
                    Socket backupServer = new Socket(backupServerName, backupServerPort);
                    out = new ObjectOutputStream(backupServer.getOutputStream());
                    in = new ObjectInputStream(backupServer.getInputStream());
                    out.writeObject("search");
                    out.writeObject(fileName);
                    String fileData = (String)in.readObject();
                    //System.out.println("GOT DATA FROM BACKUP");
                    //START CONTAINER AGAIN
                    String temp[]={fileName, fileData};
                    startContainer("Restart Container", temp);

                } catch (Exception e1) {
                    System.out.println("Cannot connect with load balancer!");
                }

            }
        }
    }

    @Override
    public void run(){
        //No need to build the image per request. Transfer the below line to some other place where build is ensured just once.

        try {

            out_client = new ObjectOutputStream(clientSocket.getOutputStream());
            in_client = new ObjectInputStream(clientSocket.getInputStream());
            String tag = (String) in_client.readObject();
            if(tag.equals("h e a r t b e a t")){
                //System.out.println("\nHeartBeat Message!");
            }else if(tag.equalsIgnoreCase("contribution")){
                String[] contribution = (String[]) in_client.readObject();
                String fileName = contribution[0];

                if(!fileToPortsMapping.containsKey(fileName)){
                    //CONTAINER GETS CREATED ONLY FOR A CONTRIBUTE REQUEST!!!
                    startContainer("'Contribute'", contribution);
                }else{
                    //ADD CODE TO UPDATE FILE HERE
                    System.out.println("\nFile already present.....Updating it!");
                    communicateWithContainer(contribution, fileToPortsMapping.get(contribution[0]));
                }

                //Files.write(new File("Contributions/"+ fileName + ".txt").toPath(), bytes_contribution);
            } else if(tag.equalsIgnoreCase("Stop")) {
                System.out.println("\nStopping the containers.....");
                stopContainers();
            } else {
                //SEARCH PART
                String searchQuery = (String) in_client.readObject();

                System.out.println("\nsearchQuery: " + searchQuery + "\n FileToPort map:  " + fileToPortsMapping + "\n");
                System.out.println("fileToPortsMapping.containsKey(searchQuery)-->" + fileToPortsMapping.containsKey(searchQuery));
                //Check if the file has been added in your map. If so then that means that a container has it.
                if(fileToPortsMapping.containsKey(searchQuery)){
                    System.out.println("File found on the container running on port : " + fileToPortsMapping.get(searchQuery));
                    System.out.println("Host--> " + InetAddress.getLocalHost().toString().split("/")[0]+".cs.rit.edu");
                    System.out.println("Communicating to container port --> " + fileToPortsMapping.get(searchQuery));
                    Socket containerSoc = new Socket(InetAddress.getLocalHost().toString().split("/")[0]+".cs.rit.edu",fileToPortsMapping.get(searchQuery));
                    out_toContainer = new ObjectOutputStream(containerSoc.getOutputStream());
                    in_fromContainer = new ObjectInputStream(containerSoc.getInputStream());
                    out_toContainer.writeObject("SEND_BACK");
                    out_toContainer.writeObject(searchQuery);
                    byte[] fileBytes = (byte[]) in_fromContainer.readObject();
                    String file = new String(fileBytes);
                    System.out.println("Sending the file back to the client...");
                    //Send data back to Client
                    out_client.writeObject(file);
                } else {
                    //THIS will never happen
                    out_client.writeObject("NO");
                }
            }
            clientSocket.close();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void main(String[] args){
        try {
            managerServSoc = new ServerSocket(PORT);
            fileToPortsMapping = new LinkedHashMap<String,Integer>();
            buildImage("~/Courses/Wikipedia_Project/forContainer");
            System.out.println("Image for the containers built!!!!\n");
            while(true){
                //System.out.println("\nWaiting for a host to connect....");
                Socket sock = managerServSoc.accept();
                new Thread(new Manager(sock)).start();
            }
        } catch(Exception e){
            e.printStackTrace();
        }

    }
}
