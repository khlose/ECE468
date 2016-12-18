import java.util.*;
public class IRNode{
	public String op, first, second, dest, type;
	
	public IRNode (String op, String first, String second, String dest, String type){
		this.op = op;
		this.first = first;
		this.second = second;
		this.dest = dest;
		this.type = type;
	}
	
	public void showNode(){
		if(op == "NEWLINE"){
			System.out.println("");
			return;
		}
		if(op == "VAR" || op == "STRING" || op == "EOGD"){
			return;
		}
		if((op == "PUSH" || op == "POP") && (first == null) && (second == null) && (dest == null)){
			System.out.println(";" + op + " ");
			return;
		}
		String part_first = "";
		String part_second = "";
		String part_dest = "";
		if (first != null){
			part_first = " " + first;
		}
		if (second != null){
			part_second = " " + second;
		}
		if (dest != null){
			part_dest = " " + dest;
		}
		System.out.println(";" + op + part_first + part_second + part_dest);
	}
	
	public void showNode_Liveness(Set<String> LiveVar){
		if(op == "NEWLINE"){
			System.out.println("");
			return;
		}
		if(op == "VAR" || op == "STRING" || op == "EOGD"){
			return;
		}
		if((op == "PUSH" || op == "POP") && (first == null) && (second == null) && (dest == null)){
			System.out.print(";" + op + " ");
			System.out.print("\t");
			System.out.print("live vars: ");
			for(String var : LiveVar){
				System.out.print(var+", ");
			}
			System.out.print("\n");
			return;
		}
		String part_first = "";
		String part_second = "";
		String part_dest = "";
		if (first != null){
			part_first = " " + first;
		}
		if (second != null){
			part_second = " " + second;
		}
		if (dest != null){
			part_dest = " " + dest;
		}
		System.out.print(";" + op + part_first + part_second + part_dest);
		System.out.print("\t");
		System.out.print("live vars: ");
		for(String var : LiveVar){
			System.out.print(var+", ");
		}
		System.out.print("\n");
	}
}