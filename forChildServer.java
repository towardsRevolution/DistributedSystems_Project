/**
 * Author: Aditya Pulekar
 * Author: Vishal Garg
 */
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;



//NOTE: THE JOB OF CHILD SERVERS WILL BE SIMPLY TO ADD FILES TO DATABASE IF THEY ARE BEING CONTRIBUTED AND CHECK/SEARCH FOR ITS EXISTENCE
//  IF THE SERVER IS BEING ASKED TO DO SO.

public class forChildServer extends Thread{
    static ServerSocket childServerSoc;
    Socket requesterSocket;
    public forChildServer(){

    }

    public forChildServer(Socket temp){
        requesterSocket = temp;
    }

    public void run(){
        System.out.println("Connection has been established with the client: " + requesterSocket + "\n");
        try {
            ObjectInputStream in_frmManager = new ObjectInputStream(requesterSocket.getInputStream());
            ObjectOutputStream out_toManager = new ObjectOutputStream(requesterSocket.getOutputStream());
            String tag = (String) in_frmManager.readObject();
            if(tag.equals("h e a r t b e a t")){

            }else if(tag.equals("ADD")){
                String[] contribution = (String[]) in_frmManager.readObject();
                byte[] bytes_contribution = contribution[1].getBytes();
                Files.write(new File(contribution[0]+".txt").toPath(),bytes_contribution);  //Here we have that file created with the name (contribution[0]+".txt")
            } else {  //i.e. if the tag is "SEND_BACK"
                String fileName = (String)in_frmManager.readObject();
                byte[] fileBytes = Files.readAllBytes(new File(fileName+".txt").toPath());
                out_toManager.writeObject(fileBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final String PROJECT_PATH = "~/Courses/WikipediaProject/";

    public void beginServer(){
        try {
            childServerSoc = new ServerSocket(2500);
            while (true) {
                System.out.println("\n Waiting for the client to connect...");
                Socket clientSocket = childServerSoc.accept();
                new forChildServer(clientSocket).start();
            }
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }


    public static void main(String[] args){
        forChildServer childServer = new forChildServer();
        childServer.beginServer();
    }
}