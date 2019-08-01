import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import edu.ucsd.act.db.connectionpool.DBConnectionManagerPC;
import edu.ucsd.act.db.connectionpool.DBPooledConnection;
import edu.ucsd.act.jlink.util.JLinkDate;

/**
 * @author Jennifer Kramer
 *
 */
public class MCBillPrintControl {

private static Connection conn = null;
private static String db = "ngn_db";
private static String process_month = "";

public static PrintWriter msg_file = null;

/**
 * @param args
 */
public static void main(String args[]) {
	try {
		MCBillPrintControl control = new MCBillPrintControl("200605");
		control.run();
	} catch(Exception e) {
		System.out.println("Error: "+e);
	} catch(Throwable t) {
		System.out.println("Error: "+t);
	} finally {
		System.exit(0);
	}
}

public MCBillPrintControl(String process_month)
{
	this.process_month = process_month;
}

public void run()
	throws Throwable
{

	conn = openConnection(db,toString());
	if(conn != null) {
		Statement stmt = null;
		ResultSet rs = null;
		String sql = null;
		try {
			msg_file = new PrintWriter(new FileWriter(McControl.MSG_PATH+"mcbillprntpdf.msg"));

			stmt = conn.createStatement();

			sql = ""
/*
				+"select distinct "
				+"  mail_code, "					//1
				+"  ifis_index, "					//2
				+"  coalesce(ifis_fund_no,' '), "	//3
				+"  coalesce(ifis_org_no,' '), "	//4
				+"  coalesce(ifis_acct_no,' '), "	//5
				+"  coalesce(ifis_prog_no,' '), "	//6
				+"  coalesce(orgn_mgr,' '), "		//7
				+"  coalesce(orgn_title,' ') "		//8
				+"from mc_billprnt_recap_view "
				+"left outer join tms_campus_db_new..ifis_ifopl_table on ifis_index = ifis_indx_no "
				+"left outer join tms_campus_db_new..org_table on ifis_org_no = orgn_code and orgn_sta='A' and orgn_end_date is null and orgn_mgr_addr is not null "
				+"where billprnt_process_month = '"+process_month+"' "
				+"  and ifis_index not in ('0000001','0000002') "

				//testing
				+"  and mail_code = '0115' "

				+"order by "
				+"  mail_code, "
				+"  ifis_index, "
				+"  ifis_fund_no, "
				+"  ifis_org_no, "
				+"  ifis_acct_no, "
				+"  ifis_prog_no, "
				+"  orgn_mgr, "
				+"  orgn_title "
*/
				+"select distinct "
				+"  recharge_financial_mgr_addr, "	//1
				+"  recharge_index, "				//2
				+"  coalesce(ifis_fund_no,' '), "	//3
				+"  coalesce(ifis_org_no,' '), "	//4
				+"  coalesce(ifis_acct_no,' '), "	//5
				+"  coalesce(ifis_prog_no,' '), "	//6
				+"  coalesce(orgn_mgr,' '), "		//7
				+"  coalesce(orgn_title,' ') "		//8
				+"from ngn.mc_recharge "
				+"left outer join tms.ifis_ifopl_table on recharge_index = ifis_indx_no "
				+"left outer join tms.org_table on ifis_org_no = orgn_code and orgn_sta='A' and orgn_end_date is null and orgn_mgr_addr is not null "
				+"where recharge_calendar_year = '"+process_month+"' "
				+"  and recharge_index not in ('0000001','0000002') "
				+"order by "
				+"  recharge_financial_mgr_addr, "
				+"  recharge_index, "
				+"  coalesce(ifis_fund_no,' '), "
				+"  coalesce(ifis_org_no,' '), "
				+"  coalesce(ifis_acct_no,' '), "
				+"  coalesce(ifis_prog_no,' '), "
				+"  coalesce(orgn_mgr,' '), "
				+"  coalesce(orgn_title,' ') "
				;
			rs = stmt.executeQuery(sql);

			rs.next();
			String last_mail_code = rs.getString(1).trim();
			String mail_code;
			String last_org_mgr = rs.getString(7);
			String org_mgr = rs.getString(7);
			String last_index = rs.getString(2).trim();
			Vector indexes = new Vector();
			Vector index_details = new Vector();

			String fund = rs.getString(3);
			String orgn = rs.getString(4);
			String acct = rs.getString(5);
			String prog = rs.getString(6);
			String org_title = rs.getString(8);

			do {
				String index = rs.getString(2).trim();

				if(!last_index.equals(index)) {
					indexes.add(last_index);
					index_details.add(org_title+"\t"+fund+"\t"+orgn+"\t"+acct+"\t"+prog);
				}
				mail_code = rs.getString(1).trim();
				fund = rs.getString(3);
				orgn = rs.getString(4);
				acct = rs.getString(5);
				prog = rs.getString(6);
				org_title = rs.getString(8);

				if(!last_mail_code.equals(mail_code)) {
					MCBillPrintCreatePdf create_pdf = new MCBillPrintCreatePdf(conn, last_mail_code, last_org_mgr, process_month, indexes, index_details);
					create_pdf.generate();
					msg_file.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> "+last_mail_code+"("+last_org_mgr+")\tIndex Vector Size: "+indexes.size());
					msg_file.flush();
					/*
					System.out.println(last_mail_code+"("+last_org_mgr+")\tIndex Details Vector Size: "+index_details.size());
					for(int i=0; i < indexes.size(); i++) {
						String key = (String)indexes.elementAt(i);
						System.out.println("\tIndex: "+key);
						System.out.println("\tIndex Details: "+index_details.elementAt(i));
					}
					*/
					org_mgr = rs.getString(7);
					indexes = new Vector();
					index_details = new Vector();
				}

				last_mail_code = mail_code;
				last_org_mgr = org_mgr;
				last_index = index;
			} while(rs.next());
			indexes.add(last_index);
			index_details.add(org_title+"\t"+fund+"\t"+orgn+"\t"+acct+"\t"+prog);

			MCBillPrintCreatePdf create_pdf = new MCBillPrintCreatePdf(conn, last_mail_code, last_org_mgr, process_month, indexes, index_details);
			create_pdf.generate();
			msg_file.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> "+mail_code+"("+last_org_mgr+")\tIndex Vector Size: "+indexes.size());
			msg_file.flush();
			/*
			System.out.println(mail_code+"("+last_org_mgr+")\tIndex Details Vector Size: "+index_details.size());
			for(int i=0; i < indexes.size(); i++) {
				String key = (String)indexes.elementAt(i);
				System.out.println("\tIndex: "+key);
				System.out.println("\tIndex Details: "+index_details.elementAt(i));
			}
			*/

			rs.close();

			stmt.close();
		} catch(SQLException sqe) {
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+sqe);
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> BILL PRINT PROCESS UNSUCCESSFUL");
			msg_file.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+sqe);
			msg_file.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> BILL PRINT PROCESS UNSUCCESSFUL");
			throw new Exception("SQLException: "+sqe.toString());
		} catch(Exception e){
			e.printStackTrace();
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+e);
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> BILL PRINT PROCESS UNSUCCESSFUL");
			msg_file.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+e);
			msg_file.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> BILL PRINT PROCESS UNSUCCESSFUL");
			throw new Exception("Exception: "+e.toString());
		} catch(Throwable t){
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+t);
			System.err.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> BILL PRINT PROCESS UNSUCCESSFUL");
			msg_file.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> ERROR: "+t);
			msg_file.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> BILL PRINT PROCESS UNSUCCESSFUL");
			throw new Exception("Throwable: "+t.toString());
		} finally {
			conn = null;
			DBConnectionManagerPC.closePooledConnections(toString());
			try {
		 		if(stmt!=null){ stmt.close(); }
			} catch(SQLException se){ }

			msg_file.flush();
			msg_file.close();
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
		connp.setDB(connp.getDB());
		conn = (Connection)connp;
	} catch(Exception dbcp) {
		if(connp != null){ connp.disconnect(); }
		throw new Exception("ERROR openConnection(), pooled connection "+dbcp);
	}
	return conn;
}

}
