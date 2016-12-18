public class IRNode{
	public String op, first, second, dest;
	
	public IRNode (String op, String first, String second, String dest){
		this.op = op;
		this.first = first;
		this.second = second;
		this.dest = dest;
	}
	
	public void showNode(){
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