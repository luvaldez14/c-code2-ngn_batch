import java.io.*;
import java.util.*;
import java.sql.*;
import edu.ucsd.act.db.connectionpool.*;
import edu.ucsd.act.jlink.util.JLinkDate;

public class NgnControl {

private static Connection conn = null;
private static String db = "ngn_db";
private static int process_month = 0;
private static int process_first_month = 0;
private static int financial_month = 0;

public static int rows_updated = 0;
public static int rows_inserted = 0;

public static int empl_match_title_month = 0;
public static int empl_match_title_month_plus = 0;
public static int empl_match_part_month = 0;
public static int empl_match_part_month_plus = 0;
public static int empl_match_none = 0;

public static int recharge_rows_returned = 0;
public static int recharge_rows_inserted = 0;
public static int no_ifop_desc = 0;

public static String file_path = "/tms/ngn/extracts/";
public static String msg_path = "/tms/ngn/msgs/";
//public static String file_path = "/tmdata/prod_test/ngn/extracts/";
//public static String msg_path = "/tmdata/prod_test/ngn/msgs/";

public static PrintWriter out = null;

public static void main(String args[]) {
	int exit_status = 0;
	try {
		NgnControl control = new NgnControl();
		if(args.length >= 2)
			control.run(args[0],args[1]);
		else
			System.out.println("Argument required");
	} catch(Exception e) {
		System.err.println("Error: "+e);
		exit_status = 1;
	} catch(Throwable t) {
		System.err.println("Error: "+t);
		exit_status = 1;
	} finally {
		System.exit(exit_status);
	}
}

/**
 * Flag Values
 *
 * String flag
 * 0 - Normal Run
 * 1 - Rerun entire process, backing out changes
 * 2 - Rerun generation step only
 *
 * String post_flag
 * Y - Run Post Processing
 * N - Do Not Run Post Processing
 */
private void run(String flag, String post_flag)
	throws Throwable
{
	DBConnectionManagerPC.createDatasources();

	conn = NgnControl.openConnection(db,toString());
	if(conn != null) {
		Statement stmt = null;
		try {
			out = new PrintWriter(new FileWriter(msg_path+"ngn_process.msg"));
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Program Started");

			String sql = "select ngn_period_process_month, ngn_period_status_code, ngn_period_financial_period from ngn.ngn_period where ngn_period_status_code in ('1','5')";
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			rs.next();

			//get needed variables out of the row before closing
			process_month = rs.getInt(1);
			String status_code = rs.getString(2);
			financial_month = Integer.parseInt(rs.getString(3));

			rs.close();
			stmt.close();

			//calculate first month of process window. Window is 12 months, eg., 200312 through 200411
			int year = process_month/100;
			int month = process_month%year;
			month = month - 11;
			if(month <= 0) {
				month = 12 + month;
				year--;
			}
			process_first_month = (year*100)+month;

			//run the preprocessing
			if(flag.equals("0") || flag.equals("1")) {
				NgnPreProcess pre_process = new NgnPreProcess(conn,process_first_month,process_month,status_code);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Pre Process Start");
				out.flush();
				pre_process.run(flag);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Pre Process End");
				out.flush();
			}

			if(flag.equals("0") || flag.equals("1")) {
				NgnCuPosting cu_post = new NgnCuPosting(conn,process_first_month,process_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> CU Posting Start");
				out.flush();
				cu_post.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> CU Posting End");
				out.flush();
			}

			if(flag.equals("0") || flag.equals("1")) {
				NgnRechargeCalc recharge = new NgnRechargeCalc(conn,process_first_month,process_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Recharge Start");
				out.flush();
				recharge.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Recharge End");
				out.flush();
			}

			if(flag.equals("0") || flag.equals("1") || flag.equals("2")) {
				NgnGenerate generate = new NgnGenerate(conn,process_first_month,process_month,financial_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Generate Start");
				out.flush();
				generate.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Generate End");
				out.flush();
			}

			if(post_flag.equals("Y")) {
				NgnPostProcess post_process = new NgnPostProcess(conn,process_first_month,process_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Post Process Start");
				out.flush();
				post_process.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Post Process End");
				out.flush();
			}

			out.println();
			out.println("Rows Updated: "+rows_updated);
			out.println("Rows Inserted: "+rows_inserted);
			out.println("Employee Match: "+empl_match_title_month);
			out.println("Employee Match (Subsequent): "+empl_match_title_month_plus);
			out.println("Employee Partial Match: "+empl_match_part_month);
			out.println("Employee Partial Match (Subsequent): "+empl_match_part_month_plus);
			out.println("Employee No Match: "+empl_match_none);
			out.println("Recharge Rows Returned: "+recharge_rows_returned);
			out.println("Recharge Rows Inserted: "+recharge_rows_inserted);
			out.println("IFOP Descriptions Not Found: "+no_ifop_desc);
			out.println();

			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> NGN PROCESS SUCCESSFUL");
			out.flush();
		} catch(SQLException sqe) {
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+sqe);
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> NGN PROCESS UNSUCCESSFUL");
			throw new Exception("SQLException: "+sqe.toString());
		} catch(Exception e){
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+e);
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> NGN PROCESS UNSUCCESSFUL");
			throw new Exception("Exception: "+e.toString());
		} catch(Throwable t){
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+t);
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> NGN PROCESS UNSUCCESSFUL");
			throw new Exception("Throwable: "+t.toString());
		} finally {
			if(out != null) {
				out.flush();
				out.close();
			}

			conn = null;
			DBConnectionManagerPC.closePooledConnections(toString());
			try {
		 		if(stmt!=null){ stmt.close(); }
			} catch(SQLException se){ }
		}
	}

}

public static Connection openConnection(String database, String id)
	throws Exception
{
	DBPooledConnection connp=null;
	Connection conn = null;
	try {
		connp = DBConnectionManagerPC.getPooledConnection(database,id);
		conn = (Connection)connp;
		conn.setCatalog(connp.getDB());
	} catch(Exception dbcp) {
		if(connp != null){ connp.disconnect(); }
		throw new Exception("ERROR openConnection(), pooled connection "+dbcp);
	}
	return conn;
}

public static String padZeros(String s, int len) {
	int slen = s.length();
	for(int	i=0; i < len - slen; i++) {
		s = "0"+s;
	}
	return s;
}

/**
 * Pad given pad string to the beginning of data string
 * to given length
 */
public static String rightJustified(String data, int maxlength, String pad)
{
	StringBuffer dataBuffer = new StringBuffer();
	int size = maxlength - data.length();
	for (int i=0; i < size; i++) {
		dataBuffer.append(pad);
	}
	dataBuffer.append(data);
	return dataBuffer.toString();
}

/**
 * Pad given pad string to the end of data string
 * to given length
 */
public static String leftJustified(String data, int maxlength, String pad)
{
	StringBuffer dataBuffer = new StringBuffer();
	dataBuffer.append(data);
	int size = maxlength - data.length();
	for (int i=0; i < size; i++) {
		dataBuffer.append(pad);
	}
	return dataBuffer.toString();
}

/**
 * Remove occurences of given character in the string
 */
public static String escapeChar (String data, char c)
{
	StringBuffer result = new StringBuffer();
	int strlen = data.length();
	char chars[] = data.toCharArray() ;
	for (int i=0; i < strlen; i++) {
		if ( chars[i] != c)
		result.append(chars[i]);
	}
	return result.toString();
}
public static String escapeCharInBag (String data, String bag)
{
	StringBuffer result = new StringBuffer();
	int strlen = data.length();
	char chars[] = data.toCharArray() ;
	for (int i=0; i < strlen; i++) {
		if ( bag.indexOf(chars[i]) < 0)
		result.append(chars[i]);
	}
	return result.toString();
}
}