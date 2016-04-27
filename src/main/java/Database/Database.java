package Database;

import Config.Config;
import GameObjects.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Created by Ryan on 16/4/18.
 */
public class Database {
    private static Logger logger = LogManager.getLogger(Database.class.getName());
    private static Config config = null;
    private static Connection conn = null;
    private static Statement stmt = null;

    public static void initialize(Config config){
        Database.config = config;
        String databaseUrl = "jdbc:mysql://localhost/" + config.getDataBaseName();
        try{
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(databaseUrl, config.getDataBaseUser(), config.getDataBasePassword());
            stmt = conn.createStatement();
            logger.info("Database connected.");
        } catch(Exception e){
            logger.catching(e);
        }
    }

    private static void initialize(){
        Database.initialize(Database.config);
    }

    public static User getUser(int id){
        String sql = "SELECT * FROM Users WHERE ID='" + String.valueOf(id) + "'";
        try{
            // reconnect if connection is closed.
            if(conn.isClosed())
                Database.initialize();

            ResultSet result = stmt.executeQuery(sql);
            if(!result.next()){
                return null;
            }
            else{
                User user = new User(
                        result.getInt("ID"),
                        result.getString("Name"),
                        result.getString("PasswordMD5"),
                        result.getInt("Points"));
                return user;
            }
        } catch(Exception e){
            logger.catching(e);
        }
        return null;
    }


    public static User getUser(String username){
        String sql = "SELECT * FROM Users WHERE Name='" + username + "'";
        try{
            // reconnect if connection is closed.
            if(conn.isClosed())
                Database.initialize();

            ResultSet result = stmt.executeQuery(sql);
            if(!result.next()){
                return null;
            }
            else{
                User user = new User(
                        result.getInt("ID"),
                        result.getString("Name"),
                        result.getString("PasswordMD5"),
                        result.getInt("Points"));
                return user;
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
            // reconnect if connection is closed.
            if(conn.isClosed())
                Database.initialize();
            
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
