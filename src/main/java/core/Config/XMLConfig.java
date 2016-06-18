package core.Config;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;

/**
 * Stores the server's configurations in XML style.
 * @author Ryan Wang
 */
public class XMLConfig extends AbstractConfig {
    private static Logger logger = LogManager.getLogger(XMLConfig.class.getName());

    /**
     * Default empty constructor.
     */
    public XMLConfig(){

    }

    /**
     * Construct the config object from file.
     * @param file
     */
    public XMLConfig(File file){
        super(file);
    }

    /**
     * Load server configurations.
     * @param node The XML node for server configs.
     */
    private void loadServerConfig(Element node){
        logger.info("Loading server configurations.");

        try{
            this.dataPort = Integer.valueOf(node.elementText("dataport"));
        } catch(NumberFormatException e){
        }

        try{
            this.controlPort = Integer.valueOf(node.elementText("controlport"));
        } catch(NumberFormatException e){
        }
    }

    /**
     * Load database configurations.
     * @param node The XML node for database configs.
     */
    private void loadDatabaseConfig(Element node){
        logger.info("Loading database configurations.");
        String ip = node.elementText("ip");
        String name = node.elementText("name");
        String username = node.elementText("username");
        String password = node.elementText("password");

        if(ip != null)
            this.databaseIP = ip;
        if(username != null)
            this.databaseUser = username;
        if(name != null)
            this.databaseName = name;
        if(password != null)
            this.databasePassword = password;
        try{
            this.databasePort = Integer.valueOf(node.elementText("port"));
        } catch(NumberFormatException e){
        }
    }

    private void loadSSLConfig(Element node){
        logger.info("Loading SSL configurations.");
        String keystorePath = node.elementText("keystore");
        String keystorePass = node.elementText("keystorepass");
        String keyPass = node.elementText("keypass");

        if(keystorePath != null)
            this.keystorePath = keystorePath;
        if(keystorePass != null)
            this.keystorePass = keystorePass;
        if(keyPass != null)
            this.keyPass = keyPass;
    }

    /**
     * Load from external file.
     * @param file External file.
     */
    public void load(File file) {
        try{
            logger.info("Start loading configurations.");
            SAXReader reader = new SAXReader();
            Document doc = reader.read(file);
            Element node = doc.getRootElement();

            // get config elements
            Element serverElement = node.element("server");
            Element databaseElement = node.element("database");
            Element sslElement = node.element("ssl");

            // load from elements
            loadServerConfig(serverElement);
            loadDatabaseConfig(databaseElement);
            loadSSLConfig(sslElement);

            logger.info("Configurations loaded.");
        } catch (DocumentException e) {
            logger.error("Error occured when parsing the file, " +
                    "it might not exist or contains illegal parameters." +
                    "Using default configurations.");
        }
    }
}
