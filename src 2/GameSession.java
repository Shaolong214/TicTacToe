// Shaolong Xu 1067946

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private  Player player1, player2, currentPlayer;
    private final char[][] board = new char[3][3];
    private final UUID gameId;
    private final List<String> chatMessages = new ArrayList<>();

    public GameSession(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1; // Player1 starts by default
        this.gameId = UUID.randomUUID();
    }

    public boolean isDraw() {
        for (char[] row : board) {
            for (char cell : row) {
                if (cell == '\0') return false;
            }
        }
        return true;
    }

    public void addChatMessage(String message) {
        if (chatMessages.size() >= 10) chatMessages.remove(0);
        chatMessages.add(message);
    }
    public boolean isWinningMove(int row, int col) {
        return (board[row][0] == board[row][1] && board[row][1] == board[row][2]) ||
                (board[0][col] == board[1][col] && board[1][col] == board[2][col]) ||
                (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0] != '\0') ||
                (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2] != '\0');
    }

    public synchronized boolean makeMove(int row, int col, char symbol) {
        if (board[row][col] == '\0') {
            board[row][col] = symbol;
            currentPlayer = (currentPlayer == player1) ? player2 : player1; // Switch turn
            return true;
        }
        return false;
    }

    // Getters and Setters
    public List<String> getChatMessages() { return chatMessages; }

    public Player getPlayerByUsername(String username) {
        return player1.getUsername().equals(username) ? player1 : player2;
    }

    public Player getOpponentByUsername(String username) {
        return player1.getUsername().equals(username) ? player2 : player1;
    }
    public UUID getGameId() { return gameId; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public char[][] getBoard() { return board; }
    public Player getCurrentPlayer() { return currentPlayer; }

    public void setPlayer(Player player) {
        if (player1.getUsername().equals(player.getUsername())) {
            this.player1 = player;
        } else if (player2.getUsername().equals(player.getUsername())) {
            this.player2 = player;
        }
        if (currentPlayer.getUsername().equals(player.getUsername())) {
            currentPlayer = player;
        }
    }
}
