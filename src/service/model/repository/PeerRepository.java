package service.model.repository;

import service.model.response.JoinResponse;
import service.model.response.LeaveResponse;
import service.model.response.UpdateResponse;

import java.util.List;

public interface PeerRepository {
    String key(String ip, Integer port);
    JoinResponse join(String ip, Integer port, List<String> files);
    List<String> search(String file);
    UpdateResponse update(String ip, Integer port, String file);
    LeaveResponse leave(String ip, Integer port);
}
