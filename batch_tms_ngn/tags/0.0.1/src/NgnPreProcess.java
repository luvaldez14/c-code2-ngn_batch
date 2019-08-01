import java.io.*;
import java.util.*;
import java.sql.*;
import edu.ucsd.act.db.connectionpool.*;
import edu.ucsd.act.jlink.util.JLinkDate;

/**
 * The function of this process is to prepare the NGN History table for posting of the incremental
 * updates performed in NGN history posting. Since all results of the NGN rechage process are
 * based on differences induced by the posting process, all '_prev_' (previous) variables in the NGN
 * history must be set to the value of the corresponding 'current' variables so that differences may
 * be later detected by comparing these variables. All NGN history rows created in the posting
 * process have their '_prev_' values set to zero so that all posted activity is included in the
 * differences measured later.
 */
public class NgnPreProcess {

private static Connection conn = null;
private static String db = "ngn_db";

private int process_first_month = 0;
private int process_month = 0;
private String status = "";

public NgnPreProcess(Connection conn, int process_first_month, int process_month, String status) {
	this.conn = conn;
	this.process_first_month = process_first_month;
	this.process_month = process_month;
	this.status = status;
}

public void run(String flag)
	throws Exception
{
	try {
		if(status.equals("1") || flag.equals("0")) {
			//first lock out the process month
			String sql = "update ngn.ngn_period set ngn_period_status_code = '5' where ngn_period_process_month = "+process_month;
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move current values to previous
			sql = ""
				+"update ngn.ngn_history set "
				+"	ngn_history_prev_group_rate = ngn_history_current_group_rate, "
				+"	ngn_history_prev_fte = ngn_history_current_fte, "
				+"	ngn_history_prev_adj_fte = ngn_history_current_adj_fte, "
				+"	ngn_history_prev_amount = ngn_history_current_amount, "
				+"	ngn_history_current_adj_fte = ngn_history_current_fte "
				+"where ngn_history_process_month >= "+process_first_month+" and ngn_history_process_month < "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
		} else {
			//status is 5 so the process is being run again. Backing out data from previous run
			//move current values to previous
			String sql = "delete from ngn.ngn_history where ngn_history_process_month = "+process_month;
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);

			sql = ""
				+"update ngn.ngn_history set "
				+"	ngn_history_current_group_rate = ngn_history_prev_group_rate, "
				+"	ngn_history_current_fte = ngn_history_prev_fte, "
				+"	ngn_history_current_adj_fte = ngn_history_prev_fte, " //not adjusted fte
				+"	ngn_history_current_amount = ngn_history_prev_amount "
				+"where ngn_history_process_month >= "+process_first_month+" and ngn_history_process_month < "+process_month
				;
			stmt.executeUpdate(sql);
			stmt.close();
		}

		updateTitleTable();
		updateLocationTable();
	} catch(Exception e) {
		throw new Exception("NgnPreProcess: "+e);
	} catch(Throwable t) {
		throw new Exception("NgnPreProcess: "+t);
	}
}

private void updateTitleTable()
	throws Exception
{
	try {
		String sql = "";
		Statement stmt = conn.createStatement();

		//update any changes
		sql = ""
			+"update ngn.ngn_title set "
			+"  ngn_title_cu_category_code = ( "
			+"  	select commuserstatus "
			+"	from   tms.titlecode "
			+"	where  titlecode = ngn_title_title_code "
			+"    and    ngn_title_cu_category_code != commuserstatus "
			+"  ), "
			+"  ngn_title_change_userid = 'MAINT', "
			+"  ngn_title_change_date = CURRENT TIMESTAMP "
			+"where ngn_title_process_month = "+process_month+" "
			+"  and exists ( "
			+"  	select 1 "
			+"	from   tms.titlecode "
			+"	where  titlecode = ngn_title_title_code "
			+"	and    ngn_title_cu_category_code != commuserstatus "
			+"  ) "
			;
		stmt.executeUpdate(sql);

		//insert any new titles
		sql = ""
			+"insert into ngn.ngn_title ( "
			+"  ngn_title_process_month, "
			+"  ngn_title_title_code, "
			+"  ngn_title_cu_category_code, "
			+"  ngn_title_description, "
			+"  ngn_title_CTO_code, "
			+"  ngn_title_CTO_description, "
			+"  ngn_title_change_userid, "
			+"  ngn_title_change_date "
			+") "
			+"select "
			+"  ngn_period_process_month, "
			+"  titlecode, "
			+"  commuserstatus, "
			+"  ppstitlename, "
			+"  cto_code, "
			+"  cto_desc, "
			+"  'MAINT', "
			+"  CURRENT TIMESTAMP "
			+"from tms.titlecode "
			+"join ngn.ngn_period on 1 = 1 "
			+"left outer join ngn.ngn_title on ngn_title_title_code = titlecode and ngn_title_process_month = ngn_period_process_month "
			+"where ngn_title_title_code is null "
			+"and   cu_reviewed_yn = 'Y' "
//			+"and   recordstatus != 'X' "
			+"and   ngn_period_process_month between "+process_first_month+" and "+process_month+" "
			;
		stmt.executeUpdate(sql);

		//delete any titles
		sql = ""
			+"delete from ngn.ngn_title "
			+"where ngn_title_process_month = "+process_month+" "
			+"  and ngn_title_title_code not in ( "
			+"		select titlecode "
			+"		from tms.titlecode "
//			+"		where recordstatus != 'X' "
			+"  ) "
			;
		stmt.executeUpdate(sql);

		stmt.close();
	} catch(Exception e) {
		throw new Exception("updateTitleTable(): "+e);
	} catch(Throwable t) {
		throw new Exception("updateTitleTable(): "+t);
	}
}

private void updateLocationTable()
	throws Exception
{
	try {
		String sql = "";
		Statement stmt = conn.createStatement();

		//update any changes
		sql = ""
			+"update ngn.ngn_location set "
			+"  ngn_loc_loc_group_code = ( "
			+"	select loc_group_code "
			+"	from  tms.building "
			+"	where building_code = ngn_loc_loc_code "
			+"	  and loc_group_code in ('FULL','MC','OFF','NO') "
			+"	  and ngn_loc_loc_group_code != loc_group_code "
			+"  ), "
			+"  ngn_loc_change_userid = 'MAINT', "
			+"  ngn_loc_change_date = CURRENT TIMESTAMP "
			+"where ngn_loc_process_month = "+process_month+" "
			+"  and exists ( "
			+"  	select 1 "
			+"	from  tms.building "
			+"	where building_code = ngn_loc_loc_code "
			+"	  and loc_group_code in ('FULL','MC','OFF','NO') "
			+"	  and ngn_loc_loc_group_code != loc_group_code "
			+"  ) "
			;
		stmt.executeUpdate(sql);

		//insert any new locations
		sql = ""
			+"insert into ngn.ngn_location ( "
			+"  ngn_loc_process_month, "
			+"  ngn_loc_loc_code, "
			+"  ngn_loc_loc_group_code, "
			+"  ngn_loc_change_userid, "
			+"  ngn_loc_change_date "
			+") "
			+"select "
			+"  ngn_period_process_month, "
			+"  substr(building_code,1,6), "
			+"  loc_group_code, "
			+"  'MAINT', "
			+"  CURRENT TIMESTAMP "
			+"from tms.building "
			+"join ngn.ngn_period on 1 = 1 "
			+"left outer join ngn.ngn_location on substr(building_code,1,6) = ngn_loc_loc_code and ngn_loc_process_month = ngn_period_process_month "
			+"where ngn_loc_loc_code is null "
			+"  and   loc_group_code in ('FULL','MC','OFF','NO') "
			+"  and   ngn_period_process_month between "+process_first_month+" and "+process_month+" "
			;
		stmt.executeUpdate(sql);

		//delete any locations
		sql = ""
			+"delete from ngn.ngn_location "
			+"where ngn_loc_process_month = "+process_month+" "
			+"  and ngn_loc_loc_code not in ('A*FU','A*MC','A*OF','A*NO') "
			+"  and ngn_loc_loc_code not in ( "
			+"	select building_code "
			+"	from tms.building "
			+"	where loc_group_code in ('FULL','MC','OFF','NO') "
			+"  ) "
			;
		stmt.executeUpdate(sql);

		stmt.close();
	} catch(Exception e) {
		throw new Exception("updateLocationTable(): "+e);
	} catch(Throwable t) {
		throw new Exception("updateLocationTable(): "+t);
	}
}
}
