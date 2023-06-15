import java.util.Arrays;

public interface Peer extends Server {
    void join();
    void update(String filename);
    void search();
    void download();

    static void main(String[] args) {
        try {
            final boolean debug = Arrays.asList(args).contains("--d");

            try (Peer peer = new PeerImpl(debug)) {
                Runtime.getRuntime().addShutdownHook(new PeerImpl.ShutdownHook(peer));

                peer.start();

                boolean running = true;

                while(running) {
                    final PeerImpl.Operation operation = PeerImpl.Operation.read();

                    switch (operation) {
                        case JOIN: peer.join(); break;
                        case SEARCH: peer.search(); break;
                        case DOWNLOAD: peer.download(); break;
                        case EXIT:
                            running = false;
                            break;
                    }
                }
            }

            System.out.println("Peer encerrado!");
        } catch (Exception e) {
            System.out.println("Falha ao iniciar peer!" );
        }
    }
}
