package MainTest;

import flyingchess.Main.Main;

/**
 * Created by Ryan on 16/4/1.
 */
public class MainTest {
    public static void main(String[] args){
        try{
            //String[] testArgs = new String[1];
            //testArgs[0] = new File(MainTest.class.getClassLoader().getResource("config.xml").toURI()).getAbsolutePath();
            Main.main(new String[0]);
        } catch(Exception e){
            e.printStackTrace();
        }

    }
}
