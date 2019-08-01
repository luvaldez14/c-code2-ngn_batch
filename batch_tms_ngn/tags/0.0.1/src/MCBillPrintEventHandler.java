import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author Jennifer Kramer
 *
 * Handle's iText Events. onEndPage is a common event to use to create header
 * and footers to a PDF document. And the end of every page this method
 * is called. The TMS bill does not have headers and footers on all pages and
 * it changes, therefore, additional variables and accessor methods were set
 * up to create communication between the PDF generation process and the
 * Events in order to create the proper header and footers.
 *
 */
public class MCBillPrintEventHandler extends PdfPageEventHelper {

	private static final float TOP_MARGIN = MCBillPrintCreatePdf.TOP_MARGIN;

	private String orgn_mgr_addr = "";
	private String orgn_mgr = "";

	private String period_end = "";

	private String header_type = "";


	public MCBillPrintEventHandler() {
		super();
	}

	public void onOpenDocument(PdfWriter writer, Document document) {}
	public void onCloseDocument(PdfWriter writer, Document document) {}
	public void onStartPage(PdfWriter writer, Document document) {}

	public void onEndPage(PdfWriter writer, Document document) {
		BaseFont BASEFONTB = null;
		try {
			BASEFONTB = BaseFont.createFont(BaseFont.COURIER,BaseFont.CP1252,BaseFont.NOT_EMBEDDED);
		} catch(Exception e) {}
		int LEADING = MCBillPrintCreatePdf.LEADING;

		PdfContentByte content = writer.getDirectContent();
		content.beginText();
		content.setFontAndSize(BASEFONTB,MCBillPrintCreatePdf.FONT_SIZE);

		if(header_type.equals("COVER")) {
			content.showTextAligned(
				PdfContentByte.ALIGN_LEFT,
				"MAIL TO ....",
				document.leftMargin(),
				document.getPageSize().height()-TOP_MARGIN-(8*LEADING),
				0
			);
			content.showTextAligned(
				PdfContentByte.ALIGN_LEFT,
				pad(orgn_mgr,38)+rightJustified(orgn_mgr_addr,4,"0"),
				document.leftMargin(),
				document.getPageSize().height()-TOP_MARGIN-(9*LEADING),
				0
			);
		} else if(header_type.equals("MC")) {
			/** Page Number on the Right **/
			content.showTextAligned(
				PdfContentByte.ALIGN_RIGHT,
				"Page "+rightJustified(""+(document.getPageNumber()+1),3," "),
				document.getPageSize().width()-document.rightMargin(),
				document.getPageSize().height()-TOP_MARGIN-(1*LEADING),
				0
			);

			/** Financial Manager Address and Name **/
			content.showTextAligned(
				PdfContentByte.ALIGN_LEFT,
				"ADDRESS: "+orgn_mgr_addr,
				document.leftMargin(),
				document.getPageSize().height()-TOP_MARGIN-(1*LEADING),
				0
			);
			content.showTextAligned(
				PdfContentByte.ALIGN_LEFT,
				"MANGER: "+orgn_mgr,
				document.leftMargin(),
				document.getPageSize().height()-TOP_MARGIN-(3*LEADING),
				0
			);

			/** Centered Header data **/
			content.showTextAligned(
				PdfContentByte.ALIGN_CENTER,
				"MED CENTER NETWORK SUPPORT SVC - RECHARGE SUMMARY",
				document.getPageSize().width()/2,
				document.getPageSize().height()-TOP_MARGIN-(5*LEADING),
				0
			);
			content.showTextAligned(
				PdfContentByte.ALIGN_CENTER,
				"UNIVERSITY OF CALIFORNIA",
				document.getPageSize().width()/2,
				document.getPageSize().height()-TOP_MARGIN-(6*LEADING),
				0
			);
			content.showTextAligned(
				PdfContentByte.ALIGN_CENTER,
				"SAN DIEGO",
				document.getPageSize().width()/2,
				document.getPageSize().height()-TOP_MARGIN-(7*LEADING),
				0
			);

			/** Month End **/
			content.showTextAligned(
				PdfContentByte.ALIGN_LEFT,
				"MONTH-END PROCESS DATE:  "+period_end.substring(4,6)+"/"+period_end.substring(0,4),
				document.leftMargin(),
				document.getPageSize().height()-TOP_MARGIN-(9*LEADING),
				0
			);
		}
		content.endText();
	}

	public void onParagraph(PdfWriter writer, Document document, float paragraphPosition) {}
	public void onParagraphEnd(PdfWriter writer,Document document,float paragraphPosition) {}
	public void onChapter(PdfWriter writer,Document document,float paragraphPosition, Paragraph title) {}
	public void onChapterEnd(PdfWriter writer,Document document,float paragraphPosition) {}
	public void onSection(PdfWriter writer,Document document,float paragraphPosition, int depth, Paragraph title) {}
	public void onSectionEnd(PdfWriter writer,Document document,float paragraphPosition) {}
    public void onGenericTag(PdfWriter writer, Document document, Rectangle rect, String text) {}

    public void setManager(String orgn_mgr_addr, String orgn_mgr) {
    	this.orgn_mgr_addr = orgn_mgr_addr;
    	this.orgn_mgr = orgn_mgr;
    }

    public void setPeriodEnd(String period_end) {
    	this.period_end = period_end;
    }

    /**
     * @param header_type values are COVER or MC
     */
    public void setHeaderType(String header_type) {
    	this.header_type = header_type;
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
