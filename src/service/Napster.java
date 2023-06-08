package service;

import model.repository.FileRepository;
import interfaces.service.INapster;
import util.Log;
import util.ConsoleLog;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class Napster extends UnicastRemoteObject implements INapster {
    private final Log log = new ConsoleLog("Napster");
    private final FileRepository fileRepository = new FileRepository();

    public Napster() throws RemoteException {
    }

    @Override
    public String join(String ip, Integer port, List<String> files) throws RemoteException {
        log.i(
                String.format(
                        "Joining peer with address %s with files %s",
                        key(ip, port),
                        String.join(", ", files)
                )
        );

        fileRepository.add(key(ip, port), files);

        return "JOIN_OK";
    }

    @Override
    public List<String> search(String filenameWithExtension) throws RemoteException {
        log.i(String.format("Peer asked for file %s", filenameWithExtension));

        return fileRepository.search(filenameWithExtension);
    }

    @Override
    public String update(String ip, Integer port, String filenameWithExtension) throws RemoteException {
        log.i(String.format("Updating peer %s with file %s", key(ip, port), filenameWithExtension));

        fileRepository.update(key(ip, port), filenameWithExtension);

        return "UPDATE_OK"; // TODO: Extract to enum
    }

    private static String key(String ip, Integer port) {
        return ip + ":" + port.toString();
    }
}
