package tcp;

import service.Napster;
import util.ILog;
import util.Log;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    private static final ILog log = new Log("Server");
    private static final Napster napster;

    static {
        try {
            napster = new Napster();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("rmi://127.0.0.1/napster", napster);

            log.i("Server ready!");
        } catch (Exception e) {
            log.e("Failed to bind service", e);
        }
    }
}
