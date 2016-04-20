package ServerTest;

import Config.Config;
import Config.XMLConfig;
import Server.SSLServer;
import Server.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;


/**
 * Created by Ryan on 16/4/2.
 */
public class SSLServerTest {
    private static Logger logger = LogManager.getLogger(SSLServerTest.class.getName());

    public static void main(String[] args){
        try{
            Config config = new XMLConfig(new File(SSLServerTest.class.getClassLoader().getResource("config.xml").toURI()));
            Server server = new SSLServer(config);
            server.start();
        } catch(Exception e){
            e.printStackTrace();
        }

    }
}
