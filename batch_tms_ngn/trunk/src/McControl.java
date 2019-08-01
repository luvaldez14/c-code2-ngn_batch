import java.io.*;
import java.util.*;
import java.util.*;
import java.sql.*;
import edu.ucsd.act.db.connectionpool.*;
import edu.ucsd.act.jlink.JLinkJndi;
import edu.ucsd.act.jlink.util.JLinkDate;

public class McControl {

private static Connection conn = null;
private static String db = "ngn_db";
private static int process_month = 0;
private static int process_first_month = 0;
private static int financial_month = 0;

public static int rows_inserted = 0;
public static int recharge_rows_inserted = 0;

public final static String MSG_PATH = JLinkJndi.getContextDocsPath()+"../msgs/"; //obtained from shell script;
public final static String EXTRACT_PATH = JLinkJndi.getContextDocsPath()+"../extracts/"; //obtained from shell script;

public static PrintWriter out = null;

public static void main(String args[]) {
	int exit_status = 0;
	try {
		McControl control = new McControl();
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
 * 3 - Rerun excel step only
 * 4 - Rerun pdf step only
 * 5 - Don't run any of the processing steps
 * 6 - Rerun from generation on (gen, excel & pdf)
 *
 * String post_flag
 * Y - Run Post Processing
 * N - Do Not Run Post Processing
 */
private void run(String flag, String post_flag)
	throws Throwable
{
	DBConnectionManagerPC.createDatasources();

	conn = McControl.openConnection(db,toString());
	if(conn != null) {
		Statement stmt = null;
		try {
			out = new PrintWriter(new FileWriter(MSG_PATH+"mc_process.msg"));
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Program Started");

			String sql = "select mc_period_process_month, mc_period_status_code, mc_period_financial_period from ngn.mc_period where mc_period_status_code in ('1','5')";
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
				McPreProcess pre_process = new McPreProcess(conn,process_first_month,process_month,status_code);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Pre Process Start");
				out.flush();
				pre_process.run(flag);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Pre Process End");
				out.flush();
			}

			if(flag.equals("0") || flag.equals("1")) {
				McPopulate populate = new McPopulate(conn,process_first_month,process_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Populate Start");
				out.flush();
				populate.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Populate End");
				out.flush();
			}

			if(flag.equals("0") || flag.equals("1")) {
				McRechargeCalc recharge = new McRechargeCalc(conn,process_first_month,process_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Recharge Start");
				out.flush();
				recharge.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Recharge End");
				out.flush();
			}

			if(flag.equals("0") || flag.equals("1") || flag.equals("2") || flag.equals("6")) {
				McGenerate generate = new McGenerate(conn,process_first_month,process_month,financial_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Generate Start");
				out.flush();
				generate.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Generate End");
				out.flush();
			}

			if(flag.equals("0") || flag.equals("1") || flag.equals("3") || flag.equals("6")) {
				McGenerateExcel generate = new McGenerateExcel(process_month,financial_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Generate Excel Start");
				out.flush();
				generate.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Generate Excel End");
				out.flush();
			}

			if(flag.equals("0") || flag.equals("1") || flag.equals("4") || flag.equals("6")) {
				MCBillPrintControl pdf = new MCBillPrintControl(""+process_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Generate PDF Start");
				out.flush();
				pdf.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Generate PDF End");
				out.flush();
			}

			if(post_flag.equals("Y")) {
				McPostProcess post_process = new McPostProcess(conn,process_first_month,process_month);
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Post Process Start");
				out.flush();
				post_process.run();
				out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Post Process End");
				out.flush();
			}

			out.println();
			out.println("Rows Inserted: "+rows_inserted);
			out.println("Recharge Rows Inserted: "+recharge_rows_inserted);
			out.println();

			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> MC PROCESS SUCCESSFUL");
			out.flush();
		} catch(SQLException sqe) {
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+sqe);
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> MC PROCESS UNSUCCESSFUL");
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+sqe);
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> MC PROCESS UNSUCCESSFUL");
			throw new Exception("SQLException: "+sqe.toString());
		} catch(Exception e){
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+e);
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> MC PROCESS UNSUCCESSFUL");
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+e);
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> MC PROCESS UNSUCCESSFUL");
			throw new Exception("Exception: "+e.toString());
		} catch(Throwable t){
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+t);
			out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> MC PROCESS UNSUCCESSFUL");
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+t);
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> MC PROCESS UNSUCCESSFUL");
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