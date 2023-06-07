package model.response;

public enum JoinResponse {
    OK("JOIN_OK"),
    NOT_OK("JOIN_ERROR");

    private final String code;

    JoinResponse(String code) {
        this.code = code;
    }

    public static JoinResponse fromCode(String code){
        switch (code) {
            case "JOIN_OK": return JoinResponse.OK;
            case "JOIN_ERROR": return JoinResponse.NOT_OK;
            default:
                throw new IllegalStateException("Unexpected value: " + code);
        }
    }

    public String getCode() {
        return code;
    }
}
