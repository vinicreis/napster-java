public interface Peer extends Server {
    void join();
    void update(String filename);
    void search();
    void download();
    Thread getShutdownHook();
}
