package edu.harvard.iq.dataverse;

/**
 * Removes a T from the persistence context.
 * @author bencomp
 */
public interface Remover<T> {
    /**
     * Removes an object from the persistence context.
     * @param object the object to remove
     */
    void delete(T object);
}
