package app;

import java.time.LocalDate;

/** Total = a flat fee, regardless of hours (the "rate" field holds the flat amount). */
public class FixedPriceInvoice extends Invoice {

    public FixedPriceInvoice(int id, int projectId, int freelancerId, int clientId,
                             double hours, double rate, Status status, LocalDate createdDate) {
        super(id, projectId, freelancerId, clientId, hours, rate, status, createdDate);
    }

    @Override
    public String getType() {
        return "FIXED";
    }

    @Override
    public double calculateTotal() {
        return rate;
    }
}
