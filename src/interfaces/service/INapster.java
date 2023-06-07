package interfaces.service;

import java.rmi.Remote;
import java.util.List;

public interface INapster extends Remote {
    String join(String ip, Integer port, List<String> files);
}
