package service;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Napster extends Remote {
    String join(String ip, Integer port, List<String> files) throws RemoteException;
    List<String> search(String filenameWithExtension) throws RemoteException;
    String update(String ip, Integer port, String filenameWithExtension) throws RemoteException;
    String leave(String ip, Integer port) throws RemoteException;
}
