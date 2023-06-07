package service;

import model.repository.FileRepository;
import model.response.JoinResponse;
import org.jetbrains.annotations.NotNull;
import interfaces.service.INapster;
import util.ILog;
import util.Log;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class Napster extends UnicastRemoteObject implements INapster {
    private final ILog log = new Log("Napster");
    private final FileRepository fileRepository = new FileRepository();

    public Napster() throws RemoteException {
    }

    @Override
    @NotNull
    public String join(String ip, Integer port, List<String> files) {
        fileRepository.add(ip, port, files);

        return JoinResponse.OK.getCode();
    }


    public List<String> search(String filenameWithExtension) {
        return fileRepository.search(filenameWithExtension);
    }

    public String update(String ip, Integer port, String filenameWithExtension) {
        fileRepository.update(ip, port, filenameWithExtension);

        return "UPDATE_OK"; // TODO: Extract to enum
    }
}
