import org.antlr.v4.runtime.*;
import java.io.IOException;

class Micro  {
	
	public static void main(String args[]){
		//create lexer object with the stream input
		try{
			MicroLexer lexi = new MicroLexer(new ANTLRFileStream(args[0]));
			//read this stream
			//while it is not the EOF, get the token name, print the statement
			Token t = lexi.nextToken();
			while(t.getType() != Token.EOF){
				printType(t.getType());
				printValue(t.getText());
				t = lexi.nextToken();
			}
			
		}catch(IOException err){
			System.out.println("Exception Caught.");
		}
	}

	private static void printType(int tokenValue){
		//skip ws & comment
		if(tokenValue == MicroLexer.WS){
			return;
		}else if(tokenValue == MicroLexer.COMMENT){
			return;
		}
		//do the real print
		if(tokenValue == MicroLexer.KEYWORD){
			System.out.println("Token Type: KEYWORD");
		}else if(tokenValue == MicroLexer.INTLITERAL){
			System.out.println("Token Type: INTLITERAL");
		}else if(tokenValue == MicroLexer.FLOATLITERAL){
			System.out.println("Token Type: FLOATLITERAL");
		}else if(tokenValue == MicroLexer.IDENTIFIER){
			System.out.println("Token Type: IDENTIFIER");
		}else if (tokenValue == MicroLexer.OPERATOR){
			System.out.println("Token Type: OPERATOR");
		}else if (tokenValue == MicroLexer.STRINGLITERAL){
			System.out.println("Token Type: STRINGLITERAL");
		}
		return;
	}
	
	private static void printValue(String val){	
		System.out.println("Value: " + val);
	}

}