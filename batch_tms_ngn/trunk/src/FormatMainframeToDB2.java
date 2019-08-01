import java.io.*;
import java.util.*;

public class FormatMainframeToDB2 {

public static BufferedReader in = null;
public static PrintWriter out = null;

private static String formatString(String s)
	throws Exception
{
	return formatString(s,true);
}

private static String formatString(String s, boolean trim)
	throws Exception
{
	try {
		if(trim) s = s.trim();
		if(s.equals(""))
			return "";
		else
			return "\""+s+"\"";

	} catch(Exception e) {
		throw new Exception("formatString: error formatting string: "+e);
	}
}

private static String formatDecimal(String s, int size, int decimals)
	throws Exception
{
	try {
		s = s.trim();
		String sign = "+";
		if(s.startsWith("-")) {
			s = s.substring(1);
			sign = "-";
		}

		String before_dec = "";
		String after_dec = "";
		int decimal_pos = s.indexOf(".");
		if(decimal_pos < 0) {
			before_dec = s;
		} else {
			before_dec = s.substring(0,decimal_pos);
			after_dec = s.substring(decimal_pos+1);
		}

		return sign+justify(before_dec,size-decimals,"right")+"."+justify(after_dec,decimals,"left");

	} catch(Exception e) {
		throw new Exception("formatString: error formatting string: "+e);
	}
}

private static String justify(String s, int size, String right_left)
	throws Exception
{
	try {
		int target_len = size-s.length();
		for(int i=0; i < target_len; i++) {
			if(right_left.equals("right"))
				s = "0"+s;
			else
				s += "0";
		}
		return s;

	} catch(Exception e) {
		throw new Exception("justify: "+e);
	}
}

public static void formatEmployee(String filename)
	throws Exception
{
	try {
		in = new BufferedReader(new FileReader(filename));
		out = new PrintWriter(new FileWriter(filename.toLowerCase()+".txt"));

		String line;
		while((line = in.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line,"\t");

			String new_line = ""
				+formatString(st.nextToken())+","		//employee ID
				+st.nextToken()+","						//process month
				+formatString(st.nextToken(),false)+","	//title code, part of table key, don't trim
				+formatString(st.nextToken())+","		//appointment type code
				+formatString(st.nextToken())+","		//student status
			;
			//the last 3 may not exist in the file
			if(st.hasMoreTokens()) {
				new_line += formatString(st.nextToken())+",";		//department code
				if(st.hasMoreTokens()) {
					new_line += formatString(st.nextToken())+",";	//mail code
					if(st.hasMoreTokens()) {
						new_line += formatString(st.nextToken());	//tms location code
					}
				} else {
					new_line += ","; //mail code, tms location code
				}
			} else {
				new_line += ",,"; //department code, mail code, tms location code
			}

			out.println(new_line);
		}
	} catch(Exception e) {
		throw new Exception("formatEmployee: "+e);
	} finally {
		if(in != null)
			in.close();
		if(out != null) {
			out.flush();
			out.close();
		}
	}
}

public static void formatPayroll(String filename)
	throws Exception
{
	try {
		in = new BufferedReader(new FileReader(filename));
		out = new PrintWriter(new FileWriter(filename.toLowerCase()+".txt"));

		String line;
		while((line = in.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line,"\t");

			String new_line = ""
				+formatString(st.nextToken(),false)+","	//employee ID
				+st.nextToken()+","						//process month
				+formatString(st.nextToken(),false)+","	//title code
				+formatString(st.nextToken(),false)+","	//pay index
				+formatString(st.nextToken(),false)+","	//dos code
				+formatString(st.nextToken(),false)+","	//pay activity code
				+formatString(st.nextToken(),false)+","	//dos type
				+formatDecimal(st.nextToken(),9,4)+","	//fte
				+formatDecimal(st.nextToken(),9,2)+","	//amount
				+formatString(st.nextToken(),false)+","	//fund
				+formatString(st.nextToken(),false)+","	//org
				+formatString(st.nextToken(),false)+","	//prog
				+st.nextToken()							//posting month
			;

			out.println(new_line);
		}
	} catch(Exception e) {
		throw new Exception("formatPayroll: "+e);
	} finally {
		if(in != null)
			in.close();
		if(out != null) {
			out.flush();
			out.close();
		}
	}
}

public static void main(String args[])
	throws Exception
{
	formatEmployee("/tms/ngn/extracts/EMPLOYEE");
	formatPayroll ("/tms/ngn/extracts/PAYROLL");
}

}
