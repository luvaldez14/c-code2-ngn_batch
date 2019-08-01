import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Vector;
import edu.ucsd.act.business.BusinessObject;
import edu.ucsd.act.business.BusinessObjectFactory;
import edu.ucsd.act.jlink.JLinkJndi;

/******************************************************************************
 * This class was created when we converted to DB2. Previously we were using
 * the bulk copy (bcp) utility in Sybase to dump the records from mc_recharge.
 * This created a tab delimited file of the records. This was then transfered
 * to data warehouse for upload into ga_extension. The DB2 export function
 * outputs a different format. This process was set up to generate a file to
 * mimic the BCP output. DB2 ga_extension will now retrieve the datadirectly
 * from the NGN schema. Once Sybase is no longer in use this class will no
 * longer be needed.
 *****************************************************************************/

public class HsGenerateRchgFile
{
    public final static String MSG_PATH     = JLinkJndi.getContextDocsPath() + "../msgs/";
    public final static String EXTRACT_PATH = JLinkJndi.getContextDocsPath() + "../extracts/";

    public static void main(String args[])
    {
        PrintWriter out = null;
        int exit_status = 0;
        try
        {
            out = new PrintWriter(new FileWriter(EXTRACT_PATH + "HSRECHGBCP"));

            // get process period and accounting period
            BusinessObject period = BusinessObjectFactory.create("ngn_db", "hs_period");
            period.setKey("hs_period.hs_period_status_code", true);
            period.setValue("hs_period.hs_period_status_code", "9");
            period.setOrderBy("hs_period.hs_period_process_month", true, true);
            period.retrieve();

            // Get Records
            BusinessObject recharges = BusinessObjectFactory.create("ngn_db", "hs_recharge");

        // @formatter:off
		String sql = ""
			+"select * "
			+"from ngn.hs_recharge "
			+"order by "
			+"  recharge_employee_id, "
			+"  recharge_pay_title_code, "
			+"	recharge_index, "
			+"  recharge_pay_activity_code, "
			+"	recharge_pay_index "
		;
        // @formatter:on

            @SuppressWarnings("rawtypes")
            Vector records = recharges.executeQuery(sql);

            for (int i = 0; i < records.size(); i++)
            {
                @SuppressWarnings("rawtypes")
                Hashtable row = (Hashtable)records.elementAt(i);

                String accounting_period = (String)row.get("RECHARGE_ACCOUNTING_PERIOD");
                String calendar_year = (String)row.get("RECHARGE_CALENDAR_YEAR");

                /* On the very first run of the HS IT Shared Services recharge, we ran both July and August in the same month therefore we have special 
                 * handling such as alternate descriptions for the July file (July data was run in August so we're identifying it as such) */
                
                if (calendar_year.equals("201707"))
                {
                    accounting_period = "201802";
                    calendar_year = "201708";
                }

            // @formatter:off
 			out.println(""
				+accounting_period+"\t"
				+leftJustified((String)row.get("RECHARGE_INDEX"),10)+"\t"
				+leftJustified((String)row.get("RECHARGE_GROUP_CODE"),2)+"\t"
				+leftJustified((String)row.get("RECHARGE_EMPLOYEE_ID"),9)+"\t"
				+leftJustified((String)row.get("RECHARGE_PAY_TITLE_CODE"),4)+"\t"
				+((String)row.get("RECHARGE_PAY_ACTIVITY_CODE")).trim()+"\t"
				+row.get("RECHARGE_CURRENT_GROUP_RATE")+"\t"
				+row.get("RECHARGE_CURRENT_FTE")+"\t"
				+row.get("RECHARGE_CURRENT_AMOUNT")+"\t"
				+row.get("RECHARGE_PRIOR_FTE")+"\t"
				+row.get("RECHARGE_PRIOR_AMOUNT")+"\t"
				+row.get("RECHARGE_TOTAL_AMOUNT")+"\t"
				+leftJustified((String)row.get("RECHARGE_MULTIPLE_ENTRY_IND"),1)+"\t"
				+leftJustified((String)row.get("RECHARGE_INDEX_SUBS_IND"),1)+"\t"
				+row.get("RECHARGE_FUND")+"\t"
				+row.get("RECHARGE_FUND_CATEGORY_CODE")+"\t"
				+row.get("RECHARGE_ORGANIZATION")+"\t"
				+row.get("RECHARGE_PROGRAM")+"\t"
				+leftJustified((String)row.get("RECHARGE_PAY_INDEX"),6)+"\t"
				+row.get("RECHARGE_PAY_FUND")+"\t"
				+row.get("RECHARGE_PAY_ORGANIZATION")+"\t"
				+row.get("RECHARGE_PAY_PROGRAM")+"\t"
				+leftJustified((String)row.get("RECHARGE_EMPLOYEE_NAME"),26)+"\t"
				+leftJustified((String)row.get("RECHARGE_GROUP_DESC"),15)+"\t"
				+leftJustified((String)row.get("RECHARGE_HOME_DEPT_CODE"),6)+"\t"
				+((String)row.get("RECHARGE_HOME_DEPT_DESC")).trim()+"\t"
				+leftJustified((String)row.get("RECHARGE_VCU_CODE"),2)+"\t"
				+leftJustified((String)row.get("RECHARGE_VCU_DESC"),15)+"\t"
				+leftJustified((String)row.get("RECHARGE_INDEX_DESC"),35)+"\t"
				+leftJustified((String)row.get("RECHARGE_FUND_DESC"),35)+"\t"
				+leftJustified((String)row.get("RECHARGE_ORGANIZATION_DESC"),35)+"\t"
				+leftJustified((String)row.get("RECHARGE_PROGRAM_DESC"),35)+"\t"
				+leftJustified((String)row.get("RECHARGE_FINANCIAL_MGR_NAME"),35)+"\t"
				+leftJustified((String)row.get("RECHARGE_FINANCIAL_MGR_ADDR"),6)+"\t"
				+leftJustified(calendar_year,6)
			);
            // @formatter:on
            }

        }
        catch (Exception e)
        {
            exit_status = 1;
            System.out.println("Error: " + e);
            e.printStackTrace();
        }
        catch (Throwable t)
        {
            exit_status = 1;
            System.out.println("Error: " + t);
        }
        finally
        {
            if (out != null)
            {
                out.flush();
                out.close();
            }
            System.exit(exit_status);
        }
    }

    /**
     * Pad given pad string to the end of data string
     * to given length
     */
    private static String leftJustified(String data, int maxlength)
    {
        StringBuffer dataBuffer = new StringBuffer();
        dataBuffer.append(data);
        int size = maxlength - data.length();
        for (int i = 0; i < size; i++)
        {
            dataBuffer.append(" ");
        }
        return dataBuffer.toString();
    }

}
