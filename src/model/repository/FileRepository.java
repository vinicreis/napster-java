package model.repository;

import java.util.*;

public class FileRepository {
    private final HashMap<String, LinkedHashSet<String>> hostMap = new HashMap<>();

    public void add(String key, List<String> files) {
        // TODO: Check if peer is already added
        // TODO: Check if filename is valid

        LinkedHashSet<String> hostFiles = hostMap.getOrDefault(key, new LinkedHashSet<>());

        hostFiles.addAll(files);

        hostMap.put(key, hostFiles);
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

    public void update(String key, String file) {
        // TODO: Check if peer exists
        // TODO: Check if filename is valid

        LinkedHashSet<String> hostFiles = hostMap.getOrDefault(key, new LinkedHashSet<>());

        hostFiles.add(file);

        hostMap.put(key, hostFiles);
    }
}
