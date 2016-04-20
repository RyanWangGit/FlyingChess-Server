package Config;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;

/**
 * Created by Ryan on 16/4/6.
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

            // load from elements
            loadServerConfig(serverElement);
            loadDatabaseConfig(databaseElement);

            logger.info("Configurations loaded.");
        } catch (DocumentException e) {
            logger.error("Error occured when parsing the file, " +
                    "it might not exist or contains illegal parameters." +
                    "Using default configurations.");
        }
    }
}
