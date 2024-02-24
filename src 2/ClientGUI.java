// Shaolong Xu 1067946

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;

import java.rmi.RemoteException;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class ClientGUI {
    private JFrame frame;
    private JButton [][] boardButtons = new JButton[3][3];
    private JTextArea chatArea;
    private JTextField chatInput;
    private JLabel statusLabel;
    private JLabel timerlabel;
    private Timer moveTimer;
    private final int time = 20;
    private int timeLeft;

    public ClientGUI(String username, Client client){
        this.timeLeft = time;
        frame = new JFrame(username + " - Tic Tac Toe");
        frame.setSize(600, 400); //

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.handleClientDisconnect();
                System.exit(0);
            }
        });

        frame.setLayout(new BorderLayout(20, 20));
        frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(15,15, 15, 15));

        JPanel boardPanel = new JPanel(new GridLayout(3,3, 3, 3));
        for (int i =0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                JButton button = new JButton("");
                final int x = i, y = j;

                button.addActionListener(e -> {
                    if (client.isTurn()) {
                        client.handleBoardClick(x, y);
                    } else {
                        JOptionPane.showMessageDialog(frame, "It's not your turn yet!", "Wait", JOptionPane.WARNING_MESSAGE);
                    }
                });
                boardButtons[i][j] = button;
                boardPanel.add(button);
            }
        }
        boardPanel.setBorder(BorderFactory.createTitledBorder("Game Board"));
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        timerlabel = new JLabel("Time left: " + timeLeft);
        timerlabel.setFont(new Font("Sans Serif", Font.BOLD, 16));
        timerlabel.setForeground(Color.GREEN);
        timerlabel.setHorizontalAlignment(JLabel.CENTER);

        moveTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeLeft--;
                timerlabel.setText("Time left: " + timeLeft);
                if (timeLeft <= 0){
                    resetTimer();
                    if(client.isTurn()){
                        client.randomMove();
                    }
                }
            }
        });

        JButton quitButton = new JButton("Quit");
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.handleQuit();
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });


        leftPanel.add(timerlabel, BorderLayout.WEST);
        leftPanel.add(quitButton, BorderLayout.SOUTH);

        chatArea = new JTextArea(10, 20);
//        chatArea.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatArea.setEditable(false);

        chatInput = new JTextField();
        chatInput.setBorder(BorderFactory.createTitledBorder("Type your message"));
        ((AbstractDocument) chatInput.getDocument()).setDocumentFilter(new LengthFilter(20));
        chatInput.addActionListener(e -> {
            client.sendChatMessage(chatInput.getText());
            chatInput.setText("");
        });

        statusLabel = new JLabel("Finding Player...");
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));

        frame.add(statusLabel, BorderLayout.NORTH);
        frame.add(leftPanel, BorderLayout.WEST);
        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(new JScrollPane(chatArea), BorderLayout.EAST);
        frame.add(chatInput, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    class LengthFilter extends DocumentFilter {
        private int max;

        public LengthFilter(int max) {
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (this.max < 0 || (fb.getDocument().getLength() + string.length()) <= this.max) {
                super.insertString(fb, offset, string, attr);
            } else {
                Toolkit.getDefaultToolkit().beep(); // signals the user with a beep
                JOptionPane.showMessageDialog(frame, "Maximum character limit is " + max + "!", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (this.max < 0 || (fb.getDocument().getLength() + text.length() - length) <= this.max) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(frame, "Maximum character limit is " + max + "!", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public void startTimer() {
        timeLeft = time;
        timerlabel.setText("Time left: " + timeLeft);
        moveTimer.start();
    }

    public JFrame getFrame(){
        return frame;
    }

    public JLabel getStatusLabel(){
        return statusLabel;
    }

    public void refresh(){
        for (int i = 0; i < 3; i++){
            for (int j = 0; j < 3; j++){
                boardButtons[i][j].setText(""); //text:
            }
        }

        chatArea.setText (""); // t:
        resetTimer();
    }

    public void resetTimer(){
        moveTimer.stop();
        timeLeft = time;
        timerlabel.setText("Time left: " + timeLeft);
    }

    public JTextArea getChatArea(){
        return chatArea;
    }

    public JButton [][] getBoardButtons(){
        return boardButtons;
    }

}
