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


public class McRechargeCalc {

private Connection conn = null;
private String db = "ngn_db";

private int process_first_month = 0;
private int process_month = 0;

public McRechargeCalc(Connection conn, int process_first_month, int process_month)
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

		//Re-derive amount
		sql = ""
			+"merge into ngn.mc_history mch "
			+"using ( "
			+"	select "
			+"		mc_rate_amount as current_group_rate, "
			+"		round(case "
			// when coalesce(ngn_history_ttl_category_code,'-') != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418')) then "  // new code (ngn_history_title_code)
			//+"			when ngn_history_ttl_category_code != '+' or ngn_history_stu_status_code > '2' then "
			//+"			when ngn_history_ttl_category_code != '+' or ngn_history_stu_status_code > '3' then "
            //+"			when ngn_history_ttl_category_code != '+' or (ngn_history_stu_status_code > '2' and ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736')) then " 
			//20170803 was active //+"			when ngn_history_ttl_category_code != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or NGN_HISTORY_INDEX in ('TPSSH11','TPSSH12'))) then " 
            //+"			when ngn_history_ttl_category_code != '+' or (ngn_history_stu_status_code > '3') or (ngn_history_stu_status_code ='3' and (ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or ngn_history_fund = '66069A')) then " 
            +"              when ngn_history_ttl_category_code != '+'  or (ngn_history_stu_status_code > '3')  or (ngn_history_stu_status_code ='3'  and (ngn_history_title_code not in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code ='000418' or NGN_HISTORY_INDEX in ('TPSSH11','TPSSH12')))  or (ngn_history_stu_status_code ='3' and (ngn_history_title_code in ('4919')  and NGN_HISTORY_INDEX in (select project_index from session.n_ifis_project))) then  "
            +"				0 "
			+"			else "
			+"				mc_rate_amount * ngn_history_current_adj_fte "
			+"		end,2) as current_amount, "
			+"		ngn_history_employee_id, "
			+"		ngn_history_process_month, "
			+"		ngn_history_title_code, "
			+"		ngn_history_index, "
			+"		ngn_history_pay_activity_code, "
			+"		ngn_history_pay_index "
			+"	from ngn.ngn_history "
			+"	join ngn.mc_rate on mc_rate_process_month = ngn_history_process_month and mc_rate_loc_group_code = ngn_history_loc_group_code and mc_rate_cu_type_code = ngn_history_cu_type_code "
			+"	where ngn_history_process_month between "+process_first_month+" and "+process_month+" "
			+"	  and ngn_history_loc_group_code = 'MC' "
			//+"	  and ngn_history_vcu_code != '05' "
			+" and (ngn_history_vcu_code != '05' OR ngn_history_fund like '601%')"
			+") as ngnh "
			+"on "
			+"	    mch.mc_history_employee_id = ngnh.ngn_history_employee_id "
			+"	and mch.mc_history_process_month = ngnh.ngn_history_process_month "
			+"	and mch.mc_history_title_code = ngnh.ngn_history_title_code "
			+"	and mch.mc_history_index = ngnh.ngn_history_index "
			+"	and mch.mc_history_pay_activity_code = ngnh.ngn_history_pay_activity_code "
			+"	and mch.mc_history_pay_index = ngnh.ngn_history_pay_index "
			+"when matched then "
			+"update set "
			+"	mch.mc_history_current_group_rate = ngnh.current_group_rate, "
			+"	mch.mc_history_current_amount = ngnh.current_amount "
			;
		stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
	} catch(Exception e) {
		throw new Exception("McRechargeCalc: "+e);
	} catch(Throwable t) {
		throw new Exception("McRechargeCalc: "+t);
	} finally {
		try {
	 		if(stmt!=null){ stmt.close(); }
		} catch(SQLException se){ }
	}
}

}