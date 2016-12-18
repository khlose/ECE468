import org.antlr.v4.runtime.*;
import java.io.IOException;

class Micro  {
	//Naming system
	//As you will see, we are naming them with a long ee sound
	//This gives our program a feminine feel
	//Hope you will love it
	public static void main(String args[]){
		try{
			//This throws IOExceptions
			MicroLexer lexi = new MicroLexer(new ANTLRFileStream(args[0]));
			//Get a list of matched tokens
			CommonTokenStream toki = new CommonTokenStream(lexi);
			//Pass the tokens to the parser
			MicroParser parsi = new MicroParser(toki);
			//Set error handler
			ANTLRErrorStrategy erri = new CustomErrorStrategy();
			parsi.setErrorHandler(erri);
			//run the parser
			parsi.program();
			//if no exceptions, then accepted
			System.out.println("Accepted");
		}catch(Exception err){
			System.out.println("Not Accepted");
		}
	}
}

class CustomErrorStrategy extends DefaultErrorStrategy {

	public void reportError (Parser recognizer, 
			RecognitionException e) throws RuntimeException{
		throw new RuntimeException();
	}
}
