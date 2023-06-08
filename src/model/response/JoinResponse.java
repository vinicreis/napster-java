package model.response;

public enum JoinResponse {
    OK("JOIN_OK"),
    NOT_AVAILABLE("JOIN_NOT_AVAILABLE"),
    ERROR("JOIN_ERROR");

    private final String code;

    JoinResponse(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
