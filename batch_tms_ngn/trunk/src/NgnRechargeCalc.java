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
 * The goal of this process is to derive or re-derive all applicable table-based parameters that
 * contribute to the NGN Recharge Amount computation, and, if necessary, to re-compute that
 * amount, for each and every ngn_history row within the scope of the 'current processing window'.
 * It must be emphasized that NGN Recharge Process definitions require that this re-derivation be
 * done in the course of every process cycle (e.g. process month) to account for the possibility that
 * one or more controlling factors (parameters) have been altered retroactively since the last
 * process cycle, thus indicating a new value for dependent values of the NGN Recharge Amount.
 * Re-computed NGN Recharge Amount values will be compared with existing (prior) values in
 * Generate NGN Recharges process (C.8) as a basis for issuing differential recharge amount
 * adjustments. Naturally, for any newly-created ngn_history row, this process constitues a
 * primary (first-time) determination and not a re-derivation. By the nature of the process, about 8-
 * 9% of rows processed will be primary determinations, and the great majority, or 91-92%, will be
 * re-derivations.
 */
public class NgnRechargeCalc {

private Connection conn = null;
private String db = "ngn_db";

private int process_first_month = 0;
private int process_month = 0;

public NgnRechargeCalc(Connection conn, int process_first_month, int process_month)
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
	int updates = 0;

	try {
		//Re-derive
    //ngn_history_fund
		conn.setAutoCommit(false);
		stmt = conn.createStatement();
		try {
			// adding code from decompiled class file 20170803 - lv
			stmt.executeUpdate(""
				+"DECLARE GLOBAL TEMPORARY TABLE session.n_ifis_project ( project_index char(10) NOT NULL ) "
				+"ON COMMIT PRESERVE ROWS NOT LOGGED");	
			
			sql = "insert into session.n_ifis_project select account_index "
				+" from ccm.PROJECT_DETAIL where COST_CENTER_KEY in "
				+" (select COST_CENTER_KEY from ccm.COST_CENTER where PROJECT = 'NGN4919E' )";
			
			stmt.executeUpdate(sql);
			
			stmt.executeUpdate(""
				+"DECLARE GLOBAL TEMPORARY TABLE session.ngn_history_rederive ( "
				+"	ngn_history_ttl_category_code	char(1), "
				+"	ngn_history_loc_group_code		varchar(5), "
				+"	ngn_history_group_code			varchar(2), "
				+"	ngn_history_group_desc			varchar(15), "
				+"	ngn_history_current_group_rate	decimal(6,2)	NOT NULL, "
				+"	ngn_history_current_amount		decimal(9,2)	NOT NULL, "
				+" "
				+"	ngn_history_employee_id			char(9)			NOT NULL, "
				+"	ngn_history_process_month		int				NOT NULL, "
				+"	ngn_history_title_code			char(4)			NOT NULL, "
				+"	ngn_history_index				char(10)		NOT NULL, "
				+"	ngn_history_pay_activity_code	char(1)			NOT NULL, "
				+"	ngn_history_pay_index			char(10)		NOT NULL "
				+") ON COMMIT PRESERVE ROWS NOT LOGGED "
			);

			  //System.out.println("before first one");
			//Create modified data
	  		sql = ""
				+"insert into session.ngn_history_rederive "
				+"select "
				+"	coalesce(ngn_title_cu_category_code,'-'), "
				+"	coalesce(ngn_loc_loc_group_code,ngn_mail_loc_group_code,ngn_history_loc_group_code,'FULL'), "
				+"	ngn_rate_group_code, "
				+"	ngn_rate_group_desc, "
				+"	ngn_rate_amount, "
				+"	round(case "
				//+"		when coalesce(ngn_title_cu_category_code,'-') != '+' or ngn_history_stu_status_code > '2' then "
				//+"		when coalesce(ngn_title_cu_category_code,'-') != '+' or ngn_history_stu_status_code > '3' then "  // new code (ngn_history_title_code)
				//+"		when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3' and ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')) then "  // new code (ngn_history_title_code)
				//+"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')) then "  // new code (ngn_history_title_code)
				//+"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')) then "  // new code (ngn_history_title_code)
				//+"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418')) then "  // new code (ngn_history_title_code)
				// 20170803 //+"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or NGN_HISTORY_INDEX in ('TPSSH11','TPSSH12'))) then "  // new code (ngn_history_title_code)
                //+"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or ngn_history_fund = '66069A')) then "  // new code (ngn_history_title_code)
                +"      when coalesce(ngn_title_cu_category_code,'-') != '+'  or (ngn_history_stu_status_code > '3')   or (ngn_history_stu_status_code ='3' and (ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or NGN_HISTORY_INDEX in ('TPSSH11','TPSSH12'))) or (ngn_history_stu_status_code ='3' and (ngn_title_title_code in ('4919')  and NGN_HISTORY_INDEX in (select project_index from session.n_ifis_project ))) then "
                +"			0 "				
				+"		else "
				+"			ngn_rate_amount * ngn_history_current_adj_fte "
				+"	end,2), "
				+" "
				+"	ngn_history_employee_id, "
				+"	ngn_history_process_month, "
				+"	ngn_history_title_code, "
				+"	ngn_history_index, "
				+"	ngn_history_pay_activity_code, "
				+"	ngn_history_pay_index "
				+"from ngn.ngn_history "
				+"left outer join ngn.ngn_title on ngn_title_process_month = ngn_history_process_month and ngn_title_title_code = ngn_history_title_code "
				+"left outer join ngn.ngn_location on ngn_history_tms_location_code is not null and ngn_loc_process_month = ngn_history_process_month and ngn_loc_loc_code = ngn_history_tms_location_code "
				+"left outer join ngn.ngn_maildrop on ngn_loc_loc_group_code is null and ngn_mail_process_month = ngn_history_process_month and ngn_history_empl_mail_code between ngn_mail_mail_code_lo and ngn_mail_mail_code_hi "
				+"join ngn.ngn_rate on ngn_rate_process_month = ngn_history_process_month and ngn_rate_loc_group_code = coalesce(ngn_loc_loc_group_code,ngn_mail_loc_group_code,ngn_history_loc_group_code,'FULL') and ngn_rate_cu_type_code = ngn_history_cu_type_code "
				+"where ngn_history_process_month between "+process_first_month+" and "+process_month+" "
				+"  and ( "
				+"  	coalesce(ngn_title_cu_category_code,'-') != ngn_history_ttl_category_code "
				+"	or coalesce(ngn_loc_loc_group_code,ngn_mail_loc_group_code,ngn_history_loc_group_code,'FULL') != ngn_history_loc_group_code "
				+"	or ngn_history_loc_group_code is null "
				+"	or ngn_rate_group_code != ngn_history_group_code "
				+"	or ngn_rate_group_desc != ngn_history_group_desc "
				+"	or ngn_rate_amount != ngn_history_current_group_rate "
				+"	or round(case "
				//+"		when coalesce(ngn_title_cu_category_code,'-') != '+' or ngn_history_stu_status_code > '2' then "
				//+"		when coalesce(ngn_title_cu_category_code,'-') != '+' or ngn_history_stu_status_code > '3' then "  // new code
				//+"		when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3' and ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')) then "  // new code (ngn_history_title_code)
				//+"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')) then "  // new code (ngn_history_title_code)
				//+"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')) then "  // new code (ngn_history_title_code)
				//+"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418')) then "  // new code (ngn_history_title_code)
				// 20170803 was active// +"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or NGN_HISTORY_INDEX in ('TPSSH11','TPSSH12'))) then "  // new code (ngn_history_title_code)
                //+"	when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or ngn_history_fund = '66069A')) then "  // new code (ngn_history_title_code)
                +" when coalesce(ngn_title_cu_category_code,'-') != '+' or (ngn_history_stu_status_code > '3')   or (ngn_history_stu_status_code ='3' and (ngn_title_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or NGN_HISTORY_INDEX in ('TPSSH11','TPSSH12')))  or (ngn_history_stu_status_code ='3' and (ngn_history_title_code in ('4919')  and NGN_HISTORY_INDEX in (select project_index from session.n_ifis_project ))) then "
                +"			0 "
				+"		else "
				+"			ngn_rate_amount * ngn_history_current_adj_fte "
				+"	end,2) != ngn_history_current_amount "
				+"  ) "
			;
			stmt.executeUpdate(sql);

	  		sql = ""
				+"merge into ngn.ngn_history n "
				+"using session.ngn_history_rederive n2 on "
				+"	n.ngn_history_employee_id = n2.ngn_history_employee_id and "
				+"	n.ngn_history_process_month = n2.ngn_history_process_month and "
				+"	n.ngn_history_title_code = n2.ngn_history_title_code and "
				+"	n.ngn_history_index = n2.ngn_history_index and "
				+"	n.ngn_history_pay_activity_code = n2.ngn_history_pay_activity_code and "
				+"	n.ngn_history_pay_index = n2.ngn_history_pay_index "
				+"when matched then "
				+"update set "
				+"	n.ngn_history_ttl_category_code = n2.ngn_history_ttl_category_code, "
				+"	n.ngn_history_loc_group_code = n2.ngn_history_loc_group_code, "
				+"	n.ngn_history_group_code = n2.ngn_history_group_code, "
				+"	n.ngn_history_group_desc = n2.ngn_history_group_desc, "
				+"	n.ngn_history_current_group_rate = n2.ngn_history_current_group_rate, "
				+"	n.ngn_history_current_amount = n2.ngn_history_current_amount "
			;
			updates = stmt.executeUpdate(sql);

			NgnControl.out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Rederive done with "+updates+" updates");
		} catch(Exception e) {
			System.out.println("SQL Exception: "+e);
		} finally {
		  conn.commit();
		}

		stmt.executeUpdate("drop table session.ngn_history_rederive");
		stmt.close();
		conn.setAutoCommit(true);

		//Normalize
		conn.setAutoCommit(false);
		stmt = conn.createStatement();
		try {
			stmt.executeUpdate(""
				+"DECLARE GLOBAL TEMPORARY TABLE session.ngn_history_fte_adj ( "
				+"	t_ngn_history_employee_id char(9) not null, "
				+"	t_ngn_history_process_month int not null, "
				+"	pdfl_adj_fte_denominator decimal(9,4) not null, "
				+"	non_pdfl_adj_fte_denominator decimal(9,4) not null "
				+") ON COMMIT PRESERVE ROWS NOT LOGGED "
			);

			/**
			 * This sql will calculate the denominators needed to divide by in order to normalize those
			 * employees with FTEs greater then 1.0010. The denominators are determined per employee per
			 * month and stored in a temporary table. They are later used to divide the FTE amounts
			 * case 1: Total FTE is too large for PDF1 rows
			 * case 2: Total FTE is too large for all employee rows
			 */
			sql = ""
				+"insert into session.ngn_history_fte_adj (t_ngn_history_employee_id,t_ngn_history_process_month,pdfl_adj_fte_denominator,non_pdfl_adj_fte_denominator) "
				+"select "
				+"	ngn_history_employee_id, "
				+"	ngn_history_process_month, "
				//calculate pdfl demonimator "
				+"	round(case "
				+"		when sum(case when ngn_history_cu_type_code = 'PDF1' then ngn_history_current_adj_fte else 0 end) > 1.001 then "
				//case 1
				+"			case "
				+"				when sum(case when ngn_history_cu_type_code != 'PDF1' then ngn_history_current_adj_fte else 0 end) > 0 then "
				//case 1 and 2
				+"					sum(case when ngn_history_cu_type_code = 'PDF1' then ngn_history_current_adj_fte else 0 end) "
				+"					* "
				+"					(1+sum(case when ngn_history_cu_type_code != 'PDF1' then ngn_history_current_adj_fte else 0 end)) "
				+"				else "
				//case 1 and not 2
				+"					sum(case when ngn_history_cu_type_code = 'PDF1' then ngn_history_current_adj_fte else 0 end) "
				+"				end "
				+"		else "
				//not case 1
				+"			case "
				+"				when sum(ngn_history_current_adj_fte) > 1.001 then "
				//not case 1 and 2
				+"					sum(ngn_history_current_adj_fte) "
				+"				else "
				//not case 1 and not 2
				+"					1 "
				+"				end "
				+"	end,4), "
				//calculate non pdfl denominator
				+"	round(case "
				+"		when sum(case when ngn_history_cu_type_code = 'PDF1' then ngn_history_current_adj_fte else 0 end) > 1.001 then "
				//case 1
				+"			case "
				+"				when sum(case when ngn_history_cu_type_code != 'PDF1' then ngn_history_current_adj_fte else 0 end) > 0 then "
				//case 1 and 2
				+"					1 + sum(case when ngn_history_cu_type_code != 'PDF1' then ngn_history_current_adj_fte else 0 end) "
				+"				else "
				//case 1 and not 2
				+"					1 "
				+"				end "
				+"		else "
				//not case 1
				+"			case "
				+"				when sum(ngn_history_current_adj_fte) > 1.001 then "
				//not case 1 and 2
				+"					sum(ngn_history_current_adj_fte) "
				+"				else "
				//not case 1 and not 2
				+"					1 "
				+"				end "
				+"	end,4) "
				+"from ngn.ngn_history "
				+"where ngn_history_process_month between "+process_first_month+" and "+process_month+" "
				+"group by "
				+"	ngn_history_employee_id, ngn_history_process_month "
				+"having "
				+"	round(case "
				+"		when sum(case when ngn_history_cu_type_code = 'PDF1' then ngn_history_current_adj_fte else 0 end) > 1.001 then "
				//case 1
				+"			case "
				+"				when sum(case when ngn_history_cu_type_code != 'PDF1' then ngn_history_current_adj_fte else 0 end) > 0 then "
				//case 1 and 2
				+"					sum(case when ngn_history_cu_type_code = 'PDF1' then ngn_history_current_adj_fte else 0 end) "
				+"					* "
				+"					(1+sum(case when ngn_history_cu_type_code != 'PDF1' then ngn_history_current_adj_fte else 0 end)) "
				+"				else "
				//case 1 and not 2
				+"					sum(case when ngn_history_cu_type_code = 'PDF1' then ngn_history_current_adj_fte else 0 end) "
				+"				end "
				+"		else "
				//not case 1
				+"			case "
				+"				when sum(ngn_history_current_adj_fte) > 1.001 then "
				//not case 1 and 2
				+"					sum(ngn_history_current_adj_fte) "
				+"				else "
				//not case 1 and not 2
				+"					1 "
				+"				end "
				+"	end,4) > 1.001 "
				+"or "
				+"	round(case "
				+"		when sum(case when ngn_history_cu_type_code = 'PDF1' then ngn_history_current_adj_fte else 0 end) > 1.001 then "
				//case 1
				+"			case "
				+"				when sum(case when ngn_history_cu_type_code != 'PDF1' then ngn_history_current_adj_fte else 0 end) > 0 then "
				//case 1 and 2
				+"					1 + sum(case when ngn_history_cu_type_code != 'PDF1' then ngn_history_current_adj_fte else 0 end) "
				+"				else "
				//case 1 and not 2
				+"					1 "
				+"				end "
				+"		else "
				//not case 1
				+"			case "
				+"				when sum(ngn_history_current_adj_fte) > 1.001 then "
				//not case 1 and 2
				+"					sum(ngn_history_current_adj_fte) "
				+"				else "
				//not case 1 and not 2
				+"					1 "
				+"				end "
				+"	end,4) > 1.001 "
			;
			stmt.executeUpdate(sql);

			//Denominators are caclulated, now dividing FTEs to normalize them
			stmt.executeUpdate(""
				+"DECLARE GLOBAL TEMPORARY TABLE session.ngn_history_fte_adj_2 ( "
				+"	current_adj_fte decimal(9,4) not null, "
				+"	current_amount decimal(9,2) not null, "
				+"	employee_id char(9) not null, "
				+"	process_month int not null, "
				+"	title_code char(4) not null, "
				+"	recharge_index char(10) not null, "
				+"	activity_code char(1) not null, "
				+"	pay_index char(10) not null "
				+") ON COMMIT PRESERVE ROWS NOT LOGGED "
			);

			//System.out.println("before second one");

			sql = ""
				+"insert into session.ngn_history_fte_adj_2 "
				+"select "
				+"	round(ngn_history_current_adj_fte / "
				+"		case "
				+"			when ngn_history_cu_type_code = 'PDF1' then "
				+"				pdfl_adj_fte_denominator "
				+"			else "
				+"				non_pdfl_adj_fte_denominator "
				+"		end,4), "
				+"	round(case "
				//+"		when coalesce(ngn_history_ttl_category_code,'-') != '+' or ngn_history_stu_status_code > '2' then "
				//+"		when coalesce(ngn_history_ttl_category_code,'-') != '+' or ngn_history_stu_status_code > '3' then "  // new code
				//+"		  when coalesce(ngn_history_ttl_category_code,'-') != '+' or (ngn_history_stu_status_code > '3' and ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')) then "  // new code (ngn_history_title_code)
				//+"	when coalesce(ngn_history_ttl_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')) then "  // new code (ngn_history_title_code)
				//+"	when coalesce(ngn_history_ttl_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418')) then "  // new code (ngn_history_title_code)
				//20170803 was active //+"	when coalesce(ngn_history_ttl_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or NGN_HISTORY_INDEX in ('TPSSH11','TPSSH12'))) then "  // new code (ngn_history_title_code)
				//+"	when coalesce(ngn_history_ttl_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or ngn_history_fund = '66069A')) then "  // new code (ngn_history_title_code)
				+"     when coalesce(ngn_history_ttl_category_code,'-') != '+' or (ngn_history_stu_status_code > '3')  or (ngn_history_stu_status_code ='3'  and (ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')  or ngn_history_home_dept_code ='000418' or NGN_HISTORY_INDEX in ('TPSSH11','TPSSH12')))  or (ngn_history_stu_status_code ='3' and (ngn_history_title_code in ('4919') and NGN_HISTORY_INDEX in (select project_index from session.n_ifis_project )))  then "
				+"			0 "
				+"		else "
				+"			ngn_history_current_group_rate * "
				+"			round(ngn_history_current_adj_fte / "
				+"			case "
				+"				when ngn_history_cu_type_code = 'PDF1' then "
				+"					pdfl_adj_fte_denominator "
				+"				else "
				+"					non_pdfl_adj_fte_denominator "
				+"			end,4) "
				+"	end,2), "
				+"	ngn_history_employee_id, "
				+"	ngn_history_process_month, "
				+"	ngn_history_title_code, "
				+"	ngn_history_index, "
				+"	ngn_history_pay_activity_code, "
				+"	ngn_history_pay_index "
				+"from ngn.ngn_history "
				+"join session.ngn_history_fte_adj on t_ngn_history_employee_id = ngn_history_employee_id and t_ngn_history_process_month = ngn_history_process_month "
			;
			stmt.executeUpdate(sql);

			sql = ""
				+"merge into ngn.ngn_history n "
				+"using session.ngn_history_fte_adj_2 n2 on "
				+"	n.ngn_history_employee_id = n2.employee_id and "
				+"	n.ngn_history_process_month = n2.process_month and "
				+"	n.ngn_history_title_code = n2.title_code and "
				+"	n.ngn_history_index = n2.recharge_index and "
				+"	n.ngn_history_pay_activity_code = n2.activity_code and "
				+"	n.ngn_history_pay_index = n2.pay_index "
				+"when matched then "
				+"update set "
				+"	n.ngn_history_current_adj_fte = n2.current_adj_fte, "
				+"	n.ngn_history_current_amount = n2.current_amount "
			;
			updates = stmt.executeUpdate(sql);

		} catch(Exception e) {
			System.out.println("SQL Exception: "+e);
		} finally {
		  conn.commit();
		}

		stmt.executeUpdate("drop table session.ngn_history_fte_adj");
		stmt.executeUpdate("drop table session.ngn_history_fte_adj_2");
		stmt.close();
		conn.setAutoCommit(true);

		NgnControl.out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Done with normalization, normalized "+updates+" records");
	} catch(Exception e) {
		throw new Exception("NgnRechargeCalc: "+e);
	} catch(Throwable t) {
		throw new Exception("NgnRechargeCalc: "+t);
	} finally {
		try {
	 		if(stmt!=null){ stmt.close(); }
		} catch(SQLException se){ }
	}
}

}