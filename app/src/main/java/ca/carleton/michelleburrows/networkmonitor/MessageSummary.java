package ca.carleton.michelleburrows.networkmonitor;

/**
 * A summary of an HTTP message pair. Contains the path, the request method, and the
 * response status code.
 * Created by Michelle on 3/21/2015.
 */
public class MessageSummary {
    private String method;
    private String status;
    private String path;

    public MessageSummary () {
        method = "(no method)";
        status = "(no status)";
        path = "(no path)";
    }

    public MessageSummary(String method, String status, String path) {
        this.method = method;
        this.status = status;
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
