package parser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.RuntimeErrorException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.BufferedTokenStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;

import br.com.porcelli.parser.plsql.PLSQLLexer;
import br.com.porcelli.parser.plsql.PLSQLParser;
import br.com.porcelli.parser.plsql.PLSQLParser.sql_script_return;
import br.com.porcelli.parser.plsql.PLSQLParser_PLSQL_DMLParser.*;
import br.com.porcelli.parser.plsql.PLSQLParser_PLSQLCommons.*;
import br.com.porcelli.parser.plsql.PLSQLParser_PLSQLKeys.new_key_return;
import br.com.porcelli.parser.plsql.PLSQLParser.*;

public class ParserMain {
	public static void main(String[] args) throws Exception {
		boolean isParseAllPackges = "--all-packages".equals(args[0]);
		if (isParseAllPackges) {
			String path = args[1];
			parseByParts(path);
			return;
		}
		
		String path = args[0];
		String inputContent = new String(Files.readAllBytes(Paths.get(path)), Charset.forName("UTF-8"));
		ParseResult parseResult = parseTreeFromString(inputContent, false);
		org.antlr.runtime.tree.Tree theTree = parseResult.tree;
		String str;
		str = (theTree).toStringTree();
		str = prettyPrint(theTree);

		System.out.println(str.length() > 400 ? str.substring(0, 400) + "..." : str);
		try (PrintStream out = new PrintStream(new FileOutputStream("workdir/output.txt"))) {
		    out.print(str);
		}
		
		/*
		TokenCounter ctr = new TokenCounter();
		ctr.addTree(theTree);
		printTokenStats(ctr.getOccurences());
		*/
		
		//StringBuilder sb = new StringBuilder();
		//SqlPrinter printer = new SqlPrinter(sb);
		//printer.visitNode(theTree);
		//System.out.println(sb);
		
		PrintResult printResult = printTreeToString(theTree);
		String printed = printResult.text;

		System.out.println(printed.length() > 400 ? printed.substring(0, 400) + "..." : printed);
		try (PrintStream out = new PrintStream(new FileOutputStream("workdir/output_printed.txt"))) {
		    out.print(printed);
		}
		
		//String errorMessage = validatePrintedTreeMatchesParsedTree(inputContent);
	}
	
	static class PrintResult {
		public List<RecognitionException> printErrors;
		public String text;
	}

	private static PrintResult printTreeToString(org.antlr.runtime.tree.Tree theTree)
			throws IOException, RecognitionException {
		DerivedSqlPrinter printer = new DerivedSqlPrinter(new CommonTreeNodeStream(theTree));
		
		try (InputStream templateInputStream = ParserMain.class.getClassLoader().getResourceAsStream("parser/PLSQLPrinterTemplates.stg")) {
			StringTemplateGroup templateGroup = new StringTemplateGroup(new InputStreamReader(templateInputStream, Charset.forName("UTF-8")), AngleBracketTemplateLexer.class);
			printer.setTemplateLib(templateGroup);
		}
		String printed = printer.sql_script().st.toString();
		PrintResult result = new PrintResult();
		result.printErrors = printer.errors;
		result.text = printed;
		return result;
	}
	
	static class ParseResult {
		public List<RecognitionException> lexerErrors;
		public List<RecognitionException> parserErrors;
		public Tree tree;
	}

	private static ParseResult parseTreeFromString(String inputContent, boolean printTokens) throws RecognitionException {
		ANTLRStringStream input = new ANTLRStringStream(inputContent);
		DerivedSqlLexer l = new DerivedSqlLexer(input);
		CommonTokenStream cts = new CommonTokenStream(l);
		
		if (printTokens)
		{
			cts.fill();
			List<? extends Token> tokens = cts.getTokens();
			DerivedSqlParser p = new DerivedSqlParser(cts);
			String[] tokenNames = p.getTokenNames();
			for (Token t: tokens) {
				int type = t.getType();
				if (type != Token.EOF && t.getChannel() != Token.HIDDEN_CHANNEL) {
					String s = tokenNames[type];
					String tokenText = t.getText();
					System.out.printf("%s '%s' %d\n", s, tokenText, t.getChannel());
				}
			}
			System.exit(0);
		}
		DerivedSqlParser p = new DerivedSqlParser(cts);
		Object tree = p.sql_script().getTree();
	
		org.antlr.runtime.tree.Tree theTree = (org.antlr.runtime.tree.Tree)tree;
		ParseResult result = new ParseResult();
		result.tree = theTree;
		result.lexerErrors = l.errors;
		result.parserErrors = p.errors;
		return result;
	}

	private static void parseByParts(String path) throws Exception {
		byte[] contentBytes = Files.readAllBytes(Paths.get(path));
		String contentString = new String(contentBytes, Charset.forName("UTF-8"));
		List<String> parts = splitContent(contentString);
		List<String> parseFailures = new ArrayList<String>();
		List<String> parseFailureBodies = new ArrayList<String>();
		List<String> printFailures = new ArrayList<String>();
		List<String> printFailureBodies = new ArrayList<String>();
		List<String> successes = new ArrayList<String>();
		TokenCounter ctr = new TokenCounter();
		long ms_start_all = System.currentTimeMillis();
		//int times = 0;
		for (String part : parts) {
			//if (times > 100) {
			//	break;
			//}
			//++times;
			String header = part.substring(0, part.indexOf('\n'));
			System.out.print(header);

			long ms_start_1 = System.currentTimeMillis();
			ParseResult parseResult = parseTreeFromString(part, false);
			String printedTree = "";
			long ms_end_1 = System.currentTimeMillis();
			System.out.printf(" %f s\n", (ms_end_1 - ms_start_1) / 1000.0);
			boolean failure = parseResult.lexerErrors.size() > 0 || parseResult.parserErrors.size() > 0;
			if (failure) {
				System.out.println("PARSE FAIL");
				parseFailures.add(header);
				parseFailureBodies.add(part);
				/*for (RecognitionException ex: p.errors) {
					System.out.println(ex.toString());
					System.out.println(p.getErrorHeader(ex) + ":" + p.getErrorMessage(ex, new String[0]));
				}*/
			} else {
				ctr.addTree(parseResult.tree);
				
				boolean is_tree_walked;
				try {
					DerivedSqlPrinter printer = new DerivedSqlPrinter(new CommonTreeNodeStream(parseResult.tree));
					try (InputStream templateInputStream = ParserMain.class.getClassLoader().getResourceAsStream("parser/PLSQLPrinterTemplates.stg")) {
						StringTemplateGroup templateGroup = new StringTemplateGroup(new InputStreamReader(templateInputStream, Charset.forName("UTF-8")), AngleBracketTemplateLexer.class);
						printer.setTemplateLib(templateGroup);
					}
					StringTemplate st = printer.sql_script().st;
					if (printer.errors.size() > 0) {
						is_tree_walked = false;
					} else {
						printedTree = st.toString();
						is_tree_walked = true;
					}
				} catch (Exception ex) {
					is_tree_walked = false;
					ex.printStackTrace();
				}
				if (is_tree_walked) {
					successes.add(header);
				} else {
					System.out.println("PRINT FAIL");
					printFailures.add(header);
					printFailureBodies.add(part);
				}
			}
			
			org.antlr.runtime.tree.Tree tree = parseResult.tree;
			String str;
			str = prettyPrint(tree);
			
			String name = tryGuessPackageName(tree);
			if (name == null) {
				name = "unguessed";
			}

			try (PrintStream out = new PrintStream(new FileOutputStream(String.format("workdir/parsetrees/%d_%s_input.txt", successes.size() - 1, name)))) {
			    out.print(part);
			}

			try (PrintStream out = new PrintStream(new FileOutputStream(String.format("workdir/parsetrees/%d_%s_%s.txt", successes.size() - 1, name, failure ? "failure" : "success")))) {
			    out.print(str);
			}

			try (PrintStream out = new PrintStream(new FileOutputStream(String.format("workdir/parsetrees/%d_%s_printed.txt", successes.size() - 1, name)))) {
			    out.print(printedTree);
			}
		}
		long ms_end_all = System.currentTimeMillis();
		System.out.printf("Total time: %f s\n", (ms_end_all - ms_start_all) / 1000.0);
		
		System.out.printf("%d succeeded, %d parse failed, %d print failed\n", successes.size(), parseFailures.size(), printFailures.size());
		int idx = 0;
		System.out.println("Parse failures:");
		for (int i = 0; i < parseFailures.size(); ++i) {
			System.out.printf("%d %s\n", idx, parseFailures.get(i));

			try (PrintStream out = new PrintStream(new FileOutputStream(String.format("workdir/failure%d.txt", idx)))) {
			    out.print(parseFailureBodies.get(i));
			}
			++idx;
		}
		try (PrintStream out = new PrintStream(new FileOutputStream("workdir/token_stats.txt"))) {
			printTokenStats(ctr.getOccurences(), out);
		}
		System.out.println("Print failures:");
		for (int i = 0; i < printFailures.size(); ++i) {
			System.out.printf("%d %s\n", idx, printFailures.get(i));

			try (PrintStream out = new PrintStream(new FileOutputStream(String.format("workdir/failure%d.txt", idx)))) {
			    out.print(printFailureBodies.get(i));
			}
			++idx;
		}
		try (PrintStream out = new PrintStream(new FileOutputStream("workdir/token_stats.txt"))) {
			printTokenStats(ctr.getOccurences(), out);
		}
	}

	private static String tryGuessPackageName(Tree tree) {
		if (tree.getType() == PLSQLParser.SQL_SCRIPT) {
			tree = tree.getChild(0);
		}
		if (tree.getType() == PLSQLParser.CREATE_PACKAGE_SPEC || tree.getType() == PLSQLParser.CREATE_PACKAGE_BODY) {
			for (int i = 0; i < tree.getChildCount(); ++i) {
				Tree childNode = tree.getChild(i);
				if (childNode.getType() == PLSQLParser.PACKAGE_NAME) {
					Tree nameNode = childNode.getChild(childNode.getChildCount() - 1);
					if (nameNode.getType() == PLSQLParser.ID) {
						String maybeQuotedName = nameNode.getText();
						if (maybeQuotedName.charAt(0) == '\"') {
							maybeQuotedName = maybeQuotedName.substring(1, maybeQuotedName.length() - 1);
						}
						if (tree.getType() == PLSQLParser.CREATE_PACKAGE_SPEC) {
							maybeQuotedName += "_spec";
						}
						return maybeQuotedName;
					}
				}
			}
		}
		return null;
	}

	private static List<String> splitContent(String contentString) {
		ArrayList<String> result = new ArrayList<String>();
		String pattern = "CREATE OR REPLACE";
		int idx = contentString.indexOf(pattern);
		while (idx < contentString.length() && idx >= 0) {
			int next = contentString.indexOf(pattern, idx + pattern.length());
			if (next == -1) {
				next = contentString.length();
			}
			String item = contentString.substring(idx, next);
			result.add(item);
			idx = next;
		}
		return result;
	}

	static String prettyPrint(org.antlr.runtime.tree.Tree tree) {
		StringBuilder sb = new StringBuilder();
		prettyPrint(tree, sb, 0);
		return sb.toString();
	}

	static void printIndent(StringBuilder sb, int indent) {
		for (int i = 0; i < indent; ++i) {
			sb.append("  ");
		}
	}
	
	private static void prettyPrint(org.antlr.runtime.tree.Tree tree, StringBuilder sb, int indent) {
		sb.append('(');
		String nodeText = prettyPrintNodeTag(tree);
		sb.append(nodeText);
		if (tree.getChildCount() == 1 && tree.getChild(0).getChildCount() == 0) {
			sb.append(" ");
			org.antlr.runtime.tree.Tree childNode = tree.getChild(0);
			String childNodeText = prettyPrintNodeTag(childNode);
			sb.append(childNodeText);
		} else {
			for (int i = 0; i < tree.getChildCount(); ++i) {
				org.antlr.runtime.tree.Tree childNode = tree.getChild(i);
				sb.append("\n");
				printIndent(sb, indent + 1);
				if (childNode.getChildCount() == 0) {
					String childNodeText = prettyPrintNodeTag(childNode);
					sb.append(childNodeText);
				} else {
					prettyPrint(childNode, sb, indent + 1);
				}
			}
		}
		sb.append(")");
	}
	
	private static String prettyPrintNodeTag(Tree tree) {
		int ttype = tree.getType();
		String tokenName = ttype >= 0 && ttype < tokenNames.length ? tokenNames[ttype] : "";
		String text = tree.getText();
		if (tokenName.equals(text)) {
			return tokenName;
		} else {
			//int ntype = Arrays.asList(PLSQLParser.tokenNames).indexOf(tree.getText());
			return String.format("%s[%s]", tokenName, text);
		}
	}

	String sql = "";
	
	static String[] tokenNames = getTokenNames();
	
	static String[] getTokenNames() {
		Field[] fields = PLSQLParser.class.getDeclaredFields();
		Map<Integer, String> tokenNamesMap = new HashMap<Integer, String>();
		int maxTokenValue = 0;
		for (Field field: fields) {
			int mod = field.getModifiers();
			if (Modifier.isStatic(mod) && Modifier.isFinal(mod) && field.getType() == int.class) {
				String name = field.getName();
				int value;
				try {
					value = field.getInt(null);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				tokenNamesMap.put(value, name);
				maxTokenValue = Math.max(maxTokenValue, value);
			}
		}
		String[] result = new String[maxTokenValue + 1];
		for (int i = 0; i < maxTokenValue; ++i) {
			if (tokenNamesMap.containsKey(i)) {
				result[i] = tokenNamesMap.get(i);
			} else {
				result[i] = "<none>";
			}
		}
		return result;
	}
	
	private static void printTokenStats(final Map<Integer, Integer> occurences) {
		printTokenStats(occurences, System.out);
	}
	
	private static void printTokenStats(final Map<Integer, Integer> occurences, PrintStream out) {
		List<Integer> keys = new ArrayList<Integer>(occurences.keySet());
		Collections.sort(keys, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return occurences.get(o2) - occurences.get(o1);
			}
		});
		for (int key : keys) {
			out.printf("%s -> %d\n", tokenNames[key], occurences.get(key));
		}
	}
}
