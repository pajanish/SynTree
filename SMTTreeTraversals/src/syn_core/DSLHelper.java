package syn_core;

import java.util.HashMap;
import java.util.Iterator;

import ast_utils.ASTStore;

import com.microsoft.z3.*;

/*
 * NOTE: On every add of a new DSL operation:
 * 	- update opCnt
 * 	- add definition as a private method,
 *  - add case to getDSLOp and update op code constants
 */

public class DSLHelper {

	public static final int OP_CNT = 9;
	
	public static final int OP_UP = 0;
	public static final int OP_DOWN_FIRST = 1;
	public static final int OP_DOWN_LAST = 2;
	public static final int OP_PREV_NODE_VAL = 3;
	public static final int OP_PREV_LEAF = 4;	
	public static final int OP_NEXT_LEAF = 5;	
	public static final int OP_LEFT = 6;	
	public static final int OP_RIGHT = 7;	
	public static final int OP_NOP = 8;
	
	// Will be called to get operation definitions,
	// during generation of the Synthesis formula
	public static Expr getDSLOp(int opIdx, IntExpr srcNdIdx, IntExpr dstNdIdx, ASTStore astStore, Context z3Ctx) {
		Expr expr = null;
		switch (opIdx) {
		case OP_NOP:
			expr = nop(srcNdIdx, dstNdIdx, z3Ctx);
			break;
		default:
			expr = (Expr) mkNestedITE(opIdx, astStore, srcNdIdx, dstNdIdx, astStore.getNdIterator(), z3Ctx);
			break;
		}
		 
		return expr;
	}
	
	public static String decodeDSLOp(int opCode) {
		switch(opCode) {
		case OP_UP:
			return "Up";
		case OP_DOWN_FIRST:
			return "DownFirst";
		case OP_DOWN_LAST:
			return "DownLast";
		case OP_PREV_NODE_VAL:
			return "PrevNodeVal";
		case OP_PREV_LEAF:
			return "PrevLeaf";
		case OP_NEXT_LEAF:
			return "NextLeaf";
		case OP_LEFT:
			return "Left";
		case OP_RIGHT:
			return "Right";
		case OP_NOP:
			return "Nop";
		default:
			return "UNKNOWN_DSL_OP";
		}
	}
		
	private static Expr nop(IntExpr srcVar, IntExpr dstVar, Context z3Ctx) {
		return z3Ctx.mkEq(dstVar, srcVar);
	}
	
	private static Expr mkNestedITE(int opCode, ASTStore astStore, IntExpr srcVar, IntExpr dstVar, Iterator it, Context z3Ctx) {
		
		HashMap.Entry pair = (HashMap.Entry) it.next();
		Integer srcNdIdx = (Integer) pair.getKey();
		BoolExpr cond = z3Ctx.mkEq(srcVar, z3Ctx.mkInt(srcNdIdx));
		
		int dstNdVal = -1;
		switch (opCode) {
		case OP_UP:
			dstNdVal = astStore.getNdParentIdx(srcNdIdx);
			break;
		case OP_DOWN_FIRST:
			Integer[] children1 = astStore.getNdChildrenIdx(srcNdIdx);
			if (children1.length == 0) {
				dstNdVal = -1;
			} else {
				dstNdVal = children1[0];
			}
			break;
		case OP_DOWN_LAST:
			Integer[] children2 = astStore.getNdChildrenIdx(srcNdIdx);
			if (children2.length == 0) {
				dstNdVal = -1;
			} else {
				dstNdVal = children2[children2.length-1];
			}
			break;
		case OP_PREV_NODE_VAL:
			dstNdVal = astStore.getNdPrevValue(srcNdIdx);
			break;
		case OP_PREV_LEAF:
			dstNdVal = astStore.getNdPrevLeafIdx(srcNdIdx);
			break;
		case OP_NEXT_LEAF:
			dstNdVal = astStore.getNdNextLeafIdx(srcNdIdx);
			break;
		case OP_LEFT:
			dstNdVal = astStore.getNdLeftIdx(srcNdIdx);
			break;
		case OP_RIGHT:
			dstNdVal = astStore.getNdRightIdx(srcNdIdx);
			break;
		case OP_NOP:
			dstNdVal = srcNdIdx;
			break;
		}
		
		Expr tBranch = z3Ctx.mkEq(dstVar, z3Ctx.mkInt(dstNdVal));
		if (!it.hasNext()) {
			//it.remove();
			Expr fBranch = z3Ctx.mkEq(dstVar, z3Ctx.mkInt(-1));
			return z3Ctx.mkITE(cond, tBranch, fBranch);
		} else {
			//it.remove();
			Expr fBranch = mkNestedITE(opCode, astStore, srcVar, dstVar, it, z3Ctx);
			return z3Ctx.mkITE(cond, tBranch, fBranch);
		}
	}
	
	public static Integer applyDSLOp(Integer srcNdIdx, Integer opInd, ASTStore astStore) {
		switch (opInd) {
		case OP_UP:
			return astStore.getNdParentIdx(srcNdIdx);
		case OP_DOWN_FIRST:
			Integer[] children1 = astStore.getNdChildrenIdx(srcNdIdx);
			if (children1.length == 0) {
				return -1;
			} else {
				return children1[0];
			}
		case OP_DOWN_LAST:
			Integer[] children2 = astStore.getNdChildrenIdx(srcNdIdx);
			if (children2.length == 0) {
				return -1;
			} else {
				return children2[children2.length-1];
			}
		case OP_PREV_NODE_VAL:
			return astStore.getNdPrevValue(srcNdIdx);
		case OP_PREV_LEAF:
			return astStore.getNdPrevLeafIdx(srcNdIdx);
		case OP_NEXT_LEAF:
			return astStore.getNdNextLeafIdx(srcNdIdx);
		case OP_LEFT:
			return astStore.getNdLeftIdx(srcNdIdx);
		case OP_RIGHT:
			return astStore.getNdRightIdx(srcNdIdx);
		case OP_NOP:
			return srcNdIdx;	
		default:
			return -1;
		}
	}
	
	public static Integer applyDSLSequence(Integer srcNdIdx, ASTStore astStore, Integer... opSequence) {
		Integer currNd = srcNdIdx;
		for (int i = 0; i < opSequence.length; i++) {
			currNd = applyDSLOp(currNd, opSequence[i], astStore);
			if (currNd == -1)
				return -1;
		}
		return currNd;
	}
	
}
