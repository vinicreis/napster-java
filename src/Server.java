import service.Napster;
import util.Log;
import util.ConsoleLog;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class Server {
    private static final int REGISTRY_PORT = 1099;
    private static final String NAPSTER_ADDRESS = "rmi://localhost/napster";
    private final Log log = new ConsoleLog("Server");
    private final Registry registry;
    private boolean debug = false;

    private Server(boolean debug) throws RemoteException {
        this.debug = debug;
        this.log.setDebug(debug);

        log.d("Creating registry...");
        registry = LocateRegistry.createRegistry(REGISTRY_PORT);
    }

    public static void main(String[] args) {
        try {
            Server server = new Server(Arrays.asList(args).contains("--d"));

            server.start();

            System.out.println("\nPress any key to stop...");

            System.in.read();

            server.stop();
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Failed to start server");
        }
    }

    public void start() {
        try {
            log.d("Binding service...");
            registry.bind(NAPSTER_ADDRESS, new Napster(debug));

            System.out.println("Server ready!");
        } catch (Exception e) {
            System.out.printf("Failed to start server: %s", e.getMessage());
            log.e("Failed to start server", e);
        }
    }

    public void stop() throws NotBoundException, RemoteException {
        // TODO: Notify peers that server stopped
        System.out.println("Stopping...");
        registry.unbind(NAPSTER_ADDRESS);
        System.out.println("Service unbound!");
    }
}
