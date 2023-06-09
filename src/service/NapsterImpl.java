package service;

import model.repository.PeerRepository;
import model.repository.PeerRepositoryImpl;
import log.ConsoleLog;
import log.Log;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class NapsterImpl extends UnicastRemoteObject implements Napster {
    private static final String TAG = "NapsterImpl";
    private final Log log = new ConsoleLog(TAG);
    private final PeerRepository repository = new PeerRepositoryImpl();

    public NapsterImpl(boolean debug) throws RemoteException {
        super();

        log.setDebug(debug);
    }

    @Override
    public String join(String ip, Integer port, List<String> files) throws RemoteException {
        log.d(
                String.format(
                        "Joining peer with address %s:%d with files %s",
                        ip,
                        port,
                        String.join(", ", files)
                )
        );

        return repository.join(ip, port, files).getCode();
    }

    @Override
    public List<String> search(String filenameWithExtension) throws RemoteException {
        log.d(String.format("Peer asked for file %s", filenameWithExtension));

        return repository.search(filenameWithExtension);
    }

    @Override
    public String update(String ip, Integer port, String filenameWithExtension) throws RemoteException {
        log.d(String.format("Updating peer %s:%d with file %s", ip, port, filenameWithExtension));

        return repository.update(ip, port, filenameWithExtension).getCode();
    }

    @Override
    public String leave(String ip, Integer port) {
        return repository.leave(ip, port).getCode();
    }
}