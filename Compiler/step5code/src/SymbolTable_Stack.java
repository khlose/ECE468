import org.antlr.v4.runtime.*;
import java.io.IOException;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Hashtable;

public class SymbolTable_Stack{
	//Declare a stack
	private static Stack<SymbolTable> ST_Stack = new Stack<SymbolTable>();
	private static Stack<SymbolTable> ST_Final = new Stack<SymbolTable>();
	private static int block_num = 1;
	
	//3 types: GLOBAL, FUNC, BLOCK
	public static void create_global(){
		ST_Stack.push(new SymbolTable("GLOBAL"));
	}
	
	public static void create_func(String function_name){
		ST_Stack.push(new SymbolTable(function_name));
	}
	
	public static void create_block(){
		String block_name = "BLOCK " + block_num;
		block_num += 1;
		ST_Stack.push(new SymbolTable(block_name));
	}
	
	//Insert Symbols - overloading
	public static void insert(String var_name, String var_type){
		//id list
		String[] var_list = var_name.trim().split(",");
		//get the top
		SymbolTable temp = ST_Stack.pop();
		//write into the top
		for(int i = 0; i < var_list.length; i++){
			//create symbol
			Symbol sym = new Symbol(var_list[i], var_type, "");
			//check duplicated
			check_dup(sym.name, temp);
			//if pass, add to list & hashtable
			temp.symbol_list.add(sym);
			temp.duplicate_checking.put(sym.name, 1);
		}
		//push it back
		ST_Stack.push(temp);
	}
	
	public static void insert(String string_name, String string_type, String string_val){
		//get string
		Symbol sym = new Symbol(string_name, string_type, string_val);
		//get the top
		SymbolTable temp = ST_Stack.pop();
		//check duplicated
		check_dup(sym.name, temp);
		//if pass, add to list & hashtable
		temp.symbol_list.add(sym);
		temp.duplicate_checking.put(sym.name, 1);
		//push it back
		ST_Stack.push(temp);
	}
	
	private static void check_dup(String name, SymbolTable sym){
		//if hashtable has that, print error, and exit the program
		if (sym.duplicate_checking.containsKey(name)){
			System.out.println("DECLARATION ERROR " + name);
			System.exit(0);
		}
	}
	//Print out
	public static void print_table(){
		while (!ST_Final.empty()){
			//get the peek
			SymbolTable top = ST_Final.peek();
			
			//print table
			System.out.println("Symbol table " + top.name);
		
			//print symbols
			int i = 0;
			Symbol s;
			for(; i < top.symbol_list.size(); i++){
				s = top.symbol_list.get(i);
				if(s.type == "STRING"){
					System.out.println("name "+ s.name + " type " + s.type + " value " + s.value);
					continue;
				}
				System.out.println("name "+ s.name + " type " + s.type);
			}
			
			//pop it
			ST_Final.pop();
			if (!ST_Final.empty()) System.out.println();
		}
	}
	
	//Pop everything after done
	public static void pop_it(){
		while(!ST_Stack.empty()) ST_Final.push(ST_Stack.pop());
	}
}

class SymbolTable{
	public String name;
	public Hashtable<String, Integer> duplicate_checking = new Hashtable<String, Integer>();
	public ArrayList<Symbol> symbol_list = new ArrayList<Symbol>();
	
	public SymbolTable(String name){
		this.name = name;
	}
}

class Symbol{
	public String name, type, value;
	
	public Symbol(String name, String type, String value){
		this.name = name;
		this.type = type;
		this.value = value;
	}
}