package app;

/**
 * Holds the currently logged-in user and enforces role rules.
 * Every dashboard page and every sensitive action checks the session before it runs.
 */
public final class Session {

    private static User user;

    private Session() {
    }

    public static synchronized void login(User loggedIn) {
        user = loggedIn;
    }

    public static synchronized void logout() {
        user = null;
    }

    public static synchronized User getUser() {
        return user;
    }

    /** Throws AccessDeniedException unless someone with exactly this role is logged in. */
    public static synchronized void requireRole(String role) {
        if (user == null || !user.getRole().equals(role)) {
            throw new AccessDeniedException("Access denied: this area is only for " + role.toLowerCase() + "s.");
        }
    }

    /** Throws AccessDeniedException unless the logged-in user is exactly this person. */
    public static synchronized void requireUser(int userId) {
        if (user == null || user.getId() != userId) {
            throw new AccessDeniedException("Access denied: you can only change your own data.");
        }
    }
}
