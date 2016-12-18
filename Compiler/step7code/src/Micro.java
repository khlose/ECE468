import org.antlr.v4.runtime.*;
import java.io.IOException;
import org.antlr.v4.runtime.tree.*;

class Micro  {
	public static void main(String args[]){
		try{
			MicroLexer lexi = new MicroLexer(new ANTLRFileStream(args[0]));
			CommonTokenStream toki = new CommonTokenStream(lexi);
			MicroParser parsi = new MicroParser(toki);
			ANTLRErrorStrategy erri = new CustomErrorStrategy();
			parsi.setErrorHandler(erri);
			ParseTree r = parsi.program();
			
			//Step4
			ParseTreeWalker master = new ParseTreeWalker();
			MicroWalker cats = new MicroWalker();
			System.out.println(";IR code");
			master.walk(cats, r);
		}catch(Exception err){}
	}
}

class CustomErrorStrategy extends DefaultErrorStrategy {
	public void reportError (Parser recognizer, 
			RecognitionException e) throws RuntimeException{
		throw new RuntimeException();
	}
}
