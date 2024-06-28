package mini_python;

import java.util.HashSet;
import java.util.Set;

// import java.util.LinkedList;

class Compile {

    static boolean debug = false;

    static X86_64 file(TFile f) {
        X86_64 asm = new X86_64();
        //myPrintfWrapper() and myMallocWrapper() are correctly adding the wrapper functions
        asm.myPrintfWrapper(); // Add printf wrapper (add some if statement here)
        asm.myMallocWrapper(); // Add malloc wrapper (same)
        boolean foundMain = false;
        asm.print_typed();
        // Ideally here we would set some flags seeing if the code uses Boolean, print, malloc so that we could avoid a .s with lots of useless stuff.
        // we can do that after the whole code is working

        for (TDef def : f.l) {
            if ("main".equals(def.f.name)) {
                foundMain = true;

                // Start of function prologue
                asm.startFunction(def.f.name);
                asm.pushq("%rbp"); 
                asm.movq("%rsp", "%rbp");
                // CodeGenerationVIsitor
                CodeGenerationVisitor visitor = new CodeGenerationVisitor(asm);
                def.body.accept(visitor);
                // Add function epilogue
                asm.movq("%rbp", "%rsp");
                asm.popq("%rbp");
                asm.ret();
                break; 
            }
            else {
                asm.startFunction(def.f.name); // This guarantees that the function has a unique name(we did that in the Typing.java)
                // Start of function prologue
                asm.pushq("%rbp");
                asm.movq("%rbp","%rsp");
                // CodeGenerationVIsitor
                CodeGenerationVisitor visitor = new CodeGenerationVisitor(asm);
                def.body.accept(visitor);
                // Add function epilogue
                asm.leave();
                asm.ret();
            }
        }
        
        if (!foundMain) {
            // Optionally handle the absence of a main function
            System.err.println("No 'main' function found.");
        }
        return asm;
    }

    // Visitor class for generating assembly code
    private static class CodeGenerationVisitor implements TVisitor {
        private final X86_64 asm;
        // public Set<String> usedErrorLabels = new HashSet<>();
        static int label_counter = 0;
        private Binop current_operation ; 

        
        public CodeGenerationVisitor(X86_64 asm) {
            this.asm = asm;
        }     
        

        

        public void strcomparison(){

            // Labels for loop and decision making
            String loopStart = "loop_start"+getLabelCounter();
            String endLoop = "end_loop"+getLabelCounter();
            String str1Less = "str1_less"+getLabelCounter();
            String str1Greater = "str1_greater" + getLabelCounter();
            String continuecomp = "continue_comparison" + getLabelCounter();

            asm.movq("8(%rsi)", "%r8") ;
            asm.movq("8(%rdi)", "%r9") ;
            asm.cmpb("%r9", "%r8") ;
            asm.jle(str1Less) ;
            asm.jg(str1Greater) ;


            // Skip over the type identifier and size of the strings in the heap structure
            asm.addq("$16", "%rsi");  // Move %rsi to point to the first char of str1
            asm.addq("$16", "%rdi");  // Move %rdi to point to the first char of str2

            asm.label(loopStart);
            asm.movb("(%rsi)", "%al");  // Load current char from str1 into %al
            asm.movb("(%rdi)", "%bl");  // Load current char from str2 into %bl
            asm.cmpb("%bl", "%al");     // Compare chars: str1[i] vs. str2[i]

            asm.jl(str1Less);           // If str1[i] < str2[i], jump to str1Less
            asm.jg(str1Greater);        // If str1[i] > str2[i], jump to str1Greater

            // Check for end of strings
            asm.testq("%al", "%al");    // Test if char from str1 is 0 (end of string)          // TO-DO
            asm.jz(endLoop);            // If end of str1, end loop
            asm.testq("%bl", "%bl");    // Test if char from str2 is 0 (end of string)      //TO-DO
            asm.jz(endLoop);            // If end of str2, end loop

            asm.incq("%rsi");           // Move to next char in str1
            asm.incq("%rdi");           // Move to next char in str2
            asm.jmp(loopStart);         // Jump back to start of loop

            asm.label(str1Less);
            // asm.movq("$16", "%rdi");     // Set result to 1 (str1 < str2)
            // asm.call("my_malloc") ;
            // asm.movq(0, " (%rax)") ;
            // asm.movq(-1, " 8(%rax)")   ;
            asm.movq(-1, "%rax") ;
            asm.jmp(endLoop);           // Jump to end of loop

            asm.label(str1Greater);
            asm.movq(1, "%rax") ;
            asm.jmp(endLoop);           // Jump to end of loop

            asm.label(endLoop);             // Compare if loop exited without finding a difference and strings were not at the end
            asm.cmpb("%al", "%bl");
            asm.je("strings_equal");
            asm.jmp(continuecomp); // Placeholder for additional logic

            asm.label("strings_equal");
            asm.movq(0, "%rax") ;
            // Placeholder label for additional comparison logic continuation
            
            asm.label(continuecomp);
            // Continue with the rest of the function if needed

                
        }  

        public void listcomparison(){           
            
            // Labels for loop and decision making
            String list1Longer = "_list1_longer" + getLabelCounter();
            String list2Longer = "_list2_longer"+ getLabelCounter();
            String compareElements = "_compare_elements"+ getLabelCounter();
            String loopStart = "_loop_start"+ getLabelCounter();
            String list1ElementGreater = "_list1_element_greater" + getLabelCounter();
            String list2ElementGreater = "_list2_element_greater" + getLabelCounter();
            String equal = "_equal"+ getLabelCounter();
            String end = "_end"+ getLabelCounter();

    
            // Load list lengths into rdx (list1 length) and rcx (list2 length)
            asm.movq("8(%rdi)", "%rdx");
            asm.movq("8(%rsi)", "%rcx");
    
            // Compare list lengths first
            asm.cmpq("%rdx", "%rcx");
            asm.jg(list2Longer);
            asm.jl(list1Longer);
    
            // Lengths are equal; proceed to compare elements
            asm.label(compareElements);
            asm.xorq("%r8", "%r8"); // Zero the index counter r8
    
            asm.label(loopStart);
            asm.cmpq("%r8", "%rdx");
            asm.jge(equal);  //
            asm.addq("$2", "%r8") ;

            // Load current elements from both lists into r9 and r10
            asm.movq("(%rdi, %r8, 8)", "%r9");
            asm.movq("(%rsi, %r8, 8)", "%r10");
    
            // Dereference to compare actual integer values, assuming type field is skipped
            asm.movq("8(%r9)", "%r11");
            asm.movq("8(%r10)", "%r12");
    
            // Compare values
            asm.cmpq("%r11", "%r12");  // compare %rdi , %rsi
            asm.jg(list2ElementGreater);
            asm.jl(list1ElementGreater);
            
            asm.incq("%r8") ;
            asm.subq("$-2","%r8");
            asm.jmp(loopStart);
    
            // Handle different cases for list comparison results
            asm.label(list1Longer);
            asm.movq("$1", "%rax");
            asm.jmp(end);
    
            asm.label(list2Longer);
            asm.movq("$-1", "%rax");
            asm.jmp(end);
    
            asm.label(list1ElementGreater);
            asm.movq("$1", "%rax");
            asm.jmp(end);
    
            asm.label(list2ElementGreater);
            asm.movq("$-1", "%rax");
            asm.jmp(end);
    
            asm.label(equal);
            asm.xorq("%rax", "%rax"); // Set %rax to 0 indicating equality
    
            asm.label(end);
    
        }  

        public void strConcatenation(){

            // must store str size in a register instead
            if (current_operation == Binop.Badd) {

                String loop1_start = "Loop_start"+getLabelCounter(); 
                String loop1_end = "Loop_end"+getLabelCounter(); 

                String loop2_start = "Loop_start"+getLabelCounter(); 
                String loop2_end = "Loop_end"+getLabelCounter(); 
                
                asm.popq("%rdi"); // Address of second operand
                asm.popq("%rsi"); // Address of first operand

                asm.movq("8(%rsi)", "%r8") ;    // size str1 
                asm.movq("8(%rdi)", "%r9") ;    // size str2 

                asm.addq("%r8", "%r9") ;         // sum sizes
                asm.movq("%r9" , "%r10");       // store str total sizes in r10

                asm.addq("$17", "%r9") ;        // get total memory size needed to allocate =  17 + strings size
                                                        // r9 now = 17 + str1 + str2 AND  r8 = str 1
                asm.movq("%r9" , "%rdi");       
                asm.call("my_malloc");                 // r10 = str1 + str2 
                asm.movq("$3", "(%rax)");                
                asm.movq("%r10", "8(%rax)");

                asm.cmpq("$0", "8(%rsi)") ;
                asm.je(loop1_end) ;

                asm.xorq("%rcx", "%rcx") ;           // initialize counter
                asm.movq("$16", "%r10") ;        // r10 now contains offset = 16

                asm.label(loop1_start) ;                // copy first str to rax

                asm.movb("(%rsi, %r10, 1)","%rbx") ;
                asm.movb("%rbx","(%rax, %r10, 1)") ;
                asm.incq("%r10") ;
                asm.incq("%rcx");
                asm.cmpq("%rcx", "8(%rsi)") ;
                asm.jl(loop1_start) ;
                
                asm.label(loop1_end) ;
                
                asm.movq("8(%rdi)", "%r8") ;
                asm.xorq("%rcx", "%rcx") ;
                asm.movq("$16", "r9") ;         // offset for string 2  

                asm.label(loop2_start);                 // // copy second str to rax

                asm.movb("(%rdi, %r9, 1)","%rbx") ;
                asm.movb("%rbx","(%rax, %r10, 1)") ;
                asm.incq("%r9") ;
                asm.incq("%r10") ;
                asm.incq("%rcx");
                asm.cmpq("%rcx", "8(%rdi)") ;
                asm.jl(loop2_start) ;
                
                asm.label(loop2_end) ;
                asm.movb("$0","(%rax, %r10, 1)") ;
                
                asm.pushq("%rax") ;

            }
        }

        public void listConcatenation(){

            if (current_operation == Binop.Badd) {

                String loop1_start = "Loop_start"+getLabelCounter(); 
                String loop1_end = "Loop_end"+getLabelCounter(); 

                String loop2_start = "Loop_start"+getLabelCounter(); 
                String loop2_end = "Loop_end"+getLabelCounter(); 
                
                asm.movq("8(%rsi)", "%r8") ;    // size 1
                asm.movq("8(%rdi)", "%r9") ;    // size 2 

                asm.addq("%r8", "%r9") ;         // sum sizes
                asm.movq("%r9" , "%r10");       // store list total sizes in r10

                asm.addq("$16", "%r9") ;        // get total memory size needed to allocate =  16 + lists size
                asm.pushq("%rdi") ;
                                                        // r9 now = 16+ l1 + l2 AND  r8 =  size of l1
                asm.movq("%r9" , "%rdi");       //%rdi now has size of new list
                asm.call("my_malloc");                 
                asm.movq("$4", "(%rax)");                
                asm.movq("%r10", "8(%rax)");        // r10 = str1 + str2 

                asm.cmpq("$0", "8(%rsi)") ;
                asm.je(loop1_end) ;

                asm.movq("$0", "%rcx") ;           // initialize counter
                asm.movq("$16", "%r10") ;        // r10 now contains offset = 16

                asm.label(loop1_start) ;                // copy first str to rax

                asm.movq("(%rsi, %r10, 1)","%rbx") ;
                asm.movq("%rbx","(%rax, %r10, 1)") ;
                asm.addq("$8", "%r10") ;
                asm.incq("%rcx");
                asm.cmpq("%rcx", "8(%rsi)") ;
                asm.jl(loop1_start) ;
                
                asm.label(loop1_end) ;
                asm.popq("%rdi") ;
                asm.movq("8(%rdi)", "%r8") ;
                asm.xorq("%rcx", "%rcx") ;
                asm.movq("$16", "%r9") ;         // offset for string 2  

                asm.label(loop2_start);                 // // copy second str to rax

                asm.movq("(%rdi, %r9, 1)","%rbx") ;
                asm.movq("%rbx","(%rax, %r10, 1)") ;
                asm.addq("$8", "%r9") ;
                asm.addq("$8", "%r10") ;
                asm.incq("%rcx");
                asm.cmpq("%rcx", "8(%rdi)") ;
                asm.jl(loop2_start) ;
                
                asm.label(loop2_end) ;
                
                asm.pushq("%rax") ;

                // pushing done after returning

            }
        }
        
        
        private void intArithmeticsCode(){
            
            asm.movq("8(%rsi)", "%rax"); // Load first operand's value
            asm.movq("8(%rdi)", "%rbx"); // Load second operand's value

            switch(current_operation){

                case Badd:
                    // Perform addition
                    asm.addq("%rbx", "%rax"); // Add second operand to first

                    break;
                case Bsub:
                    // Perform subtraction
                    asm.subq("%rbx", "%rax"); // Subtract second operand from first
                    break;
                case Bmul:
                    // Perform multiplication
                    asm.imulq("%rbx", "%rax"); // Multiply %rbx with %rax, result in %rax
                    break;
                case Bdiv:
                   
                    asm.xorq("%rdx", "%rdx"); // Clear %rdx for division
                    // Check for division by zero
                    asm.testq("%rbx", "%rbx"); // Test if divisor is zero
                    asm.jz("division_by_zero"); // Jump to error handling if divisor is zero
                    
                    // Perform division
                    asm.idivq("%rbx"); // Divide %rax by %rbx, quotient in %rax, remainder in %rdx
                    break;
                case Bmod:
                    System.out.println("Bmod");

                    // Prepare for division
                    asm.xorq("%rdx", "%rdx"); // Clear %rdx for division to hold the remainder
                
                    // Check for division by zero
                    asm.testq("%rbx", "%rbx"); // Test if divisor is zero
                    asm.jz("division_by_zero"); // Jump to error handling if divisor is zero
                
                    // Perform division
                    asm.idivq("%rbx"); // Divide %rax by %rbx, quotient in %rax, remainder in %rdx
                
                    // For modulo, we are interested in the remainder, which is now in %rdx
                    // Store the remainder (result of modulo operation) in the space allocated for the result
                    asm.movq("%rdx","%rax");
                    break;
                
                default:
                    break;
            }

            //Perform register organization
            asm.pushq("%rax") ;
            asm.movq(16, "%rdi");
            asm.call("my_malloc");
            asm.movq("$2", "(%rax)") ;

            asm.popq("%rbx") ;
            asm.movq("%rbx","8(%rax)");
            asm.pushq("%rax");
        } 



        public void intcomparison(){
            
            asm.movq("8(%rsi)", "%rax"); // Load first operand's value
            asm.movq("8(%rdi)", "%rbx"); // Load second operand's value

            asm.cmpq("%rbx", "%rax") ;
            String t = "return_true"+getLabelCounter() ;
            String f = "return_false" +getLabelCounter();

            switch(current_operation){
                
                case Beq :
                    asm.je(t) ;
                    asm.jne(f) ;
                    break;
                
                case Bneq:
                    asm.jne(t) ;
                    asm.je(f) ;
                    break;
                
                case Bgt :
                    asm.jg(t) ;
                    asm.jle(f) ;
                    break;
                
                case Bge :
                    asm.jge(t) ;
                    asm.jl(f) ;
                    break;
                
                case Blt :
                    asm.jl(t) ;
                    asm.jge(f) ;
                    break;
                
                case Ble :
                    asm.jle(t) ;
                    asm.jg(f) ;

                    break;
                
                default:
                    break;
                
                
            }
            // asm.pushq("%rax") ;
            asm.label(t) ;
            asm.emit(
            "movq $16, %rdi  \r\n"+
            "call my_malloc  \r\n"+
            "movq $1, (%rax)  \r\n"+ 
            "movq $1, 8(%rax)  \r\n"+  
            "pushq %rax  \r\n"+       
                                                  "" ) ;
            asm.label(f) ;
            asm.emit(
            "movq $16, %rdi  \r\n"+
            "call my_malloc  \r\n"+
            "movq $1, (%rax)  \r\n"+ 
            "movq $0, 8(%rax)  \r\n"+  
            "pushq %rax  \r\n"+       //
                                                  "" ) ;


        }  
        
        private void check_operandsTypes(String[] acceptableTypes){
            generateTypeCheckCode(asm, "%rdi", acceptableTypes, "r8");
            generateTypeCheckCode(asm, "%rsi", acceptableTypes, "r9");
        }

        // Check type of the operands 
        private String getOperandsType(TEbinop e){

            if ( !(e.e1 instanceof TEbinop) && !(e.e2 instanceof TEbinop)) {
            
                if (  (((TEcst)e.e1).c instanceof Cint) && (((TEcst)e.e2).c instanceof Cint)){
                    return "int" ;
                } else if ( (((TEcst)e.e1).c instanceof Cstring) && (((TEcst)e.e2).c instanceof Cstring)){
                    return "str" ;
                }
                else if (  (e.e1 instanceof TElist ) && (e.e2 instanceof TElist) ){
                    return "list" ;
                }else if (   (((TEcst)e.e1).c instanceof Cbool) && (((TEcst)e.e2).c instanceof Cbool)){
                    return "bool" ;
                }else if (   (((TEcst)e.e1).c instanceof Cnone) && (((TEcst)e.e2).c instanceof Cnone)){
                    return "none" ;
                }else {
                    return "mismatch" ;
                }
            }
            return null ;

        }

       
        private void boolcomparison(){

            asm.movq("8(%rsi)", "%rcx"); // Load first operand's value into %rcx
            asm.movq("8(%rdi)", "%rbx"); // Load second operand's value into %rbx
        
            // Compare the boolean values
            asm.cmpq("%rcx", "%rbx");
        
            // Conditional jump labels
            String equalLabel = "equal";
            String lessLabel = "less";
            String doneLabel = "done";
        
            // Generate jumps based on comparison
            asm.je(equalLabel);
            asm.jl(lessLabel);

            asm.movq("$1", "%rax"); // If here, rcx > rbx, set rax to 1 (true > false)
            asm.jmp(doneLabel);
        
            // Handle less than case
            asm.label(lessLabel);
            asm.movq("$-1", "%rax"); // Set rax to -1 (true < false)
            asm.jmp(doneLabel);
        
            // Handle equal case
            asm.label(equalLabel);
            asm.xorq("%rax", "%rax"); // Set rax to 0 (true == false)
        
            // Done label for function exit
            asm.label(doneLabel);
            asm.xorq("%rcx", "%rcx") ;
            // Continue with rest of the program or return

        }


        @Override
        public void visit(TEbinop e) {

            current_operation =e.op ;
            String[] acceptableTypes= {}; 
            System.out.println("TEbinop");
            e.e1.accept(this); //evaluate expression e1 (return must be in rax, 
            // be sure that the pushq %rax happened (for the arith tests it happened in Ecst)
            e.e2.accept(this); //evaluate expression e2 (return must be in rax, push rax)
            asm.popq("%rdi"); // Address of second operand
            asm.popq("%rsi"); // Address of first operand
        

            // String intops = "handle_intOp"+ getLabelCounter() ;
            // String strops = "handle_strOp" + getLabelCounter() ;
            // String listops = "handle_listOp"+ getLabelCounter()  ;
            // String boolops = "handle_boolOp"+ getLabelCounter()  ;
            String type_mismatch = "type_mismatch"+getLabelCounter() ;
            String endLabel = "end" + getLabelCounter() ;


            if ( (e.e1 instanceof TEcst &&  ((TEcst)e.e1).c instanceof Cint ) || e.e2 instanceof TEcst &&  ((TEcst)e.e2).c instanceof Cint  ){
                if ((e.op == Binop.Badd) || (e.op == Binop.Bmul) ||(e.op == Binop.Bdiv) ||(e.op == Binop.Bsub)  ){
                    intArithmeticsCode();
                }else if  ((e.op == Binop.Beq) ||(e.op == Binop.Bneq) ||(e.op == Binop.Ble) || (e.op == Binop.Blt) ||(e.op == Binop.Bge) ||(e.op == Binop.Bgt)  ){
                    intcomparison();
                    returnComparisonResult(e.op);
                }
            }

            else if ( ((e.e1 instanceof TEcst) && ( ((TEcst)e.e1).c instanceof Cstring)) || e.e2 instanceof TEcst &&  ((TEcst)e.e2).c instanceof Cstring  ){
                if (e.op == Binop.Badd ){
                    strConcatenation();
                }else if  ((e.op == Binop.Beq) ||(e.op == Binop.Bneq) ||(e.op == Binop.Ble) || (e.op == Binop.Blt) ||(e.op == Binop.Bge) ||(e.op == Binop.Bgt)  ) {
                    strcomparison();
                    returnComparisonResult(e.op);
                }else{
                    asm.jmp(type_mismatch) ;
                }
            }

            else if ( (e.e1 instanceof TEcst &&  ((TEcst)e.e1).c instanceof Cbool ) || e.e2 instanceof TEcst &&  ((TEcst)e.e2).c instanceof Cbool  ){
               
                if  ((e.op == Binop.Beq) ||(e.op == Binop.Bneq) ||(e.op == Binop.Ble) || (e.op == Binop.Blt) ||(e.op == Binop.Bge) ||(e.op == Binop.Bgt)  ) {
                    boolcomparison();
                    returnComparisonResult(e.op);
                }else{
                    asm.jmp(type_mismatch) ;
                }
            }

            else if ( (e.e1 instanceof TElist) || (e.e2 instanceof TElist) ){
                
                if (e.op == Binop.Badd ){
                    listConcatenation();
                }else if  ((e.op == Binop.Beq) ||(e.op == Binop.Bneq) ||(e.op == Binop.Ble) || (e.op == Binop.Blt) ||(e.op == Binop.Bge) ||(e.op == Binop.Bgt)  ) {
                    listcomparison();
                    returnComparisonResult(e.op);
                }else{
                    asm.jmp(type_mismatch) ;
                }
            }
            asm.label(endLabel) ;

        }
            
          
        public void returnComparisonResult( Binop op){
        
            String t = "return_true"+getLabelCounter() ;
            String f = "return_false" +getLabelCounter();

            switch (op) {
                case Beq:
                    asm.cmpq(0, "%rax") ;
                    asm.je(t) ;
                    asm.jne(f) ;
                    break;

                case Bneq:
                    asm.cmpq(0, "%rax") ;
                    asm.jne(t) ;
                    asm.je(f) ;
                    break;

                case Blt:
                    asm.cmpq(-1, "%rax") ;
                    asm.jne(f) ;
                    asm.je(t) ;
                    break;

                case Ble:
                    
                    asm.cmpq(0, "%rax") ;
                    asm.jle(t) ;
                    asm.jmp(f) ;
                    break;

                case Bgt:
                    asm.cmpq(1, "%rax") ;
                    asm.jne(f) ;
                    asm.je(t) ;
                    break;


                case Bge:
                    asm.cmpq(0, "%rax") ;
                    asm.jge(t) ;
                    asm.label("end_cmp") ;
                    break;

                default:
                    break;
            }

            asm.label(t) ;
            asm.emit(
            "movq $16, %rdi  \r\n"+
            "call my_malloc  \r\n"+
            "movq $1, (%rax)  \r\n"+ 
            "movq $1, 8(%rax)  \r\n"+  
            "pushq %rax  \r\n"+       
                                                  "" ) ;
            asm.label(f) ;
            asm.emit(
            "movq $16, %rdi  \r\n"+
            "call my_malloc  \r\n"+
            "movq $1, (%rax)  \r\n"+ 
            "movq $0, 8(%rax)  \r\n"+  
            "pushq %rax  \r\n"+       //
                                                  "" ) ;


        }


        private String getLabelCounter() {
            // TODO Auto-generated method stub
            label_counter += 1 ;
            return String.valueOf(label_counter) ;
        }




        @Override
        public void visit(TEunop e) {
            String[] acceptableTypes= {}; 
            System.out.println("TEunop");
            e.e.accept(this);
            switch (e.op) {
                case Uneg: // Negation
                    asm.popq("%rdi"); // Address of the second operand
                    // Type check for first operand
                    acceptableTypes = new String[]{"0", "1", "2", "3", "4"};
                    generateTypeCheckCode(asm, "%rdi", acceptableTypes, "r8");
                    // Load the integer value from the second part of the heap-allocated space
                    asm.movq("8(%rdi)", "%rax"); //   the actual value is at an 8-byte offset
                    // Perform the logical negation
                    asm.testq("%rax", "%rax");  // Test %rax against itself
                    asm.sete("%al");            // Set %al based on the zero flag
                    asm.movzbq("%al", "%rax");  // Extend %al to %rax, setting %rax to 0 or 1
                    break;
                // Implement other unary operators as needed
                case Unot:
                    break;
                default:
                    break;
            }
        }



        @Override
        public void visit(TEcst e) {
            if (e.c instanceof Cint) {
                visit((Cint)e.c); // Directly call visit(Cint c)
            }
            else if (e.c instanceof Cbool) {
                visit((Cbool)e.c);
            } 
            else if (e.c instanceof Cnone) {
                visit((Cnone)e.c);
            } 
            else if (e.c instanceof Cstring) {
                visit((Cstring)e.c);
            } 
            else{
                // Handle other types or throw an exception if unsupported type
                throw new UnsupportedOperationException("Unsupported constant type: " + e.c.getClass().getName());
            }
        }
        
        
        @Override
        public void visit(Cnone c) {
            System.out.println("Cnone");
            
            // Allocate 16 bytes: 8 for the type tag and 8 for the placeholder value
            asm.movq("$16", "%rdi"); // Set the allocation size in %rdi for my_malloc
            asm.call("my_malloc"); // Call my_malloc, address of allocated memory is in %rax
        
            // Store the type tag for 'none' (0) at the allocated address
            asm.movq("$0", "(%rax)"); // %rax holds the address returned by my_malloc
        
            // Store a placeholder value (0) right after the type tag
            asm.movq("$0", "8(%rax)"); // Move 0 into the space after the type tag as the placeholder value
        
            // Now, %rax holds the address of the allocated block containing the type tag and placeholder value
            // Push this address onto the stack for further use
            asm.pushq("%rax");
        }

        @Override
        public void visit(Cbool c) {
            System.out.println("Cbool");
            int value = c.b ? 1 : 0; // Convert the boolean to 0 (False) or 1 (True)

            // Allocate 16 bytes: 8 for the type tag and 8 for the value
            asm.movq("$16", "%rdi"); // Set the allocation size in %rdi for my_malloc
            asm.call("my_malloc"); // Call my_malloc, address of allocated memory is in %rax
        
            // Store the type tag for 'bool' (1) at the allocated address
            asm.movq("$1", "(%rax)"); // %rax holds the address returned by my_malloc
        
            // Store the actual boolean value right after the type tag
            asm.movq("$" + value, "8(%rax)"); // Move the boolean value into the space after the type tag
        
            // Now, %rax holds the address of the allocated block containing the type tag and value
            // Push this address onto the stack for further use
            asm.pushq("%rax");
        }



        @Override
        public void visit(Cint c) {
            System.out.println("Cint");
            long value = c.i;
    
            // Allocate 16 bytes: 8 for the type tag and 8 for the value
            asm.movq("$16", "%rdi"); // Set the allocation size in %rdi for my_malloc
            asm.call("my_malloc"); // Call my_malloc, address of allocated memory is in %rax
    
            // Store the type tag for 'int' (2) at the allocated address
            asm.movq("$2", "(%rax)"); // %rax holds the address returned by my_malloc
    
            // Store the actual integer value right after the type tag
            asm.movq("$" + value, "8(%rax)"); // Move the constant value into the space after the type tag
    
            // Now, %rax holds the address of the allocated block containing the type tag and value
            // Push this address onto the stack for further use
            asm.pushq("%rax");
        }
        
        @Override
        public void visit(Cstring c) {
            
            System.out.println("Cstring");
            String str = c.s; // The actual string value
            int strLength = str.length();
    
            // Calculate the total allocation size: 16 bytes (8 for type tag + 8 for length) + string length + 1 for null terminator
            long totalSize = 16 + strLength + 1;
    
            // Allocate memory
            asm.movq("$" + totalSize, "%rdi"); // Set the allocation size in %rdi for my_malloc
            asm.call("my_malloc"); // Call my_malloc, address of allocated memory is in %rax
    
            // Store the type tag for 'string' (3) at the allocated address
            asm.movq("$3", "(%rax)"); // %rax holds the address returned by my_malloc
    
            // Store the string length right after the type tag
            asm.movq("$" + strLength, "8(%rax)");
    
            // Now you need to copy the string data into the allocated block.
            //   you have a method to handle string data storage or can directly place string data in memory:
            storeStringData(str, 16); // A pseudo-method to store the string at offset 16 from the allocated address
    
            // Push the address of the allocated block onto the stack
            asm.pushq("%rax");
        }

       

        @Override
        public void visit(TElist e) {
            System.out.println("TElist");
        
            // Calculate the total size needed for the list in memory:
            // 16 bytes for type and length + 8 bytes for each element
            long totalSize = 16 + e.l.size() * 8;
        
            // Allocate memory for the list
            asm.movq("$" + totalSize, "%rdi");
            asm.call("my_malloc");
        
            // Store the type tag for 'list' (4) at the allocated address
            asm.movq("$4", "(%rax)");
        
            // Store the list length right after the type tag
            asm.movq("$" + e.l.size(), "8(%rax)");
        
            // Now, store the addresses of the list elements
            long offset = 16; // Start offset for elements 
            for (TExpr element : e.l) {
                element.accept(this); // This should leave the element's address in %rax
                asm.movq("%rax", offset + "(%rax)"); // Store the element address
                offset += 8;
            }
        
            // Push the address of the list structure onto the stack
            asm.pushq("%rax");
        }


// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% END TYPE VISITORS %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        @Override
        public void visit(TEident e) {
            System.out.println("TEident");
               // Assuming `e.id` is the name of the variable and it's a global variable
            
               String varName =  ((TEident)e).x.name; // Get the variable name from the identifier expression
               int uid = ((TEident)e).x.uid;
   
               int varAddress = variableHandling(varName,uid);
   
               // Now generate assembly to move its value into %rax
               // You might have a mapping from variable names to their memory addresses or labels
               asm.movq("$-"+varAddress+"(%rbp)", "%rax"); // Adjust based on your addressing
           }
   
           private int variableHandling(String varName, int uid) {
               // TODO Auto-generated method stub
               throw new UnsupportedOperationException("Unimplemented method 'variableHandling'");
        }

        @Override
        public void visit(TEcall e) {
            System.out.println("TEcall");
            for (TExpr arg : e.l) {
                arg.accept(this); // I assume this leaves the argument's address in %rax and then pushes it onto the stack.
            }

            String functionName = e.f.name; // The function name as used in the source code.
            asm.call(functionName);
            asm.pushq("%rax");
        }

        @Override
        public void visit(TEget e) {
            System.out.println("TEget");
        }


        @Override
        public void visit(TErange e) {
            System.out.println("TErange");
            e.e.accept(this);
        }

        @Override
        public void visit(TSif s) {
            System.out.println("TSif");

        }

        @Override
        public void visit(TSreturn s) {
            System.out.println("TSreturn");
        }

        @Override
        public void visit(TSassign s) {
            System.out.println("TSassign");
        
            // Evaluate the right-hand side expression, leaving its result (or address) in %rax
            s.e.accept(this);
            String varName = ((TSassign)s).x.name;
            
            String variableAddress = resolveVariableAddress(varName);   
        
            // Store the result (or address) in %rax into the variable's address
            // This might be a simple register move, a memory store, or something more complex
            // depending on your implementation details.
             asm.movq("%rax", variableAddress);  
        
            // Note: 'resolveVariableAddress' and the actual store operation are placeholders.
            // You need to replace them with actual logic based on your implementation.
        }
        private String resolveVariableAddress(String varName) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'resolveVariableAddress'");
        }

        int printCount = 0;
        @Override
        public void visit(TSprint s) {
            // Evaluate the expression and ensure the result is prepared for printing
            s.e.accept(this);
            System.out.println("TSprint");
            
            // i need to implement something here that always pushes an address of the variable being printed to the stack
            // i'm creating the logic for afterwards, we'll need it anyway
            asm.call("print_typed");
            asm.printNewline();
            // Check if X86_64 instance `asm` is accessible here; ensure it's initialized properly
            printCount++;
            if(printCount!=0) {
                asm.ensureNewlineString();
            }
            asm.addDataEntryForType("None"); //0
            asm.addDataEntryForType("Bool"); //1
            asm.addDataEntryForType("Int");  //2
            asm.addDataEntryForType("String"); //3
            asm.addDataEntryForType("List"); //4
            // ideally i would check inside the code which type it is but only create the code to jump and print if it was used
            // i have r8 with the type value
            // and also the 

              // i can use this structure to call the .data and the test structures and above to create something to treat with %r8

        }
        
        @Override
        public void visit(TSblock s) {
            System.out.println("TSblock");
            for (TStmt stmt : s.l) { // Use the correct field name 'l' and type 'TStmt'
                stmt.accept(this); // Visit each statement in the block
            }
        }
        
        @Override
        public void visit(TSfor s) {
            System.out.println("TSfor");
        }

        @Override
        public void visit(TSeval s) {
            System.out.println("TSeval");
        }

        @Override
        public void visit(TSset s) {
            System.out.println("TSset");
        }

        // Implement visit methods for other node types as needed

        


        /* REPETITIVE HANDLING CODES */
        /* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% begin %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% */

        private void storeStringData(String str, int offset) {
            // Base address of allocated memory is assumed to be in %rax.
            // Offset is where the string data should start, accounting for type tag and length.
        
            // First, let's move each character into the allocated memory.
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                // Convert character to ASCII value.
                int asciiValue = (int) c;
                // Calculate the actual memory address offset for the current character.
                long charOffset = offset + i;
        
                // Emit assembly to move character ASCII value into the correct memory location.
                // This assembly line moves a byte ('movb') into memory at (%rax + charOffset).
                asm.emit(String.format("movb $%d, %d(%rax)", asciiValue, charOffset));
            }
        
            // After storing all characters, add a null terminator at the end.
            int nullTerminatorOffset = offset + str.length();
            asm.emit(String.format("movb $0, %d(%rax)", nullTerminatorOffset));

        }

        private int typeCheckLabelCounter = 0;

        private void generateTypeCheckCode(X86_64 asm, String operandAddress, String[] acceptableTypes, String typeRegister) {
            // Increment the global label counter to ensure uniqueness
            typeCheckLabelCounter++;    
            // Load the type of the operand into the specified type register (e.g., %r9)
            asm.movq("0(" + operandAddress + ")", "%" + typeRegister);
        
            // Create unique labels using the global counter
            String loopEndLabel = "tc_end_" + (typeCheckLabelCounter) ;

            // Iterate over the acceptable types
            for (String type : acceptableTypes) {
                // Compare the operand's type with the current acceptable type
                asm.cmpq("$" + type, "%" + typeRegister);
                

                // If a match is found, jump to the end (bypassing the mismatch error)
                asm.je(loopEndLabel);
            }
        
            // If no match is found, jump to the common type mismatch error handling label
            asm.jmp("type_mismatch");
            asm.addErrorHandling("type_mismatch");
            // Label to jump to if a type match is found
            asm.label(loopEndLabel);
        }


        private void generateTypeCheck(X86_64 asm, String operandAddress, String[] acceptableTypes, String typeRegister) {
            // Increment the global label counter to ensure uniqueness
            typeCheckLabelCounter++;    
            // Load the type of the operand into the specified type register (e.g., %r9)
            asm.movq("0(" + operandAddress + ")", "%" + typeRegister);
        
            // Create unique labels using the global counter
            String loopEndLabel = "tc_end_" + typeCheckLabelCounter;

            // Iterate over the acceptable types
            for (String type : acceptableTypes) {
                // Compare the operand's type with the current acceptable type
                asm.cmpq("$" + type, "%" + typeRegister);
        
                // If a match is found, jump to the end (bypassing the mismatch error)
                asm.je(loopEndLabel);
            }
        
            // If no match is found, jump to the common type mismatch error handling label
            asm.jmp("type_mismatch");
            
            // does this get executed if no type match is found ?!
            // Label to jump to if a type match is found
            asm.label(loopEndLabel);
        }


        private void generateBoolCheckCode(X86_64 asm, String operandAddress) {
            // Label for checking if value is "truthy" or "falsey"
            String trueLabel = "is_true_" + (++typeCheckLabelCounter);
            String endLabel = "end_bool_check_" + typeCheckLabelCounter;
        
            // First, we need to check if the operand is None, False, or 0, as these are "falsey"
            // Load the type tag from the heap-allocated space
            asm.movq("0(" + operandAddress + ")", "%r8"); // Load type tag into %r10 for comparison
        
            // Check if type is None (0) or False (1)
            asm.cmpq("$0", "%r8"); // Check for None
            asm.je(trueLabel); // Jump if None (considered false, so we skip to setting false)
            asm.cmpq("$1", "%r8"); // Check for False
            asm.je(trueLabel); // Jump if False
        
            // For integers (2), strings (3), and lists (4), we need to check their values
            // Check if it's an integer with value 0
            asm.cmpq("$2", "%r8"); // Check if it's an integer
            asm.jne("check_string_list"); // If not, jump to checking for string or list
            asm.movq("8(" + operandAddress + ")", "%r8"); // Load integer value
            asm.testq("%r11", "%r8"); // Test if it's 0
            asm.jz(trueLabel); // If 0, it's falsey
        
            asm.label("check_string_list");
            // Additional checks for string length or list length could be added here
            // Skipping directly to true for simplicity in this example
            asm.jmp(endLabel);
        
            // Set %rax to 0 for falsey values
            asm.label(trueLabel);
            asm.movq("$0", "%rax"); // Set to 0 (false)
            asm.jmp("post_bool_check");
        
            // Set %rax to 1 for truthy values
            asm.label(endLabel);
            asm.movq("$1", "%rax"); // Set to 1 (true)
        
            asm.label("post_bool_check");
            // Continue with the rest of the code
        }

        /* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  end  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% */        


    } //Here we end the CodeGenerationVisitor



    // Ensure each AST node class (e.g., TEbinop, TEunop, TEcst, and others not shown here) 
    // implements an accept method that takes a TVisitor and calls the appropriate visit method.
}



