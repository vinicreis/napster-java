package server;

import log.ConsoleLog;
import log.Log;
import service.NapsterImpl;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class ServerImpl implements Server {
    private static final String TAG = "server.ServerImpl";
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

    public static void main(String[] args) {
        try (Server server = new ServerImpl(Arrays.asList(args).contains("--d"))) {
            server.start();

            System.out.println("\nPressione qualquer tecla para encerrar...");

            //noinspection ResultOfMethodCallIgnored
            System.in.read();
        } catch (Exception e) {
            System.out.println("Falha ao iniciar servidor!");
        }

        System.exit(0);
    }

    @Override
    public void start() {
        try {
            log.d("Binding service...");
            registry.bind(NAPSTER_ADDRESS, new NapsterImpl(debug));

            System.out.println("Servidor iniciado!");
        } catch (Exception e) {
            System.out.println("Falha ao iniciar o servidor!");
            log.e("Failed to start server", e);
        }
    }

    @Override
    public void close() {
        try {
            System.out.println("Finalizando...");
            registry.unbind(NAPSTER_ADDRESS);
            System.out.println("Serviço finalizado!");
        } catch (Exception e) {
            log.e("Failed to stop server gracefully!", e);
        }
    }
}
