package Main;


import Config.Config;
import Config.XMLConfig;
import Server.SSLServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Main {
    private static Logger logger = LogManager.getLogger(Main.class.getName());
    private static SSLServer server = null;
    private static Config serverConfig = null;

    public static void main(String[] args){
        logger.info("Setting up server");
        if(args.length < 1){
            logger.info("Too few arguments, config files ");
        }
        serverConfig = new XMLConfig(new File(args[0]));
        server = new SSLServer(serverConfig);
        logger.info("Server setup finished, ready to work.");
        server.start();
    }
}
