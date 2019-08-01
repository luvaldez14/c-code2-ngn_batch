import java.sql.Connection;
import java.sql.Statement;

/**
 * The function of this process is to prepare the MC History table for posting of the incremental
 * updates performed in MC history posting. Since all results of the MC rechage process are
 * based on differences induced by the posting process, all '_prev_' (previous) variables in the MC
 * history must be set to the value of the corresponding 'current' variables so that differences may
 * be later detected by comparing these variables. All MC history rows created in the posting
 * process have their '_prev_' values set to zero so that all posted activity is included in the
 * differences measured later.
 */
public class HsPreProcess
{

    private static Connection conn                = null;

    private int               process_first_month = 0;
    private int               process_month       = 0;
    private String            status              = "";

    public HsPreProcess(Connection con, int process_first_month, int process_month, String status)
    {
        conn = con;
        this.process_first_month = process_first_month;
        this.process_month = process_month;
        this.status = status;
    }

    public void run(String flag) throws Exception
    {
        try
        {
            if (status.equals("1") || flag.equals("0"))
            {
                // first lock out the process month
                String sql = "update ngn.hs_period set hs_period_status_code = '5' where hs_period_process_month = " + process_month;
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();

                // move current values to previous
                // @formatter:off
    			sql = ""
    				+"update ngn.hs_history set "
    				+"	hs_history_prev_group_rate = hs_history_current_group_rate, "
    				+"	hs_history_prev_amount = hs_history_current_amount "
    				+"where hs_history_process_month >= "+process_first_month+" and hs_history_process_month < "+process_month
    				;
                // @formatter:on

                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();
            }
            else
            {
                // status is 5 and flag is 1 so the process is being run again. Backing out data from previous run
                // move current values to previous
                String sql = "delete from ngn.hs_history where hs_history_process_month = " + process_month;
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(sql);

                // @formatter:off
    			sql = ""
    				+"update ngn.hs_history set "
    				+"	hs_history_current_group_rate = hs_history_prev_group_rate, "
    				+"	hs_history_current_amount = hs_history_prev_amount "
    				+"where hs_history_process_month >= "+process_first_month+" and hs_history_process_month < "+process_month
    				;
                // @formatter:on

                stmt.executeUpdate(sql);
                stmt.close();
            }
        }
        catch (Exception e)
        {
            throw new Exception("HsPreProcess: " + e);
        }
        catch (Throwable t)
        {
            throw new Exception("HsPreProcess: " + t);
        }
    }

}
