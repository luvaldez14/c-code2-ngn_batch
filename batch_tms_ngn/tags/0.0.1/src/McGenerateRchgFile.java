import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import edu.ucsd.act.business.*;

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

public class McGenerateRchgFile {

public static void main(String args[]) {
	PrintWriter out = null;
	int exit_status = 0;
	try {
		out = new PrintWriter(new FileWriter("/tms/ngn/extracts/MCRECHGBCP"));

		//get process period and accoutning period
		BusinessObject period = BusinessObjectFactory.create("ngn_db","mc_period");
		period.setKey("mc_period.mc_period_status_code",true);
		period.setValue("mc_period.mc_period_status_code","9");
		period.setOrderBy("mc_period.mc_period_process_month",true,true);
		period.retrieve();

		String process_month = period.getStringValue("mc_period.mc_period_process_month");

		//Get Records
		BusinessObject recharges = BusinessObjectFactory.create("ngn_db","mc_recharge");
		String sql = ""
			+"select * "
			+"from ngn.mc_recharge "
			+"order by "
			+"  recharge_employee_id, "
			+"  recharge_pay_title_code, "
			+"	recharge_index, "
			+"  recharge_pay_activity_code, "
			+"	recharge_pay_index "
		;

		Vector records = recharges.executeQuery(sql);

		for(int i=0; i < records.size(); i++) {
			Hashtable row = (Hashtable)records.elementAt(i);

 			out.println(""
				+row.get("RECHARGE_ACCOUNTING_PERIOD")+"\t"
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
				+leftJustified((String)row.get("RECHARGE_CALENDAR_YEAR"),6)
			);
		}

	} catch(Exception e) {
		exit_status = 1;
		System.out.println("Error: "+e);
		e.printStackTrace();
	} catch(Throwable t) {
		exit_status = 1;
		System.out.println("Error: "+t);
	} finally {
		if(out != null) {
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
public static String leftJustified(String data, int maxlength)
{
	StringBuffer dataBuffer = new StringBuffer();
	dataBuffer.append(data);
	int size = maxlength - data.length();
	for (int i=0; i < size; i++) {
		dataBuffer.append(" ");
	}
	return dataBuffer.toString();
}

}