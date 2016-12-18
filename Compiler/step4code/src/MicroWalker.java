import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MicroWalker extends MicroBaseListener {
	private List<IRNode> irList = new ArrayList<IRNode>();
	private List<TinyNode> tinyList = new ArrayList<TinyNode>();
	private Stack<String> pickle = new Stack<String>();
	private Hashtable<String, String> typemap = new Hashtable<String, String>();
	private int regNumber = 1;
	
	@Override public void exitVar_decl(MicroParser.Var_declContext ctx) { 
		String type = ctx.getChild(0).getText();
		while(!pickle.isEmpty()){
			String var = pickle.pop();
		}
		String[] id_list = ctx.getChild(1).getText().trim().split(",");
		for(String var : id_list){
			typemap.put(var, type);
			tinyList.add(new TinyNode("var", var, null));
		}
	}
	@Override public void exitProgram(MicroParser.ProgramContext ctx){
		for(int i = 0; i < irList.size(); i++){
			irList.get(i).showNode();
		}
		System.out.println(";tiny code");
		toTiny();
	}
	@Override public void exitAssign_stmt(MicroParser.Assign_stmtContext ctx){
		String operand = pickle.pop();
		String dest = pickle.pop();
		if (typemap.get(dest).contains("INT")){
			irList.add(new IRNode("STOREI", operand, null, dest));
		}else if(typemap.get(dest).contains("FLOAT")){
			irList.add(new IRNode("STOREF", operand, null, dest));
		}
	}
	@Override public void exitId(MicroParser.IdContext ctx){
		pickle.push(ctx.getText());
		//System.out.println(ctx.getText());
	}
	@Override public void exitAddop(MicroParser.AddopContext ctx) {
		pickle.push(ctx.getText());
	}
	@Override public void exitMulop(MicroParser.MulopContext ctx) {
		pickle.push(ctx.getText());
	}
	@Override public void exitPrimary(MicroParser.PrimaryContext ctx){
		if(ctx.getText().matches("[0-9]+")){
			String reg = newReg();
			irList.add(new IRNode("STOREI", ctx.getText(), null, reg));
			pickle.push(reg);
			typemap.put(reg, "INT");
		}else if(ctx.getText().matches("[0-9]*\\.[0-9]+")){
			String reg = newReg();
			irList.add(new IRNode("STOREF", ctx.getText(), null, reg));
			pickle.push(reg);
			typemap.put(reg, "FLOAT");
		}
	}
	@Override public void exitExpr(MicroParser.ExprContext ctx){
		if(ctx.getText().matches("[0-9]+")){
			return;
		}else if(ctx.getText().matches("[0-9]*\\.[0-9]+")){
			return;
		}
		if(ctx.getChild(0).getChildCount() <= 1){
			return;
		}
		ef_only();
	}
	@Override public void exitFactor(MicroParser.FactorContext ctx){
		if(ctx.getText().matches("[0-9]+")){
			return;
		}else if(ctx.getText().matches("[0-9]*\\.[0-9]+")){
			return;
		}
		if(ctx.getChild(0).getChildCount() <= 1){
			return;
		}
		ef_only();
	}
	@Override public void exitFactor_prefix(MicroParser.Factor_prefixContext ctx){
		if(ctx.getChildCount() == 0){
			return;
		}
		if(ctx.getChild(0).getChildCount() <= 1){
			return;
		}
		String extra_op = pickle.pop();
		ef_only();
		pickle.push(extra_op);
	}
	@Override public void exitExpr_prefix(MicroParser.Expr_prefixContext ctx){
		if(ctx.getChildCount() == 0){
			return;
		}
		
		if(ctx.getChild(0).getChildCount() <= 1){
			return;
		}
		String extra_op = pickle.pop();
		ef_only();
		pickle.push(extra_op);
	}
	@Override public void exitWrite_stmt(MicroParser.Write_stmtContext ctx) {
		String dest = pickle.pop();
		if(typemap.get(dest).contains("INT")){
			irList.add(new IRNode("WRITEI", null, null, dest));
		}else if(typemap.get(dest).contains("FLOAT")){
			irList.add(new IRNode("WRITEF", null, null, dest));
		}
	}
	@Override public void exitRead_stmt(MicroParser.Read_stmtContext ctx) {
		String dest = pickle.pop();
		if(typemap.get(dest).contains("INT")){
			irList.add(new IRNode("READI", null, null, dest));
		}else if(typemap.get(dest).contains("FLOAT")){
			irList.add(new IRNode("READF", null, null, dest));
		}
	}

	private String newReg(){
		String reg = "$T" + Integer.toString(regNumber);
		regNumber++;
		return reg;
	}
	private void ef_only(){
		String operand2 = pickle.pop();
		String opcode = pickle.pop();
		String operand1 = pickle.pop();
		String dest = newReg();
		pickle.push(dest);
		if (typemap.get(operand1).contains("INT")){
			typemap.put(dest, "INT");
			String real_opcode = "";
			if(opcode.contains("+")){ real_opcode = "ADDI";}
			else if(opcode.contains("-")){ real_opcode = "SUBI";}
			else if(opcode.contains("*")){ real_opcode = "MULTI";}
			else if(opcode.contains("/")){ real_opcode = "DIVI";}
			irList.add(new IRNode(real_opcode, operand1, operand2, dest));
		}else if(typemap.get(operand1).contains("FLOAT")){
			typemap.put(dest, "FLOAT");
			String real_opcode = "";
			if(opcode.contains("+")){ real_opcode = "ADDF";}
			else if(opcode.contains("-")){ real_opcode = "SUBF";}
			else if(opcode.contains("*")){ real_opcode = "MULTF";}
			else if(opcode.contains("/")){ real_opcode = "DIVF";}
			irList.add(new IRNode(real_opcode, operand1, operand2, dest));
		}
	}
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
	private void toTiny(){
		int regNum;
		for(IRNode ir: irList){
			String first = updateReg(ir.first);
			String second = updateReg(ir.second);
			String dest = updateReg(ir.dest);
			
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
					tinyList.add(new TinyNode("move", first, dest));
					break;
				case "STOREF":
					tinyList.add(new TinyNode("move", first, dest));
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
			}
		}
		tinyList.add(new TinyNode("sys halt", null, null));
		for(TinyNode tn: tinyList){
			tn.showNode();
		}
	}
	
	
	
	
	
	
	
	
}