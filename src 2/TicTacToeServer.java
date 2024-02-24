// Shaolong Xu 1067946

import java.rmi.AlreadyBoundException;
import java.util.*;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;


import java.util.concurrent.atomic.AtomicInteger;

public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {

    private final Queue<Player> waitingPlayers = new LinkedList<>();
    private final Map<Player, Timer> disconnectPlayers = new HashMap<>();
    private final Map<UUID, GameSession> activeGames = new HashMap<>();
    private final Map<String, Player> allPlayers = new HashMap<>();

    private Timer heartbeatTimer;


    public TicTacToeServer() throws RemoteException {
        // Initialization logic is already in the member declarations
    }

    @Override
    public synchronized void registerPlayer(String username, ClientInterface client) throws RemoteException{
        Player player = allPlayers.get(username);

        // If the player is not already registered, create a new player and add to allPlayers map
        if(player == null){
            player = new Player(username, client);
            allPlayers.put(username, player);
        }else {
            player.setClient(client);
        }
        updatePlayerRanks();
        if(!waitingPlayers.isEmpty()){
            Player opponent = waitingPlayers.poll();

            // Log the opponent information for debugging purposes
            System.out.println(opponent.toString() + " is polled.");

            // Check if the opponent is online by sending a heartbeat
            try{
                opponent.getClient().heartbeat();
            }catch(RemoteException e){
                waitingPlayers.add(player);
                return;
            }

            // Create a new game session and assign symbols 'X' and 'O' to the players
            GameSession newGame;
            if (Math.random() < 0.5){
                player.setSymbol('X');
                opponent.setSymbol('O');
                newGame = new GameSession(player, opponent);
            }else{
                player.setSymbol('O');
                opponent.setSymbol('X');
                newGame = new GameSession(opponent, player);
            }

            // Add the new game session to the active games map
            activeGames.put(newGame.getGameId(), newGame);

            // Notify both players about the start of the game and whose turn it is
            String startingMessage = "Welcome to Tic-Tac-Toe!" + newGame.getCurrentPlayer().toString() +
                    ", you are playing as '" + newGame.getCurrentPlayer().getSymbol() + "', It's "
                    + newGame.getCurrentPlayer().toString() + "'s turn.";
            newGame.getCurrentPlayer().getClient().switchTurn();
            player.getClient().notifyStatus(startingMessage);
            opponent.getClient().notifyStatus(startingMessage);

            // Set game IDs for both players
            player.setGameId(newGame.getGameId());
            opponent.setGameId(newGame.getGameId());
        }else{
            waitingPlayers.add(player);
            player.getClient().notifyStatus("Finding Players");
        }
    }

    @Override
    public synchronized void makeMove(UUID gameId, String username, int row, int col) throws RemoteException {
        GameSession currentGame = activeGames.get(gameId);
        Player currentPlayer = currentGame.getCurrentPlayer();

        // Check connectivity for both players and handle disconnections
        try {
            currentGame.getPlayer1().getClient().heartbeat();
            currentGame.getPlayer2().getClient().heartbeat();
        } catch (RemoteException e) {
            handleClientDisconnect(gameId, currentPlayer.getUsername());
            return;
        }

        // Attempt the move and handle the outcome
        boolean moveSuccessful = currentGame.makeMove(row, col, currentPlayer.getSymbol());
        if (moveSuccessful) {
            // Notify players of board update
            char[][] board = currentGame.getBoard();
            currentGame.getPlayer1().getClient().updateGameBoard(board);
            currentGame.getPlayer2().getClient().updateGameBoard(board);

            // Handle game outcome
            if (currentGame.isWinningMove(row, col)) {
                String winMessage = String.format("%s wins!", currentGame.getOpponentByUsername(currentGame.getCurrentPlayer().getUsername()).getUsername());
                currentGame.getOpponentByUsername(currentGame.getCurrentPlayer().getUsername()).win();
                currentGame.getCurrentPlayer().lost();


                currentGame.getPlayer1().getClient().notifyGameOutcome(winMessage);
                currentGame.getPlayer2().getClient().notifyGameOutcome(winMessage);

                // Log player points
                System.out.printf("%s's points: %d%n", currentGame.getPlayer1().getUsername(), currentGame.getPlayer1().getPoints());
                System.out.printf("%s's points: %d%n", currentGame.getPlayer2().getUsername(), currentGame.getPlayer2().getPoints());

                // Clear game from players
                currentGame.getPlayer1().setGameId(null);
                currentGame.getPlayer2().setGameId(null);

                activeGames.remove(currentGame.getGameId());
            } else if (currentGame.isDraw()) {
                // Handle draw condition
                String drawMessage = "The game is a draw!";
                currentGame.getPlayer1().getClient().notifyGameOutcome(drawMessage);
                currentGame.getPlayer2().getClient().notifyGameOutcome(drawMessage);

                // Clear game from players
                currentGame.getPlayer1().setGameId(null);
                currentGame.getPlayer2().setGameId(null);

                activeGames.remove(currentGame.getGameId());
            } else {
                // Notify players of turn
                String turnMessage = String.format("It's %s's turn (%c).", currentGame.getCurrentPlayer().toString(), currentGame.getCurrentPlayer().getSymbol());
                currentGame.getPlayer1().getClient().switchTurn();
                currentGame.getPlayer2().getClient().switchTurn();
                currentGame.getPlayer1().getClient().notifyStatus(turnMessage);
                currentGame.getPlayer2().getClient().notifyStatus(turnMessage);
            }
        }
    }

    public synchronized void sendChatMessage(UUID gameId, String username, String message) throws RemoteException {
        GameSession game = activeGames.get(gameId);

        // Ensure players are connected before proceeding
        if (!isPlayerConnected(game, game.getPlayer1()) || !isPlayerConnected(game, game.getPlayer2())) {
            return;
        }

        // Format and send the chat message
        String formattedMessage = String.format("%s: %s", game.getPlayerByUsername(username), message);
        game.addChatMessage(formattedMessage);
        broadcastChatMessages(game);
    }

    /**
     * Checks if the player's client is connected and handles disconnection if not.
     *
     * @param game   The current game session.
     * @param player The player to check connectivity for.
     * @return true if the player is connected, false otherwise.
     */
    private boolean isPlayerConnected(GameSession game, Player player) throws RemoteException {
        try {
            player.getClient().heartbeat();
            return true;
        } catch (RemoteException e) {
            handleClientDisconnect(game.getGameId(), player.getUsername());
            return false;
        }
    }

    /**
     * Broadcasts the chat history to all players in the game session.
     *
     * @param game The current game session.
     * @throws RemoteException If an error occurs during remote method invocation.
     */
    private void broadcastChatMessages(GameSession game) throws RemoteException {
        String chatHistory = String.join("\n", game.getChatMessages());
        game.getPlayer1().getClient().receiveChatMessage(chatHistory);
        game.getPlayer2().getClient().receiveChatMessage(chatHistory);
    }

    @Override
    public synchronized void quitGame(UUID gameId, String username) throws RemoteException {
        // Validate gameId and handle null case
        if (gameId == null) {
            waitingPlayers.poll();
            return;
        }

        // Retrieve game session and opponent
        GameSession game = activeGames.get(gameId);
        Player opponent = game.getOpponentByUsername(username);

        // Validate opponent existence
        if (opponent == null) {
            System.err.println("Error: Opponent not found for provided username: " + username);
            System.exit(0);
            return;
        }

        // Ensure opponent's client is connected
        try {
            opponent.getClient().heartbeat();
        } catch (RemoteException e) {
            handleClientDisconnect(gameId, opponent.getUsername());
            throw new RemoteException("Opponent's client is not reachable. Disconnect handled.", e);
        }

        // Update game status and player ranks
        game.getPlayerByUsername(username).lost();
        opponent.win();
        updatePlayerRanks();

        // Log game outcome and player points
        System.out.printf("%s won! because %s left.%n", opponent.getUsername(), username);
        System.out.printf("%s points are %d%n", game.getPlayer1(), game.getPlayer1().getPoints());
        System.out.printf("%s points are %d%n", game.getPlayer2(), game.getPlayer2().getPoints());

        // Notify opponent and clear game data safely
        try {
            opponent.getClient().notifyGameOutcome("Opponent has left the game. You win!");
        } catch (RemoteException e) {
            System.err.println("Error: Unable to notify opponent about game outcome. " + e.getMessage());
        }

        // Safely clear game data
        try {
            game.getPlayer1().setGameId(null);
        } catch (Exception e) {
            System.err.println("Error: Unable to clear gameId for player: " + game.getPlayer1().getUsername() + ". " + e.getMessage());
        }

        try {
            game.getPlayer2().setGameId(null);
        } catch (Exception e) {
            System.err.println("Error: Unable to clear gameId for player: " + game.getPlayer2().getUsername() + ". " + e.getMessage());
        }

        activeGames.remove(game.getGameId());
    }

    public void updatePlayerRanks() {
        // Validate that there are players to rank
        if (allPlayers == null || allPlayers.isEmpty()) {
            throw new IllegalStateException("No players available to update ranks.");
        }

        // Sort players and update ranks efficiently using streams
        AtomicInteger rank = new AtomicInteger(1);
        allPlayers.values().stream()
                .sorted(Comparator.comparingInt(Player::getPoints).reversed())
                .forEach(player -> player.setRank(rank.getAndIncrement()));
    }

    public synchronized void handleClientReconnect(String username, ClientInterface client) {
        Player reconnectPlayer = allPlayers.get(username);
        Timer timer = disconnectPlayers.remove(reconnectPlayer);

        if (timer != null) {
            timer.cancel();
            reconnectPlayer.setClient(client);
            GameSession game = activeGames.get(reconnectPlayer.getGameId());

            if (game != null) {
                try {
                    reconnectPlayer.getClient().updateGameBoard(game.getBoard());
                    reconnectPlayer.getClient().setGameId(game.getGameId());
                    game.setPlayer(reconnectPlayer);

                    // Construct chat history
                    String chatHistory = String.join("\n", game.getChatMessages());

                    // Update chat for all players
                    game.getPlayer1().getClient().receiveChatMessage(chatHistory);
                    game.getPlayer2().getClient().receiveChatMessage(chatHistory);

                    // Notify about the current turn
                    game.getCurrentPlayer().getClient().setTurn();
                    reconnectPlayer.getClient().notifyStatus(
                            String.format("It's %s's turn. (%c)",
                                    game.getCurrentPlayer().toString(),
                                    game.getCurrentPlayer().getSymbol())
                    );
                } catch (RemoteException e) {
                    System.err.println("Failed to update the client of " + username + ": " + e.getMessage());
                }
            }
        } else {
            try {
                registerPlayer(username, client);
            } catch (RemoteException e) {
                System.err.println("Failed to register player " + username + ": " + e.getMessage());
            }
        }
    }

    @Override
    public synchronized char[][] getBoard(UUID gameId) {
        // Validate gameId and handle potential null case
        if (gameId == null) {
            throw new IllegalArgumentException("Game ID cannot be null.");
        }

        // Retrieve and return the game board, handling potential null game session
        GameSession game = activeGames.get(gameId);
        if (game == null) {
            throw new IllegalStateException("No active game found for the provided Game ID: " + gameId);
        }
        return game.getBoard();
    }

    public void endGameAsDraw(GameSession game) {
        String message = "Match Drawn";
        Timer p1 = null;
        Timer p2 = null;

        // Notify Player 1 and handle potential issues
        try {
            p1 = disconnectPlayers.remove(game.getPlayer1());
            if (p1 != null) {
                p1.cancel();
            }
            game.getPlayer1().getClient().notifyGameOutcome(message);
        } catch (RemoteException e) {
            System.err.println("Failed to notify " + game.getPlayer1().getUsername() + " about the game outcome: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error while handling Player 1 end game: " + e.getMessage());
        }

        // Notify Player 2 and handle potential issues
        try {
            p2 = disconnectPlayers.remove(game.getPlayer2());
            if (p2 != null) {
                p2.cancel();
            }
            game.getPlayer2().getClient().notifyGameOutcome(message);
        } catch (RemoteException e) {
            System.err.println("Failed to notify " + game.getPlayer2().getUsername() + " about the game outcome: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error while handling Player 2 end game: " + e.getMessage());
        }

        // Update game status and points
        game.getPlayer1().draw();
        game.getPlayer2().draw();
        System.out.println(game.getPlayer1().toString() + " points are " + game.getPlayer1().getPoints());
        System.out.println(game.getPlayer2().toString() + " points are " + game.getPlayer2().getPoints());
        updatePlayerRanks();

        // Clean up game data safely
        try {
            game.getPlayer1().setGameId(null);
            game.getPlayer2().setGameId(null);
            activeGames.remove(game.getGameId());
        } catch (Exception e) {
            System.err.println("Error during game cleanup: " + e.getMessage());
        }
    }


    public void handleClientDisconnect(UUID gameId, String username) throws RemoteException {
        // Early exit if gameId is null
        if (gameId == null) {
            waitingPlayers.poll();
            return;
        }

        GameSession game = activeGames.get(gameId);
        Player disconnectedPlayer = game.getPlayerByUsername(username);
        System.out.println(disconnectedPlayer.getUsername() + " disconnected!");

        // Check if the opponent is already in the disconnectPlayers map
        boolean opponentIsDisconnecting = disconnectPlayers.containsKey(game.getOpponentByUsername(username));
        if (opponentIsDisconnecting) {
            endGameAsDraw(game);
            return;
        }

        // Handle the disconnecting player
        if (!disconnectPlayers.containsKey(disconnectedPlayer)) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    endGameAsDraw(game);
                }
            }, 30000); // 30 seconds

            disconnectPlayers.put(disconnectedPlayer, timer);

            // Attempt to freeze the opponent's client and log if unsuccessful
            try {
                game.getOpponentByUsername(username).getClient().freeze();
            } catch (RemoteException e) {
                System.err.println("Failed to freeze the client of "
                        + game.getOpponentByUsername(username).getUsername()
                        + ": " + e.getMessage());
            }
        }
    }

    private boolean checkClient(Player player){
        try{
            player.getClient().heartbeat();
            return true;
        }catch(RemoteException e){
            System.err.println("Heartbeat failed for player: " + player.getUsername() + ". Error: " + e.getMessage());
            return false;
        }
    }

    public void heartbeat(){
        // Implementation of the heartbeat method (if needed)
    }

    public void startHeartbeat(){
        heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (GameSession game : activeGames.values()){
                    // Check and handle Player 1
                    if(!checkClient(game.getPlayer1()) && disconnectPlayers.containsKey(game.getPlayer1())){
                        try {
                            handleClientDisconnect(game.getGameId(), game.getPlayer1().getUsername());
                        } catch (RemoteException e) {
                            System.err.println("Failed to handle disconnection for: " + game.getPlayer1().getUsername() + ". Error: " + e.getMessage());
                        }
                    }
                    // Check and handle Player 2
                    if(!checkClient(game.getPlayer2()) && disconnectPlayers.containsKey(game.getPlayer2())){
                        try {
                            handleClientDisconnect(game.getGameId(), game.getPlayer2().getUsername());
                        } catch (RemoteException e) {
                            System.err.println("Failed to handle disconnection for: " + game.getPlayer2().getUsername() + ". Error: " + e.getMessage());
                        }
                    }
                }
            }
        }, 0, 3000); //send heartbeat every 3 sec
    }

    public static void main(String[] args) {
        // Validate command-line arguments
        if (args.length < 2) {
            System.out.println("Incorrect usage. Please provide the IP address and port number for the server.");
            System.out.println("Usage: java YourServerMainClass <ip> <port>");
            return;
        }

        String ip = args[0];
        int port;

        // Validate and parse the port number
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Please ensure the port is a valid integer.");
            return;
        }

        try {
            // Set the RMI server's hostname
            System.setProperty("java.rmi.server.hostname", ip);

            // Create the registry on the given port
            Registry registry = LocateRegistry.createRegistry(port);

            // Create an instance of the TicTacToeServer
            ServerInterface server = new TicTacToeServer();

            // Bind the remote object to a name in the RMI registry
            registry.bind("GameServer", server);

            System.out.println("TicTacToe RMI Server is running on " + ip + ":" + port);
        } catch (RemoteException e) {
            System.err.println("Failed to initialize the RMI server: " + e.getMessage());
        } catch (AlreadyBoundException e) {
            System.err.println("The server is already bound: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }
}