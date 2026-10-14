package app;

/** One row of the leaderboard: a freelancer and the hours they have logged. */
public class LeaderboardEntry {

    private final int id;
    private final String name;
    private double hours;
    private int rank;

    public LeaderboardEntry(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addHours(double extra) {
        this.hours += extra;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getHours() {
        return hours;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
