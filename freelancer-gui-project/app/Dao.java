package app;

import java.util.List;
import java.util.Optional;

/** Generic data-access contract implemented by every DAO. */
public interface Dao<T> {
    int save(T item) throws DatabaseException;

    Optional<T> findById(int id) throws DatabaseException;

    List<T> findAll() throws DatabaseException;
}
