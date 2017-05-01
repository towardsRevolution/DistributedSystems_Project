package DistributedSystems_Project;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

/**
 * Created by adityapulekar on 5/1/17.
 */
public class mainServer {

    public static void main(String[] args){
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            ServerSocket serverSoc = new ServerSocket(1234);
            while(true) {
                System.out.println("Waiting for the client to connect.....");
                Socket clientConn = serverSoc.accept();
                ObjectInputStream in = new ObjectInputStream(clientConn.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientConn.getOutputStream());
                String tag = (String) in.readObject();
                //"Contribution" works perfect.
                if (tag.equals("Contribution")) {
                    String contribution = (String) in.readObject();
                    byte[] bytes_contribution = contribution.getBytes();
                    Files.write(new File("test_CONTRIBUTION.txt").toPath(), bytes_contribution);
                    System.out.println("\nCONTRIBUTION SUCCESSFULLY RECORDED in 'test_CONTRIBUTION.txt'");
                    out.writeObject("CONTRIBUTION SUCCESSFULLY RECORDED!");
                } else if (tag.equals("Search")) {
                    String searchQuery = (String) in.readObject();
                    System.out.println("SENDING RESPONSE FOR SEARCH_QUERY: " + searchQuery);
                    byte[] fileBytes = Files.readAllBytes(new File("test_SEARCH_EDIT.txt").toPath());
                    out.writeObject(fileBytes);
                    System.out.println("RESPONSE SENT!");
                }
            }

        } catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }

    }
}
