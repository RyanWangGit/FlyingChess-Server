package core.database;

import core.config.Config;
import flyingchess.game.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

/**
 * Simple wrapper for database operations.
 * @author Ryan Wang
 */
//TODO: Need to modify this class to make it application-unrelated.
public class Database {
    private static Logger logger = LogManager.getLogger(Database.class.getName());
    private static Config config = null;
    private static Connection conn = null;
    private static Statement stmt = null;

    public static void initialize(Config config){
        Database.config = config;
        String databaseUrl = "jdbc:mysql://" + config.getDataBaseIP() + ":" + config.getDataBasePort() + "/" + config.getDataBaseName();
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
     * @return whether the operation is successful or not.
     */
    public static boolean addUser(String username, String passwordMD5){
        String insertSql = "INSERT INTO Users(Name,PasswordMD5) VALUES('" + username + "','" + passwordMD5 + "')";
        try{
            // reconnect if connection is closed.
            if(conn.isClosed())
                Database.initialize();
            
            stmt.executeUpdate(insertSql);

            return true;
        } catch(SQLException e){
            logger.catching(e);
            return false;
        }
    }

    public static void updateUser(User user){
        String sql = "UPDATE Users SET PasswordMD5 = '" + user.getPassword() + "',Points = '" + user.getPoints()
                + "' WHERE ID = '" + user.getId() + "'";
        try{
            if(conn.isClosed())
                Database.initialize();

            stmt.executeUpdate(sql);
        } catch(SQLException e){
            logger.catching(e);
        }
    }


}
