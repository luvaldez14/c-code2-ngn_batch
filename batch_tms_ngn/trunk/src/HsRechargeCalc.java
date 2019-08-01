import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class HsRechargeCalc
{

    private Connection conn                = null;

    private int        process_first_month = 0;
    private int        process_month       = 0;

    public HsRechargeCalc(Connection conn, int process_first_month, int process_month) throws Exception
    {
        this.conn = conn;
        this.process_first_month = process_first_month;
        this.process_month = process_month;
    }

    public void run() throws Exception
    {
        Statement stmt = null;
        String sql = null;

        try
        {
            // @formatter:off

        	sql = ""
    			+"merge into ngn.hs_history mch "
    			+"using ( "
    			+"	select "
    			+"       hs_rate_amount as current_group_rate, "
    			+"       hs_rate_amount * ngn_history_current_adj_fte as current_amount, "
    			+"       ngn_history_employee_id, "
    			+"       ngn_history_process_month, "
    			+"       ngn_history_title_code, "
    			+"       ngn_history_pay_activity_code, "
    			+"       ngn_history_pay_index "
    			+"	from ngn.ngn_history "
    			+"	join ngn.hs_rate on hs_rate_process_month = ngn_history_process_month and hs_rate_loc_group_code = ngn_history_loc_group_code and hs_rate_cu_type_code = ngn_history_cu_type_code "
    			+"	where ngn_history_process_month between "+process_first_month+" and "+process_month+" "
    		    //+"   and ngn_history_loc_group_code in ('MC','FULL','OFFHS', 'OFF','FULLH' )"
    		    +"  and ngn_history_vcu_code = '03' " 
    			+") as ngnh "
    			+"on "
    			+"	    mch.hs_history_employee_id = ngnh.ngn_history_employee_id "
    			+"	and mch.hs_history_process_month = ngnh.ngn_history_process_month "
    			+"	and mch.hs_history_title_code = ngnh.ngn_history_title_code "
    			+"	and mch.hs_history_pay_activity_code = ngnh.ngn_history_pay_activity_code "
    			+"	and mch.hs_history_pay_index = ngnh.ngn_history_pay_index "
    			+"when matched then "
    			+"update set "
    			+"	mch.hs_history_current_group_rate = ngnh.current_group_rate, "
    			+"	mch.hs_history_current_amount = ngnh.current_amount "
    			;
            // @formatter:on

            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
        }
        catch (Exception e)
        {
            throw new Exception("hsRechargeCalc: " + e);
        }
        catch (Throwable t)
        {
            throw new Exception("HsRechargeCalc: " + t);
        }
        finally
        {
            try
            {
                if (stmt != null)
                {
                    stmt.close();
                }
            }
            catch (SQLException se)
            {}
        }
    }

}
