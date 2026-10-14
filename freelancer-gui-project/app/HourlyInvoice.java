package app;

import java.time.LocalDate;

/** Total = hours worked x hourly rate. */
public class HourlyInvoice extends Invoice {

    public HourlyInvoice(int id, int projectId, int freelancerId, int clientId,
                         double hours, double rate, Status status, LocalDate createdDate) {
        super(id, projectId, freelancerId, clientId, hours, rate, status, createdDate);
    }

    @Override
    public String getType() {
        return "HOURLY";
    }

    @Override
    public double calculateTotal() {
        return hours * rate;
    }
}
