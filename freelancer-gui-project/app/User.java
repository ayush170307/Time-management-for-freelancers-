package app;

/** Base class for every person in the system. Subclasses define their own role (polymorphism). */
public abstract class User {
    protected int id;
    protected final String name;
    protected final String email;

    protected User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public abstract String getRole();

    public abstract String getDashboardTitle();

    @Override
    public String toString() {
        return name + " (" + email + ")";
    }
}
