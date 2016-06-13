package ConfigTest;

import core.Config.Config;
import core.Config.XMLConfig;

import java.io.File;

/**
 * Created by Ryan on 16/4/6.
 */
public class XMLConfigTest {
    static Config config = new XMLConfig();

    public static void main(String[] args) {
        try{
            config.load(new File(XMLConfigTest.class.getClassLoader().getResource("config.xml").toURI()));
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
