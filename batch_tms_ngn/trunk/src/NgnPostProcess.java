import java.io.*;
import java.util.*;
import java.sql.*;
import edu.ucsd.act.db.connectionpool.*;

/**
 * When the Set Period Status process changes the status of the current period from
 * "locked" to "processed", this process is invoked to create copies of rows in all reference
 * tables (ngn_redirect, ngn_title, ngn_location, ngn_maildrop, ngn_rate,
 * ngn_fund, ngn_revenue, and ngn_cu_type) with that period's process_month, for the
 * next process_month. This process cannot commence
 * unless a row exists in the ngn_period_table for the new peroid and the status of the
 * period is 'pre-active' (ngn_period_status_code = '0'). When all copies are complete,
 * the status of the new period is set to 'active'. This process can and should be viewed as
 * a sub-process of 'Set Period Status'
 */
public class NgnPostProcess {

private static Connection conn = null;
private static String db = "ngn_db";

private int process_first_month = 0;
private int process_month = 0;
private String status = "";

public NgnPostProcess(Connection conn, int process_first_month, int process_month) {
	this.conn = conn;
	this.process_first_month = process_first_month;
	this.process_month = process_month;
}

public void run()
	throws Exception
{
	try {
		//first close the current process month and set the next to active
		String sql = "update ngn.ngn_period set ngn_period_status_code = '9' where ngn_period_process_month = "+process_month;
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();

		sql = "select ngn_period_process_month from ngn.ngn_period where ngn_period_status_code = '0' order by ngn_period_process_month";
		stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		if(rs.next()) {
			int new_process_month = rs.getInt(1);

			sql = "update ngn.ngn_period set ngn_period_status_code = '1' where ngn_period_process_month = "+new_process_month;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for ngn_redirect
			sql = ""
				+"insert into ngn.ngn_redirect ( "
				+"	ngn_redir_process_month, "
				+"	ngn_redir_pay_index, "
				+"	ngn_redir_employee_id, "
				+"	ngn_redir_recharge_index, "
				+"	ngn_redir_pay_fund, "
				+"	ngn_redir_pay_orgn, "
				+"	ngn_redir_pay_prog, "
				+"	ngn_redir_recharge_fund, "
				+"	ngn_redir_recharge_orgn, "
				+"	ngn_redir_recharge_prog, "
				+"	ngn_redir_employee_name, "
				+"	ngn_redir_change_source, "
				+"	ngn_redir_change_userid, "
				+"	ngn_redir_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	ngn_redir_pay_index, "
				+"	ngn_redir_employee_id, "
				+"	ngn_redir_recharge_index, "
				+"	ngn_redir_pay_fund, "
				+"	ngn_redir_pay_orgn, "
				+"	ngn_redir_pay_prog, "
				+"	ngn_redir_recharge_fund, "
				+"	ngn_redir_recharge_orgn, "
				+"	ngn_redir_recharge_prog, "
				+"	ngn_redir_employee_name, "
				+"	ngn_redir_change_source, "
				+"	ngn_redir_change_userid, "
				+"	ngn_redir_change_date "
				+"from ngn.ngn_redirect "
				+"where ngn_redir_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for ngn_title
			sql = ""
				+"insert into ngn.ngn_title ( "
				+"	ngn_title_process_month, "
				+"	ngn_title_title_code, "
				+"	ngn_title_cu_category_code, "
				+"	ngn_title_description, "
				+"	ngn_title_CTO_code, "
				+"	ngn_title_CTO_description, "
				+"	ngn_title_change_userid, "
				+"	ngn_title_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	ngn_title_title_code, "
				+"	ngn_title_cu_category_code, "
				+"	ngn_title_description, "
				+"	ngn_title_CTO_code, "
				+"	ngn_title_CTO_description, "
				+"	ngn_title_change_userid, "
				+"	ngn_title_change_date "
				+"from ngn.ngn_title "
				+"where ngn_title_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for ngn_location
			sql = ""
				+"insert into ngn.ngn_location ( "
				+"	ngn_loc_process_month, "
				+"	ngn_loc_loc_code, "
				+"	ngn_loc_loc_group_code, "
				+"	ngn_loc_change_userid, "
				+"	ngn_loc_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	ngn_loc_loc_code, "
				+"	ngn_loc_loc_group_code, "
				+"	ngn_loc_change_userid, "
				+"	ngn_loc_change_date "
				+"from ngn.ngn_location "
				+"where ngn_loc_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for ngn_maildrop
			sql = ""
				+"insert into ngn.ngn_maildrop ( "
				+"	ngn_mail_process_month, "
				+"	ngn_mail_mail_code_lo, "
				+"	ngn_mail_mail_code_hi, "
				+"	ngn_mail_loc_group_code, "
				+"	ngn_mail_change_userid, "
				+"	ngn_mail_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	ngn_mail_mail_code_lo, "
				+"	ngn_mail_mail_code_hi, "
				+"	ngn_mail_loc_group_code, "
				+"	ngn_mail_change_userid, "
				+"	ngn_mail_change_date "
				+"from ngn.ngn_maildrop "
				+"where ngn_mail_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for ngn_rate
			sql = ""
				+"insert into ngn.ngn_rate ( "
				+"	ngn_rate_process_month, "
				+"	ngn_rate_loc_group_code, "
				+"	ngn_rate_cu_type_code, "
				+"	ngn_rate_group_code, "
				+"	ngn_rate_group_desc, "
				+"	ngn_rate_amount, "
				+"	ngn_rate_change_userid, "
				+"	ngn_rate_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	ngn_rate_loc_group_code, "
				+"	ngn_rate_cu_type_code, "
				+"	ngn_rate_group_code, "
				+"	ngn_rate_group_desc, "
				+"	ngn_rate_amount, "
				+"	ngn_rate_change_userid, "
				+"	ngn_rate_change_date "
				+"from ngn.ngn_rate "
				+"where ngn_rate_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for ngn_fund
			sql = ""
				+"insert into ngn.ngn_fund ( "
				+"	ngn_fund_process_month, "
				+"	ngn_fund_fund_lo, "
				+"	ngn_fund_fund_hi, "
				+"	ngn_fund_category_code, "
				+"	ngn_fund_change_userid, "
				+"	ngn_fund_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	ngn_fund_fund_lo, "
				+"	ngn_fund_fund_hi, "
				+"	ngn_fund_category_code, "
				+"	ngn_fund_change_userid, "
				+"	ngn_fund_change_date "
				+"from ngn.ngn_fund "
				+"where ngn_fund_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for ngn_revenue
			sql = ""
				+"insert into ngn.ngn_revenue ( "
				+"	ngn_revenue_process_month, "
				+"	ngn_revenue_loc_group_code, "
				+"	ngn_revenue_index, "
				+"	ngn_revenue_credit_debit, "
				+"	ngn_revenue_share_factor, "
				+"	ngn_revenue_fund, "
				+"	ngn_revenue_orgn, "
				+"	ngn_revenue_acct, "
				+"	ngn_revenue_prog, "
				+"	ngn_revenue_ledger_desc, "
				+"	ngn_revenue_change_userid, "
				+"	ngn_revenue_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	ngn_revenue_loc_group_code, "
				+"	ngn_revenue_index, "
				+"	ngn_revenue_credit_debit, "
				+"	ngn_revenue_share_factor, "
				+"	ngn_revenue_fund, "
				+"	ngn_revenue_orgn, "
				+"	ngn_revenue_acct, "
				+"	ngn_revenue_prog, "
				+"	ngn_revenue_ledger_desc, "
				+"	ngn_revenue_change_userid, "
				+"	ngn_revenue_change_date "
				+"from ngn.ngn_revenue "
				+"where ngn_revenue_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move forward rows for ngn_cu_type
			sql = ""
				+"insert into ngn.ngn_cu_type ( "
				+"	ngn_cu_type_process_month, "
				+"	ngn_cu_type_title_code, "
				+"	ngn_cu_type_dos_code, "
				+"	ngn_cu_type_rule_code, "
				+"	ngn_cu_type_fte_equiv_amt, "
				+"	ngn_cu_type_cu_type_code, "
				+"	ngn_cu_type_change_userid, "
				+"	ngn_cu_type_change_date "
				+") "
				+"select "
				+"	"+new_process_month+", "
				+"	ngn_cu_type_title_code, "
				+"	ngn_cu_type_dos_code, "
				+"	ngn_cu_type_rule_code, "
				+"	ngn_cu_type_fte_equiv_amt, "
				+"	ngn_cu_type_cu_type_code, "
				+"	ngn_cu_type_change_userid, "
				+"	ngn_cu_type_change_date "
				+"from ngn.ngn_cu_type "
				+"where ngn_cu_type_process_month = "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
		}
	} catch(Exception e) {
		throw new Exception("NgnPostProcess: "+e);
	} catch(Throwable t) {
		throw new Exception("NgnPostProcess: "+t);
	}
}

}
