// Shaolong Xu 1067946

import javax.swing.*;
import java.awt.Point;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;


public class Client extends UnicastRemoteObject implements ClientInterface{
    private boolean turn;
    private Timer heartbeatTimer;
    private UUID gameId;
    private String username;
    private static ServerInterface server;
    private ClientGUI gui;

    // 1. Initialization
    public Client(String username) throws Exception {
        // Initialization of client properties
        this.username = username;
        this.turn = false;
        this.gameId = null;
        this.gui = new ClientGUI(username, this);

        // Handling client reconnection and initiating heartbeat
        server.handleClientReconnect(username, this);
        startHeartbeat();
    }

    // 2. Gameplay Actions
    public synchronized void handleBoardClick(int i, int j) {
        try {
            server.makeMove(gameId, username, i, j);
        } catch (RemoteException e) {
            handleServerCrash();
        }
    }

    public synchronized void sendChatMessage(String message) {
        if (gameId != null) {
            try {
                // Attempting to send the chat message to the server
                server.sendChatMessage(gameId, username, message);
            } catch (RemoteException e) {
                handleServerCrash();
            }
        }
    }

    public void randomMove() {
        char[][] board;
        try {
            board = server.getBoard(gameId);
        } catch (RemoteException e) {
            handleServerCrash();
            return;
        }

        List<Point> availableMoves = new ArrayList<>();

        // Identify available moves
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == '\0') {
                    availableMoves.add(new Point(i, j));
                }
            }
        }

        // Make a random move
        if (!availableMoves.isEmpty()) {
            int randomIndex = new Random().nextInt(availableMoves.size());
            Point randomMove = availableMoves.get(randomIndex);
            handleBoardClick(randomMove.x, randomMove.y);
        }
    }

    // 3. Message Handling
    @Override
    public void updateGameBoard(char[][] board) throws RemoteException {
        // Updating the GUI to reflect the current state of the game board
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.gui.getBoardButtons()[i][j].setText(Character.toString(board[i][j]));
            }
        }
    }

    @Override
    public void notifyGameOutcome(String message) throws RemoteException {
        // Ensuring GUI updates are performed on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            // Re-enabling the GUI, displaying the game outcome message, resetting the timer, and handling the outcome
            this.gui.getFrame().setEnabled(true);
            this.gui.getStatusLabel().setText(message);
            this.gui.resetTimer();
            handleGameOutcome(message);
        });
    }

    public void notifyStatus(String message) throws RemoteException {
        // Updating the status label on the GUI with the provided message
        this.gui.getStatusLabel().setText(message);
    }

    @Override
    public void receiveChatMessage(String message) throws RemoteException {
        // Updating the chat area and ensuring the GUI frame is active
        gui.getChatArea().setText("");
        gui.getChatArea().append(message + "\n");
        gui.getFrame().setEnabled(true);
    }

    // 4. Cleanup and Error Handling
    public void handleGameOutcome(String message) {
        String promptMessage = String.format("Result: %s%nWould you like to start a new game?", message);

        int option = JOptionPane.showConfirmDialog(
                this.gui.getFrame(),
                promptMessage,
                "Game Finished",
                JOptionPane.YES_NO_OPTION
        );

        if (option == JOptionPane.YES_OPTION) {
            try {
                this.gui.refresh();
                this.turn = false;
                server.registerPlayer(username, this);
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(
                        this.gui.getFrame(),
                        "Oops! There was an issue starting a new game. Please try again later.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        } else {
            JOptionPane.showMessageDialog(
                    this.gui.getFrame(),
                    "Thank you for playing! Hope to see you again soon.",
                    "Goodbye",
                    JOptionPane.INFORMATION_MESSAGE
            );
            System.exit(0);
        }
    }

    public synchronized void handleQuit() throws RemoteException {
        int confirm = JOptionPane.showConfirmDialog(
                this.gui.getFrame(),
                "Are you sure you'd like to exit the game? All progress might be lost.",
                "Exit Confirmation",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                server.quitGame(gameId, username);
                JOptionPane.showMessageDialog(
                        this.gui.getFrame(),
                        "Thank you for playing! Hope to see you again soon.",
                        "Goodbye",
                        JOptionPane.INFORMATION_MESSAGE
                );
                System.exit(0);
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(
                        this.gui.getFrame(),
                        "Oops! There was an issue quitting the game. Please try again.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void handleServerCrash() {
        gui.resetTimer();
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }

        // Inform the user about the server issue and next steps
        JOptionPane.showMessageDialog(gui.getFrame(),
                "Uh-oh! The server is currently unavailable. The application will close in 3 seconds.",
                "Connection Issue",
                JOptionPane.ERROR_MESSAGE);

        // Disable the GUI frame and wait for 3 seconds before exiting
        gui.getFrame().setEnabled(false);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.exit(0);
    }

    // 5. Game State Management
    public void switchTurn() {
        turn = !turn;
        gui.startTimer();
    }

    public void setTurn() {
        turn = true;
        gui.startTimer();
    }

    public void setGameId(UUID gameId) {
        // Setting the game ID and starting the timer
        this.gameId = gameId;
        gui.startTimer();
    }

    public boolean isTurn() {
        return turn;
    }

    // 6. Connection Management
    public void startHeartbeat() {
        heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    server.heartbeat();
                } catch (Exception e) {
                    handleServerCrash();
                }
            }
        }, 0, 3000); // Send heartbeat every 3 seconds
    }

    public void freeze() {
        SwingUtilities.invokeLater(() -> {
            // Reset the timer and disable the GUI frame
            gui.resetTimer();
            gui.getFrame().setEnabled(false);
            JOptionPane.showMessageDialog(gui.getFrame(),
                    "It looks like your opponent has disconnected.\n\n" +
                            "No worries! We're pausing the game for 30 seconds to give them a chance to return.\n" +
                            "If they're back within this window, your game will resume seamlessly.\n",
                    "A Quick Pause!",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public void heartbeat() {}

    // 7. Getters for Remote Interface
    @Override
    public UUID getGameId() throws RemoteException {
        return gameId;
    }

    @Override
    public String getUsername() throws RemoteException {
        return username;
    }

    // 8. Handling Client Disconnect
    public synchronized void handleClientDisconnect() {
        try {
            // Attempt to notify the server about the client disconnect
            server.handleClientDisconnect(gameId, username);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // 9. Main Method
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java -jar client.jar <username> <server_ip> <server_port>");
            System.exit(1);
        }

        String username = args[0];
        String serverIp = args[1];
        String serverPort = args[2];

        int port;
        try {
            port = Integer.parseInt(serverPort);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid port number. Please provide a valid integer.");
            System.exit(1);
            return;
        }

        String serverAddress = "rmi://" + serverIp + ":" + port + "/GameServer";

        try {
            // Connect to the server and create the client
            server = (ServerInterface) Naming.lookup(serverAddress);
            new Client(username);
        } catch (java.net.MalformedURLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error: Malformed URL detected. Please check the provided IP address and port number.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (java.rmi.UnknownHostException e) {
            JOptionPane.showMessageDialog(null,
                    "Error: Unknown host. Please check the provided IP address.",
                    "Unknown Host Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (java.rmi.ConnectException e) {
            JOptionPane.showMessageDialog(null,
                    "Error: Cannot connect to the server. Please check the provided IP address and port number.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "An unexpected error occurred: " + e.getMessage(),
                    "Unexpected Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
