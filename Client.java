package app;

public class Client extends User {

    public Client(int id, String name, String email) {
        super(id, name, email);
    }

    public Client(String name, String email) {
        super(0, name, email);
    }

    @Override
    public String getRole() {
        return "CLIENT";
    }

    @Override
    public String getDashboardTitle() {
        return "Client Dashboard - " + name;
    }
}
