package exam;
import java.sql.*;
public class Store {
 private final String url;
 public Store(String file){url="jdbc:sqlite:"+file;try(Connection c=open();Statement s=c.createStatement()){for(String t:new String[]{"admission","monitoring","finalization","workflow"})s.executeUpdate("CREATE TABLE IF NOT EXISTS "+t+" (id TEXT PRIMARY KEY, status TEXT NOT NULL, detail TEXT NOT NULL)");}catch(SQLException e){throw new IllegalStateException("SQLite connection failed. Run as Maven project (mvn exec:java) or build bundled JAR (mvn package then java -jar target/exam-proctoring-facade-1.0-all.jar). Cause: " + e.getMessage(),e);}}
 public Connection open()throws SQLException{return DriverManager.getConnection(url);}
 public void put(String table,String id,String status,String detail){check(table);try(Connection c=open();PreparedStatement q=c.prepareStatement("INSERT INTO "+table+"(id,status,detail) VALUES(?,?,?) ON CONFLICT(id) DO UPDATE SET status=excluded.status,detail=excluded.detail")){q.setString(1,id);q.setString(2,status);q.setString(3,detail);q.executeUpdate();}catch(SQLException e){throw new RuntimeException(e);}}
 public String[] get(String table,String id){check(table);try(Connection c=open();PreparedStatement q=c.prepareStatement("SELECT status,detail FROM "+table+" WHERE id=?")){q.setString(1,id);try(ResultSet r=q.executeQuery()){return r.next()?new String[]{r.getString(1),r.getString(2)}:null;}}catch(SQLException e){throw new RuntimeException(e);}}
 private void check(String t){if(!t.matches("admission|monitoring|finalization|workflow"))throw new IllegalArgumentException("Unknown table");}
}
