package interfaces.network;

public interface IPeer {
    Boolean open(String ip, int port);
    Boolean close();
}
