package CocoRFiles.Program;

public class Parser {
	public static final int _EOF = 0;
	public static final int _Name = 1;
	public static final int _CharData = 2;
	public static final int _Comment = 3;
	public static final int _XmlStart = 4;
	public static final int _XmlEnd = 5;
	public static final int _Eq = 6;
	public static final int _Less = 7;
	public static final int _Slash = 8;
	public static final int _More = 9;
	public static final int _Quote = 10;
	public static final int _DoubleQuote = 11;
	public static final int _AND = 12;
	public static final int maxT = 13;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;

	

	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}

	boolean isEmptyElement() {
		Token x = la; //The look ahead token, the next unread token.
		boolean bFoundSlash = false;
		while (x.kind != _More) {
			if ( x.kind == _Slash ) {
				bFoundSlash = true;
			}
			x = scanner.Peek();
		}
		return bFoundSlash;
	}

	boolean anotherIteration() {
		if ( la.kind == _Less ) {
			Token next = scanner.Peek();
			return next.kind != _Slash;
		}
		return la.kind == _Comment || la.kind == _CharData || la.kind == _Name || la.kind == _More || la.kind == _Quote || la.kind == _DoubleQuote || la.kind == _AND;
	}

	void Xml() {
		Prolog();
		Element();
		while (la.kind == 3) {
			Get();
		}
	}

	void Prolog() {
		Expect(4);
		Attribute();
		if (la.kind == 1) {
			Attribute();
		}
		Expect(5);
	}

	void Element() {
		if (isEmptyElement() ) {
			EmptyElementTag();
		} else if (la.kind == 7) {
			STag();
			Content();
			ETag();
		} else SynErr(14);
	}

	void Attribute() {
		Expect(1);
		Expect(6);
		if (la.kind == 10) {
			Get();
			while (la.kind == 1 || la.kind == 2) {
				if (la.kind == 2) {
					Get();
				} else {
					Get();
				}
			}
			Expect(10);
		} else if (la.kind == 11) {
			Get();
			while (la.kind == 1 || la.kind == 2) {
				if (la.kind == 2) {
					Get();
				} else {
					Get();
				}
			}
			Expect(11);
		} else SynErr(15);
	}

	void EmptyElementTag() {
		Expect(7);
		Expect(1);
		while (la.kind == 1) {
			Attribute();
		}
		Expect(8);
		Expect(9);
	}

	void STag() {
		Expect(7);
		Expect(1);
		while (la.kind == 1) {
			Attribute();
		}
		Expect(9);
	}

	void Content() {
		while (anotherIteration() ) {
			if (la.kind == 7) {
				Element();
			} else if (la.kind == 3) {
				Get();
			} else if (StartOf(1)) {
				switch (la.kind) {
				case 2: {
					Get();
					break;
				}
				case 1: {
					Get();
					break;
				}
				case 9: {
					Get();
					break;
				}
				case 10: {
					Get();
					break;
				}
				case 11: {
					Get();
					break;
				}
				case 12: {
					Get();
					break;
				}
				}
			} else SynErr(16);
		}
	}

	void ETag() {
		Expect(7);
		Expect(8);
		Expect(1);
		Expect(9);
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		Xml();
		Expect(0);

	}

	private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x},
		{_x,_T,_T,_x, _x,_x,_x,_x, _x,_T,_T,_T, _T,_x,_x}

	};
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "Name expected"; break;
			case 2: s = "CharData expected"; break;
			case 3: s = "Comment expected"; break;
			case 4: s = "XmlStart expected"; break;
			case 5: s = "XmlEnd expected"; break;
			case 6: s = "Eq expected"; break;
			case 7: s = "Less expected"; break;
			case 8: s = "Slash expected"; break;
			case 9: s = "More expected"; break;
			case 10: s = "Quote expected"; break;
			case 11: s = "DoubleQuote expected"; break;
			case 12: s = "AND expected"; break;
			case 13: s = "??? expected"; break;
			case 14: s = "invalid Element"; break;
			case 15: s = "invalid Attribute"; break;
			case 16: s = "invalid Content"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
