import java.util.List;
import java.util.ArrayList;
//import java.util.Stack;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
//import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

//Stephen will take care of this file
/*Hidden IRNode contains
	NEWLINE -- print out a new line, visual effect only
	STRING -- global string declaration
	VAR -- global variable declaration
	EOGD -- end of global declaration
	local_param_of -- hash table, <function name> : {local, parameter}
*/
public class Translater{
	//member variables
	private List<TinyNode> tinyList = new ArrayList<TinyNode>();
	private List<IRNode> irList;
	private int regNumber; //May need this for reference
	private Hashtable<String, int[]>LP;
	
	//optional member var
	private int local_reg_num = 0;
	private int param_reg_num = 0;
	private int reg_num = 0;
	private Hashtable<String, String> temp_reg_map = new Hashtable<String, String>();
	//set up
	public Translater(List<IRNode> foreign, int regNumber, Hashtable<String, int[]>LP){
		this.irList = foreign;
		this.regNumber = regNumber;
		this.LP = LP;
		
		//testcaseGenerator();//Comment this out
	}
	//For test cases, serves nothing in the real code
	private void testcaseGenerator(){
		try{
			String flist = "flist.txt";
			FileOutputStream listout = new FileOutputStream(flist);
			String what = Integer.toString(regNumber) + "\n";
			byte[] w = what.getBytes();
			listout.write(w);
			for (IRNode x : irList){
				String line = x.op + "," + x.first + "," + x.second + ","
						+ x.dest + "," + x.type + "\n";
				byte[] c2 = line.getBytes();
				listout.write(c2);
			}
			listout.close();
		}catch (IOException e){
			//do nothing
		}
	}
	
	//Translater translate ir to tiny
	public void start(){
		String first, second, dest, typi; //ir node info
		String function_name = null;
		for(int i = 0; i < irList.size(); i++){
			//get the current ir node and the previous ir node
			IRNode ir = irList.get(i);
			int i_previous = i - 1;
			if(i == 0){
				i_previous = 0;
			}
			IRNode ir_previous = irList.get(i_previous);
			
			//update and check register, if not a register, do nothing
			first = updateReg(ir.first);
			second = updateReg(ir.second);
			dest = updateReg(ir.dest);
			typi = ir.type;
			
			//translate
			switch(ir.op){
				case "ADDI":
					tinyList.add(new TinyNode("move", first, dest));
					tinyList.add(new TinyNode("addi", second, dest));
					break;
				case "ADDF":
					tinyList.add(new TinyNode("move", first, dest));
					tinyList.add(new TinyNode("addr", second, dest));
					break;
				case "SUBI":
					tinyList.add(new TinyNode("move", first, dest));
					tinyList.add(new TinyNode("subi", second, dest));
					break;
				case "SUBF":
					tinyList.add(new TinyNode("move", first, dest));
					tinyList.add(new TinyNode("subr", second, dest));
					break;
				case "MULTI":
					tinyList.add(new TinyNode("move", first, dest));
					tinyList.add(new TinyNode("muli", second, dest));
					break;
				case "MULTF":
					tinyList.add(new TinyNode("move", first, dest));
					tinyList.add(new TinyNode("mulr", second, dest));
					break;
				case "DIVI":
					tinyList.add(new TinyNode("move", first, dest));
					tinyList.add(new TinyNode("divi", second, dest));
					break;
				case "DIVF":
					tinyList.add(new TinyNode("move", first, dest));
					tinyList.add(new TinyNode("divr", second, dest));
					break;
				case "STOREI":
					if(!areTReg(ir.first, ir.dest)){
						String middle_reg = newrReg();
						tinyList.add(new TinyNode("move", first, middle_reg));
						tinyList.add(new TinyNode("move", middle_reg, dest));
					} else {
						tinyList.add(new TinyNode("move", first, dest));
					}
					break;
				case "STOREF":
					if(!areTReg(ir.first, ir.dest)){
						String middle_reg = newrReg();
						tinyList.add(new TinyNode("move", first, middle_reg));
						tinyList.add(new TinyNode("move", middle_reg, dest));
					} else {
						tinyList.add(new TinyNode("move", first, dest));
					}
					break;
				case "READI":
					tinyList.add(new TinyNode("sys readi", dest, null));
					break;
				case "READF":
					tinyList.add(new TinyNode("sys readr", dest, null));
					break;
				case "WRITEI":
					tinyList.add(new TinyNode("sys writei", dest, null));
					break;
				case "WRITEF":
					tinyList.add(new TinyNode("sys writer", dest, null));
					break;
				case "WRITES":
					tinyList.add(new TinyNode("sys writes", dest, null));
					break;
				case "LABEL":
					tinyList.add(new TinyNode("label", dest, null));
					break;
				case "EQ":
					condition_tiny_node(ir.first, ir.second, first, second, typi);
					tinyList.add(new TinyNode("jeq", dest, null));
					break;
				case "NE":
					condition_tiny_node(ir.first, ir.second, first, second, typi);
					tinyList.add(new TinyNode("jne", dest, null));
					break;
				case "GT":
					condition_tiny_node(ir.first, ir.second, first, second, typi);
					tinyList.add(new TinyNode("jgt", dest, null));
					break;
				case "GE":
					condition_tiny_node(ir.first, ir.second, first, second, typi);
					tinyList.add(new TinyNode("jge", dest, null));
					break;
				case "LT":
					condition_tiny_node(ir.first, ir.second, first, second, typi);
					tinyList.add(new TinyNode("jlt", dest, null));
					break;
				case "LE":
					condition_tiny_node(ir.first, ir.second, first, second, typi);
					tinyList.add(new TinyNode("jle", dest, null));
					break;
				case "JUMP":
					tinyList.add(new TinyNode("jmp", dest, null));
					break;
				case "STRING":
					tinyList.add(new TinyNode("str", first, dest));
					break;
				case "LINK":
					local_reg_num = LP.get(ir_previous.dest)[0];
					param_reg_num = LP.get(ir_previous.dest)[1];
					tinyList.add(new TinyNode("link", Integer.toString(local_reg_num), null));
					break;
				case "RET":
					tinyList.add(new TinyNode("unlnk", null, null));
					tinyList.add(new TinyNode("ret", null, null));
					break;
				case "PUSH":
					tinyList.add(new TinyNode("push", dest, null));
					break;
				case "POP":
					tinyList.add(new TinyNode("pop", dest, null));
					break;
				case "JSR":
					tinyList.add(new TinyNode("push", "r0", null));
					tinyList.add(new TinyNode("push", "r1", null));
					tinyList.add(new TinyNode("push", "r2", null));
					tinyList.add(new TinyNode("push", "r3", null));
					tinyList.add(new TinyNode("jsr", dest, null));
					tinyList.add(new TinyNode("pop", "r3", null));
					tinyList.add(new TinyNode("pop", "r2", null));
					tinyList.add(new TinyNode("pop", "r1", null));
					tinyList.add(new TinyNode("pop", "r0", null));
					break;
				case "EOGD":
					tinyList.add(new TinyNode("push", null, null));
					tinyList.add(new TinyNode("push", "r0", null));
					tinyList.add(new TinyNode("push", "r1", null));
					tinyList.add(new TinyNode("push", "r2", null));
					tinyList.add(new TinyNode("push", "r3", null));
					tinyList.add(new TinyNode("jsr", "main", null));
					tinyList.add(new TinyNode("sys halt", null, null));
					break;
				case "NEWLINE":
					temp_reg_map.clear();
					reg_num = 0;
					break;
				
			}
		}
		tinyList.add(new TinyNode("end", null, null));
		for(TinyNode tn: tinyList){
			tn.showNode();
		}
	}
	
	
	//Private Func
	private String updateReg(String old){
		if(old == null){
			return old;
		}
		//temp reg
		if (temp_reg_map.containsKey(old)){
			return temp_reg_map.get(old);
		}
		String pattern_t = "\\$T([1-9]+[0-9]*)";
		Pattern rt = Pattern.compile(pattern_t);
		Matcher mt = rt.matcher(old);
		if(mt.find()){
			String r = "r" + Integer.toString(reg_num);
			reg_num++;
			temp_reg_map.put(old, r);
			return r;
		}
		//param reg
		String pattern_p = "\\$P([1-9]+[0-9]*)";
		Pattern rp = Pattern.compile(pattern_p);
		Matcher mp = rp.matcher(old);
		if(mp.find()){
			int regNum = Integer.parseInt(mp.group(1));
			return "$" + Integer.toString(6 + param_reg_num - regNum);
		}
		//local reg
		String pattern_l = "\\$L([1-9]+[0-9]*)";
		Pattern rl = Pattern.compile(pattern_l);
		Matcher ml = rl.matcher(old);
		if(ml.find()){
			int regNum = Integer.parseInt(ml.group(1));
			return "$-" + Integer.toString(regNum);
		}
		//r reg
		if(old == "$R"){
			return "$" + Integer.toString(6 + param_reg_num);
		}
		return old;
	}
	private String newrReg(){
		String r = "r" + Integer.toString(reg_num);
		reg_num++;
		return r;
	}
	private boolean areTReg(String first, String dest){
		return (first.contains("$T") || dest.contains("$T"));
	}
	private void condition_tiny_node(String irfirst, String irsecond, String first, String second, String typi){
		String second_reg = second;
		if(!areTReg(irfirst, irsecond)){
			second_reg = newrReg();
			tinyList.add(new TinyNode("move", second, second_reg));
		}
		if(typi.contains("FLOAT") || typi.contains("FLOAT")){
			tinyList.add(new TinyNode("cmpr", first, second_reg));
		}else{
			tinyList.add(new TinyNode("cmpi", first, second_reg));
		}
	}
	
}