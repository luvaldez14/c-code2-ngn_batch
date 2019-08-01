import java.text.DecimalFormat;

import java.util.Hashtable;
import java.util.StringTokenizer;

import java.sql.*;

import edu.ucsd.act.db.connectionpool.*;
import edu.ucsd.act.jlink.util.JLinkDate;

import java.io.PrintWriter;
import java.io.FileWriter;

public class McPopulate {

private Connection conn = null;
private String db = "ngn_db";

private int process_first_month = 0;
private int process_month = 0;

public McPopulate(Connection conn, int process_first_month, int process_month)
	throws Exception
{
	this.conn = conn;
	this.process_first_month = process_first_month;
	this.process_month = process_month;
}

public void run()
	throws Exception
{
	Statement stmt = null;
	String sql = null;

	try {
		sql = ""
			+"insert into ngn.mc_history ( "
			+"	mc_history_employee_id, "
			+"	mc_history_process_month, "
			+"	mc_history_title_code, "
			+"	mc_history_index, "
			+"	mc_history_pay_activity_code, "
			+"	mc_history_pay_index, "
			+"	mc_history_group_code, "
			+"	mc_history_current_group_rate, "
			+"	mc_history_current_amount, "
			+"	mc_history_prev_group_rate, "
			+"	mc_history_prev_amount "
			+") "
			+"select "
			+"	ngn_history_employee_id, "
			+"	ngn_history_process_month, "
			+"	ngn_history_title_code, "
			+"	ngn_history_index, "
			+"	ngn_history_pay_activity_code, "
			+"	ngn_history_pay_index, "
			+"	ngn_history_group_code, "
			+"	0, "
			+"	0, "
			+"	0, "
			+"	0 "
			+"from ngn.ngn_history "
			+"left outer join ngn.mc_history on "
			+"	    mc_history_employee_id = ngn_history_employee_id "
			+"	and mc_history_process_month = ngn_history_process_month "
			+"	and mc_history_title_code = ngn_history_title_code "
			+"	and mc_history_index = ngn_history_index "
			+"	and mc_history_pay_activity_code = ngn_history_pay_activity_code "
			+"	and mc_history_pay_index = ngn_history_pay_index "
			+"where mc_history_employee_id is null "
			+"  and ngn_history_process_month between "+process_first_month+" and "+process_month+" "
			+"  and ngn_history_loc_group_code = 'MC' "
			//+"  and ngn_history_vcu_code != '05' "
			+" and (ngn_history_vcu_code != '05' OR ngn_history_fund like '601%')"
			;
		stmt = conn.createStatement();
		McControl.rows_inserted = stmt.executeUpdate(sql);
		stmt.close();

	} catch(Exception e) {
		throw new Exception("McPopulate: "+e);
	} catch(Throwable t) {
		throw new Exception("McPopulate: "+t);
	} finally {
		try {
	 		if(stmt!=null){ stmt.close(); }
		} catch(SQLException se){ }
	}
}


}
