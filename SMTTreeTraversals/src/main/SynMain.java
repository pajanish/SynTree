package main;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import syn_core.DSLHelper;
import syn_core.SynContext;

import ast_utils.ASTStore;

import com.microsoft.z3.*;

public class SynMain {

	/**
	 * @param args
	 */
	
	private static final String program1 = "programs_augmented1.json";
	private static final String program2 = "programs_augmented2.json";
	private static final String program3 = "programs_augmented3.json";
	private static final String program4 = "programs_augmented4.json";
	private static final String program5 = "programs_augmented5.json";
	
	public static void main(String[] args) {
		Global.ToggleWarningMessages(true);
		
		// Toggle model generation on in Z3 solver
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		
		// Parse AST nodes from an augmented JSON file to a store
		String fileLoc = program2;
		int treeIdx = 0;
		ASTStore store = new ASTStore(fileLoc, treeIdx);
		// Initialize synthesis context (which is also a wrapper for the Z3 context)
		SynContext ctx = new SynContext(cfg, store);
		
		int opNum = 10;
		ctx.setOpNum(opNum);
		test2(ctx);
		
		try {
			BoolExpr synFormula = ctx.mkSynthesisFormula();
			//System.out.println(synFormula.toString());
			Solver solve = ctx.mkSolver();
			solve.add(synFormula);
			System.out.println("main: Checking SMT of synthesis formula with max DSL op count: " + opNum + "...");
			Status stat = solve.check();
			if (stat == Status.SATISFIABLE) {
				System.out.println("Following program found:");
				Model mod = solve.getModel();
				TreeMap<Integer, Integer> interp = ctx.mkModelInterpretation(mod);
				
				Iterator<Map.Entry<Integer, Integer>> it = interp.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer, Integer> curr = it.next();
					System.out.println(curr.getKey().toString() + ": " + DSLHelper.decodeDSLOp((Integer) curr.getValue()));
				}
			} else {		
				System.out.println("Cannot find a program that satisfies all given src/dst pairs. Requested DSL op. number: " + opNum);				
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		ctx.dispose();
	}
	
	private static void test1(SynContext ctx) {
		ctx.addSrcDstPair(353, 330);
		ctx.addSrcDstPair(379, 353);
		ctx.addSrcDstPair(330, 309);		
	}
	
	private static void test2(SynContext ctx) {
		/*ctx.addSrcDstPair(353, 330);
		ctx.addSrcDstPair(379, 353);*/
		ctx.addSrcDstPair(330, 309);		
		ctx.addSrcDstPair(311, 290);
		ctx.addSrcDstPair(332, 311);
		ctx.addSrcDstPair(355, 332);
		ctx.addSrcDstPair(381, 355);		
	}
	
	private static void test3(SynContext ctx) {

		ctx.addSrcDstPair(194, 119);
		ctx.addSrcDstPair(284, 194);
		ctx.addSrcDstPair(362, 284);
		
	}
	
	private static void test4(SynContext ctx) {

		//easy
		ctx.addSrcDstPair(210, 202);
		ctx.addSrcDstPair(232, 224);
		ctx.addSrcDstPair(286, 246);
		
		// hard
		/*ctx.addSrcDstPair(451, 440);
		ctx.addSrcDstPair(467, 456);*/
		
	}
	
	private static void test5(SynContext ctx) {
		// easy
		ctx.addSrcDstPair(24, 21);
		ctx.addSrcDstPair(43, 40);
		ctx.addSrcDstPair(66, 63);
		ctx.addSrcDstPair(91, 88);
		
		// harder (doesnt involve these above)
		// even harder (involves above)

		ctx.addSrcDstPair(27, 24);
		ctx.addSrcDstPair(46, 43);
		ctx.addSrcDstPair(49, 46);
		ctx.addSrcDstPair(69, 66);
		ctx.addSrcDstPair(72, 69);
		ctx.addSrcDstPair(75, 72);
		ctx.addSrcDstPair(94, 91);
		
	}

}