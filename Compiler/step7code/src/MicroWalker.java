import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MicroWalker extends MicroBaseListener {
	//Step 4 Dec.
	private List<IRNode> irList = new ArrayList<IRNode>();
	private Stack<String> pickle = new Stack<String>();
	private Hashtable<String, String> typemap = new Hashtable<String, String>();
	private int regNumber = 1;
	//Step 5 Dec.
	private int labelNumber = 1;
	private Stack<String[]> cond_pickle = new Stack<String[]>();
	//Step 6 Dec.
	private int parameter_reg = 1;
	private int local_reg = 1;
	private int tmp_reg = 1;
	private Hashtable<String, String> parameter_replace = new Hashtable<String, String>();
	private boolean isGlobal = true;
	private Hashtable<String, int[]> local_param_of = new Hashtable<String, int[]>();
	//Step 4 - ASSIGN, DECL., MATH
	@Override public void exitVar_decl(MicroParser.Var_declContext ctx) { 
		String type = ctx.getChild(0).getText();
		while(!pickle.isEmpty()){
			String var = pickle.pop();
		}
		String[] id_list = ctx.getChild(1).getText().trim().split(",");
		for(String var : id_list){
			typemap.put(var, type);
			//Create a local reg
			if(!isGlobal){
				String newLreg = newLocalReg();
				parameter_replace.put(var, newLreg);
				typemap.put(newLreg, type);
			//This comes later
			}else if(isGlobal){
				irList.add(new IRNode("VAR", var, null, null,type.trim()));
			}
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
		if (parameter_replace.containsKey(dest)){
			dest = parameter_replace.get(dest);
		}
		if (parameter_replace.containsKey(operand)){
			operand = parameter_replace.get(operand);
		}
		if (typemap.get(dest).contains("INT")){
			irList.add(new IRNode("STOREI", operand, null, dest, "INT"));
		}else if(typemap.get(dest).contains("FLOAT")){
			irList.add(new IRNode("STOREF", operand, null, dest, "FLOAT"));
		}
	}
	@Override public void exitId(MicroParser.IdContext ctx){
		pickle.push(ctx.getText().trim());
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
			irList.add(new IRNode("STOREI", ctx.getText(), null, reg, "INT"));
			pickle.push(reg);
			typemap.put(reg, "INT");
		}else if(ctx.getText().matches("[0-9]*\\.[0-9]+")){
			String reg = newReg();
			irList.add(new IRNode("STOREF", ctx.getText(), null, reg, "FLOAT"));
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
		String[] id_list = ctx.getChild(2).getText().trim().split(",");
		for(String dest : id_list){
			pickle.pop();
			if (parameter_replace.containsKey(dest)){
				dest = parameter_replace.get(dest);
			}
			if(typemap.get(dest).contains("INT")){
				irList.add(new IRNode("WRITEI", null, null, dest, "INT"));
			}else if(typemap.get(dest).contains("FLOAT")){
				irList.add(new IRNode("WRITEF", null, null, dest, "FLOAT"));
			}else if(typemap.get(dest).contains("STRING")){
				irList.add(new IRNode("WRITES", null, null, dest, "STRING"));
			}
		}
	}
	@Override public void exitRead_stmt(MicroParser.Read_stmtContext ctx) {
		String[] id_list = ctx.getChild(2).getText().trim().split(",");
		for(String dest : id_list){
			pickle.pop();
			if (parameter_replace.containsKey(dest)){
				dest = parameter_replace.get(dest);
			}
			if(typemap.get(dest).contains("INT")){
				irList.add(new IRNode("READI", null, null, dest, "INT"));
			}else if(typemap.get(dest).contains("FLOAT")){
				irList.add(new IRNode("READF", null, null, dest, "FLOAT"));
			}else if(typemap.get(dest).contains("STRING")){
				irList.add(new IRNode("READS", null, null, dest, "STRING"));
			}
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
		//Check replacement
		if (parameter_replace.containsKey(operand1)){
			operand1 = parameter_replace.get(operand1);
		}
		if (parameter_replace.containsKey(operand2)){
			operand2 = parameter_replace.get(operand2);
		}
		if (typemap.get(operand1).contains("INT")){
			typemap.put(dest, "INT");
			String real_opcode = "";
			if(opcode.contains("+")){ real_opcode = "ADDI";}
			else if(opcode.contains("-")){ real_opcode = "SUBI";}
			else if(opcode.contains("*")){ real_opcode = "MULTI";}
			else if(opcode.contains("/")){ real_opcode = "DIVI";}
			irList.add(new IRNode(real_opcode, operand1, operand2, dest, "INT"));
		}else if(typemap.get(operand1).contains("FLOAT")){
			typemap.put(dest, "FLOAT");
			String real_opcode = "";
			if(opcode.contains("+")){ real_opcode = "ADDF";}
			else if(opcode.contains("-")){ real_opcode = "SUBF";}
			else if(opcode.contains("*")){ real_opcode = "MULTF";}
			else if(opcode.contains("/")){ real_opcode = "DIVF";}
			irList.add(new IRNode(real_opcode, operand1, operand2, dest, "FLOAT"));
		}
	}
	
	//Step 5 - IF & DO...WHILE
	private String newLabel(){
		String label = "label" + Integer.toString(labelNumber);
		labelNumber++;
		return label;
	}
	@Override public void enterIf_stmt(MicroParser.If_stmtContext ctx) {
		String nt_label = newLabel();
		String t_label = newLabel();
		String[] the_pickle = {"if", nt_label, t_label};
		cond_pickle.push(the_pickle);
	}
	@Override public void exitIf_stmt(MicroParser.If_stmtContext ctx) {
		while(cond_pickle.peek()[0] != "if"){
			cond_pickle.pop();
		}
		String t_label = cond_pickle.pop()[2];
		irList.add(new IRNode("LABEL", null, null, t_label, "NA"));
	}
	@Override public void enterElse_part(MicroParser.Else_partContext ctx) {
		String t_label = cond_pickle.peek()[2];
		String my_label = cond_pickle.peek()[1];
		irList.add(new IRNode("JUMP", null, null, t_label, "NA"));
		irList.add(new IRNode("LABEL", null, null, my_label, "NA"));
		
		if(ctx.getChildCount() > 1){
			String nt_label = newLabel();
			String[] the_pickle = {"else_if", nt_label, t_label};
			cond_pickle.push(the_pickle);
		}
	}
	@Override public void exitCond(MicroParser.CondContext ctx) {
		String nt_label = cond_pickle.peek()[1];
		if(ctx.getChildCount() == 1){
			String always_tf = ctx.getChild(0).getText();
			String reg1 = newReg();
			String reg2 = newReg();
			irList.add(new IRNode("STOREI", "1", null, reg1, "INT"));
			irList.add(new IRNode("STOREI", "1", null, reg2, "INT"));
			typemap.put(reg1, "INT");
			typemap.put(reg2, "INT");
			if(always_tf.contains("TRUE")){
				irList.add(new IRNode("NE", reg1, reg2, nt_label, "INT"));
			}else if(always_tf.contains("FALSE")){
				irList.add(new IRNode("EQ", reg1, reg2, nt_label, "INT"));
			}
			return;
		}
		String op2 = pickle.pop();
		String op1 = pickle.pop();
		String compop = ctx.getChild(1).getText();
		if(cond_pickle.peek()[0] == "do_while"){
			irList.add(new IRNode("LABEL", null, null, cond_pickle.peek()[3], "NA"));
		}
		if(parameter_replace.containsKey(op1)){
			op1 = parameter_replace.get(op1);
		}
		if(parameter_replace.containsKey(op2)){
			op2 = parameter_replace.get(op2);
		}
		String compType = typemap.get(op1).trim();
		//System.out.println(op1 + compop + op2);
		if(compop.contains(">=")){
			irList.add(new IRNode("LT", op1, op2, nt_label, compType));
		}else if(compop.contains("<=")){
			irList.add(new IRNode("GT", op1, op2, nt_label, compType));
		}else if(compop.contains("!=")){
			irList.add(new IRNode("EQ", op1, op2, nt_label, compType));
		}else if(compop.contains(">")){
			irList.add(new IRNode("LE", op1, op2, nt_label, compType));
		}else if(compop.contains("<")){
			irList.add(new IRNode("GE", op1, op2, nt_label, compType));
		}else if(compop.contains("=")){
			irList.add(new IRNode("NE", op1, op2, nt_label, compType));
		}
	}
	@Override public void enterDo_while_stmt(MicroParser.Do_while_stmtContext ctx) {
		String t_label = newLabel();
		String nt_label = newLabel();
		String dumb_label = newLabel();
		String[] the_pickle = {"do_while", nt_label, t_label, dumb_label};
		cond_pickle.push(the_pickle);
		irList.add(new IRNode("LABEL", null, null, t_label, "NA"));
	}
	@Override public void exitDo_while_stmt(MicroParser.Do_while_stmtContext ctx) {
		String t_label = cond_pickle.peek()[2];
		String nt_label = cond_pickle.pop()[1];
		irList.add(new IRNode("JUMP", null, null, t_label, "NA"));
		irList.add(new IRNode("LABEL", null, null, nt_label, "NA"));
	}
	
	//Step 6 - FUNC & STRING
	private String newParamReg(){
		String Preg = "$P" + Integer.toString(parameter_reg);
		parameter_reg++;
		return Preg;
	}
	private String newLocalReg(){
		String Lreg = "$L" + Integer.toString(local_reg);
		local_reg++;
		return Lreg;
	}
	@Override public void exitParam_decl(MicroParser.Param_declContext ctx) {
		String var = pickle.pop();
		String type = ctx.getChild(0).getText();
		String newPreg = newParamReg();
		parameter_replace.put(var, newPreg);
		typemap.put(newPreg, type);
	}
	@Override public void enterFunc_decl(MicroParser.Func_declContext ctx) {
		if (isGlobal) { //if it is the first func we encounter
			isGlobal = false;
			irList.add(new IRNode("EOGD", null, null ,null, "NA"));
		}
		String func_name = ctx.getChild(2).getText().trim();
		irList.add(new IRNode("LABEL", null, null, func_name, "NA"));
		irList.add(new IRNode("LINK", null, null, null, "NA"));
	}
	@Override public void exitReturn_stmt(MicroParser.Return_stmtContext ctx) {
		String returnReg = pickle.pop();
		if(parameter_replace.containsKey(returnReg.trim())){
			returnReg = parameter_replace.get(returnReg.trim());
		}
		if(typemap.get(returnReg).contains("INT")){
			irList.add(new IRNode("STOREI", returnReg, null, "$R", "INT"));
		}else if(typemap.get(returnReg).contains("FLOAT")){
			irList.add(new IRNode("STOREF", returnReg, null, "$R", "FLOAT"));
		}else if(typemap.get(returnReg).contains("STRING")){
			irList.add(new IRNode("STORES", returnReg, null, "$R", "STRING"));
		}
		irList.add(new IRNode("RET", null, null, null, "NA"));
	}
	@Override public void exitFunc_decl(MicroParser.Func_declContext ctx) {
		//set up the local_param_of table
		String function_name = ctx.getChild(2).getText().trim();
		int LP [] = new int[3];
		LP[0] = local_reg - 1;
		LP[1] = parameter_reg - 1;
		LP[2] = regNumber - 1;
		local_param_of.put(function_name, LP);
		//reset
		parameter_reg = 1;
		local_reg = 1;
		tmp_reg = regNumber;
		regNumber = 1;
		parameter_replace.clear();
		//Check if in need of an addtional RET
		if(irList.get(irList.size() - 1).op != "RET"){
			irList.add(new IRNode("RET", null, null, null, "NA"));
		}
		irList.add(new IRNode("NEWLINE", null, null, null, "NA"));
	}
	@Override public void exitString_decl(MicroParser.String_declContext ctx) {
		String stringName = ctx.getChild(1).getText().trim();
		String stringContent = ctx.getChild(3).getText().trim();
		typemap.put(stringName,"STRING");
		irList.add(new IRNode("STRING", stringName, null, stringContent, "STRING"));
	}
	@Override public void exitCall_expr(MicroParser.Call_exprContext ctx) {
		//This function is written to celebrate that Jun got the lumberjack
		String [] chop_chop = ctx.getChild(2).getText().trim().split(",");
		int count = 0;
		boolean freeze = false;
		for(String timber: chop_chop){
			if(timber.contains("(")){
				freeze = true;
			}else if(timber.contains(")")){
				freeze = false;
			}
			
			if(!freeze){
				count++;
			}
		}
		Stack<String> lumberjack = new Stack<String>();
		//Push
		irList.add(new IRNode("PUSH", null, null, null, "NA"));
		//Get the function name
		for (int i = 0; i < count; i++){
			lumberjack.push(pickle.pop());
		}
		String func_name = pickle.pop();
		//Push the parameters, add to a list
		List<String> the_log = new ArrayList<String>();
		while (!lumberjack.isEmpty()){
			the_log.add(lumberjack.pop());
		}
		for(String rage : the_log){
			if(parameter_replace.containsKey(rage.trim())){
				irList.add(new IRNode("PUSH", null , null, parameter_replace.get(rage.trim()), "NA"));
			}else{
				irList.add(new IRNode("PUSH", null , null, rage.trim(), "NA"));
			}
		}
		irList.add(new IRNode("JSR", null , null, func_name.trim(), "NA"));
		//Pop everything
		for(String rage : the_log){
			irList.add(new IRNode("POP", null,null, null, "NA"));
		}
		//Pop the result to a temp reg
		String newTreg = newReg();
		irList.add(new IRNode("POP", null, null ,newTreg, "NA"));
		pickle.push(newTreg);
	}
	//Translate to tiny code
	private void toTiny(){
		Translater translater = new Translater(irList, tmp_reg, local_param_of);
		//translater.start();
	}
	
}