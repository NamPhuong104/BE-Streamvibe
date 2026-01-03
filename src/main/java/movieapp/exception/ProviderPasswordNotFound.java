package movieapp.exception;

public class ProviderPasswordNotFound extends RuntimeException {
    public ProviderPasswordNotFound(String message) {
        super(message);
    }
}
