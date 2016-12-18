import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

//Stephen will take care of this file
public class Translater{
	private List<TinyNode> tinyList = new ArrayList<TinyNode>();
	private List<IRNode> irList;
	private int regNumber; //May need this for reference
	public Translater(List<IRNode> foreign, int regNumber){
		this.irList = foreign;
		this.regNumber = regNumber;
		
		testcaseGenerator();//Comment this out
	}
	//For test cases
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
		int regNum;
		String first, second, dest, typi;
		for(IRNode ir: irList){
			first = updateReg(ir.first);
			second = updateReg(ir.second);
			dest = updateReg(ir.dest);
			typi = ir.type;
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
					if(!checkOperands(ir.first, ir.dest)){
						tinyList.add(new TinyNode("move", first, "r"+Integer.toString(regNumber)));
						tinyList.add(new TinyNode("move", "r"+Integer.toString(regNumber), dest));
						regNumber++;
					} else {
						tinyList.add(new TinyNode("move", first, dest));
					}
					break;
				case "STOREF":
					if(!checkOperands(ir.first, ir.dest)){
						tinyList.add(new TinyNode("move", first, "r"+Integer.toString(regNumber)));
						tinyList.add(new TinyNode("move", "r"+Integer.toString(regNumber), dest));
						regNumber++;
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
			}
		}
		tinyList.add(new TinyNode("sys halt", null, null));
		for(TinyNode tn: tinyList){
			tn.showNode();
		}
	}
	
	
	//Private Func
	private String updateReg(String old){
		if(old == null){
			return old;
		}
		String pattern = "\\$T([1-9]+[0-9]*)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(old);
		if(m.find()){
			int regNum = Integer.parseInt(m.group(1)) - 1;
			return "r" + Integer.toString(regNum);
		}
		return old;
	}
	private boolean checkOperands(String first, String dest){
		return (first.charAt(0) == '$' || dest.charAt(0) == '$');
	}
	private void condition_tiny_node(String irfirst, String irsecond, String first, String second, String typi){
		String second_reg = second;
		if(!checkOperands(irfirst, irsecond)){
			second_reg = "r"+Integer.toString(regNumber);
			tinyList.add(new TinyNode("move", second, second_reg));
			regNumber++;
		}
		if(typi.contains("FLOAT") || typi.contains("FLOAT")){
			tinyList.add(new TinyNode("cmpr", first, second_reg));
		}else{
			tinyList.add(new TinyNode("cmpi", first, second_reg));
		}
	}
	
}