import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import edu.ucsd.act.business.*;

public class McGenerateGL {

private static int[] dayLookup = {
//	jan,feb,mar,apr,may,jun,jul,aug,sep,oct,nov,dec
	31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
};

public static void main(String args[]) {
	PrintWriter out = null;
	int exit_status = 0;
	try {
		out = new PrintWriter(new FileWriter("/tms/ngn/extracts/MCGLDOC"));

		//get process period and accoutning period
		BusinessObject period = BusinessObjectFactory.create("ngn_db","mc_period");
		period.setKey("mc_period.mc_period_status_code",true);
		period.setValue("mc_period.mc_period_status_code","9");
		period.setOrderBy("mc_period.mc_period_process_month",true,true);
		period.retrieve();

		String calendar_year = period.getStringValue("mc_period.mc_period_process_month");
		String accounting_period = period.getStringValue("mc_period.mc_period_financial_period");

		//for all record
		//String subsystem = "PAYROLL1";
		String subsystem = "MEDCTR2 ";
		String univ_id = "01";

		String document_no = "FHMCN";
		int month = Integer.parseInt(accounting_period.substring(4));
		String mm = ""+month;
		if(month < 10) mm = "0"+month;
		document_no = document_no + mm + "1";

		//header records only
		String header_rec = "1";
		String header_desc = "MED CENTER NETWORK SUPPORT SVC";
		int cal_month = Integer.parseInt(calendar_year.substring(4));

		int temp_day = dayLookup[cal_month-1];
		if(cal_month == 2 && Integer.parseInt(calendar_year.substring(0,4))%4 == 0)
			temp_day = 29;
		String month_end = calendar_year + temp_day;


		//detail records only
		String detail_rec = "2";
		String journal_type = "F904";
		String detail_desc = "MED CTR NETWORK SUPPORT RECHG";
		String coa_code = "A";
		String expense_acct = "634107";

		//records to be entered into ledger
		BusinessObject recharges = BusinessObjectFactory.create("ngn_db","mc_recharge");
		String sql = ""
			+"select "
			+"	recharge_index, "
			+"	recharge_fund, "
			+"	recharge_organization, "
			+"	recharge_program, "
			+"	sum(recharge_total_amount) as total_amount "
			+"from ngn.mc_recharge "
			+"where "
			+"	recharge_accounting_period = "+accounting_period+" "
			+"group by "
			+"	recharge_index, "
			+"	recharge_fund, "
			+"	recharge_organization, "
			+"	recharge_program "
			+"order by "
			+"	recharge_index "
		;
		Vector records = recharges.executeQuery(sql);

		int sequence_no = 1;
		int sum = 0;
		int unsigned_sum = 0;
		StringBuffer gl = new StringBuffer();

		for(int i=0; i < records.size(); i++) {
			Hashtable record = (Hashtable)records.elementAt(i);

			String amount = (String)record.get("total_amount".toUpperCase());
			amount = escapeChar(amount,'.');
			int amt = Integer.parseInt(amount);
			int uamt = amt;
			String cd_db = "D"; //credit or debit
			if(uamt < 0) {
				cd_db = "C";
				uamt = uamt * -1; //removing the sign
			}
			sum += amt;
			unsigned_sum += uamt;
			amount = ""+uamt;

			if(amt != 0) {
				gl.append(""
					+subsystem
					+univ_id
					+document_no
					+detail_rec
					+rightJustified(""+sequence_no,4,"0")
					+journal_type
					+rightJustified(""+amount,12,"0")
					+leftJustified(detail_desc,35," ")
					+cd_db
					+coa_code
					+record.get("recharge_fund".toUpperCase())
					+record.get("recharge_organization".toUpperCase())
					+expense_acct
					+record.get("recharge_program".toUpperCase())
					+leftJustified(" ",6," ")
					+leftJustified(" ",6," ")
					+((String)record.get("recharge_index".toUpperCase())).trim()
					+"\n"
				);

				sequence_no++;
			}
		}

		//MC Recharge account(s). Written when there is only one account, but can handle multiple
		BusinessObject mc_revenue = BusinessObjectFactory.create("ngn_db","mc_revenue");
		mc_revenue.setKey("mc_revenue.mc_revenue_process_month",true);
		mc_revenue.setValue("mc_revenue.mc_revenue_process_month",calendar_year);
		mc_revenue.retrieve();

		DecimalFormat precision = new DecimalFormat( "#0.00" );   // value format
		Vector redist = new Vector();
		int redist_sum = 0;
		do {
			Hashtable data = new Hashtable();
			double factor = Double.parseDouble(mc_revenue.getStringValue("mc_revenue.mc_revenue_share_factor"));
			String amount = escapeChar(precision.format(factor * sum),'.');
			redist_sum += Integer.parseInt(amount);

			data.put("amount", amount);
			data.put("desc",mc_revenue.getStringValue("mc_revenue.mc_revenue_ledger_desc"));
			data.put("credit_debit",mc_revenue.getStringValue("mc_revenue.mc_revenue_credit_debit"));
			data.put("indx",mc_revenue.getStringValue("mc_revenue.mc_revenue_index"));
			data.put("fund",mc_revenue.getStringValue("mc_revenue.mc_revenue_fund"));
			data.put("orgn",mc_revenue.getStringValue("mc_revenue.mc_revenue_orgn"));
			data.put("acct",mc_revenue.getStringValue("mc_revenue.mc_revenue_acct"));
			data.put("prog",mc_revenue.getStringValue("mc_revenue.mc_revenue_prog"));
			redist.add(data);
		} while(mc_revenue.next() != null);

		if(redist_sum != sum) {
			int diff = sum - redist_sum;
			int biggest = 0;
			int index = 0;
			for(int i = 0; i < redist.size(); i++) {
				int amount = Integer.parseInt((String)((Hashtable)redist.elementAt(i)).get("amount"));
				if(amount > biggest) {
					biggest = amount;
					index = i;
				}
			}
			Hashtable data = (Hashtable)redist.elementAt(index);
			int amount = Integer.parseInt((String)data.get("amount"));
			amount += diff;
			data.put("amount",""+amount);
		}

		for(int i = 0; i < redist.size(); i++) {
			Hashtable data = (Hashtable)redist.elementAt(i);

			String amount = (String)data.get("amount");
			unsigned_sum += Double.parseDouble(amount);

			gl.append(""
				+subsystem
				+univ_id
				+document_no
				+detail_rec
				+rightJustified(""+sequence_no,4,"0")
				+journal_type
				+rightJustified(amount,12,"0")
				+leftJustified((String)data.get("desc"),35," ")
				+data.get("credit_debit")
				+coa_code
				+data.get("fund")
				+data.get("orgn")
				+data.get("acct")
				+data.get("prog")
				+leftJustified(" ",6," ")
				+leftJustified(" ",6," ")
				+data.get("indx")
				+"\n"
			);
			sequence_no++;
		}

		//tacking the header onto the beginning of the gl
		gl.insert(0,""
			+subsystem
			+univ_id
			+document_no
			+header_rec
			+leftJustified(header_desc,35," ")
			+month_end
			+rightJustified(""+unsigned_sum,12,"0")
			+"N"
			+"\n"
		);

		out.print(gl.toString());
	} catch(Exception e) {
		System.err.println("Error: "+e);
		exit_status = 1;
	} catch(Throwable t) {
		System.err.println("Error: "+t);
		exit_status = 1;
	} finally {
		if(out != null) {
			out.flush();
			out.close();
		}
		System.exit(exit_status);
	}
}

private static String escapeChar (String data, char c)
{
	StringBuffer result = new StringBuffer();
	int strlen = data.length();
	char chars[] = data.toCharArray() ;
	for (int i=0; i < strlen; i++) {
		if ( chars[i] != c)
		result.append(chars[i]);
	}
	return result.toString();
}

/**
 * Pad given pad string to the beginning of data string
 * to given length
 */
public static String rightJustified(String data, int maxlength, String pad)
{
	StringBuffer dataBuffer = new StringBuffer();
	int size = maxlength - data.length();
	for (int i=0; i < size; i++) {
		dataBuffer.append(pad);
	}
	dataBuffer.append(data);
	return dataBuffer.toString();
}

/**
 * Pad given pad string to the end of data string
 * to given length
 */
public static String leftJustified(String data, int maxlength, String pad)
{
	StringBuffer dataBuffer = new StringBuffer();
	dataBuffer.append(data);
	int size = maxlength - data.length();
	for (int i=0; i < size; i++) {
		dataBuffer.append(pad);
	}
	return dataBuffer.toString();
}

}