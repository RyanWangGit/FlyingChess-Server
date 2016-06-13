package flyingchess.Main;


import core.Config.Config;
import core.Config.XMLConfig;
import core.Server.SSLServer;
import core.Server.Server;
import flyingchess.GameObjects.ObjectManager;
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
        if(args.length < 1){
            logger.info("Too few arguments, try to find config.xml in current directory.");
            args = new String[1];
            args[0] = "./config.xml";
        }
        serverConfig = new XMLConfig(new File(args[0]));
        server = new SSLServer(serverConfig);
        logger.info("Server setup finished, ready to work.");
        FCDataPackProcessor processor = new FCDataPackProcessor(objectManager);
        server.start(processor);
    }
}
