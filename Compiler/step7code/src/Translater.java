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
	private Optimizer ie335;
	//set up
	public Translater(List<IRNode> foreign, int regNumber, Hashtable<String, int[]>LP){
		this.irList = foreign;
		this.regNumber = regNumber;
		this.LP = LP;
		this.ie335 = new Optimizer(foreign, LP);
	}
}