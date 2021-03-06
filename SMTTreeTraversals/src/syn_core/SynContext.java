package syn_core;

import ast_utils.ASTStore;

import com.microsoft.z3.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import utils.*;


public class SynContext extends Context {
	
	private ArrayList<Pair<Integer, Integer>> srcDstPairs;
	private ASTStore astStore;
	private int opNum;
	private boolean efficientLookup;
	
	private Expr[] opIndVars;
	
	public SynContext(HashMap<String, String> cfg, ASTStore astStore, boolean efficientLookup) {
		super(cfg);
		this.astStore = astStore;
		opNum = 1;
		srcDstPairs = new ArrayList<Pair<Integer, Integer>>();
		this.efficientLookup = efficientLookup;
	}
		
	public void setOpNum(int opNum) {
		this.opNum = opNum;
	}
	
	public void addSrcDstPair(Pair<Integer, Integer> srcDstPair) {
		srcDstPairs.add(srcDstPair);
	}
	
	/* Following method tries to generate the following formula. 
	 * If instrNo is 1 and DSL opCnt is 3 the following is generated.
	 *  src = 3, dst = 1
		0: (declare-const dst Int)
		1: (assert 
		2: (exists ((op Int)) 
		3:	(and 
		4:		(ite (= op 1) 
		5:			(= dst (up 3))
		6:			(ite (= op 2)
		7:				(= dst (down-first 3))
		8:				(= dst (down-last 3))))
		9:		(= dst 1))))
	 */
	public BoolExpr mkSynthesisFormula() throws Exception {
		// TODO mkExists
		
		if (opNum == 0) 
			throw new Exception("Cannot synthesise a program with 0 instructions...");
		
		if (DSLHelper.OP_CNT == 0) 
			throw new Exception("Cannot synthesise a program with empty DSL...");
		
		// DSL op indices as bound variables for existential quantifier
		// Line 2 in above example
		//Expr[] opInd = new Expr[instrNo];	
		Sort[] opIndSorts = new Sort[opNum];
		Symbol[] opIndNames = new Symbol[opNum];
		opIndVars = new Expr[opNum];
		ArrayList<IntExpr[]> dstVars = new ArrayList<IntExpr[]>();
		
		// Initialize Op variables as switches for DSL operations
		for (int i = 0; i < opNum; i++) {
			opIndSorts[i] = mkIntSort();
			opIndNames[i] = mkSymbol("op"+i);
			opIndVars[i] = mkConst(opIndNames[i], opIndSorts[i]);
		}
		
		// Initialize destination variables (chained in sequence for each program, one chain for each in/out pair)
		for (int i = 0; i < srcDstPairs.size(); i++) {
			IntExpr[] dstVar = new IntExpr[opNum];
			for (int j = 0; j < opNum; j++) {
				dstVar[j] = mkIntConst("dst"+"_"+i+"_"+j);			
			}	
			dstVars.add(dstVar);
		}

		// If DSL ops are encoded as lookups into DSL defined arrays, then also add the definition of stores of these arrays;
		BoolExpr dslArrayDefinitions = null;
		if (efficientLookup) {
			dslArrayDefinitions = DSLHelper.initDSLArrays(astStore, this);
		}
	
		Expr existsBody = null;
		for (int i = 0; i < srcDstPairs.size(); i++) {
			
			// Add first nested ITE and the final equality
			Expr nestITE = mkNestedITE(opIndVars[0], dstVars.get(i)[0], mkInt(srcDstPairs.get(i).first), 0);
			BoolExpr reachedDst = mkEq(dstVars.get(i)[opNum-1], mkInt(srcDstPairs.get(i).second));
			//BoolExpr boolNestITE = mk
			BoolExpr currPairBody = mkAnd((BoolExpr) nestITE, reachedDst);
			
			// For each following instruction in the sequence,
			// add a ITE switch for choosing the instruction as a nested ITE 
			for (int j = 1; j < opNum; j++) {
				nestITE = mkNestedITE(opIndVars[j], dstVars.get(i)[j], dstVars.get(i)[j-1]/*mkInt(inOutPairs.get(i).first)*/, 0);
				currPairBody = mkAnd((BoolExpr) nestITE, (BoolExpr) currPairBody);
			}
			
			// In first iteration, just assign currPairBody to existsBody
			if (existsBody == null) {
				existsBody = currPairBody;
			} else {
				existsBody = mkAnd((BoolExpr) existsBody, currPairBody);
			}
		}

		// Introduce a constraint for DSL operation switches: op[i] >= 0 && op[i] <= MAX_NUM_OF_INSTRUCTIONS-1
		BoolExpr existDSLOpSwitches = mkExists(opIndSorts, opIndNames, existsBody, 1, null, null, null, null);
		BoolExpr constraintsDSLOpSwitches = mkAnd(
												mkGe((ArithExpr) opIndVars[0], mkInt(0)), 
												mkLe((ArithExpr) opIndVars[0], mkInt(DSLHelper.OP_CNT-1)));
		for (int i = 1; i < opNum; i++) {
			constraintsDSLOpSwitches = mkAnd(constraintsDSLOpSwitches, 
											mkAnd(
												mkGe((ArithExpr) opIndVars[i], mkInt(0)), 
												mkLe((ArithExpr) opIndVars[i], mkInt(DSLHelper.OP_CNT-1))));
		}

		
		BoolExpr synFormula = mkAnd(constraintsDSLOpSwitches, existDSLOpSwitches);
		
		
		//ArrayList<Object> res = new ArrayList<>();
		//res.add(synFormula);
		//res.add(dslArrayDefinitions);
		if (efficientLookup)
			synFormula = mkAnd(dslArrayDefinitions, synFormula);
			//synFormula = dslArrayDefinitions;
		return synFormula;
	}

	/* Recursive function of generating a nested ITE constraint in the following example form:
	 * 
		4:		(ite (= op 1) 
		5:			(= dst (up 3))
		6:			(ite (= op 2)
		7:				(= dst (down-first 3))
		8:				(= dst (down-last 3))))
	 */
	private Expr mkNestedITE(Expr opIdxVar, IntExpr dstVar, IntExpr srcVar, int currOpInd) {
		BoolExpr cond = mkEq(opIdxVar, mkInt(currOpInd));
		Expr tBranch = DSLHelper.getDSLOp(currOpInd, srcVar, dstVar, astStore, this, efficientLookup);
		if (currOpInd == DSLHelper.OP_CNT - 1) {
			//Expr fBranch = DSLHelper.getDSLOp(currOpInd, srcVar, dstVar, astStore, this, efficientLookup);
			Expr fBranch = mkEq(dstVar, mkInt(-1));
			return mkITE(cond, tBranch, fBranch);
		} else {
			Expr fBranch = mkNestedITE(opIdxVar, dstVar, srcVar, currOpInd+1);
			return mkITE(cond, tBranch, fBranch);
		}
	}
	
	public TreeMap<Integer,Integer> mkModelInterpretation(Model model) {
		TreeMap<Integer, Integer> res = new TreeMap<>();
		for (int i = 0; i < opNum; i++) {
			Expr interp = model.getConstInterp(opIndVars[i]);
			res.put(i, ((IntNum) interp).getInt());
		}
		
		return res;
	}	
} 
