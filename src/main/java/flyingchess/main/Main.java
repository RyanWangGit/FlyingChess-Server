package flyingchess.main;


import core.config.Config;
import core.config.XMLConfig;
import core.server.SSLServer;
import core.server.Server;
import flyingchess.game.ObjectManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Main {
    private static Logger logger = LogManager.getLogger(Main.class.getName());
    private static Server server = null;
    private static Config serverConfig = null;
    private static ObjectManager objectManager = new ObjectManager();

    public static void main(String[] args){
        logger.info("Setting up server");
        String configPath;
        if(args.length < 1){
            logger.info("Too few arguments, try to find config.xml in current directory.");
            configPath = "./config.xml";
        }
        else{
            configPath = args[0];
        }
        serverConfig = new XMLConfig(new File(configPath));
        server = new SSLServer(serverConfig);
        logger.info("Server setup finished, ready to work.");
        FCDataPackProcessor processor = new FCDataPackProcessor(objectManager);
        server.start(processor);
    }
}
