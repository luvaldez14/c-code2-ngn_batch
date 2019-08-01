import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The goal of this process is to develop new current period charges, and differential charges for
 * prior periods caused by (a) changes to the CU (fte) adjustments incoming from payroll processing, or
 * (b) retroactive changes to control table parameters. The reason for basing incremental charges
 * for prior periods on changes relative to existing 'prev' values is that these represent cumulative
 * charges that have already been posted to the General Ledger.
 *
 * This process involves extracting all ngn_history rows within the 'current processing window'
 * where there is a difference between the 'current' and 'prev' recharge amount fields. The resulting
 * ngn_recharge rows will be developed based on ngn_history information accumulated at the
 * level of all ngn_history primary key elements except for ngn_history_process_month and
 * ngng_history_pay_index. Differential FTE and amount quantities for the current period will be
 * summed into ngn_recharge 'current' fields and quantities from prior periods will be summed into
 * ngn_recharge 'prior' fields. This may be accomplished using various techniques. I will use one
 * in the following description only for the sake of illustration.
 *
 * The ngn_recharge rows developed in this process are inserted to a temporary table that is part
 * of the NGN Recharge database. The information is later unloaded and passed to the Data
 * Warehouse as one of the 'publishing' tasks described in Section C.9.
 */
public class HsGenerate
{
    private Connection conn                = null;

    private int        process_first_month = 0;
    private int        process_month       = 0;
    private int        financial_month     = 0;

    public HsGenerate(Connection conn, int process_first_month, int process_month, int financial_month) throws Exception
    {
        this.conn = conn;
        this.process_first_month = process_first_month;
        this.process_month = process_month;
        this.financial_month = financial_month;
    }

    public void run() throws Exception
    {
        Statement stmt = null;
        String sql = null;

        try
        {
            stmt = conn.createStatement();

            // initialize table
            sql = "delete from ngn.hs_recharge";
            stmt.executeUpdate(sql);

            // create temp table
            // @formatter:off
    		sql = ""
    			+"DECLARE GLOBAL TEMPORARY TABLE session.hs_recharge ( "
    			+"    hs_history_index char(10) not null, "
    			+"    hs_history_group_code char(2) not null, "
    			+"    hs_history_employee_id char(9) not null, "
    			+"    hs_history_title_code char(4) not null, "
    			+"    hs_history_pay_activity_code char(1) not null, "
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
    			+"    hs_history_process_month int not null, "
    			+"    hs_history_pay_index char(10) "
    			+") ON COMMIT PRESERVE ROWS NOT LOGGED "
    		;
            // @formatter:on
            stmt.executeUpdate(sql);

            // insert initial records into temp table. Pay Index will be set in an update following this
            // @formatter:off
    		sql = ""
    			+"insert into session.hs_recharge ( "
    			+"    hs_history_index, "
    			+"    hs_history_group_code, "
    			+"    hs_history_employee_id, "
    			+"    hs_history_title_code, "
    			+"    hs_history_pay_activity_code, "
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
    			+"    hs_history_process_month "
    			+") "
    			+"select "
    			+"    m.hs_history_index, "
    			+"    m.hs_history_group_code, "
    			+"    m.hs_history_employee_id, "
    			+"    m.hs_history_title_code, "
    			+"    m.hs_history_pay_activity_code, "
    			+"    coalesce(sum(case when m.hs_history_process_month = "+process_month+" then n.ngn_history_current_adj_fte - n.ngn_history_prev_adj_fte else 0 end),0), "
    			+"    coalesce(sum(case when m.hs_history_process_month = "+process_month+" then m.hs_history_current_amount - m.hs_history_prev_amount else 0 end),0), "
    			+"    coalesce(sum(case when m.hs_history_process_month != "+process_month+" then n.ngn_history_current_adj_fte - n.ngn_history_prev_adj_fte else 0 end),0), "
    			+"    coalesce(sum(case when m.hs_history_process_month != "+process_month+" then m.hs_history_current_amount - m.hs_history_prev_amount else 0 end),0), "
    			+"    coalesce(sum(case when m.hs_history_process_month = "+process_month+" then m.hs_history_current_amount - m.hs_history_prev_amount else 0 end),0) "
    			+"    	+ coalesce(sum(case when m.hs_history_process_month != "+process_month+" then m.hs_history_current_amount - m.hs_history_prev_amount else 0 end),0), "
    			+"    coalesce(i.ifis_title,''), "
    			+"    coalesce(f.fund_title,''), "
    			+"    coalesce(o.orgn_title,''), "
    			+"    coalesce(o.orgn_mgr,''), "
    			+"    coalesce(o.orgn_mgr_addr,''), "
    			+"    coalesce(p.program_title,''), "
    			+"    max(m.hs_history_process_month) "
    			+"from ngn.hs_history m "
    			+"join ngn.ngn_history n on "
    			+"	    hs_history_employee_id = ngn_history_employee_id "
    			+"	and hs_history_process_month = ngn_history_process_month "
    			+"	and hs_history_title_code = ngn_history_title_code "
    			+"	and hs_history_pay_activity_code = ngn_history_pay_activity_code "
    			+"	and hs_history_pay_index = ngn_history_pay_index "
    			+"left outer join ngn.tms_ifopl_v i on i.ifis_indx_no = m.hs_history_index "
    			+"left outer join ngn.coa_fund_v f on f.fund_code = m.hs_history_fund  "
    			+"left outer join ngn.tms_org_v o on o.orgn_code = m.hs_history_organization "
    			+"left outer join ngn.coa_program_v p on p.program = m.hs_history_program "
    			+"where hs_history_process_month between "+process_first_month+" and "+process_month+" "
    			+"  and hs_history_current_amount != hs_history_prev_amount "
    			+"  and (hs_history_current_amount - hs_history_prev_amount) not between -0.01 and 0.01 "
    			+"group by "
    			+"    m.hs_history_index, "
    			+"    m.hs_history_group_code, "
    			+"    m.hs_history_employee_id, "
    			+"    m.hs_history_title_code, "
    			+"    m.hs_history_pay_activity_code, "
    			+"    coalesce(i.ifis_title,''), "
    			+"    coalesce(f.fund_title,''), "
    			+"    coalesce(o.orgn_title,''), "
    			+"    coalesce(o.orgn_mgr,''), "
    			+"    coalesce(o.orgn_mgr_addr,''), "
    			+"    coalesce(p.program_title,'') "
    		;
            // @formatter:on
            stmt.executeUpdate(sql);

            // Choose which pay index should be set for employees
            // @formatter:off
    		sql = ""
    			+"update session.hs_recharge m "
    			+"set "
    			+"    hs_history_pay_index = ( "
    			+"    select max(hs_history_pay_index) "
    			+"    from ngn.hs_history m2 "
    			+"    where m.hs_history_index = m2.hs_history_index "
    			+"      and m.hs_history_group_code = m2.hs_history_group_code "
    			+"      and m.hs_history_employee_id = m2.hs_history_employee_id "
    			+"      and m.hs_history_title_code = m2.hs_history_title_code "
    			+"      and m.hs_history_pay_activity_code = m2.hs_history_pay_activity_code "
    			+"      and m2.hs_history_process_month = m.hs_history_process_month "
    			+"      and m2.hs_history_current_amount = ( "
    			+"        select max(m3.hs_history_current_amount) "
    			+"        from ngn.hs_history m3 "
    			+"        where m2.hs_history_index = m3.hs_history_index "
    			+"          and m2.hs_history_group_code = m3.hs_history_group_code "
    			+"          and m2.hs_history_employee_id = m3.hs_history_employee_id "
    			+"          and m2.hs_history_title_code = m3.hs_history_title_code "
    			+"          and m2.hs_history_pay_activity_code = m3.hs_history_pay_activity_code "
    			+"          and m3.hs_history_process_month = m2.hs_history_process_month)) "
    		;
            // @formatter:on
            stmt.executeUpdate(sql);

            // Insert results into hs_recharge table
            // @formatter:off
    		sql = ""
    			+"insert into ngn.hs_recharge ( "
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
    			+"    m.hs_history_index, "
    			+"    m.hs_history_group_code, "
    			+"    m.hs_history_employee_id, "
    			+"    m.hs_history_title_code, "
    			+"    m.hs_history_pay_activity_code, "
    			+"    m2.hs_history_current_group_rate, "
    			+"    m.recharge_current_fte, "
    			+"    m.recharge_current_amount, "
    			+"    m.recharge_prior_fte, "
    			+"    m.recharge_prior_amount, "
    			+"    m.recharge_total_amount, "
    			+"    ' ', "
    			+"    m2.hs_history_index_subs_ind, "
    			+"    m2.hs_history_fund, "
    			+"    m2.hs_history_fund_category_code, "
    			+"    m2.hs_history_organization, "
    			+"    m2.hs_history_program, "
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
    			+"from session.hs_recharge m "
    			+"join ngn.hs_history m2 "
    			+"    on  m.hs_history_index = m2.hs_history_index "
    			+"    and m.hs_history_group_code = m2.hs_history_group_code "
    			+"    and m.hs_history_employee_id = m2.hs_history_employee_id "
    			+"    and m.hs_history_title_code = m2.hs_history_title_code "
    			+"    and m.hs_history_pay_activity_code = m2.hs_history_pay_activity_code "
    			+"    and m2.hs_history_pay_index = m.hs_history_pay_index "
    			+"    and m2.hs_history_process_month = m.hs_history_process_month "
    			+"join ngn.ngn_history n on "
    			+"	    m.hs_history_employee_id = ngn_history_employee_id "
    			+"	and m.hs_history_process_month = ngn_history_process_month "
    			+"	and m.hs_history_title_code = ngn_history_title_code "
    			+"	and m.hs_history_pay_activity_code = ngn_history_pay_activity_code "
    			+"	and m.hs_history_pay_index = ngn_history_pay_index "
    			+"order by "
    			+"    hs_history_employee_id, "
    			+"    hs_history_title_code, "
    			+"    hs_history_index, "
    			+"    hs_history_pay_activity_code, "
    			+"    hs_history_group_code "
    		;
            // @formatter:on
            HsControl.recharge_rows_inserted = stmt.executeUpdate(sql);

            // clean up temp table
            stmt.executeUpdate("drop table session.hs_recharge");

            // updating multiple entry id field
            // @formatter:off
    		sql = ""
    			+"update	ngn.hs_recharge "
    			+"set		recharge_multiple_entry_ind = '*' "
    			+"where	recharge_employee_id in ( "
    			+"	select		recharge_employee_id "
    			+"	from		ngn.hs_recharge "
    			+"	group by	recharge_employee_id "
    			+"	having		count(*) > 1 "
    			+") "
    		;
            // @formatter:on
            stmt.executeUpdate(sql);

        }
        catch (Exception e)
        {
            throw new Exception("HsGenerate: " + e);
        }
        catch (Throwable t)
        {
            throw new Exception("HsGenerate: " + t);
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
