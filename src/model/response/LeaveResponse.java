package model.response;

public enum LeaveResponse {
    OK("LEAVE_OK"),
    NOT_JOINED("LEAVE_NOT_AVAILABLE"),
    ERROR("LEAVE_ERROR");

    private final String code;

    LeaveResponse(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
