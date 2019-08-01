import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class HsPopulate
{

    private Connection conn                = null;

    private int        process_first_month = 0;
    private int        process_month       = 0;

    public HsPopulate(Connection conn, int process_first_month, int process_month) throws Exception
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
                +"insert into ngn.hs_history ( "
                +"  hs_history_employee_id, "
                +"  hs_history_process_month, "
                +"  hs_history_title_code, "
                +"  hs_history_index, "
                +"  hs_history_pay_activity_code, "
                +"  hs_history_pay_index, "
                +"  hs_history_group_code, "
                +"  hs_history_current_group_rate, "
                +"  hs_history_current_amount, "
                +"  hs_history_prev_group_rate, "
                +"  hs_history_prev_amount, "
                +"  hs_history_index_subs_ind, "
                +"  hs_history_fund, "
                +"  hs_history_fund_category_code, "
                +"  hs_history_organization, "
                +"  hs_history_program "
                +") "

                +"select "
                +"  ngn_history_employee_id, "
                +"  ngn_history_process_month, "
                +"  ngn_history_title_code, "
                
                // Use the redirected index if one found, default to pay 
                +"  coalesce(nr1.hs_redir_recharge_index,nr2.hs_redir_recharge_index,ngn_history_pay_index), "

                +"  ngn_history_pay_activity_code, "
                +"  ngn_history_pay_index, "
                +"  ngn_history_group_code, "
                +"  0, "
                +"  0, "
                +"  0, "
                +"  0, "

                // If the recharge index was redirected, the substitution indicator is set to Y for easy of reporting.
                // FOP and fund category are the values associated with the recharge index
                +"  case when ngn_history_pay_index != coalesce(nr1.hs_redir_recharge_index,nr2.hs_redir_recharge_index,ngn_history_pay_index) then 'N' else 'Y' end, "
                +"  coalesce(nr1.hs_redir_recharge_fund,nr2.hs_redir_recharge_fund,ngn_history_pay_fund), "
                +"  coalesce(ngn_fund_category_code,'OT'), "
                +"  coalesce(nr1.hs_redir_recharge_orgn,nr2.hs_redir_recharge_orgn,ngn_history_pay_organization), " 
                +"  coalesce(nr1.hs_redir_recharge_prog,nr2.hs_redir_recharge_prog,ngn_history_pay_program) "
                
                +"from ngn.ngn_history "

                +"left outer join ngn.hs_history on "
                +"      hs_history_employee_id = ngn_history_employee_id "
                +"  and hs_history_process_month = ngn_history_process_month "
                +"  and hs_history_title_code = ngn_history_title_code "
                +"  and hs_history_pay_activity_code = ngn_history_pay_activity_code "
                +"  and hs_history_pay_index = ngn_history_pay_index "
                
                // Check for employee redirect. Employee redirect takes priority over general index redirect
                +"left outer join ngn.hs_redirect nr1 on nr1.hs_redir_pay_index = ngn_history_pay_index "
                +"  and nr1.hs_redir_process_month = ngn_history_process_month "
                +"  and nr1.hs_redir_employee_id = ngn_history_employee_id "
                
                // Check for general index redirect
                +"left outer join ngn.hs_redirect nr2 on nr1.hs_redir_process_month is null "
                +"  and nr2.hs_redir_pay_index = ngn_history_pay_index  "
                +"  and nr2.hs_redir_process_month = ngn_history_process_month  "
                +"  and nr2.hs_redir_employee_id = '' "
                
                // Records with recharge in Health Sciences VC Area (if the employee is in VC area, records that are not funded by HS will be filtered out) 
                +"join coa_db.orgnhier_table on "
                +"      orgn_code = coalesce(nr1.hs_redir_recharge_orgn,nr2.hs_redir_recharge_orgn,ngn_history_pay_organization) "
                +"  and code_1 = 'JAAAAA' "

                // After redirecting the recharge index, we need to obtain the fund category code for the new fund 
                +"join ngn.ngn_fund on " 
                +"      ngn_fund_process_month = ngn_history_process_month " 
                +"  and coalesce(nr1.hs_redir_recharge_fund,nr2.hs_redir_recharge_fund,ngn_history_pay_fund) between ngn_fund_fund_lo and ngn_fund_fund_hi "

                +"where "
                
                // Because this process can be rerun, only grab new records
                +"      hs_history_employee_id is null "
                
                +"  and ngn_history_process_month between "+process_first_month+" and "+process_month+" "
                //+"  and ngn_history_loc_group_code in ('MC','FULL','OFFHS', 'OFF') "
                //+"  and ngn_history_loc_group_code in ('MC','FULL','OFFHS', 'OFF' ,'FULLH' ) "     // adding fullh to see if it makes a difference
                +" and coalesce(nr1.hs_redir_recharge_fund,nr2.hs_redir_recharge_fund,ngn_history_pay_fund) not like '60106%' "
                +" and coalesce(nr1.hs_redir_recharge_fund,nr2.hs_redir_recharge_fund,ngn_history_pay_fund) <> '60738A' "
                
                // People in Health Sciences VC Area 
                +"  and ngn_history_vcu_code = '03' " 
            ;
            // @formatter:on

            stmt = conn.createStatement();
            HsControl.rows_inserted = stmt.executeUpdate(sql);
            stmt.close();
        }
        catch (Exception e)
        {
            throw new Exception("HsPopulate: " + e);
        }
        catch (Throwable t)
        {
            throw new Exception("HsPopulate: " + t);
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
