import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.sql.*;
import java.util.Vector;
import edu.ucsd.act.business.*;
import edu.ucsd.act.db.connectionpool.*;

public class McGenerateExcel {

private static final String DB_NGN = "ngn_db";
private static final String DB_GAE = "ga_extension";
private int process_month = 0;
private int financial_month = 0;
//private Connection conn = null;

private String[] monthLookup = {
	"Jan","Feb","Mar","Apr","May","Jun",
	"Jul","Aug","Sep","Oct","Nov","Dec"
};

//public McGenerateExcel(Connection conn,int process_month, int financial_month)
public McGenerateExcel(int process_month, int financial_month)
	throws Exception
{
  //this.conn = conn;
	this.process_month = process_month;
	this.financial_month = financial_month;
}

public void run()
	throws Exception
{
		//Statement stmt = null;

	try {
		createDeptExcel();
		createTitleExcel();
		createDetailedExcel();
	} catch(Exception e) {
		throw new Exception("McGenerateExcel: "+e);
	} catch(Throwable t) {
		throw new Exception("McGenerateExcel: "+t);
	}
}

public void createDeptExcel()
	throws Exception
{
	PrintWriter out = null;
	try {

		//getting which fiscal year to display report for
		String accounting_period = ""+financial_month;
		String calendar_year = ""+process_month;

		//Statement stmt = null;
		//stmt = conn.createStatement();

		out = new PrintWriter(new FileWriter(McControl.EXTRACT_PATH+"MC"+calendar_year.substring(2)+"D.xls"));

		//get previous accounting period
		int year = Integer.parseInt(accounting_period.substring(0,4));
		int month = Integer.parseInt(accounting_period.substring(4));

		int prev_month = month - 1;
		int prev_year = year;
		if(prev_month == 0) {
			prev_month = 12;
			prev_year = year - 1;
		}
		String prev_accounting_period = prev_year+"";
		if(prev_month < 10)
			prev_accounting_period += "0"+prev_month;
		else
			prev_accounting_period += ""+prev_month;

		//get previous calendar year
		year = Integer.parseInt(calendar_year.substring(0,4));
		month = Integer.parseInt(calendar_year.substring(4));

		prev_month = month - 1;
		prev_year = year;
		if(prev_month == 0) {
			prev_month = 12;
			prev_year = year - 1;
		}
		String prev_calendar_year = prev_year+"";
		if(prev_month < 10)
			prev_calendar_year += "0"+prev_month;
		else
			prev_calendar_year += ""+prev_month;

		//table header
		String border = "border: 1px black solid;";
		String border_top = "border-top: 1px black solid;";
		String border_right = "border-right: 1px black solid;";
		String border_bottom = "border-bottom: 1px black solid;";
		String border_left = "border-left: 1px black solid;";
		StringBuffer html = new StringBuffer(""
			+"<table border=\"1\" style=\""+border+"\" cellpadding=\"0\" cellspacing=\"0\">\n"
			+"<tr>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" "+border_right+" width: .6in;\"><b>IX3</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1.3in;\"><b>"+monthLookup[prev_month-1]+" CU (Curr)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1.3in;\"><b>"+monthLookup[prev_month-1]+" CU (Prior)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1.3in;\"><b>"+monthLookup[prev_month-1]+" Chg (Tot)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" "+border_left+" width: 1.3in;\"><b>"+monthLookup[month-1]+" CU (Curr)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1.3in;\"><b>"+monthLookup[month-1]+" CU (Prior)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" "+border_right+" width: 1.3in;\"><b>"+monthLookup[month-1]+" Chg (Tot)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1in;\"><b>Diff CU</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1in;\"><b>Diff Chg</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" "+border_left+" width: 1in;\"><b>% Diff Chg</b></td>\n"
			+"</tr>\n"
		);

		Vector records = null;
		String sql = "";

		sql = ""
			+"select "
			+"	substr(recharge_index,1,3) as department, "
			+"	0 as prev_CU, "
			+"	0 as prev_CU_prior, "
			+"	0 as prev_recharge, "
			+"	sum(recharge_current_fte) as CU, "
			+"	sum(recharge_prior_fte) as CU_prior, "
			+"	sum(recharge_total_amount) as recharge "
			+"from ngn.mc_recharge "
			+"where recharge_accounting_period = "+accounting_period+" "
			+"group by substr(recharge_index,1,3) "
			+"order by substr(recharge_index,1,3) "
		;


    
 		BusinessObject bo1 = BusinessObjectFactory.create(DB_NGN,"mc_recharge");
 		Vector r1 = bo1.executeQuery(sql);
 		//Vector r1 = stmt.executeQuery(sql);

    //System.out.println("executed query with NBN_db");


    //ResultSet counts = stmt.executeQuery(sql);
    
		sql = ""
			+"select "
			+"	substr(recharge_index,1,3) as department, "
			+"	sum(recharge_current_fte) as prev_CU, "
			+"	sum(recharge_prior_fte) as prev_CU_prior, "
			+"	sum(recharge_total_amount) as prev_recharge, "
			+"	0 as CU, "
			+"	0 as CU_prior, "
			+"	0 as recharge "
			+"from ga_extension.mcn_recharges "
			+"where recharge_accounting_period = "+prev_accounting_period+" "
			+"group by substr(recharge_index,1,3) "
			+"order by substr(recharge_index,1,3) "
		;
 		BusinessObject bo2 = BusinessObjectFactory.create(DB_GAE,"mcn_recharges");
 		Vector r2 = bo2.executeQuery(sql);
 		//Vector r2 = stmt.executeQuery(sql);

    //System.out.println("executed query with dw_db");

 		records = combineDeptRecords(r1,r2);
 		//records = combineDeptRecords(r2,r2);

		for(int i=2; i < records.size()+2; i++) {
			Hashtable row = (Hashtable)records.elementAt(i-2);
			html.append("<tr>\n"
				+"<td class=\"fonts\" align=\"center\" style=\""+border_right+"\"><b>"+row.get("department".toUpperCase())+"</b></td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000;\">"+row.get("prev_CU".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000;\">"+row.get("prev_CU_prior".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.00;\">"+row.get("prev_recharge".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_left+"\">"+row.get("CU".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.0000;\">"+row.get("CU_prior".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.00; "+border_right+"\">"+row.get("recharge".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000;\">=E"+i+"-B"+i+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.00;\">=G"+i+"-D"+i+"</td>\n"
			);
			double prev_chg = Double.parseDouble((String)row.get("prev_recharge".toUpperCase()));
			if(prev_chg == 0)
				html.append("<td class=\"fonts\" align=\"center\" bgcolor=\"magenta\" style=\"mso-number-format:\\#0\\.00; "+border_left+"\"><b>***</b></td>\n");
			else
				html.append("<td class=\"fonts\" align=\"right\" bgcolor=\"lightyellow\" style=\"mso-number-format:\\#0\\.00; "+border_left+"\">=(100*(I"+i+"/D"+i+"))</td>\n");
			html.append("</tr>\n");
		}

		//close main table
		html.append(""
			+"<tr><td colspan=\"10\" style=\""+border_top+"\"></td></tr><tr>\n"
			+"<td style=\""+border_top+" "+border_right+"\"></td>\n"
			+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+"\"><b>=SUM(B2:B"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+"\"><b>=SUM(C2:C"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.00; "+border_top+"\"><b>=SUM(D2:D"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+" "+border_left+"\"><b>=SUM(E2:E"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+"\"><b>=SUM(F2:F"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.00; "+border_top+" "+border_right+"\"><b>=SUM(G2:G"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+"\"><b>=SUM(H2:H"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"magenta\" style=\"mso-number-format:\\#\\,##0\\.00; "+border_top+"\"><b>=SUM(I2:I"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightyellow\" style=\"mso-number-format:\\#0\\.00; "+border_top+" "+border_left+"\"><b>=(100*(I"+(records.size()+3)+"/D"+(records.size()+3)+"))</b></td>\n"
			+"</tr></table>"
		);

		out.println(""
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
			+"<x:Name>Summary by Department</x:Name>\n"
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
			+"<x:ProtectStructure>False</x:ProtectStructure>\n"
			+"<x:ProtectWindows>False</x:ProtectWindows>\n"
			+"</x:ExcelWorkbook>\n"
			+"</xml>\n"
			+"<style>\n"
			+"  .fonts { font-family:\"Courier New\", monospace; }"
			+"</style>\n"
			+"</head>\n"
			+"<body>\n"
			);
		out.println(html.toString());
		out.println("</body></html>");
	} catch(Exception e) {
		throw new Exception("createDeptExcel: "+e);
	} catch(Throwable t) {
		throw new Exception("createDeptExcel: "+t);
	} finally {
		if(out != null) {
			out.flush();
			out.close();
		}
	}
}

public void createTitleExcel()
	throws Exception
{
	PrintWriter out = null;
	try {
		//getting which fiscal year to display report for
		String accounting_period = ""+financial_month;
		String calendar_year = ""+process_month;

 		//Statement stmt = null;
    //stmt = conn.createStatement();

		out = new PrintWriter(new FileWriter(McControl.EXTRACT_PATH+"MC"+calendar_year.substring(2)+"T.xls"));

		//get previous accounting period
		int year = Integer.parseInt(accounting_period.substring(0,4));
		int month = Integer.parseInt(accounting_period.substring(4));

		int prev_month = month - 1;
		int prev_year = year;
		if(prev_month == 0) {
			prev_month = 12;
			prev_year = year - 1;
		}
		String prev_accounting_period = prev_year+"";
		if(prev_month < 10)
			prev_accounting_period += "0"+prev_month;
		else
			prev_accounting_period += ""+prev_month;

		//get previous calendar year
		year = Integer.parseInt(calendar_year.substring(0,4));
		month = Integer.parseInt(calendar_year.substring(4));

		prev_month = month - 1;
		prev_year = year;
		if(prev_month == 0) {
			prev_month = 12;
			prev_year = year - 1;
		}
		String prev_calendar_year = prev_year+"";
		if(prev_month < 10)
			prev_calendar_year += "0"+prev_month;
		else
			prev_calendar_year += ""+prev_month;

		//table header
		String border = "border: 1px black solid;";
		String border_top = "border-top: 1px black solid;";
		String border_right = "border-right: 1px black solid;";
		String border_bottom = "border-bottom: 1px black solid;";
		String border_left = "border-left: 1px black solid;";
		StringBuffer html = new StringBuffer(""
			+"<table border=\"1\" style=\""+border+"\" cellpadding=\"0\" cellspacing=\"0\">\n"
			+"<tr>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" "+border_right+" width: .6in;\"><b>IX3</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1.3in;\"><b>"+monthLookup[prev_month-1]+" CU (Curr)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1.3in;\"><b>"+monthLookup[prev_month-1]+" CU (Prior)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1.3in;\"><b>"+monthLookup[prev_month-1]+" Chg (Tot)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" "+border_left+" width: 1.3in;\"><b>"+monthLookup[month-1]+" CU (Curr)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1.3in;\"><b>"+monthLookup[month-1]+" CU (Prior)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" "+border_right+" width: 1.3in;\"><b>"+monthLookup[month-1]+" Chg (Tot)</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1in;\"><b>Diff CU</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" width: 1in;\"><b>Diff Chg</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"yellow\" style=\""+border_bottom+" "+border_left+" width: 1in;\"><b>% Diff Chg</b></td>\n"
			+"</tr>\n"
		);

		Vector records = null;
		String sql = "";

		sql = ""
			+"select "
			+"	recharge_pay_title_code as title, "
			+"	0 as prev_CU, "
			+"	0 as prev_CU_prior, "
			+"	0 as prev_recharge, "
			+"	sum(recharge_current_fte) as CU, "
			+"	sum(recharge_prior_fte) as CU_prior, "
			+"	sum(recharge_total_amount) as recharge "
			+"from ngn.mc_recharge "
			+"where recharge_accounting_period = "+accounting_period+" "
			+"group by recharge_pay_title_code "
			+"order by recharge_pay_title_code "
		;
 		BusinessObject bo1 = BusinessObjectFactory.create(DB_NGN,"mc_recharge");
 		Vector r1 = bo1.executeQuery(sql);
 		//Vector r1 = stmt.executeQuery(sql);

		sql = ""
			+"select "
			+"	recharge_pay_title_code as title, "
			+"	sum(recharge_current_fte) as prev_CU, "
			+"	sum(recharge_prior_fte) as prev_CU_prior, "
			+"	sum(recharge_total_amount) as prev_recharge, "
			+"	0 as CU, "
			+"	0 as CU_prior, "
			+"	0 as recharge "
			+"from ga_extension.mcn_recharges "
			+"where recharge_accounting_period = "+prev_accounting_period+" "
			+"group by recharge_pay_title_code "
			+"order by recharge_pay_title_code "
		;
 		BusinessObject bo2 = BusinessObjectFactory.create(DB_GAE,"mcn_recharges");
 		Vector r2 = bo2.executeQuery(sql);
    //Vector r2 = stmt.executeQuery(sql);

 		records = combineTitleRecords(r1,r2);

		for(int i=2; i < records.size()+2; i++) {
			Hashtable row = (Hashtable)records.elementAt(i-2);
			html.append("<tr>\n"
				+"<td class=\"fonts\" align=\"center\" style=\"mso-number-format:0000; "+border_right+"\"><b>"+row.get("title".toUpperCase())+"</b></td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000;\">"+row.get("prev_CU".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000;\">"+row.get("prev_CU_prior".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.00;\">"+row.get("prev_recharge".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_left+"\">"+row.get("CU".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.0000;\">"+row.get("CU_prior".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.00; "+border_right+"\">"+row.get("recharge".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000;\">=E"+i+"-B"+i+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.00;\">=G"+i+"-D"+i+"</td>\n"
			);
			double prev_chg = Double.parseDouble((String)row.get("prev_recharge".toUpperCase()));
			if(prev_chg == 0)
				html.append("<td class=\"fonts\" align=\"center\" bgcolor=\"magenta\" style=\"mso-number-format:\\#0\\.00; "+border_left+"\"><b>***</b></td>\n");
			else
				html.append("<td class=\"fonts\" align=\"right\" bgcolor=\"lightyellow\" style=\"mso-number-format:\\#0\\.00; "+border_left+"\">=(100*(I"+i+"/D"+i+"))</td>\n");
			html.append("</tr>\n");
		}

		//close main table
		html.append(""
			+"<tr><td colspan=\"10\" style=\""+border_top+"\"></td></tr><tr>\n"
			+"<td style=\""+border_top+" "+border_right+"\"></td>\n"
			+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+"\"><b>=SUM(B2:B"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+"\"><b>=SUM(C2:C"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.00; "+border_top+"\"><b>=SUM(D2:D"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+" "+border_left+"\"><b>=SUM(E2:E"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+"\"><b>=SUM(F2:F"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightblue\" style=\"mso-number-format:\\#\\,##0\\.00; "+border_top+" "+border_right+"\"><b>=SUM(G2:G"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:\\#\\,##0\\.0000; "+border_top+"\"><b>=SUM(H2:H"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"magenta\" style=\"mso-number-format:\\#\\,##0\\.00; "+border_top+"\"><b>=SUM(I2:I"+(records.size()+1)+")</b></td>\n"
			+"<td class=\"fonts\" align=\"right\" bgcolor=\"lightyellow\" style=\"mso-number-format:\\#0\\.00; "+border_top+" "+border_left+"\"><b>=(100*(I"+(records.size()+3)+"/D"+(records.size()+3)+"))</b></td>\n"
			+"</tr></table>"
		);

		out.println(""
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
			+"<x:Name>Summary by Title</x:Name>\n"
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
			+"<x:ProtectStructure>False</x:ProtectStructure>\n"
			+"<x:ProtectWindows>False</x:ProtectWindows>\n"
			+"</x:ExcelWorkbook>\n"
			+"</xml>\n"
			+"<style>\n"
			+"  .fonts { font-family:\"Courier New\", monospace; }"
			+"</style>\n"
			+"</head>\n"
			+"<body>\n"
			);
		out.println(html.toString());
		out.println("</body></html>");
	} catch(Exception e) {
		throw new Exception("createTitleExcel: "+e);
	} catch(Throwable t) {
		throw new Exception("createTitleExcel: "+t);
	} finally {
		if(out != null) {
			out.flush();
			out.close();
		}
	}
}

private void createDetailedExcel()
	throws Exception
{
	PrintWriter out = null;
	try {
		out = new PrintWriter(new FileWriter(McControl.EXTRACT_PATH+"MC"+(""+process_month).substring(2)+"Detl.xls"));

		//Statement stmt = null;
    //stmt = conn.createStatement();
    
		//table header
		String border = "border: 1px black solid;";
		String border_top = "border-top: 1px black solid;";
		String border_right = "border-right: 1px black solid;";
		String border_bottom = "border-bottom: 1px black solid;";
		String border_left = "border-left: 1px black solid;";
		StringBuffer html = new StringBuffer(""
			+"<table border=\"1\" style=\""+border+"\" cellpadding=\"0\" cellspacing=\"0\">\n"
			+"<tr>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 1.0in;\"><b>Employee ID</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 2.5in;\"><b>Employee Name</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 0.5in;\"><b>Title Code</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 2.8in;\"><b>PPS Title Name</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 0.8in;\"><b>Rechg Index</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 0.8in;\"><b>CU Percent</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 0.7in;\"><b>Rechg Amount</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 0.6in;\"><b>Dept Code</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 2.8in;\"><b>Dept Name</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 2.5in;\"><b>Location Name</b></td>\n"
			+"<td class=\"fonts\" align=\"center\" bgcolor=\"#E4EEFC\" style=\""+border_bottom+" width: 3.5in;\"><b>Campus Location</b></td>\n"
			+"</tr>\n"
		);

 		String sql = ""
			+"select "
			+"	ngn_history_employee_id, "
			+"	ngn_history_employee_name, "
			+"	ngn_history_title_code, "
			+"	ppstitlename, "
			+"	ngn_history_index, "
			+"	ngn_history_current_adj_fte, "
			+"	coalesce(mc_history_current_amount,0) as mc_history_current_amount, "
			+"	ngn_history_home_dept_code, "
			+"	ngn_history_home_dept_desc, "
			+"	coalesce(bldg_name,'') as bldg_name, "
			+"	coalesce(bldg_campus_loc,'') as bldg_campus_loc "
			+"from ngn.ngn_history "
			+"left outer join ngn.mc_history on mc_history_employee_id = ngn_history_employee_id and mc_history_process_month = ngn_history_process_month and mc_history_title_code = ngn_history_title_code and mc_history_index = ngn_history_index and mc_history_pay_index = ngn_history_pay_index and mc_history_pay_activity_code = ngn_history_pay_activity_code "
			+"left outer join tms.building on building_code = ngn_history_tms_location_code "
			+"left outer join tms.titlecode on titlecode = ngn_history_title_code "
			+"where ngn_history_process_month = "+process_month+" "
			//+"  and ngn_history_vcu_desc != 'MEDICAL CENTER' "
  		+" and (ngn_history_vcu_code != '05' OR ngn_history_fund like '601%') "
			//+"  and ngn_history_stu_status_code < '4' "  // changes
			//+"  and ngn_history_stu_status_code < '3' "  // original
			//+"  and ngn_history_stu_status_code < '3' or (ngn_history_stu_status_code < '4' and ngn_history_title_code in ('4919','7278','7277','7276','7275','7274','0738','0737','0736'))" // new code
      //+"  and ngn_history_stu_status_code < '3' or (ngn_history_stu_status_code ='3' and (ngn_history_title_code in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') or ngn_history_home_dept_code <>'000418'))" // new code
      //+"  and ngn_history_stu_status_code < '3' or (ngn_history_stu_status_code ='3' and ngn_history_title_code in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') and ngn_history_home_dept_code <>'000418')" // new code
			+" and ((ngn_history_stu_status_code ='3' and ngn_history_title_code in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') and ngn_history_home_dept_code <>'000418') or ngn_history_stu_status_code <'3' or NGN_HISTORY_INDEX in ('TPSSH11','TPSSH12')) "
			//+" and ((ngn_history_stu_status_code ='3' and ngn_history_title_code in ('4919','7278','7277','7276','7275','7274','0738','0737','0736') and ngn_history_home_dept_code <>'000418') or ngn_history_stu_status_code <'3' or ngn_history_fund = '66069A' ) "
			+"  and ngn_history_ttl_category_code = '+' "
			+"  and ngn_history_loc_group_code = 'MC' "
			+"order by ngn_history_loc_group_code, ngn_history_home_dept_code, ngn_history_employee_name, ngn_history_title_code, ngn_history_index "
		;
 		BusinessObject bo = BusinessObjectFactory.create(DB_NGN,"mc_history");
 		Vector results = bo.executeQuery(sql);
 		//Vector results = stmt.executeQuery(sql);

 		for(int i=0; i < results.size(); i++) {
			Hashtable row = (Hashtable)results.elementAt(i);
			html.append("<tr>\n"
				+"<td class=\"fonts\" align=\"center\" style=\"mso-number-format:000000000;\">"+row.get("ngn_history_employee_id".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"left\">"+row.get("ngn_history_employee_name".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"center\" style=\"mso-number-format:0000;\">"+row.get("ngn_history_title_code".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"left\">"+stripLeadingDash((String)row.get("ppstitlename".toUpperCase()))+"</td>\n"
				+"<td class=\"fonts\" align=\"left\">"+row.get("ngn_history_index".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:#\\,##0\\.0000;\">"+row.get("ngn_history_current_adj_fte".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"right\" style=\"mso-number-format:#\\,##0\\.00;\">"+row.get("mc_history_current_amount".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"center\" style=\"mso-number-format:0000;\">"+row.get("ngn_history_home_dept_code".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"left\">"+row.get("ngn_history_home_dept_desc".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"left\">"+row.get("bldg_name".toUpperCase())+"</td>\n"
				+"<td class=\"fonts\" align=\"left\">"+row.get("bldg_campus_loc".toUpperCase())+"</td>\n"
				+"</tr>\n"
			);
 		}
		//close main table
		html.append("</table>");


		out.println(""
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
			+"<x:Name>Detailed MCN Report</x:Name>\n"
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
			+"<x:ProtectStructure>False</x:ProtectStructure>\n"
			+"<x:ProtectWindows>False</x:ProtectWindows>\n"
			+"</x:ExcelWorkbook>\n"
			+"</xml>\n"
			+"<style>\n"
			+"  .fonts { font-family:\"Courier New\", monospace; }"
			+"</style>\n"
			+"</head>\n"
			+"<body>\n"
			);
		out.println(html.toString());
		out.println("</body></html>");
	} catch(Exception e) {
		throw new Exception("createDetailedExcel: "+e);
	} catch(Throwable t) {
		throw new Exception("createDetailedExcel: "+t);
	} finally {
		if(out != null) {
			out.flush();
			out.close();
		}
	}
}

private Vector combineDeptRecords(Vector r1, Vector r2)
	throws Exception
{
	Vector results = new Vector();
	int r1i = 0;
	int r2i = 0;
	while(r1i < r1.size() && r2i < r2.size()) {
		Hashtable r1h = (Hashtable)r1.elementAt(r1i);
		Hashtable r2h = (Hashtable)r2.elementAt(r2i);

		String r1d = (String)r1h.get("DEPARTMENT");
		String r2d = (String)r2h.get("DEPARTMENT");

		int compared = r1d.compareTo(r2d);
		if(compared < 0) {
			results.add(r1h);

			r1i++;
		} else if(compared == 0) {
			Hashtable rh = new Hashtable();
			rh.put("DEPARTMENT",r1d);
			rh.put("PREV_CU",r2h.get("PREV_CU"));
			rh.put("PREV_CU_PRIOR",r2h.get("PREV_CU_PRIOR"));
			rh.put("PREV_RECHARGE",r2h.get("PREV_RECHARGE"));
			rh.put("CU",r1h.get("CU"));
			rh.put("CU_PRIOR",r1h.get("CU_PRIOR"));
			rh.put("RECHARGE",r1h.get("RECHARGE"));
			results.add(rh);

			r1i++;
			r2i++;
		} else {
			results.add(r2h);

			r2i++;
		}
	}
	if(r1i < r1.size()) {
		for(int i = r1i; i < r1.size(); i++) {
			results.add(r1.elementAt(i));
		}
	} else if(r2i < r2.size()) {
		for(int i = r2i; i < r2.size(); i++) {
			results.add(r2.elementAt(i));
		}
	}
	return results;
}

private Vector combineTitleRecords(Vector r1, Vector r2)
	throws Exception
{
	Vector results = new Vector();
	int r1i = 0;
	int r2i = 0;
	while(r1i < r1.size() && r2i < r2.size()) {
		Hashtable r1h = (Hashtable)r1.elementAt(r1i);
		Hashtable r2h = (Hashtable)r2.elementAt(r2i);

		String r1d = (String)r1h.get("TITLE");
		String r2d = (String)r2h.get("TITLE");

		int compared = r1d.compareTo(r2d);
		if(compared < 0) {
			results.add(r1h);

			r1i++;
		} else if(compared == 0) {
			Hashtable rh = new Hashtable();
			rh.put("TITLE",r1d);
			rh.put("PREV_CU",r2h.get("PREV_CU"));
			rh.put("PREV_CU_PRIOR",r2h.get("PREV_CU_PRIOR"));
			rh.put("PREV_RECHARGE",r2h.get("PREV_RECHARGE"));
			rh.put("CU",r1h.get("CU"));
			rh.put("CU_PRIOR",r1h.get("CU_PRIOR"));
			rh.put("RECHARGE",r1h.get("RECHARGE"));
			results.add(rh);

			r1i++;
			r2i++;
		} else {
			results.add(r2h);

			r2i++;
		}
	}
	if(r1i < r1.size()) {
		for(int i = r1i; i < r1.size(); i++) {
			results.add(r1.elementAt(i));
		}
	} else if(r2i < r2.size()) {
		for(int i = r2i; i < r2.size(); i++) {
			results.add(r2.elementAt(i));
		}
	}
	return results;
}

private String stripLeadingDash(String s)
	throws Exception
{
	try {
		if(s.length() == 0) return s;

		char[] c = s.toCharArray();
		int i = 0;
		for(; i < c.length; i++) {
			if(c[i] != '-')
				break;
		}

		return s.substring(i);

	} catch(Exception e) {
		throw new Exception("stripLeadingDash: "+e);
	}
}
}