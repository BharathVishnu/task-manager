package taskamanager.backend.exception;

public class AppException extends RuntimeException {
    private final int status;
    private final String code;

    public AppException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public int getStatus() { return status; }
    public String getCode() { return code; }

    public static AppException notFound() {
        return new AppException(404, "NOT_FOUND", "Not found");
    }
}