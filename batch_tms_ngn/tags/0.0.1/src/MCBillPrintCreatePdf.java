import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.util.Vector;

import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;

import edu.ucsd.act.jlink.util.JLinkDate;

/**
 * @author Jennifer Kramer
 *
 */
public class MCBillPrintCreatePdf {

//DEBUGGING
public static final boolean SHOW_LINE = false;
public static final boolean DEBUG = false;

public static final int FONT_SIZE = 8;
private static final Font documentFont = FontFactory.getFont(FontFactory.COURIER, FONT_SIZE, Font.NORMAL, new Color(0, 0, 0));
public static final int LEADING = 7; //changing this value will require max_lines to be adjusted
public static final float TOP_MARGIN = 24;
public static final float BOTTOM_MARGIN = 24;
public static final float RIGHT_MARGIN = 48;
public static final float LEFT_MARGIN = 48;
public static final String SEPARATOR  = File.separator;


private static DecimalFormat fte_format = new DecimalFormat( "#0.0000 ;#0.0000-" );
private static DecimalFormat amt_format = new DecimalFormat( "#0.00 ;#0.00-" );
private static DecimalFormat amt_unsigned_format = new DecimalFormat( "#0.00" );

private Connection conn = null;

private String path          = McControl.EXTRACT_PATH;
private String filename      = "";
private String orgn_mgr_addr = "";
private String orgn_mgr      = "";
private String process_month     = "";
private Vector indexes       = null;
private Vector index_details = null;

private Document document = null;
private PdfWriter writer = null;
private MCBillPrintEventHandler event_handler = null;

public MCBillPrintCreatePdf(Connection conn, String orgn_mgr_addr, String orgn_mgr, String process_month, Vector indexes, Vector index_details)
{
	this.conn = conn;

	filename = "MCNBill_"+process_month+"_"+orgn_mgr_addr+".pdf";
	this.orgn_mgr_addr = orgn_mgr_addr;
	this.orgn_mgr = orgn_mgr;
	this.process_month = process_month;
	this.indexes = indexes;
	this.index_details = index_details;
}

public void generate()
	throws Exception
{
	document = new Document(PageSize.LETTER);
	writer = PdfWriter.getInstance(document, new FileOutputStream(path+filename));
	event_handler = new MCBillPrintEventHandler();

	event_handler.setPeriodEnd(process_month);
	event_handler.setManager(orgn_mgr_addr, orgn_mgr);

	writer.setPageEvent(event_handler);

	document.setMargins(LEFT_MARGIN,RIGHT_MARGIN,TOP_MARGIN+(10*LEADING),BOTTOM_MARGIN+(7*LEADING));
	document.open();

	if(DEBUG) System.out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> Start Cover Page");
	doCoverPage();
	if(DEBUG) System.out.println((new JLinkDate()).convert("MMM dd yyyy hh:mm:ssa")+" --> End Cover Page");

	document.resetPageCount();

	for(int i=0; i < indexes.size(); i++) {
		//Set up for header
		String index = (String)indexes.elementAt(i);
		doMCPage(index);
	}

	document.close();

}

private void doCoverPage()
	throws Exception
{
	try {
		event_handler.setHeaderType("COVER");
		Paragraph p = null;

		//List of Indexes
		Table t = new Table(3,1);
		t.setWidth(100);
		t.setBorder(Table.NO_BORDER);
		t.setPadding(0);
		t.setSpacing(0);
		t.endHeaders();

		String column_list = "";
		Cell c = null;
		int per_column = (int)Math.ceil((double)indexes.size()/3);
		for(int i = 0; i < indexes.size(); i++) {
			if(i%per_column == 0 && i != 0) {
				p = new Paragraph(column_list,documentFont);
				p.setLeading(LEADING);
				c = new Cell(p);
				c.setBorder(Cell.NO_BORDER);
				t.addCell(c);
				column_list = "";
			}
			String index = (String)indexes.elementAt(i);
			String index_desc = (String)index_details.elementAt(i);
			StringTokenizer st = new StringTokenizer(index_desc,"\t");
			index_desc = st.nextToken();
			if(index_desc.length() < 10)
				index_desc = leftJustified(index_desc,10," ");
			else
				index_desc = index_desc.substring(0,10);
			column_list += index_desc+"  "+index+"\n";
		}
		p = new Paragraph(column_list,documentFont);
		p.setLeading(LEADING);
		c = new Cell(p);
		c.setBorder(Cell.NO_BORDER);
		t.addCell(c);
		document.add(t);

		//News
		/** Med Center News has not been set up
		String sql = ""
			+"select "
			+"  cover_line "
			+"from tms_campus_db_new..billing_cover_page "
			+"where cover_line_type in ('B','T') " //T=TMS, B=BOTH
			+"order by cover_line_seq";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		boolean first = true;
		while(rs.next()) {
			p = new Paragraph(rs.getString(1),documentFont);
			if(first) {
				p.setLeading(LEADING+20);
				first = false;
			} else
				p.setLeading(LEADING);
			document.add(p);
		}
		**/

		document.newPage();
	} catch(Exception e) {
		throw new Exception("doCoverPage: "+e);
	}

}

private void doMCPage(String index)
	throws Exception
{
	event_handler.setHeaderType("MC");
	document.resetPageCount();

	Statement stmt = null;
	try {
		String sql = ""
			+"select * "
			+"from ngn.mc_recharge "
			+"where recharge_calendar_year = '"+process_month+"' "
			+"  and recharge_index = '"+index+"' "
			+"  and recharge_financial_mgr_addr = '"+orgn_mgr_addr+"' "
			+"order by recharge_group_code, recharge_employee_id "
		;
		stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);

		//get first record to print more header information. If it does not retrieve, there was no NGN data
		if(rs.next()) {
			//the manager description is sometimes different. Set it again
			event_handler.setManager(orgn_mgr_addr, rs.getString("recharge_financial_mgr_name"));

			//print header information
			Paragraph p = new Paragraph(""
				+"INDEX :     "+leftJustified(index,7," ")+" "+rs.getString("recharge_index_desc")+"\n"
				+"ORG   :     "+leftJustified(rs.getString("recharge_organization"),7," ")+" "+rs.getString("recharge_organization_desc")+"\n"
				+"PROG  :     "+leftJustified(rs.getString("recharge_program"),7," ")+" "+rs.getString("recharge_program_desc")+"\n"
				+"FUND  :     "+leftJustified(rs.getString("recharge_fund"),7," ")+" "+rs.getString("recharge_fund_desc")+"\n"
				,documentFont
			);
			p.setLeading(LEADING);
			document.add(p);

			String last_group_code = "";
			String group_code = "";

			String last_group_desc = "";
			String group_desc = "";

			double current_fte_subtotal = 0;
			double current_amt_subtotal = 0;
			double current_fte_grandtotal = 0;
			double current_amt_grandtotal = 0;

			double prior_fte_subtotal = 0;
			double prior_amt_subtotal = 0;
			double prior_fte_grandtotal = 0;
			double prior_amt_grandtotal = 0;

			double total_amt_subtotal = 0;
			double total_amt_grandtotal = 0;

			do {
				group_code = rs.getString("recharge_group_code").trim();
				group_desc = rs.getString("recharge_group_desc").trim();

				if(!last_group_code.equals(group_code)) {
					if(!last_group_code.equals("")) {
						//print subtotals from last group
						p = new Paragraph(""
							+"                                         -------- -------    --------- ------   -------\n"
							+"TOTAL - GROUP:   "+leftJustified(last_group_desc,15," ")
							+"         "+rightJustified(fte_format.format(current_fte_subtotal),8," ")
							+" "+rightJustified(amt_format.format(current_amt_subtotal),8," ")
							+"   "+rightJustified(fte_format.format(prior_fte_subtotal),9," ")
							+" "+rightJustified(amt_format.format(prior_amt_subtotal),7," ")
							+"  "+rightJustified(amt_format.format(total_amt_subtotal),8," ")
							+"\n "
							,documentFont
						);
						p.setLeading(LEADING);
						document.add(p);

						current_fte_subtotal = 0;
						current_amt_subtotal = 0;
						prior_fte_subtotal = 0;
						prior_amt_subtotal = 0;
						total_amt_subtotal = 0;
					}
					p = new Paragraph("\n"
						+leftJustified(group_desc,15," ")
						+" ( $"+amt_unsigned_format.format(rs.getDouble("recharge_current_group_rate"))+"/MONTH/FTE )"
						+"       ACTIVITY FOR       ADJUSTMENTS FOR\n"
						+"                                  TITL   - CURRENT MTH -     - PRIOR PERIODS -  TOTAL\n"
						+"                           EMPL # CODE     F-T-E    COST       F-T-E   COST     COST\n"
						+"                           ------ ----   ----------------    ----------------   -------\n"
						,documentFont
					);
					p.setLeading(LEADING);
					document.add(p);
				}

				double curr_fte = rs.getDouble("recharge_current_fte");
				double curr_amt = rs.getDouble("recharge_current_amount");
				double prior_fte = rs.getDouble("recharge_prior_fte");
				double prior_amt = rs.getDouble("recharge_prior_amount");
				double total_amt = rs.getDouble("recharge_total_amount");

				current_fte_subtotal += curr_fte;
				current_amt_subtotal += curr_amt;
				prior_fte_subtotal += prior_fte;
				prior_amt_subtotal += prior_amt;
				total_amt_subtotal += total_amt;

				current_fte_grandtotal += curr_fte;
				current_amt_grandtotal += curr_amt;
				prior_fte_grandtotal += prior_fte;
				prior_amt_grandtotal += prior_amt;
				total_amt_grandtotal += total_amt;

				p = new Paragraph(""
					+leftJustified(rs.getString("recharge_employee_name"),26," ")+" "
					+rs.getString("recharge_employee_id").substring(3)+" "
					+rs.getString("recharge_pay_title_code")+" "
					+rs.getString("recharge_multiple_entry_ind")+" "
					+rightJustified(fte_format.format(curr_fte),8," ")+" "
					+rightJustified(amt_format.format(curr_amt),8," ")+"   "
					+rightJustified(fte_format.format(prior_fte),9," ")+" "
					+rightJustified(amt_format.format(prior_amt),7," ")+"  "
					+rightJustified(amt_format.format(total_amt),8," ")+" "
					,documentFont
				);
				p.setLeading(LEADING);
				document.add(p);

				last_group_code = group_code;
				last_group_desc = group_desc;
			} while(rs.next());
			rs.close();

			//print subtotals from last group
			p = new Paragraph(""
				+"                                         -------- -------    --------- ------   -------\n"
				+"TOTAL - GROUP:   "+leftJustified(group_desc,15," ")
				+"         "+rightJustified(fte_format.format(current_fte_subtotal),8," ")
				+" "+rightJustified(amt_format.format(current_amt_subtotal),8," ")
				+"   "+rightJustified(fte_format.format(prior_fte_subtotal),9," ")
				+" "+rightJustified(amt_format.format(prior_amt_subtotal),7," ")
				+"  "+rightJustified(amt_format.format(total_amt_subtotal),8," ")
				+"\n "
				,documentFont
			);
			p.setLeading(LEADING);
			document.add(p);

			//print grandtotals from all groups
			p = new Paragraph("\n"
				+"                                         -------- -------    --------- ------   -------\n"
				+"TOTAL - INDEX:   "+leftJustified(index,10," ")
				+"              "+rightJustified(fte_format.format(current_fte_grandtotal),8," ")
				+" "+rightJustified(amt_format.format(current_amt_grandtotal),8," ")
				+"   "+rightJustified(fte_format.format(prior_fte_grandtotal),9," ")
				+" "+rightJustified(amt_format.format(prior_amt_grandtotal),7," ")
				+"  "+rightJustified(amt_format.format(total_amt_grandtotal),8," ")
				+"\n "
				,documentFont
			);
			p.setLeading(LEADING);
			document.add(p);

			//print the notes
			p = new Paragraph("\n"
				+"Note:      (1) A * following the title code indicates there is activity for the\n"
				+"               employee under other indexes, in other titles within the index,\n"
				+"               or may be due to expense transfer activity.\n"
				+"\n"
				+"           (2) The total cost for each index should appear on your Operating\n"
				+"               Ledger with Expense Account 634107, Med Ctr Network Supp Rechg\n"
				+"\n"
				+"           (3) For information regarding the Med Ctr Network Supp Rechg\n"
				+"               report please contact Med Center Customer Support at 36444."
				,documentFont
			);
			p.setLeading(LEADING);
			document.add(p);
			document.newPage();
		}
	} catch(Exception e) {
		throw new Exception("doMCPage: "+e);
	} finally {
		if(stmt != null)
			stmt.close();
	}
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

public static String pad(String data, int length)
{
	StringBuffer temp = new StringBuffer(data);
	while (temp.length() < length) {
		temp.append(' ');
	}
	return temp.toString();
}

}
