import java.text.DecimalFormat;

import java.util.Hashtable;
import java.util.StringTokenizer;

import java.sql.*;

import edu.ucsd.act.db.connectionpool.*;
import edu.ucsd.act.jlink.util.JLinkDate;

import java.io.PrintWriter;
import java.io.FileWriter;

/**
 * The function of this process is to update the ngn_history_current_fte fields with fte (and rate)
 * information from the monthly payroll activity supplied via the ngn_payroll table that is produced
 * in the Payroll Activity Extract process. It should be noted again that any month's set of
 * payroll activity will contain activity both for the current (latest) process_month and for prior
 * process_month's, even though the great majority of the activity is for the current month. This
 * preponderance of current month activity will mean that most activity will require the creation of
 * new ngn_history rows, with fewer cases resulting in the updating of existing rows (for prior
 * months).
 */
public class NgnCuPosting {

private Connection conn = null;
private String db = "ngn_db";

private DecimalFormat precision4 = new DecimalFormat( "#0.0000" );

private int process_first_month = 0;
private int process_month = 0;

public NgnCuPosting(Connection conn, int process_first_month, int process_month)
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
		//Obtain counts for matches on ngn_employee table data
		conn.setAutoCommit(false);
		stmt = conn.createStatement();
		try {
			stmt.executeUpdate(""
				+"DECLARE GLOBAL TEMPORARY TABLE session.ngn_cu_counts ( "
				+"	match int not null, "
				+"	match_plus1 int not null, "
				+"	match_partial int not null, "
				+"	match_partial_plus1 int not null, "
				+"	num_records int not null, "
				+"	employee_id char(9) not null, "
				+"	process_month int not null, "
				+"	title_code char(4) not null, "
				+"	recharge_index char(10) not null, "
				+"	activity_code char(1) not null, "
				+"	pay_index char(10) not null "
				+") ON COMMIT PRESERVE ROWS NOT LOGGED "
			);

			stmt.executeUpdate(""
				+"insert into session.ngn_cu_counts "
				+"select "
				+"	case when count(ne1.ngn_empl_employee_id) > 0 then 1 else 0 end, "
				+"	case when count(ne2.ngn_empl_employee_id) > 0 then 1 else 0 end, "
				+"	case when count(ne3.ngn_empl_employee_id) > 0 then 1 else 0 end, "
				+"	case when count(ne4.ngn_empl_employee_id) > 0 then 1 else 0 end, "
				+"	case when count(*) > 0 then 1 else 0 end, "
				+"	ngn_pay_employee_id, "
				+"	ngn_pay_process_month, "
				+"	ngn_pay_title_code, "
				+"	coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index), "
				+"	ngn_pay_pay_activity_code, "
				+"	ngn_pay_index "
				+"from ngn.ngn_payroll "
				+"left outer join ngn.ngn_redirect nr1 on nr1.ngn_redir_pay_index = ngn_pay_index and nr1.ngn_redir_process_month = ngn_pay_process_month and nr1.ngn_redir_employee_id = ngn_pay_employee_id "
				+"left outer join ngn.ngn_redirect nr2 on nr1.ngn_redir_process_month is null and nr2.ngn_redir_pay_index = ngn_pay_index and nr2.ngn_redir_process_month = ngn_pay_process_month and nr2.ngn_redir_employee_id = '' "
				+"left outer join ngn.ngn_employee ne1 on ne1.ngn_empl_employee_id = ngn_pay_employee_id and ne1.ngn_empl_process_month = ngn_pay_process_month and ne1.ngn_empl_title_code = ngn_pay_title_code "
				+"left outer join ngn.ngn_employee ne2 on ne1.ngn_empl_employee_id is null and ngn_pay_process_month != "+process_month+" and ne2.ngn_empl_employee_id = ngn_pay_employee_id and ne2.ngn_empl_process_month = ((( mod(ngn_pay_process_month,100) + 88) / 100 * 100) + ( mod(( mod(ngn_pay_process_month,100)),12) + 1) + (ngn_pay_process_month / 100 * 100)) and ne2.ngn_empl_title_code = ngn_pay_title_code "
				+"left outer join ngn.ngn_employee ne3 on ne1.ngn_empl_employee_id is null and ne2.ngn_empl_employee_id is null and ne3.ngn_empl_employee_id = ngn_pay_employee_id and ne3.ngn_empl_process_month = ngn_pay_process_month "
				+"left outer join ngn.ngn_employee ne4 on ne1.ngn_empl_employee_id is null and ne2.ngn_empl_employee_id is null and ne3.ngn_empl_employee_id is null and ngn_pay_process_month != "+process_month+" and ne4.ngn_empl_employee_id = ngn_pay_employee_id and ne4.ngn_empl_process_month = ((( mod(ngn_pay_process_month,100) + 88) / 100 * 100) + ( mod(( mod(ngn_pay_process_month,100)),12) + 1) + (ngn_pay_process_month / 100 * 100)) "
				+"left outer join ngn.ngn_history on ngn_history_employee_id = ngn_pay_employee_id and ngn_history_process_month = ngn_pay_process_month and ngn_history_title_code = ngn_pay_title_code and ngn_history_index = coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index) and ngn_history_pay_index = ngn_pay_index and ngn_history_pay_activity_code = ngn_pay_pay_activity_code "
				+"where "
				+"	(ngn_pay_process_month = "+process_month+" "
				+"	 or (ngn_pay_process_month between "+process_first_month+" and "+process_month+" "
				+"	     and ngn_pay_posting_month = "+process_month+")) "
				+"	and (ngn_pay_dos_type = 'NR01' or ngn_pay_dos_code in ('FEL','FES','FEN')) "
				+"	and ngn_history_employee_id is null "
				+"group by "
				+"	ngn_pay_employee_id, "
				+"	ngn_pay_process_month, "
				+"	ngn_pay_title_code, "
				+"	coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index), "
				+"	ngn_pay_pay_activity_code, "
				+"	ngn_pay_index "
			);

			sql = ""
				+"select "
				+"	sum(match) as match, "
				+"	sum(match_plus1) as match_plus1, "
				+"	sum(match_partial) as match_partial, "
				+"	sum(match_partial_plus1) as match_partial_plus1, "
				+"	sum(num_records) as num_records "
				+"from session.ngn_cu_counts "
			;

			ResultSet counts = stmt.executeQuery(sql);

			NgnControl.out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> counting done");
			counts.next();
			NgnControl.empl_match_title_month = counts.getInt("match");
			NgnControl.empl_match_title_month_plus = counts.getInt("match_plus1");
			NgnControl.empl_match_part_month = counts.getInt("match_partial");
			NgnControl.empl_match_part_month_plus = counts.getInt("match_partial_plus1");
			NgnControl.empl_match_none = counts.getInt("num_records");
			NgnControl.empl_match_none = NgnControl.empl_match_none
				- NgnControl.empl_match_title_month
				- NgnControl.empl_match_title_month_plus
				- NgnControl.empl_match_part_month
				- NgnControl.empl_match_part_month_plus;
			counts.close();
		} catch(Exception e) {
			System.out.println("SQL Exception: "+e);
		} finally {
		  conn.commit();
		}
		stmt.executeUpdate("drop table session.ngn_cu_counts");
		stmt.close();
		conn.setAutoCommit(true);

		//Updating records
		conn.setAutoCommit(false);
		stmt = conn.createStatement();
		try {
			stmt.executeUpdate(""
				+"DECLARE GLOBAL TEMPORARY TABLE session.ngn_add_to_history ( "
				+"	fte decimal(9,4) not null, "
				+"	employee_id char(9) not null, "
				+"	process_month int not null, "
				+"	title_code char(4) not null, "
				+"	recharge_index char(10) not null, "
				+"	activity_code char(1) not null, "
				+"	pay_index char(10) not null "
				+") ON COMMIT PRESERVE ROWS NOT LOGGED "
			);
			sql = ""
				+"insert into session.ngn_add_to_history (fte,employee_id,process_month,title_code,recharge_index,activity_code,pay_index) "
				+"select "
				+"	round(sum(case when ngn_cu_type_cu_type_code is not null and ngn_cu_type_rule_code = 'A' then ngn_pay_amount / ngn_cu_type_fte_equiv_amt else ngn_pay_fte end),4), "
				+"	ngn_pay_employee_id, "
				+"	ngn_pay_process_month, "
				+"	ngn_pay_title_code, "
				+"	coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index), "
				+"	ngn_pay_pay_activity_code, "
				+"	ngn_pay_index "
				+"from ngn.ngn_payroll "
				+"left outer join ngn.ngn_redirect nr1 on nr1.ngn_redir_pay_index = ngn_pay_index and nr1.ngn_redir_process_month = ngn_pay_process_month and nr1.ngn_redir_employee_id = ngn_pay_employee_id "
				+"left outer join ngn.ngn_redirect nr2 on nr1.ngn_redir_process_month is null and nr2.ngn_redir_pay_index = ngn_pay_index and nr2.ngn_redir_process_month = ngn_pay_process_month and nr2.ngn_redir_employee_id = '' "
				+"left outer join ngn.ngn_cu_type on ngn_cu_type_process_month = ngn_pay_process_month and ngn_cu_type_title_code = ngn_pay_title_code and ngn_cu_type_dos_code = ngn_pay_dos_code "
				+"left outer join ngn.ngn_history on ngn_history_employee_id = ngn_pay_employee_id and ngn_history_process_month = ngn_pay_process_month and ngn_history_title_code = ngn_pay_title_code and ngn_history_index = coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index) and ngn_history_pay_index = ngn_pay_index and ngn_history_pay_activity_code = ngn_pay_pay_activity_code "
				+"where "
				+"	(ngn_pay_process_month = "+process_month+" "
				+"	 or (ngn_pay_process_month between "+process_first_month+" and "+process_month+" "
				+"	     and ngn_pay_posting_month = "+process_month+")) "
				+"	and (ngn_pay_dos_type = 'NR01' or ngn_pay_dos_code in ('FEL','FES','FEN')) "
				+"	and ngn_history_employee_id is not null "
				+"group by "
				+"	ngn_pay_employee_id, "
				+"	ngn_pay_process_month, "
				+"	ngn_pay_title_code, "
				+"	coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index), "
				+"	ngn_pay_pay_activity_code, "
				+"	ngn_pay_index "
			;
			stmt.executeUpdate(sql);

			sql = ""
				+"merge into ngn.ngn_history n "
				+"using session.ngn_add_to_history n2 on "
				+"	n.ngn_history_employee_id = n2.employee_id and "
				+"	n.ngn_history_process_month = n2.process_month and "
				+"	n.ngn_history_title_code = n2.title_code and "
				+"	n.ngn_history_index = n2.recharge_index and "
				+"	n.ngn_history_pay_activity_code = n2.activity_code and "
				+"	n.ngn_history_pay_index = n2.pay_index "
				+"when matched then "
				+"update set "
				+"	n.ngn_history_current_fte     = n.ngn_history_current_fte + n2.fte, "
				+"	n.ngn_history_current_adj_fte = n.ngn_history_current_fte + n2.fte "
			;
			NgnControl.rows_updated = stmt.executeUpdate(sql);

		} catch(Exception e) {
			System.out.println("SQL Exception: "+e);
		} finally {
		  conn.commit();
		}
		stmt.executeUpdate("drop table session.ngn_add_to_history");
		stmt.close();
		conn.setAutoCommit(true);
		NgnControl.out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> updating done");

		sql = ""
			+"insert into ngn.ngn_history ( "
			+"	ngn_history_employee_id, "
			+"	ngn_history_process_month, "
			+"	ngn_history_title_code, "
			+"	ngn_history_index, "
			+"	ngn_history_pay_activity_code, "
			+"	ngn_history_ttl_category_code, "
			+"	ngn_history_appt_type_code, "
			+"	ngn_history_stu_status_code, "
			+"	ngn_history_cu_type_code, "
			+"	ngn_history_tms_location_code, "
			+"	ngn_history_empl_mail_code, "
			+"	ngn_history_current_group_rate, "
			+"	ngn_history_current_fte, "
			+"	ngn_history_current_adj_fte, "
			+"	ngn_history_current_amount, "
			+"	ngn_history_prev_group_rate, "
			+"	ngn_history_prev_fte, "
			+"	ngn_history_prev_adj_fte, "
			+"	ngn_history_prev_amount, "
			+"	ngn_history_index_subs_ind, "
			+"	ngn_history_fund, "
			+"	ngn_history_fund_category_code, "
			+"	ngn_history_organization, "
			+"	ngn_history_program, "
			+"	ngn_history_pay_index, "
			+"	ngn_history_pay_fund, "
			+"	ngn_history_pay_organization, "
			+"	ngn_history_pay_program, "
			+"	ngn_history_employee_name, "
			+"	ngn_history_home_dept_code, "
			+"	ngn_history_home_dept_desc, "
			+"	ngn_history_vcu_code, "
			+"	ngn_history_vcu_desc "
			+") "
			+"select "
			+"	ngn_pay_employee_id, "
			+"	ngn_pay_process_month, "
			+"	ngn_pay_title_code, "
			+"	coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index), "
			+"	ngn_pay_pay_activity_code, "
			+"	'-', "
			+"	coalesce(ne1.ngn_empl_appt_type_code,ne2.ngn_empl_appt_type_code), "
			//Ended up not being a 1-1 join. Added a min() function and removed from group by
			+"	min(coalesce(ne1.ngn_empl_student_status_code,ne2.ngn_empl_student_status_code,ne3.ngn_empl_student_status_code,ne4.ngn_empl_student_status_code)), "
			+"	coalesce(ngn_cu_type_cu_type_code,'EMPL'), "
			+"	coalesce(ne1.ngn_empl_tms_location_code,ne2.ngn_empl_tms_location_code,ne3.ngn_empl_tms_location_code,ne4.ngn_empl_tms_location_code,dir_loc_code), "
			+"	coalesce(ne1.ngn_empl_mail_code,ne2.ngn_empl_mail_code,ne3.ngn_empl_mail_code,ne4.ngn_empl_mail_code,emp_mailcode), "
			+"	0, "
			+"	round(sum(case when ngn_cu_type_cu_type_code is not null and ngn_cu_type_rule_code = 'A' then ngn_pay_amount / ngn_cu_type_fte_equiv_amt else ngn_pay_fte end) / count(distinct coalesce(ne1.ngn_empl_title_code,ne2.ngn_empl_title_code,ne3.ngn_empl_title_code,ne4.ngn_empl_title_code,'0000')),4), "
			+"	round(sum(case when ngn_cu_type_cu_type_code is not null and ngn_cu_type_rule_code = 'A' then ngn_pay_amount / ngn_cu_type_fte_equiv_amt else ngn_pay_fte end) / count(distinct coalesce(ne1.ngn_empl_title_code,ne2.ngn_empl_title_code,ne3.ngn_empl_title_code,ne4.ngn_empl_title_code,'0000')),4), "
			+"	0, "
			+"	0, "
			+"	0, "
			+"	0, "
			+"	0, "
			+"	case when ngn_pay_index != coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index) then 'N' else 'Y' end as subs_ind, "
			+"	coalesce(nr1.ngn_redir_recharge_fund,nr2.ngn_redir_recharge_fund,ngn_pay_fund) as recharge_fund, "
			+"	coalesce(ngn_fund_category_code,'OT') as ngn_fund_category_code, "
			+"	coalesce(nr1.ngn_redir_recharge_orgn,nr2.ngn_redir_recharge_orgn,ngn_pay_orgn) as recharge_orgn, "
			+"	coalesce(nr1.ngn_redir_recharge_prog,nr2.ngn_redir_recharge_prog,ngn_pay_prog) as recharge_prog, "
			+"	ngn_pay_index, "
			+"	ngn_pay_fund, "
			+"	ngn_pay_orgn, "
			+"	ngn_pay_prog, "
			+"	emb_employee_name, "
			+"	emp_home_department_code, "
			+"	emp_emp_home_department_name, "
			+"	case when vice_chancellor_unit_code is not null then '0'||vice_chancellor_unit_code else null end as vice_chancellor_unit_code, "
			+"	vice_chancellor_unit_name "
			+"from ngn.ngn_payroll "
			+"left outer join ngn.ngn_redirect nr1 on nr1.ngn_redir_pay_index = ngn_pay_index and nr1.ngn_redir_process_month = ngn_pay_process_month and nr1.ngn_redir_employee_id = ngn_pay_employee_id "
			+"left outer join ngn.ngn_redirect nr2 on nr1.ngn_redir_process_month is null and nr2.ngn_redir_pay_index = ngn_pay_index and nr2.ngn_redir_process_month = ngn_pay_process_month and nr2.ngn_redir_employee_id = '' "
			+"left outer join ngn.ngn_cu_type on ngn_cu_type_process_month = ngn_pay_process_month and ngn_cu_type_title_code = ngn_pay_title_code and ngn_cu_type_dos_code = ngn_pay_dos_code "
			+"left outer join ngn.ngn_employee ne1 on ne1.ngn_empl_employee_id = ngn_pay_employee_id and ne1.ngn_empl_process_month = ngn_pay_process_month and ne1.ngn_empl_title_code = ngn_pay_title_code "
			+"left outer join ngn.ngn_employee ne2 on ne1.ngn_empl_employee_id is null and ngn_pay_process_month != "+process_month+" and ne2.ngn_empl_employee_id = ngn_pay_employee_id and ne2.ngn_empl_process_month = ((( mod(ngn_pay_process_month,100) + 88) / 100 * 100) + ( mod(( mod(ngn_pay_process_month,100) ),12) + 1) + (ngn_pay_process_month / 100 * 100)) and ne2.ngn_empl_title_code = ngn_pay_title_code "
			+"left outer join ngn.ngn_employee ne3 on ne1.ngn_empl_employee_id is null and ne2.ngn_empl_employee_id is null and ne3.ngn_empl_employee_id = ngn_pay_employee_id and ne3.ngn_empl_process_month = ngn_pay_process_month "
			+"left outer join ngn.ngn_employee ne4 on ne1.ngn_empl_employee_id is null and ne2.ngn_empl_employee_id is null and ne3.ngn_empl_employee_id is null and ngn_pay_process_month != "+process_month+" and ne4.ngn_empl_employee_id = ngn_pay_employee_id and ne4.ngn_empl_process_month = ((( mod(ngn_pay_process_month,100) + 88) / 100 * 100) + ( mod(( mod(ngn_pay_process_month,100) ),12) + 1) + (ngn_pay_process_month / 100 * 100)) "
			+"left outer join tms.directory on right(ngn_pay_employee_id,7) = dir_emp_id "
			+"join employee.p_employee on emb_employee_id = ngn_pay_employee_id "
			+"left outer join employee.department on department_code = emp_home_department_code "
			+"join ngn.ngn_fund on ngn_fund_process_month = ngn_pay_process_month and coalesce(nr1.ngn_redir_recharge_fund,nr2.ngn_redir_recharge_fund,ngn_pay_fund) between ngn_fund_fund_lo and ngn_fund_fund_hi "
			+"left outer join ngn.ngn_history on ngn_history_employee_id = ngn_pay_employee_id and ngn_history_process_month = ngn_pay_process_month and ngn_history_title_code = ngn_pay_title_code and ngn_history_index = coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index) and ngn_history_pay_index = ngn_pay_index and ngn_history_pay_activity_code = ngn_pay_pay_activity_code "
			+"where "
			+"	(ngn_pay_process_month = "+process_month+" "
			+"	 or (ngn_pay_process_month between "+process_first_month+" and "+process_month+" "
			+"	     and ngn_pay_posting_month = "+process_month+")) "
			+"	and (ngn_pay_dos_type = 'NR01' or ngn_pay_dos_code in ('FEL','FES','FEN')) "
			+"	and ngn_history_employee_id is null "
			+"group by "
			+"	ngn_pay_employee_id, "
			+"	ngn_pay_process_month, "
			+"	ngn_pay_title_code, "
			+"	ngn_pay_index, "
			+"	ngn_pay_pay_activity_code, "
			+"	ngn_pay_fund, "
			+"	ngn_pay_orgn, "
			+"	ngn_pay_prog, "
			+"	coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index), "
			+"	case when ngn_pay_index != coalesce(nr1.ngn_redir_recharge_index,nr2.ngn_redir_recharge_index,ngn_pay_index) then 'Y' else null end, "
			+"	coalesce(nr1.ngn_redir_recharge_fund,nr2.ngn_redir_recharge_fund,ngn_pay_fund), "
			+"	coalesce(nr1.ngn_redir_recharge_orgn,nr2.ngn_redir_recharge_orgn,ngn_pay_orgn), "
			+"	coalesce(nr1.ngn_redir_recharge_prog,nr2.ngn_redir_recharge_prog,ngn_pay_prog), "
			+"	coalesce(ngn_cu_type_cu_type_code,'EMPL'), "
			+"	coalesce(ne1.ngn_empl_appt_type_code,ne2.ngn_empl_appt_type_code), "
			//Ended up not being a 1-1 join. Removed this group by and added a min() function in the select list "
			+"	coalesce(ne1.ngn_empl_tms_location_code,ne2.ngn_empl_tms_location_code,ne3.ngn_empl_tms_location_code,ne4.ngn_empl_tms_location_code,dir_loc_code), "
			+"	coalesce(ngn_fund_category_code,'OT'), "
			+"	emb_employee_name, "
			+"	emp_home_department_code, "
			+"	emp_emp_home_department_name, "
			+"	coalesce(ne1.ngn_empl_mail_code,ne2.ngn_empl_mail_code,ne3.ngn_empl_mail_code,ne4.ngn_empl_mail_code,emp_mailcode), "
			+"	case when vice_chancellor_unit_code is not null then '0'||vice_chancellor_unit_code else null end, "
			+"	vice_chancellor_unit_name "
		;
		stmt = conn.createStatement();
		NgnControl.rows_inserted = stmt.executeUpdate(sql);
		NgnControl.out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> inserting done");

	} catch(Exception e) {
		throw new Exception("NgnCuPosting: "+e);
	} catch(Throwable t) {
		throw new Exception("NgnCuPosting: "+t);
	} finally {
		try {
	 		if(stmt!=null){ stmt.close(); }
		} catch(SQLException se){ }
	}
}


}
