package MainTest;

import Main.Main;

import java.io.File;

/**
 * Created by Ryan on 16/4/1.
 */
public class MainTest {
    public static void main(String[] args){
        try{
            String[] testArgs = new String[1];
            testArgs[0] = new File(MainTest.class.getClassLoader().getResource("config.xml").toURI()).getAbsolutePath();
            Main.main(testArgs);
        } catch(Exception e){
            e.printStackTrace();
        }

    }
}
