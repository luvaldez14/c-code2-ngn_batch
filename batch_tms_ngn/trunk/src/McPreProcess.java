import java.io.*;
import java.util.*;
import java.sql.*;
import edu.ucsd.act.db.connectionpool.*;
import edu.ucsd.act.jlink.util.JLinkDate;

/**
 * The function of this process is to prepare the MC History table for posting of the incremental
 * updates performed in MC history posting. Since all results of the MC rechage process are
 * based on differences induced by the posting process, all '_prev_' (previous) variables in the MC
 * history must be set to the value of the corresponding 'current' variables so that differences may
 * be later detected by comparing these variables. All MC history rows created in the posting
 * process have their '_prev_' values set to zero so that all posted activity is included in the
 * differences measured later.
 */
public class McPreProcess {

private static Connection conn = null;
private static String db = "ngn_db";

private int process_first_month = 0;
private int process_month = 0;
private String status = "";

public McPreProcess(Connection conn, int process_first_month, int process_month, String status) {
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
			String sql = "update ngn.mc_period set mc_period_status_code = '5' where mc_period_process_month = "+process_month;
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();

			//move current values to previous
			sql = ""
				+"update ngn.mc_history set "
				+"	mc_history_prev_group_rate = mc_history_current_group_rate, "
				+"	mc_history_prev_amount = mc_history_current_amount "
				+"where mc_history_process_month >= "+process_first_month+" and mc_history_process_month < "+process_month
				;
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
		} else {
			//status is 5 and flag is 1 so the process is being run again. Backing out data from previous run
			//move current values to previous
			String sql = "delete from ngn.mc_history where mc_history_process_month = "+process_month;
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);

			sql = ""
				+"update ngn.mc_history set "
				+"	mc_history_current_group_rate = mc_history_prev_group_rate, "
				+"	mc_history_current_amount = mc_history_prev_amount "
				+"where mc_history_process_month >= "+process_first_month+" and mc_history_process_month < "+process_month
				;
			stmt.executeUpdate(sql);
			stmt.close();

			//delete any PDFs that were generated already
			String FS = File.separator;
			String path = FS+"opt2"+FS+"filerepository"+FS+"files"+FS+"telecomStatements"+FS;
			File files = new File(path);
			String[] list = files.list();
			if(list != null) {
				for(int i=0; i < list.length; i++) {
					String file_name = list[i];
					if(file_name.startsWith("MCNBill_"+process_month+"_")) {
						File rm = new File(path+file_name);
						System.out.println("deleting "+file_name);
						rm.delete();
					}
				}
			}
		}
	} catch(Exception e) {
		throw new Exception("McPreProcess: "+e);
	} catch(Throwable t) {
		throw new Exception("McPreProcess: "+t);
	}
}

}
