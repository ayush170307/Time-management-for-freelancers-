package app;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** A short coloured label ("pill") shown in table cells, such as Paid, Pending or Overdue. */
public final class Badge implements Comparable<Badge> {

    public enum Tone {
        SUCCESS, WARNING, DANGER, NEUTRAL
    }

    private static final DateTimeFormatter RECEIPT_TIME = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    private final String text;
    private final Tone tone;

    public Badge(String text, Tone tone) {
        this.text = text;
        this.tone = tone;
    }

    public String getText() {
        return text;
    }

    public Tone getTone() {
        return tone;
    }

    /** Project status; an unfinished project past its deadline shows as Overdue (red). */
    public static Badge of(Project project) {
        if (project.getStatus() != Project.Status.COMPLETED && project.getDeadline().isBefore(LocalDate.now())) {
            return new Badge("Overdue", Tone.DANGER);
        }
        switch (project.getStatus()) {
            case COMPLETED:
                return new Badge("Completed", Tone.SUCCESS);
            case IN_PROGRESS:
                return new Badge("In progress", Tone.WARNING);
            default:
                return new Badge("Not started", Tone.NEUTRAL);
        }
    }

    public static Badge of(Invoice.Status status) {
        if (status == Invoice.Status.PAID) {
            return new Badge("Paid", Tone.SUCCESS);
        }
        return new Badge("Pending", Tone.WARNING);
    }

    /** Status shown in the inbox. */
    public static Badge ofInbox(Message message) {
        if (message.getReadAt() == null) {
            return new Badge("New", Tone.WARNING);
        }
        return new Badge("Read", Tone.NEUTRAL);
    }

    /** Read receipt shown to the sender. */
    public static Badge ofReceipt(Message message) {
        if (message.getReadAt() == null) {
            return new Badge("Delivered", Tone.NEUTRAL);
        }
        return new Badge("Read " + message.getReadAt().format(RECEIPT_TIME), Tone.SUCCESS);
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public int compareTo(Badge other) {
        return text.compareTo(other.text);
    }
}
