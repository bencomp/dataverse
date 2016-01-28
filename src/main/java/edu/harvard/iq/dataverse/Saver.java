package edu.harvard.iq.dataverse;

/**
 * Definition of how to save entities.
 * @author bencomp
 */
public interface Saver<T> {
    /**
     * Saves an object using the entity manager. Using this indirection allows to not use the
     * {@link javax.persistence.EntityManager} directly.
     * @param object the object to save
     * @return the object that was saved
     */
    T save(T object);
}
