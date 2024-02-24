// Shaolong Xu 1067946

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface ClientInterface extends Remote {
    void notifyStatus(String string) throws RemoteException;;
    void setTurn() throws RemoteException;
    void freeze() throws RemoteException;
    void receiveChatMessage(String message) throws RemoteException;
    void setGameId(UUID gameId) throws RemoteException;
    void switchTurn() throws RemoteException;
    void heartbeat() throws RemoteException;
    void updateGameBoard(char[][] board) throws RemoteException;
    void notifyGameOutcome(String message) throws RemoteException;
    UUID getGameId() throws RemoteException;
    String getUsername() throws RemoteException;
}
