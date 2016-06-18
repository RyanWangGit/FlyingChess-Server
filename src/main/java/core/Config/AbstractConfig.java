package core.Config;

import java.io.File;

/**
 * Provide basic implementations of getter and setter, note that
 * load(File file) is abstract and needs to be implemented.
 * @author Ryan Wang
 */
public abstract class AbstractConfig implements Config {
    protected int dataPort = 6666;
    protected int controlPort = 7777;

    protected int databasePort = 3306;
    protected String databaseIP = "127.0.0.1";
    protected String databaseName = "database";
    protected String databaseUser = "root";
    protected String databasePassword = "password";
    protected String keystorePath = "./keystore.keystore";
    protected String keystorePass = "password";
    protected String keyPass = "password";

    /**
     * Default empty constructor
     */
    public AbstractConfig(){

    }

    /**
     * Load config from external file.
     * @param file External file
     */
    public AbstractConfig(File file){
        load(file);
    }

    /**
     * Load from different sources.
     * @param file External file.
     */
    public abstract void load(File file);


    /**
     * Returns the database's IP.
     *
     * @return The databases's IP.
     */
    public String getDataBaseIP() {
        return databaseIP;
    }

    /**
     * Returns the database's port.
     *
     * @return The database's port.
     */
    public int getDataBasePort() {
        return databasePort;
    }

    /**
     * Returns the database's name.
     *
     * @return The database's name.
     */
    public String getDataBaseName() {
        return databaseName;
    }

    /**
     * Returns the database's username.
     *
     * @return The database's username.
     */
    public String getDataBaseUser() {
        return databaseUser;
    }

    /**
     * Returns the database's password.
     *
     * @return The databses's password.
     */
    public String getDataBasePassword() {
        return databasePassword;
    }

    /**
     * Returns the port the server listens on.
     *
     * @return The port the server listens on.
     */
    public int getDataPort() {
        return dataPort;
    }

    /**
     * Returns the port the server controller listens on.
     *
     * @return The port the server controller listens on.
     */
    public int getControlPort() {
        return controlPort;
    }

    /**
     * Returns the path of the keystore file.
     * @return The path of the keystore file.
     */
    public String getKeyStorePath() { return keystorePath; }

    /**
     * Returns the password of the keystore.
     * @return The password of the keystore.
     */
    public String getKeyStorePassword() { return keystorePass; }

    /**
     * Returns the password of the key
     * @return The password of the key.
     */
    public String getKeyPassword() { return keyPass; }
}
