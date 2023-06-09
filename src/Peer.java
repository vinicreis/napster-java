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

                    try {
                        switch (operation) {
                            case UPDATE: peer.update(); break;
                            case SEARCH: peer.search(); break;
                            case DOWNLOAD: peer.download(); break;
                            case EXIT:
                                running = false;
                                break;
                        }
                    } catch (Exception e) {
                        System.out.printf("Falha ao executar operação %s\n", operation.getFormattedName());
                    }
                }
            }

            System.out.println("Peer encerrado!");
        } catch (Exception e) {
            System.out.println("Falha ao iniciar peer!" );
        }
    }
}
