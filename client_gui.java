/**
 * Author: Aditya Pulekar
 * Author: Vishal Garg
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class client_gui implements ActionListener {  // extends JFrame implements ActionListener
    JButton JB_C; JButton JB_S; JButton JB_E; JButton JB_Reset;
    JTextField textF_Search;
    //JTextField textF;
    JTextArea textA;
    JTextArea textA_suggestions;
    JFrame frame;
    JPanel panel_searchField;
    //JPanel panel_searchResult;
    //JPanel panel_searchSuggestions;


    public client_gui(){
        JB_C = new JButton("Contribute");
        JB_S = new JButton("Search");
        JB_E = new JButton("Edit");
        JB_Reset = new JButton("Reset");
        frame = new JFrame();
        panel_searchField = new JPanel();
        /*panel_searchResult = new JPanel();
        panel_searchSuggestions = new JPanel();*/

        panel_searchField.setBounds(5,53,90,30);
        textF_Search = new JTextField();
        JLabel tfield_label = new JLabel("SEARCH :");
        panel_searchField.add(tfield_label);
        frame.add(panel_searchField);

        textA_suggestions = new JTextArea();
        textA_suggestions.setLineWrap(true);
        JLabel ta_sug_label = new JLabel("DID YOU MEAN...");
        ta_sug_label.setAlignmentX(560);
        ta_sug_label.setAlignmentY(80);
        //panel_searchSuggestions.add(ta_sug_label);
        JScrollPane textA_suggestions_scroll = new JScrollPane(textA_suggestions);



        textA = new JTextArea();
        textA.setLineWrap(true);
        JLabel ta_label = new JLabel("SEARCH RESULT");
        ta_label.setAlignmentX(25);
        ta_label.setAlignmentY(200);
        //panel_searchResult.add(ta_label);
        JScrollPane textA_scroll = new JScrollPane(textA);

        JB_C.setBounds(110,470,100,40);
        JB_S.setBounds(110,570,100,40);
        JB_E.setBounds(230,470,100,40);
        JB_Reset.setBounds(230,570,100,40);
        textF_Search.setBounds(100,50,350,40);
        //textA.setBounds(100,100,350,150);
        //textA_suggestions.setBounds(560,60,200,150);
        textA_scroll.setBounds(50,100,500,350);
        textA_suggestions_scroll.setBounds(560,100,200,200);

        JB_Reset.addActionListener(this);
        JB_S.addActionListener(this);
        JB_E.addActionListener(this);
        JB_C.addActionListener(this);

        frame.add(textA_suggestions_scroll);
        frame.add(textA_scroll);
        frame.add(JB_C);frame.add(JB_E);frame.add(JB_S);
        frame.add(JB_Reset);frame.add(textF_Search);//frame.add(textA_suggestions);frame.add(textA);
        frame.add(ta_label); frame.add(ta_sug_label);
        frame.add(panel_searchField);//frame.add(panel_searchSuggestions);
        //frame.add(panel_searchResult);
        //frame.add(panel_searchSuggestions);

        frame.setSize(800,700);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent ae){
        String serverToConnect = "yes.cs.rit.edu"; //Default server to connect
        int portToConnect = 1234;				   //Default port to connect
        Socket communicaterWithServer;
        ObjectOutputStream out;
        ObjectInputStream in;
        boolean fileFound = false;
        String request, response;
        if (ae.getSource() == JB_Reset){ //For Reset
            textA.setText("");
            textF_Search.setText("");
            textA.setEditable(true);
            //validate();
        }else{
            try {
                System.out.println("\nConnecting to: "+serverToConnect+" : "+portToConnect);
                communicaterWithServer = new Socket(serverToConnect, portToConnect);
                System.out.println("Connected with the Main Server!!!");
                out = new ObjectOutputStream(communicaterWithServer.getOutputStream());
                in = new ObjectInputStream(communicaterWithServer.getInputStream());
                String fileName = textF_Search.getText();
                //Get what is the request
                if (ae.getSource() == JB_C) {
                    request = "Contribution";
                    String newContribution = textA.getText();
                    out.writeObject(request);
                    out.writeObject(fileName);
                    out.writeObject(newContribution);
                    //response = (String) in.readObject();
                }else{
                    request = "Search";
                    out.writeObject(request);
                    out.writeObject(fileName);

                    response = (String)in.readObject();

                    //Now here the client gets to know which host (having the particular Manager) to connect to.
                    while(response.equalsIgnoreCase("connect")){
                        serverToConnect = (String)in.readObject();
                        portToConnect = (Integer)in.readObject();
                        System.out.println("Re-Routing to: "+serverToConnect+" : "+portToConnect);
                        //Make the connection with the new host.
                        communicaterWithServer = new Socket(serverToConnect, portToConnect);
                        out = new ObjectOutputStream(communicaterWithServer.getOutputStream());
                        in = new ObjectInputStream(communicaterWithServer.getInputStream());
                        out.writeObject(request);
                        out.writeObject(fileName);
                        response = (String)in.readObject();
                    }

                    textA.setText(response);
                    //textF.setText(response);

                }

                //Set editable for edit
                if(ae.getSource() == JB_E){
                    textA.setEditable(true);
                } else if(ae.getSource() == JB_S){
                    textA.setEditable(false);
                }

            } catch (IOException | ClassNotFoundException e){
                //e.printStackTrace();
            }
        }

    }

    public static void main(String[] args){
        client_gui gui = new client_gui();
        gui.frame.setVisible(true);
        System.out.println("DONE WITH THE EXECUTION!");

    }
}

