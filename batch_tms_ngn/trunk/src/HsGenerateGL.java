import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Vector;
import edu.ucsd.act.business.BusinessObject;
import edu.ucsd.act.business.BusinessObjectCriteria;
import edu.ucsd.act.business.BusinessObjectFactory;
import edu.ucsd.act.jlink.JLinkJndi;

public class HsGenerateGL
{
    public final static String MSG_PATH     = JLinkJndi.getContextDocsPath() + "../msgs/";
    public final static String EXTRACT_PATH = JLinkJndi.getContextDocsPath() + "../extracts/";

    private static int[]       dayLookup    = {
        // jan,feb,mar,apr,may,jun,jul,aug,sep,oct,nov,dec
        31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    @SuppressWarnings("unchecked")
    public static void main(String args[])
    {
        PrintWriter out = null;
        int exit_status = 0;
        try
        {
            out = new PrintWriter(new FileWriter(EXTRACT_PATH + "HSGLDOC"));

            // get process period and accounting period
            BusinessObject period = BusinessObjectFactory.create("ngn_db", "hs_period");
            period.setKey("hs_period.hs_period_status_code", true);
            period.setValue("hs_period.hs_period_status_code", "9");
            period.setOrderBy("hs_period.hs_period_process_month", true, true);
            period.retrieve();

            String calendar_year = period.getStringValue("hs_period.hs_period_process_month");
            String accounting_period = period.getStringValue("hs_period.hs_period_financial_period");

            /* On the very first run of the HS IT Shared Services recharge, we ran both July and August in the same month therefore we have special 
             * handling such as alternate descriptions for the July file (July data was run in August so we're identifying it as such) and for the 
             * August file (it's the second GL we're loading so the document number reflects that) */
            boolean first_batch_run = false;
            boolean second_batch_run = false;
            if (calendar_year.equals("201707"))
            {
                first_batch_run = true;
            }
            else if (calendar_year.equals("201708"))
            {
                second_batch_run = true;
            }

            // for all record
            String subsystem = "HLTHSCI1";
            String univ_id = "01";

            // String document_no = "FRITS";
            String document_no = "FHSTS";
            int month = Integer.parseInt(accounting_period.substring(4));

            // TODO when running actual August you can remove || second_batch_run. Just needed for testing
            if (first_batch_run || second_batch_run)
            {
                month = 2; // 201802 is August 2017's accounting period
            }
            String mm = "" + month;
            if (month < 10) mm = "0" + month;
            document_no = document_no + mm + "1";

            if (second_batch_run)
            {
                // Two documents will be loaded in August so second run needs to advance the document number
                document_no = "FHSTS" + mm + "2";
            }

            // header records only
            String header_rec = "1";
            String header_desc = "HS-TSC TECHNOLOGY SERVICES CHARGE";
            int cal_month = Integer.parseInt(calendar_year.substring(4));
            int cal_year = Integer.parseInt(calendar_year.substring(0, 4));
            int temp_day = dayLookup[cal_month - 1];
            if (cal_month == 2 && cal_year % 4 == 0) temp_day = 29;
            String month_end = calendar_year + temp_day;

            // TODO when running actual August you can remove || second_batch_run. Just needed for testing
            if (first_batch_run || second_batch_run)
            {
                month_end = "20170831";
            }

            // detail records only
            String detail_rec = "2";
            String journal_type = "F823";
            String detail_desc = "HS TECHNOLOGY SERVICES CHARGE";
            if (first_batch_run)
            {
                detail_desc = "HS TECHNOLOGY SERVICES CHARGE-JULY";
            }
            String coa_code = "A";
            String expense_acct = "634454";  // changing acct number per Fritz from 564454 to 634454

            // records to be entered into ledger
            BusinessObject recharges = BusinessObjectFactory.create("ngn_db", "hs_recharge");

            // @formatter:off
            String sql = "" 
                + "select " 
                + "	recharge_index, " 
                + "	recharge_fund, " 
                + "	recharge_organization, " 
                + "	recharge_program, " 
                + "	sum(recharge_total_amount) as total_amount " 
                + "from ngn.hs_recharge " 
                + "where " 
                + "	recharge_accounting_period = " + accounting_period + " " 
                + "group by " 
                + "	recharge_index, " 
                + "	recharge_fund, " 
                + "	recharge_organization, " 
                + "	recharge_program " 
                + "order by " 
                + "	recharge_index ";
            // @formatter:on

            @SuppressWarnings("rawtypes")
            Vector records = recharges.executeQuery(sql);

            int sequence_no = 1;
            int sum = 0;
            int unsigned_sum = 0;
            StringBuffer gl = new StringBuffer();

            for (int i = 0; i < records.size(); i++)
            {
                @SuppressWarnings("rawtypes")
                Hashtable record = (Hashtable)records.elementAt(i);

                String amount = (String)record.get("total_amount".toUpperCase());
                amount = escapeChar(amount, '.');
                int amt = Integer.parseInt(amount);
                int uamt = amt;
                String cd_db = "D"; // credit or debit
                if (uamt < 0)
                {
                    cd_db = "C";
                    uamt = uamt * -1; // removing the sign
                }
                sum += amt;
                unsigned_sum += uamt;
                amount = "" + uamt;

                if (amt != 0)
                {
                // @formatter:off
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
	            // @formatter:on

                    sequence_no++;
                }
            }

            // Get totals for each group Full(1)/Med Center(2)/Off Campus(3)/Off HS(6)
            // @formatter:off
    		sql = ""
    			+"select "
    			+"	substr(recharge_group_code,2,1) as group_type, "
    			+"	sum(recharge_total_amount) as group_total "
    			+"from ngn.hs_recharge "
    			+"where recharge_accounting_period = "+accounting_period+" "
    			+"group by substr(recharge_group_code,2,1) "
    		;
            // @formatter:on

            @SuppressWarnings("rawtypes")
            Vector groups = recharges.executeQuery(sql);

            double full_total = 0;
            double mc_total = 0;
            double off_total = 0;
            double off_hs_total = 0;
            double no_total = 0;
            double fullh_total = 0;

            for (int i = 0; i < groups.size(); i++)
            {
                @SuppressWarnings("rawtypes")
                Hashtable group = (Hashtable)groups.elementAt(i);
                int group_type = Integer.parseInt((String)group.get("group_type".toUpperCase()));
                switch (group_type)
                {
                    case 1: // FULL
                        full_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
                        break;
                    case 2: // MEDICAL CENTER
                        mc_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
                        break;
                    case 3: // OFF CAMPUS
                        off_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
                        break;
                    case 6: // OFF HS
                        off_hs_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
                        break;
                    case 5: // NO
                        no_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
                        break;
                    case 7: // FULLH 
                        fullh_total = Double.parseDouble((String)group.get("group_total".toUpperCase()));
                        break;

                }
            }

            // NGN accounts
            BusinessObject hs_revenue = BusinessObjectFactory.create("ngn_db", "hs_revenue");
            hs_revenue.setKey("hs_revenue.hs_revenue_process_month", true);
            hs_revenue.setValue("hs_revenue.hs_revenue_process_month", calendar_year);
            hs_revenue.setCriteria("hs_revenue.hs_revenue_loc_group_code", BusinessObjectCriteria.NOT_NULL);
            hs_revenue.setOrderBy("hs_revenue.hs_revenue_loc_group_code", true);
            hs_revenue.setOrderBy("hs_revenue.hs_revenue_share_factor", true, true);
            hs_revenue.retrieve();

            DecimalFormat precision = new DecimalFormat("#0.00");   // value format

            @SuppressWarnings("rawtypes")
            Vector redist = new Vector();
            int redist_sum = 0;
            do
            {
                @SuppressWarnings("rawtypes")
                Hashtable data = new Hashtable();
                double factor = Double.parseDouble(hs_revenue.getStringValue("hs_revenue.hs_revenue_share_factor"));
                String group_type = hs_revenue.getStringValue("hs_revenue.hs_revenue_loc_group_code");
                String amount = "";
                if (group_type.equals("FULL"))
                {
                    amount = escapeChar(precision.format(factor * full_total), '.');
                }
                else if (group_type.equals("MC"))
                {
                    amount = escapeChar(precision.format(factor * mc_total), '.');
                }
                else if (group_type.equals("OFF"))
                {
                    amount = escapeChar(precision.format(factor * off_total), '.');
                }
                else if (group_type.equals("OFFHS"))
                {
                    amount = escapeChar(precision.format(factor * off_hs_total), '.');
                }
                else if (group_type.equals("NO"))
                {
                    amount = escapeChar(precision.format(factor * no_total), '.');
                }
                else if (group_type.equals("FULLH"))
                {
                    amount = escapeChar(precision.format(factor * fullh_total), '.');
                }
                redist_sum += Integer.parseInt(amount);

                data.put("amount", amount);
                data.put("desc", hs_revenue.getStringValue("hs_revenue.hs_revenue_ledger_desc"));
                data.put("credit_debit", hs_revenue.getStringValue("hs_revenue.hs_revenue_credit_debit"));
                data.put("indx", hs_revenue.getStringValue("hs_revenue.hs_revenue_index"));
                data.put("fund", hs_revenue.getStringValue("hs_revenue.hs_revenue_fund"));
                data.put("orgn", hs_revenue.getStringValue("hs_revenue.hs_revenue_orgn"));
                data.put("acct", hs_revenue.getStringValue("hs_revenue.hs_revenue_acct"));
                data.put("prog", hs_revenue.getStringValue("hs_revenue.hs_revenue_prog"));
                redist.add(data);
            }
            while (hs_revenue.next() != null);

            if (redist_sum != sum)
            {
                int diff = sum - redist_sum;
                int biggest = 0;
                int index = 0;
                for (int i = 0; i < redist.size(); i++)
                {
                    @SuppressWarnings("rawtypes")
                    int amount = Integer.parseInt((String)((Hashtable)redist.elementAt(i)).get("amount"));
                    if (amount > biggest)
                    {
                        biggest = amount;
                        index = i;
                    }
                }
                @SuppressWarnings("rawtypes")
                Hashtable data = (Hashtable)redist.elementAt(index);
                int amount = Integer.parseInt((String)data.get("amount"));
                amount += diff;
                data.put("amount", "" + amount);
            }

            for (int i = 0; i < redist.size(); i++)
            {
                @SuppressWarnings("rawtypes")
                Hashtable data = (Hashtable)redist.elementAt(i);
                String amount = (String)data.get("amount");
                String desc = (String)data.get("desc");

                unsigned_sum += Double.parseDouble(amount);

                // @formatter:off
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
                // @formatter:on
                sequence_no++;
            }

            // tacking the header onto the beginning of the gl
            // @formatter:off
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
            // @formatter:on

            out.print(gl.toString());
        }
        catch (Exception e)
        {
            System.err.println("Error: " + e);
            exit_status = 1;
            e.printStackTrace();
        }
        catch (Throwable t)
        {
            System.err.println("Error: " + t);
            exit_status = 1;
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

    private static String escapeChar(String data, char c)
    {
        StringBuffer result = new StringBuffer();
        int strlen = data.length();
        char chars[] = data.toCharArray();
        for (int i = 0; i < strlen; i++)
        {
            if (chars[i] != c) result.append(chars[i]);
        }
        return result.toString();
    }

    /**
     * Pad given pad string to the beginning of data string
     * to given length
     */
    private static String rightJustified(String data, int maxlength, String pad)
    {
        StringBuffer dataBuffer = new StringBuffer();
        int size = maxlength - data.length();
        for (int i = 0; i < size; i++)
        {
            dataBuffer.append(pad);
        }
        dataBuffer.append(data);
        return dataBuffer.toString();
    }

    /**
     * Pad given pad string to the end of data string
     * to given length
     */
    private static String leftJustified(String data, int maxlength, String pad)
    {
        StringBuffer dataBuffer = new StringBuffer();
        dataBuffer.append(data);
        int size = maxlength - data.length();
        for (int i = 0; i < size; i++)
        {
            dataBuffer.append(pad);
        }
        return dataBuffer.toString();
    }

}
