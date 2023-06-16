package service.model.response;

public enum UpdateResponse {
    OK("UPDATE_OK"),
    NOT_JOINED("UPDATE_NOT_JOINED"),
    ERROR("UPDATE_ERROR");

    private final String code;

    UpdateResponse(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
