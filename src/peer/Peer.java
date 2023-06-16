package peer;

public interface Peer extends AutoCloseable {
    void start();
    void join();
    void update(String filename);
    void search();
    void download();
    Thread onShutdown();
}
