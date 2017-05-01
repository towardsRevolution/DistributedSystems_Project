package DistributedSystems_Project;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by adityapulekar on 5/1/17.
 */
public class client_gui extends JFrame implements ActionListener {
    JButton JB_C; JButton JB_S; JButton JB_E; JButton JB_Reset;
    JTextField textF_Search;
    JTextField textF;


    public client_gui(){
        JB_C = new JButton("Contribute");
        JB_S = new JButton("Search");
        JB_E = new JButton("Edit");
        JB_Reset = new JButton("Reset");
        JB_C.setBounds(130,300,100,40);
        JB_S.setBounds(130,400,100,40);
        JB_E.setBounds(250,300,100,40);
        JB_Reset.setBounds(250,400,100,40);
        textF_Search = new JTextField();
        textF = new JTextField();
        textF_Search.setBounds(120,50,350,40);
        textF.setBounds(120,100,350,150);
        JB_Reset.addActionListener(this);
        JB_S.addActionListener(this);
        JB_E.addActionListener(this);
        JB_C.addActionListener(this);
        add(JB_C);add(JB_E);add(JB_S);add(JB_Reset);add(textF);add(textF_Search);
        setSize(700,700);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void performSearch(ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{
        //Picks up the content from the search area.
        String query = textF_Search.getText();

        //Sending the "Search" tag. (READ op)
        out.writeObject("Search");

        //Sending search query to the main server.
        out.writeObject(query);

        //Receiving file from the main server for reading.
        byte[] bytesFile_rec = (byte[]) in.readObject();
        textF.setText(new String(bytesFile_rec));
    }

    @Override
    public void actionPerformed(ActionEvent ae){
        try {

            if (ae.getSource() == JB_C) {
                Socket clientSoc = new Socket("yes.cs.rit.edu", 1234);
                System.out.println("Connected to glados: " + clientSoc);
                ObjectOutputStream out = new ObjectOutputStream(clientSoc.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSoc.getInputStream());

                //Picks up the content from the text area.
                String newContribution = textF.getText();

                //Sending the "Contribution" tag.
                out.writeObject("Contribution");

                //Sending the new Contribution to the main server.
                out.writeObject(newContribution);

                //Receiving file from the main server for reading.
                String response = (String) in.readObject();
                System.out.println("\n" + response);

            }
            if (ae.getSource() == JB_S || ae.getSource() == JB_E) {
                Socket clientSoc = new Socket("yes.cs.rit.edu", 1234);
                System.out.println("Connected to glados: " + clientSoc);
                ObjectOutputStream out = new ObjectOutputStream(clientSoc.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSoc.getInputStream());

                //Fetches the file from the main server (OR Entry point server).
                performSearch(out,in);

                if(ae.getSource() == JB_S)
                    textF.setEditable(false);
                else
                    textF.setEditable(true);

            }
            if (ae.getSource() == JB_Reset){ //For Reset

                textF.setText("");
                textF_Search.setText("");
                //validate();
            }
            //revalidate();
        } catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        client_gui gui = new client_gui();
        gui.setVisible(true);
        System.out.println("DONE WITH THE EXECUTION!");

    }
}
