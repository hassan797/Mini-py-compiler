package mini_python;

import java.beans.Expression;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Typing {

static boolean debug = false;
static List<String> notallowedfuncs = new ArrayList<>() ;


// use this method to signal typing errors
static void error(Location loc, String msg) {
  throw new Error(loc + "\nerror: " + msg);
}

static TFile file(File afile) {

  //error(null, "TODO");
  final TFile Tfile = new TFile();

  final InnerTyping visitor = new InnerTyping(Tfile);
  
  notallowedfuncs.add("len") ;
  notallowedfuncs.add("range") ;
  notallowedfuncs.add("list") ;
  notallowedfuncs.add("print") ;
  
 
  for (Def currentfct : afile.l) {

    String f_name = currentfct.f.id ;
    if(f_name=="main"){
      f_name = "main_fun" ;
    }

    if (f_name.length()==0){
      error(currentfct.f.loc, "Function name is not specified");
    }
    else if (visitor.fct_formalargs.containsKey(f_name)) {    // check if function name is taken
      error(currentfct.f.loc, "Function name " + f_name + " is already used");
    } 
    else if (notallowedfuncs.contains(f_name)){             // fct is not len, range, list...
      error(currentfct.f.loc, "function name: "+ f_name+ " is not allowed, used for builtin funcitons!");
    }

    else {

      LinkedList<Variable> parameters = new LinkedList<Variable>();
      List<String> p_names = new ArrayList<String>() ;
      
      // add function formal parameters to list
      for (Ident p: currentfct.l){  
        if (p_names.contains(p.id)) {
          Typing.error(p.loc, "Parameter name: " + p.id + " used at least twice");
        }
        p_names.add(p.id) ;
        parameters.add(Variable.mkVariable(p.id)) ;
      }

      Function f = new Function(f_name, parameters) ;

      visitor.functions.put(f.name, f) ;                      // add fct
      visitor.fct_formalargs.put(f_name, parameters);   // record formal args for every fct
     
      Sblock stblock = (Sblock) currentfct.s ;
      ArrayList<Variable> localVars = new ArrayList<>() ;      // local fct variables

      for ( Stmt st: stblock.l){
        if ( st instanceof Sassign){
          String var_name = ((Sassign)st).x.id  ;
          if(!p_names.contains(var_name)) {             // add vars not in formal parameters
            localVars.add(Variable.mkVariable(var_name)) ;
          }
        }
      }

      visitor.fct_localVars.put(f_name, localVars) ;       // store fct local vars

      visitor.setcurFunction(f);
      TDef tdef = new TDef(f, visitor.evalStmt(currentfct.s));
      Tfile.l.add(tdef);

    }
  
  }

  LinkedList<Variable> mainvars =  new LinkedList<>() ;
  if (afile.s instanceof Sblock) {
      
    Sblock sblock = (Sblock) afile.s;
    for (Stmt newstmt: sblock.l){             // add variables defined outside functions to list
      if (newstmt instanceof Sassign){

        Ident x1 = ((Sassign)newstmt).x ;
        visitor.global_Vars.put(x1.id , new Variable(x1.id, 0)) ;
        mainvars.add(Variable.mkVariable(x1.id)) ;
        
        }
      }
      
  }

  Function main_ft = new Function("main", mainvars) ;             // add main body stmts to function main in tfile
  TDef main_def = new TDef(main_ft, visitor.evalStmt(afile.s)) ;
  Tfile.l.add(main_def) ;

  return Tfile;
}

}

/**
 * InnerTyping implements TVis
 */
class InnerTyping implements Visitor {

  TFile tfile ;
  TExpr currentTExpr;
  TStmt currenTStmt;
  TSblock tstmtblock ;
  Function currenFunction ;


  public HashMap<String,LinkedList<Variable>> fct_formalargs = new HashMap<String,LinkedList<Variable> >();         // functions with nb of formal arguments

  public HashMap<String,Function> functions = new HashMap<String,Function >();   
  public HashMap<String, Variable> global_Vars =  new HashMap<String, Variable>() ;      //variables defined outside functions to list
  public HashMap<String, ArrayList<Variable>> fct_localVars =  new HashMap<String, ArrayList<Variable>>() ;

  public InnerTyping(TFile tfile) {
    this.tfile = tfile;
  }


  public void setcurFunction(Function new_fun) {
    // TODO Auto-generated method stub
    // throw new UnsupportedOperationException("Unimplemented method 'setcurFunction'");
    this.currenFunction = new_fun ;
  }


  TExpr evalExpr(Expr e) {
    e.accept(this);
    TExpr exp = currentTExpr ;
    return exp ; 
  }

  TStmt evalStmt(Stmt s) {
    s.accept(this);
    TStmt stmt1 = currenTStmt;
    return stmt1;
  }

  public void error(String msg) {
    throw new Error("\nerror: " + msg);
  }


  
  @Override
  public void visit(Ecst e) {
    currentTExpr = new TEcst(e.c);
  }



  @Override
  public void visit(Cnone c) {
  }
  @Override
  public void visit(Cbool c) {
  }
  @Override
  public void visit(Cstring c) {
  }

  @Override
  public void visit(Cint c) {
  }

  @Override
  public void visit(Ebinop e) {
    currentTExpr = new TEbinop(e.op, evalExpr(e.e1), evalExpr(e.e2));
   }

  @Override
  public void visit(Eunop e) {
    currentTExpr = new TEunop(e.op, evalExpr(e.e));
  }


  @Override
  public void visit(Eident e) {
   
    System.out.println("Eident");
    currentTExpr = new TEident(Variable.mkVariable(e.x.id)) ;

    // for (Variable fun_param : currenFunction.params) {    // SOMETHING WRONG HERE
    //   if (fun_param.name.equals(e.x.id)) {
    //     currentTExpr = new TEident(fun_param);
    //     return;
    //   }
    // }

  }

  @Override
  public void visit(Ecall e) {

    System.out.println("Ecall");
    List<String> allowedfuncs = new ArrayList<>() ;
    allowedfuncs.add("len") ;
    allowedfuncs.add("range") ;
    allowedfuncs.add("list") ;

    LinkedList<TExpr> TexprArgs = new LinkedList<TExpr>();
    
    for (Expr exp : e.argspassed) {
      TExpr tx = evalExpr(exp);
      TexprArgs.add(tx);
    }

    switch (e.f.id) {
       

        case "len" :
          System.out.println("NAME =    "+e.f.id);
          if( e.argspassed.size() != 1){
            error("function: "+e.f.id+" only takes 1 argument !" );
          }
          break;
        case "list":
          if ( e.argspassed.size() != 1 ){
          error("function: "+e.f.id+" needs 1 argument only !" );
          }
          Expr inner = e.argspassed.get(0) ;
          if ( !( (inner instanceof Ecall) && (((Ecall)inner).f.id == "range")) ) {
            error("function list must be used exclusivly with range() !" );
          }
          currentTExpr = new TElist(TexprArgs) ;        // might be wrong
          break;
        case "range":
          if ( e.argspassed.size() != 1 ){
            error("function: "+e.f.id+" needs 1 argument only !" );
          }
          currentTExpr = new TErange(evalExpr(e)) ;
          break;
        default :
          if(!this.fct_formalargs.containsKey(e.f.id) && !allowedfuncs.contains(e.f.id)){            // check if function is defined
              error("function: "+ e.f.id + " is not defined !");
          }
          if(e.argspassed.size() != this.fct_formalargs.get(e.f.id).size()){                  // check if function is called with right nb of args
            error("function: "+e.f.id+" called with wrong number of parameters" );
          }

          currentTExpr = new TEcall(this.functions.get(e.f.id), TexprArgs) ;
  }

}

  @Override
  public void visit(Eget e) {
    currentTExpr =  new TEget(evalExpr(e.e1), evalExpr(e.e2));
  }

  @Override
  public void visit(Elist e) {
    
    TElist newListTExp = new TElist(new LinkedList<TExpr>());
    
    System.out.println("Elist");
    for (Expr exprElement : e.l) {
      TExpr texp = evalExpr(exprElement);
      newListTExp.l.add(texp);
    }
    currentTExpr = newListTExp;
  }

  @Override
  public void visit(Sif s) {
    System.out.println("Sif");
    currenTStmt = new TSif(evalExpr(s.e), evalStmt(s.s1), evalStmt(s.s2));
  }

  @Override
  public void visit(Sreturn s) {
    System.out.println("Sreturn");
    currenTStmt = new TSreturn(evalExpr(s.e)) ;
  }

  @Override
  public void visit(Sassign s) {
    System.out.println("Sassign");
    currenTStmt = new TSassign(Variable.mkVariable(s.x.id), evalExpr(s.e)) ;
  }

  @Override
  public void visit(Sprint s) {

    System.out.println("Sprint");   
    s.e.accept(this);
    currenTStmt = new TSprint(evalExpr(s.e));

  }


  @Override
  public void visit(Sblock s) {
    System.out.println("Sblock");
    LinkedList<TStmt> newl = new LinkedList<TStmt>();
    for (Stmt stmt : s.l) {
    
      stmt.accept(this); 
      newl.add(currenTStmt);
    }
    currenTStmt = new TSblock(newl);
  }


  @Override
  public void visit(Sfor s) {
    System.out.println("Sfor");
    currenTStmt = new TSfor(Variable.mkVariable(s.x.id), evalExpr(s.e), evalStmt(s.s)) ;
  }
  

  
  @Override
  public void visit(Seval s) {
    System.out.println("Seval");
    currenTStmt = new TSeval(evalExpr(s.e)) ;
  }


  @Override
  public void visit(Sset s) {
    System.out.println("Sset");
    currenTStmt = new TSset(evalExpr(s.e1), evalExpr(s.e2), evalExpr(s.e3)) ;
  }

  
  
  
}
