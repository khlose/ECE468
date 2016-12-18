import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

public class Optimizer{
	//original
	private List<IRNode> irList;
	//CFG
	private Hashtable<IRNode, List<IRNode>> predecessors = new Hashtable<IRNode, List<IRNode>>();
	private Hashtable<IRNode, List<IRNode>> successors = new Hashtable<IRNode, List<IRNode>>();
	//LIVENESS
	private List<String> global_var = new ArrayList<String>();
	private List<String> global_string = new ArrayList<String>();
	private Hashtable<IRNode, Set<String>> generate = new Hashtable<IRNode, Set<String>>();
	private Hashtable<IRNode, Set<String>> kill = new Hashtable<IRNode, Set<String>>();
	private Hashtable<IRNode, Set<String>> in = new Hashtable<IRNode, Set<String>>();
	private Hashtable<IRNode, Set<String>> out = new Hashtable<IRNode, Set<String>>();
	private List<Integer> leader = new ArrayList<Integer>();
	//REG ALLOC
	private String[] register4 = new String[4];
	private boolean[] dirty4 = new boolean[4];
	private Set<String> current_live_out;
	private int index_track = 0;
	private Hashtable<String, int[]>LP;
	private String current_func;
	
	public Optimizer(List<IRNode> irList, Hashtable<String, int[]> LP){
		this.irList = irList;
		this.LP = LP;
		setupCFG();
		setupGen_Kill();
		setupIn_Out();
		initialize_registers();
		setupRegAlloc();
	}
 	
	private void setupCFG(){
		for(int i = 0; i < irList.size(); i++){
			IRNode ir = irList.get(i);
			//Loop fusion
			if(ir.op == "VAR"){
				global_var.add(ir.first);
			}else if(ir.op == "STRING"){
				global_string.add(ir.first);
			}
			//Initialize the list
			List<IRNode> slist = new ArrayList<IRNode>();
			List<IRNode> plist = new ArrayList<IRNode>();
			predecessors.put(ir, plist);
			//start
			//return has 0 successor
			if(ir.op == "RET"){
				successors.put(ir, slist);
				continue;
			}
			//jump has 1 successor -- the target
			if(ir.op == "JUMP"){
				for(IRNode toNode : irList){
					if(toNode.op == "LABEL" && toNode.dest == ir.dest){
						slist.add(toNode);
						break;
					}
				}
				successors.put(ir, slist);
				continue;
			}
			//condition has 2 successors -- first the target
			if(ir.op == "LE" || ir.op == "GE" || ir.op == "NE" || 
					ir.op == "GT" || ir.op == "LT" || ir.op == "EQ"){
				for(IRNode toNode : irList){
					if(toNode.op == "LABEL" && toNode.dest == ir.dest){
						slist.add(toNode);
						break;
					}
				}
			}
			//Normal
			if(i+1 != irList.size()){
				slist.add(irList.get(i+1));
			}
			successors.put(ir, slist);
		}
		//get slist 
		//loop through slist
		//for each list member, see if it exists already in the predecessor hashtable
		//if it does, append the plist for that key
		//else add the key and value to the p hashtable
		
		
		for(int i = 0; i < irList.size(); i++){
			IRNode ir = irList.get(i);
			//get slist
			List<IRNode> slist = successors.get(ir);
			//loop through slist
			for(IRNode toNode : slist){
				//Initialize the list
				List<IRNode> plist = predecessors.get(toNode);
				if(!plist.contains(ir)){
					plist.add(ir);
				}
				predecessors.put(toNode, plist);
			}
		}
	}
	private void setupGen_Kill(){
		for(IRNode ir : irList){
			String op = ir.op;
			String first = ir.first;
			String second = ir.second;
			String dest = ir.dest;
			//Kill (defined)
			Set<String> kset = new HashSet<String>();
			//Gen (used)
			Set<String> gset = new HashSet<String>();
			
			//Push use the reg/var
			if(op == "PUSH"){
				if(dest != null){
					gset.add(dest);
				}
				generate.put(ir, gset);
				kill.put(ir, kset);
				continue;
			}
			//Pop define the reg/var
			if(op == "POP"){
				if(dest != null){
					kset.add(dest);
				}
				generate.put(ir, gset);
				kill.put(ir, kset);
				continue;
			}
			//Write use the reg/var
			if(op.contains("WRITE")){
				gset.add(dest);
				generate.put(ir, gset);
				kill.put(ir, kset);
				continue;
			}
			//Read define the reg/var
			if(op.contains("READ")){
				kset.add(dest);
				generate.put(ir, gset);
				kill.put(ir, kset);
				continue;
			}
			//Other dumb labels
			if(op == "NEWLINE" || op == "VAR" || op == "STRING" || op == "EOGD"){
				generate.put(ir, gset);
				kill.put(ir, kset);
				continue;
			}
			if(op == "JUMP" || op == "LABEL" || op == "LINK"){
				generate.put(ir, gset);
				kill.put(ir, kset);
				continue;
			}
			//Branches
			if(op == "LE" || op == "GE" || op == "NE" || op == "GT" || op == "LT" || op == "EQ"){
				gset.add(first);
				gset.add(second);
				generate.put(ir, gset);
				kill.put(ir, kset);
				continue;
			}
			//Function call
			if(op == "JSR"){
				for(String v : global_var){
					gset.add(v);
				}
				generate.put(ir, gset);
				kill.put(ir, kset);
				continue;
			}
			//Store a number
			if(op.contains("STORE")){
				if(!(first.matches("[0-9]+") || first.matches("[0-9]*\\.[0-9]+"))
						&& first != null){
					gset.add(first);
				}
				if(dest != null){
					kset.add(dest);
				}
				generate.put(ir, gset);
				kill.put(ir, kset);
				continue;
			}
			//Normal
			if(first != null){
				gset.add(first);
			}
			if(second != null){
				gset.add(second);
			}
			if(dest != null){
				kset.add(dest);
			}
			generate.put(ir, gset);
			kill.put(ir, kset);
		}
	}
	private void setupIn_Out(){
		setupLeader();
		//From now on, all for loop need ind
		List<Integer> worklist = new ArrayList<Integer>();
		for(int i = 0; i < irList.size(); i++){
			worklist.add(i);
		}
		while(worklist.isEmpty() == false){
			int current = worklist.remove(worklist.size() - 1);
			IRNode ir = irList.get(current);
			Set<String> inset_old = in.get(ir);
			Set<String> outset_old = out.get(ir);
			Set<String> use = generate.get(ir);
			Set<String> def = kill.get(ir);
			if(ir.op == "RET"){
				//No need to update the outset
				Set<String> inset = new HashSet<String>(use);
				Set<String> temp_out = new HashSet<String>(outset_old);
				for(String d : def){
					temp_out.remove(d);
				}
				for(String t : temp_out){
					inset.add(t);
				}
				in.put(ir, inset);
			}else{
				//Update Outset
				Set<String> outset = new HashSet<String>(outset_old);
				List<IRNode> its_successors = successors.get(ir);
				for(IRNode ir_suc : its_successors){
					Set<String> inset_suc = in.get(ir_suc);
					for(String i_suc : inset_suc){
						outset.add(i_suc);
					}
				}
				out.put(ir, outset);
				//Update inset
				Set<String> inset = new HashSet<String>(use);
				Set<String> temp_out = new HashSet<String>(outset);
				for(String d : def){
					temp_out.remove(d);
				}
				for(String t : temp_out){
					inset.add(t);
				}
				in.put(ir, inset);
				//Update Worklist
				if(inset.containsAll(inset_old) && inset_old.containsAll(inset)){
					continue;
				}else{
					//get index of predecessors
					for(IRNode p : predecessors.get(ir)){
						int ind = irList.indexOf(p);
						if(!worklist.contains(ind)){
							worklist.add(ind);
						}
					}
				}
			}
		}
		return;
	}
	private void setupLeader(){
		for(int ind = 0; ind < irList.size(); ind++){
			leader.add(0);
		}
		for(int ind = 0; ind < irList.size(); ind++){
			IRNode ir = irList.get(ind);
			//Initialize IN & OUT
			Set<String> inset = new HashSet<String>();
			Set<String> outset = new HashSet<String>();
			if(ir.op == "RET"){
				for(String g : global_var){
					//outset.add(g);
				}
			}
			in.put(ir, inset);
			out.put(ir, outset);
			//Setup Leader
			List<IRNode> irPred = predecessors.get(ir);
			if(irPred.isEmpty()){
				leader.set(ind, 1);
			}else{
				for(IRNode predIR : irPred){
					switch(predIR.op){
					case "JUMP":
					case "EQ":
					case "NE":
					case "LE":
					case "LT":
					case "GE":
					case "GT":
						leader.set(ind, 1);
						break;
					}
				}
			}
		}
	}
	private void setupRegAlloc(){
		for(IRNode ir : irList){
			String op = ir.op;
			String first = ir.first;
			String second = ir.second;
			String dest = ir.dest;
			index_track++;
			if(index_track < irList.size() && irList.get(index_track).op == "LINK"){
				current_func = dest;
			}
			//skip some dumb ir nodes
			if(op == "NEWLINE"){
				System.out.println(";-------------------------");
				continue;
			}
			if(op == "VAR"){
				TinyNode t = new TinyNode("var", first, null);
				t.showNode();
				continue;
			}
			if(op == "STRING"){
				TinyNode t = new TinyNode("str", first, dest);
				t.showNode();
				continue;
			}
			if(op == "EOGD"){
				List<TinyNode> tinyList = new ArrayList<TinyNode>();
				tinyList.add(new TinyNode("push", null, null));
				tinyList.add(new TinyNode("push", "r0", null));
				tinyList.add(new TinyNode("push", "r1", null));
				tinyList.add(new TinyNode("push", "r2", null));
				tinyList.add(new TinyNode("push", "r3", null));
				tinyList.add(new TinyNode("jsr", "main", null));
				tinyList.add(new TinyNode("sys halt", null, null));
				for(int j = 0; j < tinyList.size(); j++){
					tinyList.get(j).showNode();
				}
				continue;
			}
			//printout live analysis
			current_live_out = out.get(ir);
			ir.showNode_Liveness(current_live_out);
			if(op == "LINK"){
				int l_t = LP.get(current_func)[0] + LP.get(current_func)[2];
				TinyNode t = new TinyNode("link", Integer.toString(l_t), null);
				t.showNode();
				continue;
			}
			//Special case - branches
			if (op == "EQ" || op == "NE" || op == "LE" || op == "LT" || op == "GE" || op == "GT") {
				int rg1 = ensure(first);
				String r1 = "r" + Integer.toString(rg1);
				int rg2 = ensure(second);
				String r2 = "r" + Integer.toString(rg2);
				
				String typi = ir.type;
				if(typi.contains("FLOAT")){
					TinyNode c = new TinyNode("cmpr", r1, r2);
					c.showNode();
				}else{
					TinyNode c = new TinyNode("cmpi", r1, r2);
					c.showNode();
				}
				if(!current_live_out.contains(register4[rg1])){
					free(rg1);
				}
				if(!current_live_out.contains(register4[rg2])){
					free(rg2);
				}
			}
			//If it this the end of the block, spill_bb()
			if(index_track >= leader.size()){
				//Nah
			}else if(op != "LE" && op != "LT" && op != "GE" 
				&& op != "GT" && op != "NE" && op != "EQ" && op != "JUMP"){
				//Do it later
			}else if(leader.get(index_track) == 1 && op != "RET"){
				spill_bb();
			}
			
			//ensure the used registers
			if(op == "JSR"){
				List<TinyNode> tinyList = new ArrayList<TinyNode>();
				tinyList.add(new TinyNode("push", "r0", null));
				tinyList.add(new TinyNode("push", "r1", null));
				tinyList.add(new TinyNode("push", "r2", null));
				tinyList.add(new TinyNode("push", "r3", null));
				tinyList.add(new TinyNode("jsr", dest, null));
				tinyList.add(new TinyNode("pop", "r3", null));
				tinyList.add(new TinyNode("pop", "r2", null));
				tinyList.add(new TinyNode("pop", "r1", null));
				tinyList.add(new TinyNode("pop", "r0", null));
				for(int j = 0; j < tinyList.size(); j++){
					tinyList.get(j).showNode();
				}
				continue;
			}
			if(op == "LABEL"){
				TinyNode t = new TinyNode("label", dest, null);
				t.showNode();
				continue;
			}
			if((op == "PUSH") && (first == null) && (second == null) && (dest == null)){
				TinyNode t = new TinyNode("push", first, dest);
				t.showNode();
				continue;
			}
			if((op == "POP") && (first == null) && (second == null) && (dest == null)){
				TinyNode t = new TinyNode("pop", first, dest);
				t.showNode();
				continue;
			}
			
			//ALU Operation
			if(op == "ADDI" || op == "ADDF" || op == "SUBI" || op == "SUBF" 
				|| op == "MULTI" || op == "MULTF" || op == "DIVI" || op == "DIVF"){
				int rg1 = ensure(first);
				int rg2 = ensure(second);
				
				String r1 = "r" + Integer.toString(rg1);
				String r2 = "r" + Integer.toString(rg2);
				boolean isdead1 = current_live_out.contains(first);
				boolean isdead2 = current_live_out.contains(second);
				
				int rs = rg1;
				int rd = rg2;
				
				//Make rs's reg our dest reg
				System.out.print(";Switching owner of register " + r1 + " to " + dest + " ");
				register4status();
				if(dirty4[rg1]){
					System.out.println(";Spilling variable: " + first);
					String mem_loc = reg_to_mem(first);
					System.out.println("move r" + Integer.toString(rg1) + " " + mem_loc);
				}
				dirty4[rg1] = false;
				register4[rg1] = dest;
				rd = rg1;
				rs = rg2;
				
				String rdest = "r" + Integer.toString(rd);
				String rsource = "r" + Integer.toString(rs);
				dirty4[rd] = true;
				
				if(op == "ADDI"){
					TinyNode t = new TinyNode("addi", rsource , rdest);
					t.showNode();
				} else if ( op == "ADDF"){
					TinyNode t = new TinyNode("addr", rsource , rdest);
					t.showNode();
				} else if ( op == "SUBI"){
					TinyNode t = new TinyNode("subi", rsource , rdest);
					t.showNode();
				} else if ( op == "SUBF"){
					TinyNode t = new TinyNode("subr", rsource , rdest);
					t.showNode();
				} else if ( op == "MULTI"){
					TinyNode t = new TinyNode("muli", rsource , rdest);
					t.showNode();
				} else if ( op == "MULTF"){
					TinyNode t = new TinyNode("mulr", rsource , rdest);
					t.showNode();
				} else if ( op == "DIVI"){
					TinyNode t = new TinyNode("divi", rsource , rdest);
					t.showNode();
				} else if ( op == "DIVF"){
					TinyNode t = new TinyNode("divr", rsource , rdest);
					t.showNode();
				}
				
				//free things
				if(!current_live_out.contains(register4[rs])){
					free(rs);
				}
				if(!current_live_out.contains(register4[rd])){
					free(rd);
				}
			}
			//Store
			else if (op == "STOREI" || op == "STOREF"){
				if(first.matches("[0-9]+") || first.matches("[0-9]*\\.[0-9]+")){
					int rd = ensure(dest);
					dirty4[rd] = true;
					String rdest = "r" + Integer.toString(rd);
					
					TinyNode t = new TinyNode("move", first, rdest);
					t.showNode();
					if(!current_live_out.contains(register4[rd])){
						free(rd);
					}
				}else{
					if(dest != "$R"){
						int rs = ensure(first);
						int rd = ensure(dest);
						dirty4[rd] = true;
						String rsource = "r" + Integer.toString(rs);
						String rdest = "r" + Integer.toString(rd);
						
						TinyNode t = new TinyNode("move", rsource, rdest);
						t.showNode();
						if(!current_live_out.contains(register4[rs])){
							free(rs);
						}
						if(!current_live_out.contains(register4[rd])){
							free(rd);
						}
					}else{
						int rs = ensure(first);
						String rsource = "r" + Integer.toString(rs);
						String rdest = reg_to_mem(dest);
						
						TinyNode t = new TinyNode("move", rsource, rdest);
						t.showNode();
						if(!current_live_out.contains(register4[rs])){
							free(rs);
						}
					}
				}
			}
			//Read & Write
			else if (op == "READI" || op == "READF"){
				int rd = ensure(dest);
				String r = "r" + Integer.toString(rd);
				dirty4[rd] = true;
				if (op == "READI"){
					TinyNode t = new TinyNode("sys readi", r, null);
					t.showNode();
				} else if (op == "READF"){
					TinyNode t = new TinyNode("sys readr", r, null);
					t.showNode();
				}
				if(!current_live_out.contains(register4[rd])){
					free(rd);
				}
			} else if (op == "WRITEI" || op == "WRITEF"){
				int rg1 = ensure(dest);
				String r = "r" + Integer.toString(rg1);
				if (op == "WRITEI"){
					TinyNode t = new TinyNode("sys writei", r, null);
					t.showNode();
				} else if (op == "WRITEF"){
					TinyNode t = new TinyNode("sys writer", r, null);
					t.showNode();
				}
				if(!current_live_out.contains(register4[rg1])){
					free(rg1);
				}
			} else if (op == "WRITES"){
				TinyNode t = new TinyNode("sys writes", dest, null);
				t.showNode();
			}
			//Branches
			else if (op == "EQ" || op == "NE" || op == "LE" || op == "LT" || op == "GE" || op == "GT") {
				String tinyop = "j" + op.toLowerCase();
				TinyNode t = new TinyNode(tinyop, dest, null);
				t.showNode();
			} 
			//Push & Pop
			else if (op == "POP"){
				int rd = ensure(dest);
				String r = "r" + Integer.toString(rd);
				dirty4[rd] = true;
				TinyNode t = new TinyNode("pop", r, null);
				t.showNode();
				if(!current_live_out.contains(register4[rd])){
					free(rd);
				}
			} else if (op == "PUSH"){
				int rg1 = ensure(dest);
				String r = "r" + Integer.toString(rg1);
				
				TinyNode t = new TinyNode("push", r, null);
				t.showNode();
				if(!current_live_out.contains(register4[rg1])){
					free(rg1);
				}
			}
			//Return, Jump & Link
			else if (op == "RET"){
				spill_bb();
				List<TinyNode> tinyList = new ArrayList<TinyNode>();
				tinyList.add(new TinyNode("unlnk", null, null));
				tinyList.add(new TinyNode("ret", null, null));
				for(int j = 0; j < tinyList.size(); j++){
					tinyList.get(j).showNode();
				}
			} else if (op == "JUMP"){
				TinyNode t = new TinyNode("jmp", dest, null);
				t.showNode();
			}
			if(index_track >= leader.size()){
				//Nah
			}else if(op != "LE" && op != "LT" && op != "GE" 
				&& op != "GT" && op != "NE" && op != "EQ" && op != "JUMP"){
				if(leader.get(index_track) == 1 && op != "RET"){
					spill_bb();
				}
			}
		}
	}
	private void initialize_registers(){
		register4[0] = null;
		register4[1] = null;
		register4[2] = null;
		register4[3] = null;
		
		dirty4[0] = false;
		dirty4[1] = false;
		dirty4[2] = false;
		dirty4[3] = false;
	}
	private void register4status(){
		String r4 = "{";
		for(int i = 0; i < 4; i++){
			r4 += " r" + Integer.toString(i) + "->";
			if(register4[i] == null){
				r4 += "null";
			}else{
				r4 += register4[i];
			}
		}
		r4 += " }";
		/*r4 += dirty4[0]? "1":"0";
		r4 += dirty4[1]? "1":"0";
		r4 += dirty4[2]? "1":"0";
		r4 += dirty4[3]? "1":"0";*/
		System.out.println(r4);
	}
	private String reg_to_mem(String old){
		if(old == null){
			return old;
		}
		 
		int lnum = LP.get(current_func)[0];
		int pnum = LP.get(current_func)[1];
		//$L, $R, $T, $P
		//$Ll = $-l
		//$Tt = $-(l+t)
		//$Pp = $(6 + p - p)
		//$P1 = $(6 + p - 1)
		//$R = $(6 + p)
		String pattern_t = "\\$T([1-9]+[0-9]*)";
		Pattern rt = Pattern.compile(pattern_t);
		Matcher mt = rt.matcher(old);
		if(mt.find()){
			int regNum = Integer.parseInt(mt.group(1));
			return "$-" + Integer.toString(lnum + regNum);
		}
		
		if(old == "$R"){
			return "$" + Integer.toString(6 + pnum);
		}
		
		String pattern_l = "\\$L([1-9]+[0-9]*)";
		Pattern rl = Pattern.compile(pattern_l);
		Matcher ml = rl.matcher(old);
		if(ml.find()){
			int regNum = Integer.parseInt(ml.group(1));
			return "$-" + Integer.toString(regNum);
		}
		
		String pattern_p = "\\$P([1-9]+[0-9]*)";
		Pattern rp = Pattern.compile(pattern_p);
		Matcher mp = rp.matcher(old);
		if(mp.find()){
			int regNum = Integer.parseInt(mp.group(1));
			return "$" + Integer.toString(6 + pnum - regNum);
		}
		
		return old;
	}
	//four functions, ensure(), free(), allocate(), choose();
	private int ensure(String opr){
		String start = ";ensure(): " + opr;
		//if register4 has it
		for(int i = 3; i >= 0; i--){
			if(opr.equals(register4[i])){
				String reg_in_use = "r" + Integer.toString(i);
				System.out.print(start + " has register " + reg_in_use);
				register4status();
				return i;
			}
		}
		//if not
		int r_load = allocate(opr);
		String reg_to_use = "r" + Integer.toString(r_load);
		System.out.print(start + " gets register " + reg_to_use);
		register4status();
		//generate load
		if(!opr.contains("$T")){
			System.out.println(";loading " + opr + " to register " + reg_to_use);
			System.out.println("move " + reg_to_mem(opr) + " " + reg_to_use);
		}
		return r_load;
	}
	private void free(int i){
		System.out.println(";Freeing unused variable " + register4[i]);
		if(dirty4[i]){
			//generate store
			System.out.println(";Spilling variable: " + register4[i]);
			String mem_loc = reg_to_mem(register4[i]);
			System.out.println("move r" + Integer.toString(i) + " " + mem_loc);
		}
		register4[i] = null;
		dirty4[i] = false;
	}
	private int allocate(String opr){
		for(int i = 3; i >= 0; i--){
			if(register4[i] == null){
				register4[i] = opr;
				return i;
			}
		}
		int dest = choose();
		System.out.println(";allocate() has to spill " + register4[dest]);
		free(dest);
		register4[dest] = opr;
		return dest;
	}
	private int choose(){
		IRNode ir = irList.get(index_track - 1);
		for(int i = 0; i < 4; i++){
			String x = register4[i];
			if(ir.first != null && ir.first.equals(x)){
				continue;
			}
			if(ir.second != null && ir.second.equals(x)){
				continue;
			}
			if(ir.dest != null && ir.dest.equals(x)){
				continue;
			}
			return i;
		}
		return 3;
	}
	private void spill_bb(){
		System.out.println(";Spilling registers at the end of the Basic Block");
		for(int i = 3; i >= 0; i--){
			if(register4[i] != null){
				System.out.println(";Spilling variable: " + register4[i]);
				String mem_loc = reg_to_mem(register4[i]);
				System.out.println("move r" + Integer.toString(i) + " " + mem_loc);
				register4[i] = null;
				dirty4[i] = false;
			}
		}
	}
}//end of class