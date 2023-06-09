import log.ConsoleLog;
import log.Log;
import service.NapsterImpl;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerImpl implements Server {
    private static final String TAG = "ServerImpl";
    private static final int REGISTRY_PORT = 1099;
    private static final String NAPSTER_ADDRESS = "rmi://localhost/napster";
    private final Log log = new ConsoleLog(TAG);
    private final Registry registry;
    private final boolean debug;

    public ServerImpl(boolean debug) throws RemoteException {
        this.debug = debug;
        this.log.setDebug(debug);

        log.d("Creating registry...");
        registry = LocateRegistry.createRegistry(REGISTRY_PORT);
    }

    @Override
    public void start() {
        try {
            log.d("Binding service...");
            registry.bind(NAPSTER_ADDRESS, new NapsterImpl(debug));

            System.out.println("Server ready!");
        } catch (Exception e) {
            System.out.printf("Failed to start server: %s", e.getMessage());
            log.e("Failed to start server", e);
        }
    }

    @Override
    public void close() {
        // TODO: Notify peers that server stopped
        try {
            System.out.println("Stopping...");
            registry.unbind(NAPSTER_ADDRESS);
            System.out.println("Service unbound!");
        } catch (Exception e) {
            log.e("Failed to stop server gracefully!", e);
        }
    }
}
