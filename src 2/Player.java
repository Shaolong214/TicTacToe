// Shaolong Xu 1067946

import java.rmi.RemoteException;
import java.util.UUID;

public class Player {

    private final String username;
    private char symbol;
    private ClientInterface client;
    private int points;
    private int rank;
    private UUID gameId;

    public Player(String username, ClientInterface client) {
        this.username = username;
        this.client = client;
        this.points = 0;
    }

    // Getters
    public String getUsername() { return username; }
    public char getSymbol() { return symbol; }
    public ClientInterface getClient() { return client; }
    public int getPoints() { return points; }
    public UUID getGameId() { return gameId; }

    // Setters
    public void setClient(ClientInterface client) { this.client = client; }
    public void setSymbol(char symbol) { this.symbol = symbol; }
    public void setRank(int rank) { this.rank = rank; }
    public void setGameId(UUID gameId) {
        this.gameId = gameId;
        try {
            this.client.setGameId(gameId);
        } catch (RemoteException e) {
            // Consider logging the exception or handling it as per your use case
        }
    }

    // Game Outcome Handlers
    public void win() { points += 5; }
    public void lost() {
        points -= 5;
        if (points < 0) { points = 0; }
    }
    public void draw() { points += 2; }

    @Override
    public String toString() {
        return String.format("Rank #%d %s", rank, username);
    }
}
