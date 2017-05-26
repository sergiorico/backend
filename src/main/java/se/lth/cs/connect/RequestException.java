package se.lth.cs.connect;

/**
 * Whenever the user must provide data, the data can in some cases be invalid.
 * In such cases, throw an exception.
 */
public class RequestException extends RuntimeException {
    private int status;

    public RequestException(String msg) {
        super(msg);
        status = 400;
    }

    public RequestException(int status, String msg) {
        super(msg);
        this.status = status;
    }

    public int getStatus() { return status; }
}
