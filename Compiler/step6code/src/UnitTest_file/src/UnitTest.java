import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*; 

public class UnitTest{
	public static void main(String [] args){
		String file = args[1];
		int regNumber = 0;
		List<IRNode> irList = new ArrayList<IRNode>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		    String line;
		    int cnt = 0;
		    while ((line = br.readLine()) != null) {
		    	//Get the reg number
		    	if(cnt == 0){
		    		regNumber = Integer.parseInt(line.trim());
		    	}
		    	//Set up ir node and add to list
		    	else if (cnt > 0){
		    		String[] alist = line.trim().split(",");
		    		String op, f, s, d, t;
		    		op = alist[0];
		    		f = alist[1];
		    		s = alist[2];
		    		d = alist[3];
		    		t = alist[4];
		    		if(op == "null"){
		    			op = null;
		    		}
		    		if(f == "null"){
		    			f = null;
		    		}
		    		if(s == "null"){
		    			s = null;
		    		}
		    		if(d == "null"){
		    			d = null;
		    		}
		    		if(t == "null"){
		    			t = null;
		    		}
		    		IRNode o = new IRNode(op, f, s, d, t);
		    		irList.add(o);
		    	}
		    }
		    toTiny(irList, regNumber);
		    br.close();
		}catch(IOException e){
			//do nothing
		}
	}
	private static void toTiny(List<IRNode> irList, int tmp_reg){
		Translater translater = new Translater(irList, tmp_reg);
		translater.start();
	}
}