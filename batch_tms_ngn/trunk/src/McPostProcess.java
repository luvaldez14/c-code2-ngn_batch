import java.io.*;
import java.util.*;
import java.sql.*;
import edu.ucsd.act.db.connectionpool.*;

public class McPostProcess {

private static Connection conn = null;
private static String db = "ngn_db";

private int process_first_month = 0;
private int process_month = 0;
private String status = "";

public McPostProcess(Connection conn, int process_first_month, int process_month) {
	this.conn = conn;
	this.process_first_month = process_first_month;
	this.process_month = process_month;
}

public void run()
	throws Exception
{
	try {
		//first close the current process month and set the next to active
		String sql = "update ngn.mc_period set mc_period_status_code = '9' where mc_period_process_month = "+process_month;
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();

		sql = "select mc_period_process_month from ngn.mc_period where mc_period_status_code = '0' order by mc_period_process_month";
		stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		if(rs.next()) {
			int new_process_month = rs.getInt(1);

			sql = "update ngn.mc_period set mc_period_status_code = '1' where mc_period_process_month = "+new_process_month;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for mc_rate
			sql = ""
				+"insert into ngn.mc_rate ( "
				+"	mc_rate_process_month, "
				+"	mc_rate_loc_group_code, "
				+"	mc_rate_cu_type_code, "
				+"	mc_rate_group_code, "
				+"	mc_rate_group_desc, "
				+"	mc_rate_amount, "
				+"	mc_rate_change_userid, "
				+"	mc_rate_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	mc_rate_loc_group_code, "
				+"	mc_rate_cu_type_code, "
				+"	mc_rate_group_code, "
				+"	mc_rate_group_desc, "
				+"	mc_rate_amount, "
				+"	mc_rate_change_userid, "
				+"	mc_rate_change_date "
				+"from ngn.mc_rate "
				+"where mc_rate_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for mc_revenue
			sql = ""
				+"insert into ngn.mc_revenue ( "
				+"	mc_revenue_process_month, "
				+"	mc_revenue_loc_group_code, "
				+"	mc_revenue_index, "
				+"	mc_revenue_credit_debit, "
				+"	mc_revenue_share_factor, "
				+"	mc_revenue_fund, "
				+"	mc_revenue_orgn, "
				+"	mc_revenue_acct, "
				+"	mc_revenue_prog, "
				+"	mc_revenue_ledger_desc, "
				+"	mc_revenue_change_userid, "
				+"	mc_revenue_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	mc_revenue_loc_group_code, "
				+"	mc_revenue_index, "
				+"	mc_revenue_credit_debit, "
				+"	mc_revenue_share_factor, "
				+"	mc_revenue_fund, "
				+"	mc_revenue_orgn, "
				+"	mc_revenue_acct, "
				+"	mc_revenue_prog, "
				+"	mc_revenue_ledger_desc, "
				+"	mc_revenue_change_userid, "
				+"	mc_revenue_change_date "
				+"from ngn.mc_revenue "
				+"where mc_revenue_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
		}
	} catch(Exception e) {
		throw new Exception("McPostProcess: "+e);
	} catch(Throwable t) {
		throw new Exception("McPostProcess: "+t);
	}
}

}
