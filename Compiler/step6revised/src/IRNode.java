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
}