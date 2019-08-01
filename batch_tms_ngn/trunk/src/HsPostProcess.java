import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class HsPostProcess
{

    private static Connection conn          = null;

    private int               process_month = 0;

    public HsPostProcess(Connection con, int process_month)
    {
        conn = con;
        this.process_month = process_month;
    }

    public void run() throws Exception
    {
        try
        {
            // first close the current process month and set the next to active
            String sql = "update ngn.hs_period set hs_period_status_code = '9' where hs_period_process_month = " + process_month;
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();

            sql = "select hs_period_process_month from ngn.hs_period where hs_period_status_code = '0' order by hs_period_process_month";
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next())
            {
                int new_process_month = rs.getInt(1);

                sql = "update ngn.hs_period set hs_period_status_code = '1' where hs_period_process_month = " + new_process_month;
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();

                // move forward redirect values
                // @formatter:off
    			sql = ""
    				+"insert into ngn.hs_redirect ( "
    				+"	hs_redir_process_month, "
    				+"	hs_redir_pay_index, "
    				+"	hs_redir_employee_id, "
    				+"	hs_redir_recharge_index, "
    				+"	hs_redir_pay_fund, "
    				+"	hs_redir_pay_orgn, "
    				+"	hs_redir_pay_prog, "
    				+"	hs_redir_recharge_fund, "
    				+"	hs_redir_recharge_orgn, "
    				+"	hs_redir_recharge_prog, "
    				+"	hs_redir_employee_name, "
    				+"	hs_redir_change_source, "
    				+"	hs_redir_change_userid, "
    				+"	hs_redir_change_date "
    				+") "
    				+"select "
    				+"	"+new_process_month+", "
    				+"	hs_redir_pay_index, "
    				+"	hs_redir_employee_id, "
    				+"	hs_redir_recharge_index, "
    				+"	hs_redir_pay_fund, "
    				+"	hs_redir_pay_orgn, "
    				+"	hs_redir_pay_prog, "
    				+"	hs_redir_recharge_fund, "
    				+"	hs_redir_recharge_orgn, "
    				+"	hs_redir_recharge_prog, "
    				+"	hs_redir_employee_name, "
    				+"	hs_redir_change_source, "
    				+"	hs_redir_change_userid, "
    				+"	hs_redir_change_date "
    				+"from ngn.hs_redirect "
    				+"where hs_redir_process_month = "+process_month
    				;
                // @formatter:on
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();

                // move forward rows for mc_rate
                // @formatter:off
    			sql = ""
    				+"insert into ngn.hs_rate ( "
    				+"	hs_rate_process_month, "
    				+"	hs_rate_loc_group_code, "
    				+"	hs_rate_cu_type_code, "
    				+"	hs_rate_group_code, "
    				+"	hs_rate_group_desc, "
    				+"	hs_rate_amount, "
    				+"	hs_rate_change_userid, "
    				+"	hs_rate_change_date "
    				+") "
    				+"select "
    				+"	"+new_process_month+", "
    				+"	hs_rate_loc_group_code, "
    				+"	hs_rate_cu_type_code, "
    				+"	hs_rate_group_code, "
    				+"	hs_rate_group_desc, "
    				+"	hs_rate_amount, "
    				+"	hs_rate_change_userid, "
    				+"	hs_rate_change_date "
    				+"from ngn.hs_rate "
    				+"where hs_rate_process_month = "+process_month
    				;
                // @formatter:on
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();

                // move forward rows for mc_revenue
                // @formatter:off
    			sql = ""
    				+"insert into ngn.hs_revenue ( "
    				+"	hs_revenue_process_month, "
    				+"	hs_revenue_loc_group_code, "
    				+"	hs_revenue_index, "
    				+"	hs_revenue_credit_debit, "
    				+"	hs_revenue_share_factor, "
    				+"	hs_revenue_fund, "
    				+"	hs_revenue_orgn, "
    				+"	hs_revenue_acct, "
    				+"	hs_revenue_prog, "
    				+"	hs_revenue_ledger_desc, "
    				+"	hs_revenue_change_userid, "
    				+"	hs_revenue_change_date "
    				+") "
    				+"select "
    				+"	"+new_process_month+", "
    				+"	hs_revenue_loc_group_code, "
    				+"	hs_revenue_index, "
    				+"	hs_revenue_credit_debit, "
    				+"	hs_revenue_share_factor, "
    				+"	hs_revenue_fund, "
    				+"	hs_revenue_orgn, "
    				+"	hs_revenue_acct, "
    				+"	hs_revenue_prog, "
    				+"	hs_revenue_ledger_desc, "
    				+"	hs_revenue_change_userid, "
    				+"	hs_revenue_change_date "
    				+"from ngn.hs_revenue "
    				+"where hs_revenue_process_month = "+process_month
    				;
                // @formatter:on
                stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();
            }
        }
        catch (Exception e)
        {
            throw new Exception("HsPostProcess: " + e);
        }
        catch (Throwable t)
        {
            throw new Exception("HsPostProcess: " + t);
        }
    }

}
