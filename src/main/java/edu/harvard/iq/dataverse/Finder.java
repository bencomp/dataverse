package edu.harvard.iq.dataverse;

/**
 * Definition of how to find entities by their primary key.
 * @author bencomp
 */
public interface Finder<T> {
    /**
     * Finds the entity of class T by its primary key.
     * @param pk the primary key of the entity
     * @return the entity whose key is given, or {@code null} if it was not found
     */
    T find(Object pk);
}
