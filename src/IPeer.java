import java.rmi.RemoteException;

public interface IPeer {
    void join() throws RuntimeException, RemoteException;
    void update() throws RemoteException;
    void search() throws RemoteException;
    void download();
}
