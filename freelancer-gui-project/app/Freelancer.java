package app;

public class Freelancer extends User {

    public Freelancer(int id, String name, String email) {
        super(id, name, email);
    }

    public Freelancer(String name, String email) {
        super(0, name, email);
    }

    @Override
    public String getRole() {
        return "FREELANCER";
    }

    @Override
    public String getDashboardTitle() {
        return "Freelancer Dashboard - " + name;
    }
}
