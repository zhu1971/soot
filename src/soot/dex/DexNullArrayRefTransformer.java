package soot.dex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Immediate;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;
import soot.jimple.LengthExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.nullcheck.NullnessAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;

/**
 * If Dalvik bytecode contains statements using a base array which is always
 * null, Soot's fast type resolver will fail with the following exception:
 * "Exception in thread "main" java.lang.RuntimeException: Base of array reference is not an array!"
 * 
 * Those statements are replaced by a throw statement (this is what will happen in practice if
 * the code is executed).
 * 
 * @author alex
 *
 */
public class DexNullArrayRefTransformer extends BodyTransformer {
  
  public static DexNullArrayRefTransformer v() {
    return new DexNullArrayRefTransformer();
}

  protected void internalTransform(final Body body, String phaseName, @SuppressWarnings("rawtypes") Map options) {

    final ExceptionalUnitGraph g = new ExceptionalUnitGraph(body);
    
    List<Stmt> arrayRefs = new ArrayList<Stmt>();
    List<Stmt> lengthExprs = new ArrayList<Stmt>();
    for (Unit u: body.getUnits()) {
      Stmt s = (Stmt)u;
      if (s.containsArrayRef()) {
        arrayRefs.add(s);
      } else if (s instanceof AssignStmt) {
        AssignStmt ass = (AssignStmt)s;
        Value rightOp = ass.getRightOp();
        if (rightOp instanceof LengthExpr) {
          lengthExprs.add (s);
        }
      }
    }
    NullnessAnalysis na = new NullnessAnalysis (g);
    for (Stmt s: arrayRefs) {
      Debug.printDbg("statement contains arrayref: "+ s);
      ArrayRef ar = s.getArrayRef();
      Value base = ar.getBase();
      boolean isAlwaysNullBefore = na.isAlwaysNullBefore(s, (Immediate) base);
      Debug.printDbg("is always null: "+ isAlwaysNullBefore);
      if (isAlwaysNullBefore) {
        body.getUnits().swapWith(s, Jimple.v().newNopStmt());
      }       
    }
    for (Stmt s: lengthExprs) {
      Debug.printDbg("statement contains length expr: "+ s);
      LengthExpr l = (LengthExpr)((AssignStmt)s).getRightOp();
      Value base = l.getOp();
      boolean isAlwaysNullBefore = na.isAlwaysNullBefore(s, (Immediate) base);
      Debug.printDbg("is always null: "+ isAlwaysNullBefore);
      if (isAlwaysNullBefore) {
        body.getUnits().swapWith(s, Jimple.v().newNopStmt());
      }       
    }
    
  }    
}



