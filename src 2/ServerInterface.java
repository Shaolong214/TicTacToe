// Shaolong Xu 1067946

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;
public interface ServerInterface extends Remote{
    void handleClientDisconnect(UUID gameId, String username) throws RemoteException;
    void handleClientReconnect(String username, ClientInterface client) throws  RemoteException;
    void sendChatMessage(UUID gameId, String username, String message) throws RemoteException;
    void quitGame(UUID gameId, String username) throws RemoteException;
    void heartbeat() throws RemoteException;
    void registerPlayer(String username, ClientInterface client) throws RemoteException;
    void makeMove(UUID gameId, String username, int row, int col) throws RemoteException;
    char[][] getBoard(UUID gameId) throws RemoteException;

}
