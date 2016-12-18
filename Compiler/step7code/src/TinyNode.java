public class TinyNode{
	public String op, first, second;
	
	public TinyNode (String op, String first, String second){
		this.op = op;
		this.first = first;
		this.second = second;
	}
	
	public void showNode(){
		String part_first = "";
		String part_second = "";
		if (first != null){
			part_first = " " + first;
		}
		if (second != null){
			part_second = " " + second;
		}
		System.out.println(op + part_first + part_second);
	}
}