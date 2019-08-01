import java.text.DecimalFormat;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import java.sql.*;

import edu.ucsd.act.db.connectionpool.*;
import edu.ucsd.act.jlink.util.JLinkDate;

import java.io.PrintWriter;
import java.io.FileWriter;

import com.javaunderground.jdbc.DebugLevel;
import com.javaunderground.jdbc.StatementFactory;


/**
 * The goal of this process is to develop new current period charges, and differential charges for
 * prior periods caused by (a) changes to teh CU (fte) adjustments incomming from payroll processing, or
 * (b) retroactive changes to control table parameters. The reason for basing incremental charges
 * for prior periods on changes relative to existing 'prev' values is that these represent cumulative
 * charges that have already been posted to the General Ledger.
 *
 * This process involves extracting all ngn_history rows within the 'current processing window'
 * where there is a difference between the 'current' and 'prev' recharge amount fields. The resulting
 * ngn_recharge rows will be developed based on ngn_history information accumulated at the
 * level of all ngn_history primary key elements except for ngn_history_process_month and
 * ngng_history_pay_index. Differential fte and amount quantities for the current period will be
 * summed into ngn_recharge 'current' fields and quantities from prior periods will be summed into
 * ngn_recharge 'prior' fields. This may be accomplished using various techniques. I will use one
 * in the following description only for the sake of illustration.
 *
 * The ngn_recharge rows developed in this process are inserted to a temporary table that is part
 * of the NGN Recharge database. The information is later unloaded and passed to the Data
 * Warehouse as one of the 'publishing' tasks described in Section C.9.
 */
public class McGenerate {

private static boolean DEBUG = false;

private Connection conn = null;
private String db = "ngn_db";

private int process_first_month = 0;
private int process_month = 0;
private int financial_month = 0;

public McGenerate(Connection conn, int process_first_month, int process_month, int financial_month)
	throws Exception
{
	this.conn = conn;
	this.process_first_month = process_first_month;
	this.process_month = process_month;
	this.financial_month = financial_month;
}

public void run()
	throws Exception
{
	Statement stmt = null;
	String sql = null;

	try {
		stmt = conn.createStatement();

		//initialize table
		sql = "delete from ngn.mc_recharge";
		stmt.executeUpdate(sql);

		//create temp table
		sql = ""
			+"DECLARE GLOBAL TEMPORARY TABLE session.mc_recharge ( "
			+"    mc_history_index char(10) not null, "
			+"    mc_history_group_code char(2) not null, "
			+"    mc_history_employee_id char(9) not null, "
			+"    mc_history_title_code char(4) not null, "
			+"    mc_history_pay_activity_code char(1) not null, "
			+"    recharge_current_fte decimal(9,4) not null, "
			+"    recharge_current_amount decimal(9,2) not null, "
			+"    recharge_prior_fte decimal(9,4) not null, "
			+"    recharge_prior_amount decimal(9,2) not null, "
			+"    recharge_total_amount decimal(9,2) not null, "
			+"    ifis_title varchar(35) not null, "
			+"    fund_title varchar(35) not null, "
			+"    orgn_title varchar(35) not null, "
			+"    orgn_mgr varchar(35) not null, "
			+"    orgn_mgr_addr varchar(35) not null, "
			+"    program_title varchar(35) not null, "
			+"    mc_history_process_month int not null, "
			+"    mc_history_pay_index char(10) "
			+") ON COMMIT PRESERVE ROWS NOT LOGGED "
		;
		stmt.executeUpdate(sql);

		//insert intial records into temp table. Pay Index will be set in an update following this
		sql = ""
			+"insert into session.mc_recharge ( "
			+"    mc_history_index, "
			+"    mc_history_group_code, "
			+"    mc_history_employee_id, "
			+"    mc_history_title_code, "
			+"    mc_history_pay_activity_code, "
			+"    recharge_current_fte, "
			+"    recharge_current_amount, "
			+"    recharge_prior_fte, "
			+"    recharge_prior_amount, "
			+"    recharge_total_amount, "
			+"    ifis_title, "
			+"    fund_title, "
			+"    orgn_title, "
			+"    orgn_mgr, "
			+"    orgn_mgr_addr, "
			+"    program_title, "
			+"    mc_history_process_month "
			+") "
			+"select "
			+"    m.mc_history_index, "
			+"    m.mc_history_group_code, "
			+"    m.mc_history_employee_id, "
			+"    m.mc_history_title_code, "
			+"    m.mc_history_pay_activity_code, "
			+"    coalesce(sum(case when m.mc_history_process_month = "+process_month+" then n.ngn_history_current_adj_fte - n.ngn_history_prev_adj_fte else 0 end),0), "
			+"    coalesce(sum(case when m.mc_history_process_month = "+process_month+" then m.mc_history_current_amount - m.mc_history_prev_amount else 0 end),0), "
			+"    coalesce(sum(case when m.mc_history_process_month != "+process_month+" then n.ngn_history_current_adj_fte - n.ngn_history_prev_adj_fte else 0 end),0), "
			+"    coalesce(sum(case when m.mc_history_process_month != "+process_month+" then m.mc_history_current_amount - m.mc_history_prev_amount else 0 end),0), "
			+"    coalesce(sum(case when m.mc_history_process_month = "+process_month+" then m.mc_history_current_amount - m.mc_history_prev_amount else 0 end),0) "
			+"    	+ coalesce(sum(case when m.mc_history_process_month != "+process_month+" then m.mc_history_current_amount - m.mc_history_prev_amount else 0 end),0), "
			+"    coalesce(i.ifis_title,''), "
			+"    coalesce(f.fund_title,''), "
			+"    coalesce(o.orgn_title,''), "
			+"    coalesce(o.orgn_mgr,''), "
			+"    coalesce(o.orgn_mgr_addr,''), "
			+"    coalesce(p.program_title,''), "
			+"    max(m.mc_history_process_month) "
			+"from ngn.mc_history m "
			+"join ngn.ngn_history n on "
			+"	    mc_history_employee_id = ngn_history_employee_id "
			+"	and mc_history_process_month = ngn_history_process_month "
			+"	and mc_history_title_code = ngn_history_title_code "
			+"	and mc_history_index = ngn_history_index "
			+"	and mc_history_pay_activity_code = ngn_history_pay_activity_code "
			+"	and mc_history_pay_index = ngn_history_pay_index "
			+"left outer join ngn.tms_ifopl_v i on i.ifis_indx_no = m.mc_history_index "
			+"left outer join ngn.coa_fund_v f on f.fund_code = n.ngn_history_fund "
			+"left outer join ngn.tms_org_v o on o.orgn_code = n.ngn_history_organization "
			+"left outer join ngn.coa_program_v p on p.program = n.ngn_history_program "
			+"where mc_history_process_month between "+process_first_month+" and "+process_month+" "
			+"  and mc_history_current_amount != mc_history_prev_amount "
			+"  and (mc_history_current_amount - mc_history_prev_amount) not between -0.01 and 0.01 "
			+"group by "
			+"    m.mc_history_index, "
			+"    m.mc_history_group_code, "
			+"    m.mc_history_employee_id, "
			+"    m.mc_history_title_code, "
			+"    m.mc_history_pay_activity_code, "
			+"    coalesce(i.ifis_title,''), "
			+"    coalesce(f.fund_title,''), "
			+"    coalesce(o.orgn_title,''), "
			+"    coalesce(o.orgn_mgr,''), "
			+"    coalesce(o.orgn_mgr_addr,''), "
			+"    coalesce(p.program_title,'') "
		;
		stmt.executeUpdate(sql);

		//Choose which pay index should be set for employees
		sql = ""
			+"update session.mc_recharge m "
			+"set "
			+"    mc_history_pay_index = ( "
			+"    select max(mc_history_pay_index) "
			+"    from ngn.mc_history m2 "
			+"    where m.mc_history_index = m2.mc_history_index "
			+"      and m.mc_history_group_code = m2.mc_history_group_code "
			+"      and m.mc_history_employee_id = m2.mc_history_employee_id "
			+"      and m.mc_history_title_code = m2.mc_history_title_code "
			+"      and m.mc_history_pay_activity_code = m2.mc_history_pay_activity_code "
			+"      and m2.mc_history_process_month = m.mc_history_process_month "
			+"      and m2.mc_history_current_amount = ( "
			+"        select max(m3.mc_history_current_amount) "
			+"        from ngn.mc_history m3 "
			+"        where m2.mc_history_index = m3.mc_history_index "
			+"          and m2.mc_history_group_code = m3.mc_history_group_code "
			+"          and m2.mc_history_employee_id = m3.mc_history_employee_id "
			+"          and m2.mc_history_title_code = m3.mc_history_title_code "
			+"          and m2.mc_history_pay_activity_code = m3.mc_history_pay_activity_code "
			+"          and m3.mc_history_process_month = m2.mc_history_process_month)) "
		;
		stmt.executeUpdate(sql);

		//Insert results into mc_recharge table
		sql = ""
			+"insert into ngn.mc_recharge ( "
			+"    recharge_accounting_period, "
			+"    recharge_index, "
			+"    recharge_group_code, "
			+"    recharge_employee_id, "
			+"    recharge_pay_title_code, "
			+"    recharge_pay_activity_code, "
			+"    recharge_current_group_rate, "
			+"    recharge_current_fte, "
			+"    recharge_current_amount, "
			+"    recharge_prior_fte, "
			+"    recharge_prior_amount, "
			+"    recharge_total_amount, "
			+"    recharge_multiple_entry_ind, "
			+"    recharge_index_subs_ind, "
			+"    recharge_fund, "
			+"    recharge_fund_category_code, "
			+"    recharge_organization, "
			+"    recharge_program, "
			+"    recharge_pay_index, "
			+"    recharge_pay_fund, "
			+"    recharge_pay_organization, "
			+"    recharge_pay_program, "
			+"    recharge_employee_name, "
			+"    recharge_group_desc, "
			+"    recharge_home_dept_code, "
			+"    recharge_home_dept_desc, "
			+"    recharge_vcu_code, "
			+"    recharge_vcu_desc, "
			+"    recharge_index_desc, "
			+"    recharge_fund_desc, "
			+"    recharge_organization_desc, "
			+"    recharge_program_desc, "
			+"    recharge_financial_mgr_name, "
			+"    recharge_financial_mgr_addr, "
			+"    recharge_calendar_year "
			+") "
			+"select "
			+"    "+financial_month+", "
			+"    m.mc_history_index, "
			+"    m.mc_history_group_code, "
			+"    m.mc_history_employee_id, "
			+"    m.mc_history_title_code, "
			+"    m.mc_history_pay_activity_code, "
			+"    m2.mc_history_current_group_rate, "
			+"    m.recharge_current_fte, "
			+"    m.recharge_current_amount, "
			+"    m.recharge_prior_fte, "
			+"    m.recharge_prior_amount, "
			+"    m.recharge_total_amount, "
			+"    ' ', "
			+"    n.ngn_history_index_subs_ind, "
			+"    n.ngn_history_fund, "
			+"    n.ngn_history_fund_category_code, "
			+"    n.ngn_history_organization, "
			+"    n.ngn_history_program, "
			+"    n.ngn_history_pay_index, "
			+"    n.ngn_history_pay_fund, "
			+"    n.ngn_history_pay_organization, "
			+"    n.ngn_history_pay_program, "
			+"    n.ngn_history_employee_name, "
			+"    n.ngn_history_group_desc, "
			+"    n.ngn_history_home_dept_code, "
			+"    n.ngn_history_home_dept_desc, "
			+"    case when n.ngn_history_vcu_code is null then '  ' else case when length(n.ngn_history_vcu_code) = 2 then right(n.ngn_history_vcu_code,1)||' ' end end, "
			+"    n.ngn_history_vcu_desc, "
			+"    m.ifis_title, "
			+"    m.fund_title, "
			+"    m.orgn_title, "
			+"    m.program_title, "
			+"    m.orgn_mgr, "
			+"    m.orgn_mgr_addr, "
			+"    '"+process_month+"' "
			+"from session.mc_recharge m "
			+"join ngn.mc_history m2 "
			+"    on  m.mc_history_index = m2.mc_history_index "
			+"    and m.mc_history_group_code = m2.mc_history_group_code "
			+"    and m.mc_history_employee_id = m2.mc_history_employee_id "
			+"    and m.mc_history_title_code = m2.mc_history_title_code "
			+"    and m.mc_history_pay_activity_code = m2.mc_history_pay_activity_code "
			+"    and m2.mc_history_pay_index = m.mc_history_pay_index "
			+"    and m2.mc_history_process_month = m.mc_history_process_month "
			+"join ngn.ngn_history n on "
			+"	    m.mc_history_employee_id = ngn_history_employee_id "
			+"	and m.mc_history_process_month = ngn_history_process_month "
			+"	and m.mc_history_title_code = ngn_history_title_code "
			+"	and m.mc_history_index = ngn_history_index "
			+"	and m.mc_history_pay_activity_code = ngn_history_pay_activity_code "
			+"	and m.mc_history_pay_index = ngn_history_pay_index "
			+"order by "
			+"    mc_history_employee_id, "
			+"    mc_history_title_code, "
			+"    mc_history_index, "
			+"    mc_history_pay_activity_code, "
			+"    mc_history_group_code "
		;
		McControl.recharge_rows_inserted = stmt.executeUpdate(sql);

		//clean up temp table
		stmt.executeUpdate("drop table session.mc_recharge");

		//updating multiple entry id field
		sql = ""
			+"update	ngn.mc_recharge "
			+"set		recharge_multiple_entry_ind = '*' "
			+"where	recharge_employee_id in ( "
			+"	select		recharge_employee_id "
			+"	from		ngn.mc_recharge "
			+"	group by	recharge_employee_id "
			+"	having		count(*) > 1 "
			+") "
		;
		stmt.executeUpdate(sql);

	} catch(Exception e) {
		throw new Exception("McGenerate: "+e);
	} catch(Throwable t) {
		throw new Exception("McGenerate: "+t);
	} finally {
		try {
	 		if(stmt!=null){ stmt.close(); }
		} catch(SQLException se){ }
	}
}

}