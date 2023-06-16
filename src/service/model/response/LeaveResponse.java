package service.model.response;

public enum LeaveResponse {
    OK("LEAVE_OK"),
    NOT_JOINED("LEAVE_NOT_AVAILABLE");

    private final String code;

    LeaveResponse(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
