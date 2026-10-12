package app;

/** Implemented by screens that own background threads and must clean up when closed. */
public interface Closable {
    void onClose();
}
