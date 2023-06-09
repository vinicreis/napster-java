import java.rmi.RemoteException;
import java.util.Arrays;

public interface Peer extends Server {
    void join() throws RuntimeException, RemoteException;
    void update() throws RemoteException;
    void search() throws RemoteException;
    void download();

    static void main(String[] args) {
        try {
            assert args.length >= 1 : "IP and port argument are mandatory";

            final String ip = args[0];
            final int port = Integer.parseInt(args[1]);
            final boolean debug = Arrays.asList(args).contains("--d");

            try (Peer peer = new PeerImpl(ip, port, debug)) {
                Runtime.getRuntime().addShutdownHook(new PeerImpl.ShutdownHook(peer));

                peer.start();
                peer.join();

                boolean running = true;

                while(running) {
                    final PeerImpl.Operation operation = PeerImpl.Operation.read();

                    switch (operation) {
                        case UPDATE: peer.update(); break;
                        case SEARCH: peer.search(); break;
                        case DOWNLOAD: peer.download(); break;
                        case EXIT:
                            running = false;
                            break;
                    }
                }
            }

            System.out.println("Peer finished!");
        } catch (NumberFormatException e) {
            System.out.printf("Invalid port: %s\n", args[1]);
        } catch (Exception e) {
            System.out.printf("Failed to initialize peer: %s\n", e.getMessage());
        }
    }
}
