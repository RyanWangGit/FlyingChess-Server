package Database;

import Config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ryan on 16/4/18.
 */
public class Database {
    private static Logger logger = LogManager.getLogger(Database.class.getName());
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static String DB_NAME = null;
    private static String USER_NAME = null;
    private static String PASSWORD = null;
    private static Connection conn = null;
    private static Statement stmt = null;

    public static void initialize(Config config){
        DB_NAME = config.getDataBaseName();
        USER_NAME = config.getDataBaseUser();
        PASSWORD = config.getDataBasePassword();
        String databaseUrl = "jdbc:mysql://localhost/" + DB_NAME;
        try{
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(databaseUrl, USER_NAME, PASSWORD);
            stmt = conn.createStatement();
            logger.info("Database connected.");
        } catch(Exception e){
            logger.catching(e);
        }
    }

    public static List<String> getUser(int id){
        String sql = "SELECT * FROM Users WHERE ID='" + String.valueOf(id) + "'";
        try{
            ResultSet result = stmt.executeQuery(sql);
            if(!result.next()){
                return null;
            }
            else{
                List<String> userInfo = new ArrayList<>();
                userInfo.add(result.getString("ID"));
                userInfo.add(result.getString("Name"));
                userInfo.add(result.getString("PasswordMD5"));
                userInfo.add(result.getString("Points"));
                return userInfo;
            }
        } catch(Exception e){
            logger.catching(e);
        }
        return null;
    }


    public static List<String> getUser(String username){
        String sql = "SELECT * FROM Users WHERE Name='" + username + "'";
        try{
            ResultSet result = stmt.executeQuery(sql);
            if(!result.next()){
                return null;
            }
            else{
                List<String> userInfo = new ArrayList<>();
                userInfo.add(result.getString("ID"));
                userInfo.add(result.getString("Name"));
                userInfo.add(result.getString("PasswordMD5"));
                userInfo.add(result.getString("Points"));
                return userInfo;
            }
        } catch(Exception e){
            logger.catching(e);
        }
        return null;
    }


    /**
     * Add the user's info into database, returns the index of the user.
     * @param username The username.
     * @param passwordMD5 The MD5 string of the password.
     * @return The index of the user.
     */
    public static Integer addUser(String username, String passwordMD5){
        String insertSql = "INSERT INTO Users(Name,PasswordMD5) VALUES('" + username + "','" + passwordMD5 + "')";
        String querySql = "SELECT ID FROM Users WHERE Name='" + username + "'";
        try{
            stmt.executeUpdate(insertSql);
            ResultSet result = stmt.executeQuery(querySql);
            if(!result.next())
                return -1;
            else
                return result.getInt("ID");
        } catch(Exception e){
            logger.catching(e);
        }
        return -1;
    }

}
