package model.repository;

import java.util.*;

public class FileRepository {
    private final HashMap<String, LinkedHashSet<String>> hostMap = new HashMap<>();

    public void add(String ip, Integer port, List<String> files) {
        // TODO: Check if peer is already added
        // TODO: Check if filename is valid

        LinkedHashSet<String> hostFiles = hostMap.getOrDefault(ip + ":" + port.toString(), new LinkedHashSet<>());

        hostFiles.addAll(files);
    }

    public List<String> search(String filenameWithExtension) {
        // TODO: Check if filename is valid
        List<String> foundOn = new ArrayList<>();

        for (Map.Entry<String, LinkedHashSet<String>> entry : hostMap.entrySet()) {
            if(entry.getValue().stream().anyMatch(file -> file.equals(filenameWithExtension))) {
                foundOn.add(entry.getKey());
            }
        }

        return foundOn;
    }

    public void update(String ip, Integer port, String file) {
        // TODO: Check if peer exists
        // TODO: Check if filename is valid

        hostMap.get(ip + ":" + port.toString()).add(file);
    }
}
