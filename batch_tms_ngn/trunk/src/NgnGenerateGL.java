import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import edu.ucsd.act.business.*;

public class NgnGenerateGL {

private static int[] dayLookup = {
//	jan,feb,mar,apr,may,jun,jul,aug,sep,oct,nov,dec
	31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
};

public static void main(String args[]) {
	PrintWriter out = null;
	int exit_status = 0;
	try {
		out = new PrintWriter(new FileWriter("/tms/ngn/extracts/GLDOC"));
//		out = new PrintWriter(new FileWriter("/tmdata/prod_test/ngn/extracts/GLDOC"));

		//get process period and accoutning period
		BusinessObject period = BusinessObjectFactory.create("ngn_db","ngn_period");
		period.setKey("ngn_period.ngn_period_status_code",true);
		period.setValue("ngn_period.ngn_period_status_code","9");
		period.setOrderBy("ngn_period.ngn_period_process_month",true,true);
		period.retrieve();

		String calendar_year = period.getStringValue("ngn_period.ngn_period_process_month");
		String accounting_period = period.getStringValue("ngn_period.ngn_period_financial_period");

		//for all record
		String subsystem = "TELECOM1";
		String univ_id = "01";

		String document_no = "FRNGN";
		int month = Integer.parseInt(accounting_period.substring(4));
		String mm = ""+month;
		if(month < 10) mm = "0"+month;
		document_no = document_no + mm + "1";

		//header records only
		String header_rec = "1";
		String header_desc = "NGN COMMUNICATION USER-FINL - M0508";
		int cal_month = Integer.parseInt(calendar_year.substring(4));

		int temp_day = dayLookup[cal_month-1];
		if(cal_month == 2 && Integer.parseInt(calendar_year.substring(0,4))%4 == 0)
			temp_day = 29;
		String month_end = calendar_year + temp_day;


		//detail records only
		String detail_rec = "2";
		String journal_type = "F824";
		String detail_desc = "NGN COMMUNICATION USER RECHARGE";
		String coa_code = "A";
		String expense_acct = "634015";

		//records to be entered into ledger
		BusinessObject recharges = BusinessObjectFactory.create("ngn_db","ngn_recharge");
		String sql = ""
			+"select "
			+"	recharge_index, "
			+"	recharge_fund, "
			+"	recharge_organization, "
			+"	recharge_program, "
			+"	sum(recharge_total_amount) as total_amount "
			+"from ngn.ngn_recharge "
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

		//Get totals for each group Full(1)/Med Center(2)/Off Campus(3)
		sql = ""
			+"select "
			+"	substr(recharge_group_code,2,1) as group_type, "
			+"	sum(recharge_total_amount) as group_total "
			+"from ngn.ngn_recharge "
			+"where recharge_accounting_period = "+accounting_period+" "
			+"group by substr(recharge_group_code,2,1) "
		;
		Vector groups = recharges.executeQuery(sql);

		double full_total = 0;
		double mc_total = 0;
		double off_total = 0;
		double full_hs_total=0;
		double off_hs_total=0;
		
		for(int i=0; i < groups.size(); i++) {
			Hashtable group = (Hashtable)groups.elementAt(i);
			int group_type = Integer.parseInt((String)group.get("group_type".toUpperCase()));
			switch(group_type) {
				case 1: //FULL
					full_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
					break;
				case 2: //MEDICAL CENTER
					mc_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
					break;
				case 3: //OFF CAMPUS
					off_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
					break;
				case 6: //OFFHS - OFF HS is a new rate code for NGN and HS - 10/10/2017  
					off_hs_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
					break;
				case 7: //FULLH - FULL HS is a new rate code for NGN and HS - 10/10/2017  
					full_hs_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
					break;
			}
		}

		//NGN accounts
		BusinessObject ngn_revenue = BusinessObjectFactory.create("ngn_db","ngn_revenue");
		ngn_revenue.setKey("ngn_revenue.ngn_revenue_process_month",true);
		ngn_revenue.setValue("ngn_revenue.ngn_revenue_process_month",calendar_year);
		ngn_revenue.setCriteria("ngn_revenue.ngn_revenue_loc_group_code",BusinessObjectCriteria.NOT_NULL);
		ngn_revenue.setOrderBy("ngn_revenue.ngn_revenue_loc_group_code",true);
		ngn_revenue.setOrderBy("ngn_revenue.ngn_revenue_share_factor",true,true);
		ngn_revenue.retrieve();

		DecimalFormat precision = new DecimalFormat( "#0.00" );   // value format
		Vector redist = new Vector();
		int redist_sum = 0;
		do {
			Hashtable data = new Hashtable();
			double factor = Double.parseDouble(ngn_revenue.getStringValue("ngn_revenue.ngn_revenue_share_factor"));
			String group_type = ngn_revenue.getStringValue("ngn_revenue.ngn_revenue_loc_group_code");
			String amount = "";
			if(group_type.equals("FULL")) {
				amount = escapeChar(precision.format(factor * full_total),'.');
			} else if(group_type.equals("MC")) {
				amount = escapeChar(precision.format(factor * mc_total),'.');
			} else if(group_type.equals("OFF")) {
				amount = escapeChar(precision.format(factor * off_total),'.');
			} else if(group_type.equals("OFFHS")) {
				amount = escapeChar(precision.format(factor * off_hs_total),'.');
			} else if(group_type.equals("FULLH")) {
				amount = escapeChar(precision.format(factor * full_hs_total),'.');
			}
			redist_sum += Integer.parseInt(amount);

			data.put("amount", amount);
			data.put("desc",ngn_revenue.getStringValue("ngn_revenue.ngn_revenue_ledger_desc"));
			data.put("credit_debit",ngn_revenue.getStringValue("ngn_revenue.ngn_revenue_credit_debit"));
			data.put("indx",ngn_revenue.getStringValue("ngn_revenue.ngn_revenue_index"));
			data.put("fund",ngn_revenue.getStringValue("ngn_revenue.ngn_revenue_fund"));
			data.put("orgn",ngn_revenue.getStringValue("ngn_revenue.ngn_revenue_orgn"));
			data.put("acct",ngn_revenue.getStringValue("ngn_revenue.ngn_revenue_acct"));
			data.put("prog",ngn_revenue.getStringValue("ngn_revenue.ngn_revenue_prog"));
			redist.add(data);
		} while(ngn_revenue.next() != null);

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
			String desc = (String)data.get("desc");

			unsigned_sum += Double.parseDouble(amount);

			gl.append(""
				+subsystem
				+univ_id
				+document_no
				+detail_rec
				+rightJustified(""+sequence_no,4,"0")
				+journal_type
				+rightJustified(amount,12,"0")
				+leftJustified(desc,35," ")
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
		e.printStackTrace();
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