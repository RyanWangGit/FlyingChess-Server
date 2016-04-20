package Config;

import java.io.File;

/**
 * Created by Ryan on 16/4/11.
 */
public interface Config {
    /**
     * Load the config from external file.
     * @param file The external file.
     */
    void load(File file);


    /**
     * Database related configs.
     */

    /**
     * Returns the database's IP.
     * @return The databases's IP.
     */
    String getDataBaseIP();

    /**
     * Returns the database's port.
     * @return The database's port.
     */
    int getDataBasePort();

    /**
     * Returns the database's name.
     * @return The database's name.
     */
    String getDataBaseName();

    /**
     * Returns the database's username.
     * @return The database's username.
     */
    String getDataBaseUser();

    /**
     * Returns the database's password.
     * @return The databses's password.
     */
    String getDataBasePassword();

    /**
     * Server related configs.
     */

    /**
     * Returns the port the server listens on.
     * @return The port the server listens on.
     */
    int getDataPort();

    /**
     * Returns the port the server controller listens on.
     * @return The port the server controller listens on.
     */
    int getControlPort();
}
