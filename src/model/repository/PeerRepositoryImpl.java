package model.repository;

import model.response.JoinResponse;
import model.response.LeaveResponse;
import model.response.UpdateResponse;
import log.ConsoleLog;
import log.Log;

import java.util.*;

public class PeerRepositoryImpl implements PeerRepository {
    private static final String TAG = "PeerRepositoryImpl";
    private static final Log log = new ConsoleLog(TAG);
    private final HashMap<String, LinkedHashSet<String>> peerMap = new HashMap<>();

    @Override
    public String key(String ip, Integer port) {
        return ip + ":" + port.toString();
    }

    @Override
    public JoinResponse join(String ip, Integer port, List<String> files) {
        try {
            if (peerMap.containsKey(key(ip, port)))
                return JoinResponse.NOT_AVAILABLE;

            LinkedHashSet<String> hostFiles = peerMap.getOrDefault(key(ip, port), new LinkedHashSet<>());

            hostFiles.addAll(files);

            peerMap.put(key(ip, port), hostFiles);

            return JoinResponse.OK;
        } catch (Exception e) {
            log.e(String.format("Failed to join peer %s", key(ip, port)));

            return JoinResponse.ERROR;
        }
    }

    @Override
    public List<String> search(String file) {
        List<String> foundOn = new ArrayList<>();

        for (Map.Entry<String, LinkedHashSet<String>> entry : peerMap.entrySet()) {
            if(entry.getValue().stream().anyMatch(peerFile -> peerFile.equals(file))) {
                foundOn.add(entry.getKey());
            }
        }

        return foundOn;
    }

    @Override
    public UpdateResponse update(String ip, Integer port, String file) {
        try {
            if(!peerMap.containsKey(key(ip, port)))
                return UpdateResponse.NOT_JOINED;

            LinkedHashSet<String> hostFiles = peerMap.getOrDefault(key(ip, port), new LinkedHashSet<>());

            hostFiles.add(file);

            peerMap.put(key(ip, port), hostFiles);

            return UpdateResponse.OK;
        } catch (Exception e) {
            log.e(String.format("Failed to update peer %s with file %s", key(ip, port), file));

            return UpdateResponse.ERROR;
        }
    }

    @Override
    public LeaveResponse leave(String ip, Integer port) {
        try {
            if (!peerMap.containsKey(key(ip, port)))
                return LeaveResponse.NOT_JOINED;

            peerMap.remove(key(ip, port));

            return LeaveResponse.OK;
        } catch (Exception e) {
            log.e(String.format("Failed to release peer %s", key(ip, port)));

            return LeaveResponse.OK;
        }
    }
}
