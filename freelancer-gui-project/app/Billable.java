package app;

/** Anything that can produce a billable amount. */
public interface Billable {
    double calculateTotal();
}
