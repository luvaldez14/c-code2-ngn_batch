import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import edu.ucsd.act.db.connectionpool.DBConnectionManagerPC;
import edu.ucsd.act.db.connectionpool.DBPooledConnection;
import edu.ucsd.act.jlink.JLinkAudit;
import edu.ucsd.act.jlink.JLinkJndi;
import edu.ucsd.act.jlink.util.JLinkDate;
import edu.ucsd.act.util.JLinkEmailer;

// ********** REVIEW EMAIL ADDRESSES *********** LV

public class HsReports
{

    private final static String PATH            = JLinkJndi.getContextDocsPath();
    private final static String ENVIRONMENT     = System.getProperty("tms.batch.environment");
    private DBPooledConnection  connp           = null;
    private Connection          conn            = null;
    private Statement           stmt            = null;
    private StringBuffer        html            = null;
    private String              process_date    = "";
    private String              process_month   = "";
    private String              process_year    = "";
    private String              YYMM            = "";
    private ResultSet           rs              = null;
    private String              outputDirectory = "";
    private String              yyyymmdd        = "";
    private String              query           = "";
    private PrintWriter         outFile         = null;
    private PrintWriter         outFile2        = null;
    private String[]            monthLookup     = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };

    private final static String DBALIAS         = "ngn_db";

    public static void main(String args[])
    {

        try
        {

            HsReports run = new HsReports();
            run.execute();

        }	// end try

        catch (Exception ex)
        {

            System.out.println("Error: " + ex.toString());
            System.err.println("Error: " + ex.toString());

        }	// end catch

    }	// end main()

    public void execute()
    {
        try
        {
            JLinkAudit.setFilePath(PATH + "../msgs/");

            JLinkJndi.setEnv(ENVIRONMENT);
            
            DBConnectionManagerPC.createDatasources();
            connp = DBConnectionManagerPC.getPooledConnection(DBALIAS, toString());
            connp.setDB(connp.getDB());
            conn = (Connection)connp;

            stmt = conn.createStatement();
            
            log(" --> STARTING HsReports PROCESS...");

            JLinkDate dt = new JLinkDate();
            yyyymmdd = JLinkDate.convert(dt.toString(), "yyyyMMdd");

            outputDirectory = PATH + "../extracts/All_Employees_at_Health_Sciences" + yyyymmdd + ".xls"; // output
                                                                                                         // directory
            outFile = new PrintWriter(new FileWriter(outputDirectory));

            String sql = "select distinct recharge_calendar_year from ngn.hs_recharge";
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            rs.next();
            
            process_date = Integer.toString(rs.getInt(1));
            process_year = process_date.substring(0, 4);
            process_month = process_date.substring(4);
            YYMM = process_date.substring(2);
            rs.close();

            // @formatter:off
    		String xml_report = ""
    		    +"<html xmlns:v=\"urn:schemas-microsoft-com:vml\"\n"
    			+"  xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n"
    			+"  xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n"
    			+"  xmlns=\"http://www.w3.org/TR/REC-html40\">\n"
    			+"<html xmlns:x=\"urn:schemas-microsoft-com:office:excel\">\n"
    			+"<head>\n"
    			+"<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"
    			+"<xml>\n"
    			+"<x:ExcelWorkbook>\n"
    			+"<x:ExcelWorksheets>\n"
    			+"<x:ExcelWorksheet>\n"
    			+"<x:Name>Health Sciences Reports</x:Name>\n"
    			+"<x:WorksheetOptions>\n"
    			+"<x:Selected/>\n"
    			+"<x:ProtectContents>False</x:ProtectContents>\n"
    			+"<x:ProtectObjects>False</x:ProtectObjects>\n"
    			+"<x:ProtectScenarios>False</x:ProtectScenarios>\n"
    			+"</x:WorksheetOptions>\n"
    			+"</x:ExcelWorksheet>\n"
    			+"</x:ExcelWorksheets>\n"
    			+"<x:WindowHeight>8835</x:WindowHeight>\n"
    			+"<x:WindowWidth>11340</x:WindowWidth>\n"
    			+"<x:WindowTopX>480</x:WindowTopX>\n"
    			+"<x:WindowTopY>120</x:WindowTopY>\n"
    			+"<x:RefModeR1C1/>\n"
    			+"<x:ProtectStructure>False</x:ProtectStructure>\n"
    			+"<x:ProtectWindows>False</x:ProtectWindows>\n"
    			+"</x:ExcelWorkbook>\n"
    			+"</xml>\n"
    			+"</head>\n"
    			+"<body>\n"
    			;
            // @formatter:on

            // All Employees at Health Sciences .xml report
            html = new StringBuffer("");
            html.append(getAllEmplHS());

            outFile.println(xml_report);
            outFile.println(html.toString());
            outFile.println("</body></html>");
            outFile.flush();
            outFile.close();

            // Not sure the index prefix files are required.
            // Employee List for MCH-MCL-MSC .xml report
            //html = new StringBuffer("");
            //html.append(getEmplListHS());

            //outFile2.println(xml_report);
            //outFile2.println(html.toString());
            //outFile2.println("</body></html>");
            //outFile2.flush();
            //outFile2.close();

            log(" --> Send HS Index and All HS Employee Spreadsheets...");

            String msg = "All HS Employee Spreadsheets for " + monthLookup[Integer.parseInt(process_month) - 1] + " " + process_year;

            ArrayList<String> receivers = new ArrayList<String>();
            //receivers.add("lbierer@ucsd.edu");
            //receivers.add("mamccray@ucsd.edu");
            //receivers.add("akchang@ucsd.edu");
            receivers.add("luvaldez@ucsd.edu");
            
            String[] files = new String[1];
            files[0] = "/tms/ngn/extracts/All_Employees_at_Health_Sciences" + yyyymmdd + ".xls";  // this report seems to be correct now.
            //files[1] = "/tms/ngn/extracts/All_Employees_at_Health_Sciences" + yyyymmdd + ".xls";  // placeholder for second report.

            ArrayList<String> cc = new ArrayList<String>();
            cc.add("mrusakoff@ucsd.edu");
            //cc.add("akibble@ucsd.edu");
            //cc.add("kcolestock@ucsd.edu");
            //cc.add("tms-support-l@ucsd.edu");
            cc.add("luvaldez@ucsd.edu");

            sendMail((String[])receivers.toArray(new String[receivers.size()]), files, (String[])cc.toArray(new String[cc.size()]), msg);

            log(" --> Send HS Spreadsheets...");
            msg = "HS Spreadsheets for " + monthLookup[Integer.parseInt(process_month) - 1] + " " + process_year;

            //receivers.add("eveldin@ucsd.edu");
            //cc.add("sgerbracht@ucsd.edu");

            String[] files1 = new String[4];

            files1[0] = "/tms/ngn/extracts/HS" + YYMM + "Detl.xls";
            files1[1] = "/tms/ngn/extracts/HS" + YYMM + "T.xls";
            files1[2] = "/tms/ngn/extracts/HS" + YYMM + "D.xls";
            files1[3] = "/tms/ngn/extracts/HSGLDOC";

            sendMail((String[])receivers.toArray(new String[receivers.size()]), files1, (String[])cc.toArray(new String[cc.size()]), msg);
        }
        catch (Exception e)
        {
            if (connp != null)
            {
                connp.disconnect();
                log("ERROR openConnection(), pooled connection " + e);
                log2("ERROR openConnection(), pooled connection " + e);
                System.exit(1);

            }
            else logException(e);
            System.exit(1);
        }
        finally
        {
            conn = null;
            DBConnectionManagerPC.closePooledConnections(toString());
            System.exit(0);
        }

    }

    private String getAllEmplHS()
    {
        outFile.flush();
        ArrayList<String> headers = new ArrayList<String>();
        ArrayList<String> arrayList = new ArrayList<String>();

        try
        {
            headers.add("Employee_ID");
            headers.add("Employee_Name");
            headers.add("Title_Code");
            headers.add("Recharge_Index");
            headers.add("CU_Percent");
            headers.add("Department");
            headers.add("Location");

            headers.trimToSize();

            // @formatter:off
            query = ""
				+"SELECT"
				+" ngn_history_employee_id,"
				+" ngn_history_employee_name,"
				+" ngn_history_title_code,"
				+" ngn_history_index,"
				+" ngn_history_current_adj_fte,"
				+" ngn_history_home_dept_desc,"
				+" coalesce(bldg_name,'')"
				+" FROM ngn.ngn_history"
				// add join to hs_history to make this report correct
				+" join ngn.hs_history on " 
				+" hs_history_employee_id = ngn_history_employee_id and hs_history_process_month = ngn_history_process_month "
				+" and hs_history_title_code = ngn_history_title_code  and hs_history_pay_index = ngn_history_pay_index " 
				+" and hs_history_pay_activity_code = ngn_history_pay_activity_code " 
				+" LEFT OUTER JOIN tms.building  on ngn_history_tms_location_code = building_code"
				+" WHERE ngn_history_process_month="+process_date
				+" AND ngn_history_loc_group_code in ('MC','FULL','OFFHS')"
				+" ORDER BY ngn_history_employee_id";
	        // @formatter:on

            log(query);
            rs = stmt.executeQuery(query);

            while (rs.next())
            {
                // @formatter:off
                arrayList.add(""
                    +rs.getString(1)+":"
                    +rs.getString(2)+":"
                    +rs.getString(3)+":"
                    +rs.getString(4)+":"
                    +rs.getDouble(5)+":"
                    +rs.getString(6)+":"
                    +rs.getString(7)) ;           
                // @formatter:on
            }
            arrayList.trimToSize();

        }
        catch (SQLException sql)
        {
            log("getAllEmplHS has failed: " + sql);
            log2("getAllEmplHS has failed: " + sql);
            System.exit(1);
        }
        catch (Throwable thw)
        {
            log("getAllEmplHS has failed: " + thw);
            log2("getAllEmplHS has failed: " + thw);
            System.exit(1);
        }
        outFile.flush();
        return buildReport(arrayList, headers);
    }

    private String getEmplListHS()
    {
        outFile2.flush();
        ArrayList<String> headers = new ArrayList<String>();
        ArrayList<String> arrayList = new ArrayList<String>();

        // String invIssue = "";
        try
        {
            headers.add("Recharge_Index");
            headers.add("Employee_ID");
            headers.add("Employee_Name");
            headers.add("Department_Code");
            headers.add("Department_Desc");
            headers.add("Title_Code");
            headers.add("Post_Doc");
            headers.add("Mail_Code");
            headers.add("FTE");
            headers.add("Pay_Index");
            headers.add("Vice_Chancellor_Unit");

            headers.trimToSize();

            // @formatter:off
			query = ""
				+"SELECT"
				+" ngn_history_index,"
				+" ngn_history_employee_id,"
				+" ngn_history_employee_name,"
				+" ngn_history_home_dept_code,"
				+" ngn_history_home_dept_desc,"
				+" ngn_history_title_code,"
				+" CASE WHEN ngn_history_cu_type_code = 'EMPL' then ''"
				+" ELSE 'Post Doc' end as Post_Doc ,"
				+" ngn_history_empl_mail_code,"
				+" ngn_history_current_adj_fte,"
				+" ngn_history_pay_index,"
				+" ngn_history_vcu_desc"
				+" FROM ngn.ngn_history"
				+" WHERE ngn_history_process_month="+process_date
				+" AND ngn_history_loc_group_code = 'MC'"
				+" AND (ngn_history_index like 'MCH%'"
        	    +" OR  ngn_history_index like 'MCL%'"
        	    +" OR  ngn_history_index like 'MSC%')"
        	    +" and ngn_history_ttl_category_code = '+'"
        	    +"and ngn_history_stu_status_code <= '3'"    // 06/28/2011 - lv to include undergraduates
				+" ORDER BY ngn_history_index";
	        // @formatter:on

            log(query);
            rs = stmt.executeQuery(query);

            while (rs.next())
            {
                // @formatter:off
				arrayList.add(""
				    +rs.getString(1)+":"
                    +rs.getString(2)+":"
                    +rs.getString(3)+":"
                    +rs.getString(4)+":"
                    +rs.getString(5)+":"
                    +rs.getString(6)+":"
                    +rs.getString(7)+":"
                    +rs.getString(8)+":"
                    +rs.getDouble(9)+":"
                    +rs.getString(10)+":"
                    +rs.getString(11));           
		        // @formatter:on

            }
            arrayList.trimToSize();

        }
        catch (SQLException sql)
        {
            log("getEmplListHs() has failed: " + sql);
            log2("getEmplListHS() has failed: " + sql);
            System.exit(1);
        }
        catch (Throwable thw)
        {
            log("getEmplListHS() has failed: " + thw);
            log2("getEmplListHS() has failed: " + thw);
            System.exit(1);
        }
        outFile2.flush();
        return buildReport(arrayList, headers);
    }

    private String buildReport(ArrayList<String> arrayList, ArrayList<String> headers)
    {
        StringBuffer html = null;
        try
        {

            html = new StringBuffer("" + "<br>" + "<table style=\"border-top: #000 3px solid;\" cellpadding=\"3\" cellspacing=\"0\">\n" + "<tr>");

            for (int h = 0; h < headers.size(); h++)
            {

                html.append("<td align=\"center\"><b>" + headers.get(h) + "</b></td>\n");
            }
            html.append("</tr>");

            for (int i = 0; i < arrayList.size(); i++)
            {
                html.append("<tr>");
                String record = (String)arrayList.get(i);
                String[] row = null;
                row = record.split(":");
                for (int j = 0; j < row.length; j++)
                {

                    if (row[j].length() == 9) // formatting employee_id column - to have leading zeros
                        html.append("<td align=\"right\" style=\"mso-number-format:\\#000000000;\">" + row[j] + "</td>\n");
                    else if (j != 8 && j != 4 && row[j].length() == 6) // formatting employee_id column - to have
                                                                       // leading zeros
                        html.append("<td align=\"right\" style=\"mso-number-format:\\#000000;\">" + row[j] + "</td>\n");
                    else if (row[j].length() == 4) // formatting employee_id column - to have leading zeros
                        html.append("<td align=\"right\" style=\"mso-number-format:\\#0000;\">" + row[j] + "</td>\n");
                    else html.append("<td align=\"right\" >" + row[j] + "</td>\n");

                }
                html.append("</tr>");

            }
            html.append("</table>\n");

        }
        catch (Throwable thw)
        {
            log("buildReport  has failed: " + thw);
            log2("buildReport has failed: " + thw);
            System.exit(1);
        }

        return html.toString();
    }

    private void sendMail(String[] receivers, String[] files, String[] cc, String msg)
    {
        try
        {
            String sender = "tms-support-l@ucsd.edu";

            JLinkEmailer jMail = new JLinkEmailer();

            jMail.setFromAddress(sender);
            jMail.setToAddress(receivers);
            jMail.setCcAddress(cc);
            jMail.setSubjectText(msg);
            jMail.setMessageBody("Please see attached.");
            jMail.setFileAttachments(files);
            jMail.sendEmail();
        }
        catch (Exception e)
        {
            System.out.println("send mail:" + e);
        }

    }

    private void log(String pMessage)
    {

        JLinkAudit.audit("health_sciences_reports", pMessage);
    }

    private void log2(String pMessage)
    {
        System.out.println(pMessage);
        System.err.println(pMessage);
    }

    private void logException(Exception pException)
    {
        JLinkAudit.audit("health_sciences_reports", pException);
        pException.printStackTrace(System.out);
    }

}
