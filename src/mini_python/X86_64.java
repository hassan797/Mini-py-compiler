package mini_python;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
// import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/** a label (Lab) or an instruction (Asm) */
abstract class LabelAsm {
  String s;
}

class Lab extends LabelAsm {
  Lab(String s) {
    this.s = s;
  }
}

class Asm extends LabelAsm {
  Asm(String s) {
    this.s = s;
  }
}

/** Assembly Program x86-64, AT&T */
public class X86_64 {

  // %%%%%%%%%%%%%%%%%  BEGIN MYWRAPPERS  %%%%%%%%%%%%%%%%%%%%
  public void myPrintfWrapper() {
    label("my_printf");
    pushq("%rbp");
    movq("%rsp", "%rbp");

    // Align stack to 16-byte boundary
    andq("$-16", "%rsp");
    // Assuming %rdi, %rsi, %rdx, ... hold printf arguments
    call("printf");

    movq("%rbp", "%rsp");
    popq("%rbp");
    ret();

/*my_printf:
	pushq %rbp
	movq %rsp, %rbp
	andq $-16, %rsp # 16-byte stack alignment
	call printf
	movq %rbp, %rsp
	popq %rbp
	ret */

}
  public void myMallocWrapper() {
    label("my_malloc");
    pushq("%rbp");
    movq("%rsp", "%rbp");  
    andq("$-16", "%rsp"); // 16-byte stack alignment
    call("malloc");
    movq("%rbp","%rsp");
    popq("%rbp");
    ret();
  }

  /*
   * my_malloc:
pushq %rbp
movq %rsp, %rbp
andq $-16, %rsp # 16-byte stack alignment
call malloc
movq %rbp, %rsp
popq %rbp
ret
   */

// %%%%%%%%%%%%%%%%%  END   MYWRAPPERS   %%%%%%%%%%%%%%%%%%%%

// %%%%%%%%%%%%%%%%%  BEGIN   USEFUL   %%%%%%%%%%%%%%%%%%%%

 public void startFunction(String functionName) {
  // Emit the .global and .type directives for the function main
  if (functionName.equals("main")){
    globl(functionName);
    emitNoTab(".type " + functionName + ", @function");
  }
  // Start the function definition with its label
  label(functionName);
}

public void addErrorHandling(String error) {
  // Check if the type mismatch error handling is used in the code
    // Check if error message is already in dataEntries to avoid duplicates
    if (!dataEntries.contains("typeMismatchMsg")) {
        // Add the error message string to the .data section
        this.dlabel("typeMismatchMsg");
        this.data(".string \"Type mismatch error.\\n\"");
        dataEntries.add("typeMismatchMsg");
        // Add type_mismatch label and instructions to the .text section for error handling
        this.label("type_mismatch");
        this.leaq("typeMismatchMsg(%rip)", "%rdi"); // Load address of the error message
        this.call("printf"); // Call printf to print the error message
        this.movq("$60", "%rdi"); // Use syscall number for exit in %rdi (Linux specific)
        this.movq("$1", "%rsi"); // Status code 1 indicates an error
        this.call("exit"); // Call exit to terminate the program
    }
  // Handle division by zero error
    if (!dataEntries.contains("divisionByZeroMsg")) {
        this.dlabel("divisionByZeroMsg");
        this.data(".string \"Division by zero error.\\n\"");
        dataEntries.add("divisionByZeroMsg");
        this.label("division_by_zero");
        this.leaq("divisionByZeroMsg(%rip)", "%rdi");
        this.call("printf");
        this.movq("$60", "%rdi"); // Again, using syscall number for exit in %rdi (Linux specific)
        this.movq("$1", "%rsi"); // Status code 1 indicates an error
        this.call("exit");
    }

    

  }

  
public void print_typed() {

  label("print_typed");
  
  pushq("%rbp");
  movq("%rsp", "%rbp");

  movq("16(%rbp)", "%rdi");

  movq("8(%rdi)", "%rax");

  // Handle None Type Printing
  cmpq("$0", "(%rdi)");
  jne("check_bool");  //if not None jump to next check and print
  leaq("noneString(%rip)", "%rdi");
  xorq("%rax", "%rax");
  call("my_printf");
  jmp("end_check");

  // Handle Bool Type Printing
  label("check_bool");
  cmpq("$1", "(%rdi)"); //if not Bool jump to next check and print
  jne("check_int");
  
  leaq("trueString(%rip)", "%rdi");
  cmpq("$1","%rax"); 
  jne("check_bool2");
  leaq("falseString(%rip)","%rdi");
  label("check_bool2");
  xorq("%rax", "%rax");
  call("my_printf");
  
  jmp("end_check");
  
  // Handle Int Type Printing
  label("check_int");
  cmpq("$2", "(%rdi)");
  jne("check_string"); //if not Int jump to next check and print

  leaq("intFormatString(%rip)", "%rdi");
  movq("%rax", "%rsi");
  call("my_printf");
  jmp("end_check");
  
  // Handle String Type Printing
  label("check_string");
  cmpq("$3", "(%rdi)"); //if not String jump to next check and print
  jne("check_list");

  addq("$16","%rax");
  leaq("strFormatString(%rip)","%rdi");
  call("my_printf");

  jmp("end_check");

  // Handle List Type Printing 
              
  // =======================================================================================

  label("check_list");
  cmpq("$4", "(%rdi)");
  jne("type_mismatch"); //if not List jump to type_mismatch

  // Print list start
  leaq("listStart(%rip)", "%rdi");
  call("my_printf");

  // Calculate address of the first element
  movq("%rdi", "%rbx");  // Copy base address of list to %rbx for iteration
  addq("$16", "%rbx");   // Move to first element

  // Loop through each element
  movq("%rax", "%rcx");  // Copy list length to %rcx for counter

  label("loop_list");

  cmpq("$0", "%rcx");
  je("end_list");  // Jump to end if we've processed all elements

  // Print current element - assuming print_typed can be called directly
  
  pushq("%rcx"); // Push current loop counter onto the stack for print_typed
  pushq("%rbx"); // Push current element's pointer onto the stack for print_typed
  pushq("(%rbx)"); // Push current element's address onto the stack for print_typed

  call("print_typed"); //print whatever is on the top of the stack recursively

  popq("%rbx"); // Pop current element's pointer onto the stack for print_typed
  popq("%rcx");// Pop current loop counter onto the stack for print_typed

  addq("$8", "%rbx");  // Move to next element address

  // Decrement counter and print separator if not last element
  decq("%rcx");
  je("end_list_elements");  // Jump to end_list_elements if this was the last one
  // Print separator
  leaq("listSeparator(%rip)", "%rdi");
  call("my_printf");
  jmp("loop_list");

  label("end_list_elements");
  // Print list end
  leaq("listEnd(%rip)", "%rdi");
  call("my_printf");

  label("end_list");
  
  // =======================================================================================

  label("end_check");

  leave();
  ret();

}


// Modify the method to accept a type Enum or String
public void printNewline() {
  ensureNewlineString(); // Make sure the newline string is defined
  leaq("newlineString(%rip)", "%rdi"); // Load address of the newline string into RDI
  xorq("%rax", "%rax"); // Clear RAX since printf is a variadic function
  call("my_printf"); // Call printf to print the newline
}

public void ensureNewlineString() {
  if (!dataEntries.contains("newlineString")) {
      dlabel("newlineString");
      data(".string \"\\n\"");
      dataEntries.add("newlineString"); // Mark this label as added
  }
}

public void addDataEntryForType(String type) {
  String label = null;
  String value = null;

  switch (type) {
      case "Int":
          label = "intFormatString";
          value = ".string \"%ld\"";
          break;
      case "String":
          label = "strFormatString";
          value = ".string \"%s\"";
          break;
      case "Bool":
      // Add "True" and "False" strings to the data section if not already present
      if (!dataEntries.contains("trueString")) {
          data.append("trueString: .string \"True\"\n");
          dataEntries.add("trueString");
          data.append(" printTrue: \r\n"+     //
                        "leaq trueString(%rip), %rdi \r\n" + //
                        "call my_printf              \r\n" + //
                        "ret\r\n" + //
                                                            "");
          dataEntries.add("printTrue");
          
          data.append("return_true:  \n"+
                          "movq $16, %rdi  \n"+
                          "call my_malloc  \n"+
                          "movq $1, (%rax)  \n"+ 
                          "movq $1, 8(%rax)  \n"+  
                          "pushq %rax  \n"+       
                          "ret\r\n" + //
                                                                ""  );
          dataEntries.add("return_true") ;                  
 
        }
      
      if (!dataEntries.contains("falseString")) {
          data.append("falseString: .string \"False\"\n");
          dataEntries.add("falseString");
          data.append(" printFalse: \r\n"+     //
                        "leaq falseString(%rip), %rdi \r\n" + //
                        "call my_printf              \r\n" + //
                        "ret\r\n" + //
                                                            "");
          dataEntries.add("printFalse") ;

          data.append("return_false:  \r\n"+
                          "movq $16, %rdi  \r\n"+
                          "call my_malloc  \r\n"+
                          "movq $1, (%rax)  \r\n"+ 
                          "movq $0, 8(%rax)  \r\n"+  
                          "pushq %rax  \r\n"+       
                          "ret\r\n" + //
                                                                ""  );
          dataEntries.add("return_false") ;  
          
      }
      break;
      // Add more cases for other types if needed
      case "None":
          // Assuming you want to print something for None type
          label = "noneString";
          value = ".string \"None\"";
          break;
      case "List":
          // No specific label or value needed here since list printing is more complex
          // Ensure the data section contains the strings needed for list formatting
          if (!dataEntries.contains("listStart")) {
            data.append("listStart: .string \"[ \" \n");
            dataEntries.add("listStart");
          }
          if (!dataEntries.contains("listEnd")) {
            data.append("listEnd: .string \" ]\" \n");
            dataEntries.add("listEnd");
          }
          if (!dataEntries.contains("listSeparator")) {
            data.append("listSeparator: .string \", \"\n");
            dataEntries.add("listSeparator");
          }
          break;
      default:
          // Handle unknown types or throw an error
          System.err.println("Unknown type for format string: " + type);
          return;
  }

  // Now, add the determined label and value if they haven't been added already
  if (label != null && value != null && !dataEntries.contains(label)) {
      data.append(label + ": " + value + "\n");
      dataEntries.add(label); // Mark this label as added
  }
}

// %%%%%%%%%%%%%%%%%  END   USEFUL   %%%%%%%%%%%%%%%%%%%%

 
  /** code segment */
  private LinkedList<LabelAsm>	text;
  private StringBuffer            inline;
  private Set<String> dataEntries = new HashSet<>(); //  Initialize the dataEntries as empty
  private Set<String> textEntries = new HashSet<>(); // Initialize the textEntries as empty
  /** data segment */   
  private StringBuffer			data;

  X86_64() {
    this.text = new LinkedList<>();
    this.inline = new StringBuffer();
    this.data = new StringBuffer();
  }

  /** adds a new instruction to the end of the code */
  X86_64 emit(String s) {
    this.text.add(new Asm("\t" + s + "\n"));
    return this;
  }
  X86_64 emitNoTab(String s) {
    this.text.add(new Asm(s + "\n"));
    return this;
  }
  /**
   * adds a label (for example, the label
   * of a function)
   */
  X86_64 label(String s) {
    this.text.add(new Lab(s));
    return this;
  }

  /**
   * adds assembler to the end of the text area
   * (for example, to add primitives written in assembler)
   */
  X86_64 inline(String s) {
    this.inline.append(s);
    return this;
  }

  X86_64 movb(String op1, String op2) {
    return emit("movb " + op1 + ", " + op2);
  }

  X86_64 movq(String op1, String op2) {
    return emit("movq " + op1 + ", " + op2);
  }

  X86_64 movq(int n, String op) {
    return movq("$" + n, op);
  }

  X86_64 movzbq(String op1, String op2) {
    return emit("movzbq " + op1 + ", " + op2);
  }

  X86_64 incq(String op) {
    return emit("incq " + op);
  }

  X86_64 decq(String op) {
    return emit("decq " + op);
  }

  X86_64 negq(String op) {
    return emit("negq " + op);
  }

  X86_64 addq(String op1, String op2) {
    return emit("addq " + op1 + ", " + op2);
  }

  X86_64 subq(String op1, String op2) {
    return emit("subq " + op1 + ", " + op2);
  }

  X86_64 imulq(String op1, String op2) {
    return emit("imulq " + op1 + ", " + op2);
  }

  X86_64 idivq(String op) {
    return emit("idivq " + op);
  }

  X86_64 cqto() {
    return emit("cqto");
  }

  X86_64 leaq(String op1, String op2) {
    return emit("leaq " + op1 + ", " + op2);
  }

  X86_64 notq(String op) {
    return emit("notq " + op);
  }

  X86_64 andq(String op1, String op2) {
    return emit("andq " + op1 + ", " + op2);
  }

  X86_64 orq(String op1, String op2) {
    return emit("orq " + op1 + ", " + op2);
  }

  X86_64 xorq(String op1, String op2) {
    return emit("xorq " + op1 + ", " + op2);
  }

  X86_64 shlq(String op1, String op2) {
    return emit("shlq " + op1 + ", " + op2);
  }

  X86_64 shrq(String op1, String op2) {
    return emit("shrq " + op1 + ", " + op2);
  }

  X86_64 sarq(String op1, String op2) {
    return emit("sarq " + op1 + ", " + op2);
  }

  X86_64 pushq(String op) {
    return emit("pushq " + op);
  }

  X86_64 popq(String op) {
    return emit("popq " + op);
  }

  X86_64 ret() {
    return emit("ret");
  }

  X86_64 leave() {
    return emit("leave");
  }

  X86_64 call(String s) {
    return emit("call " + s);
  }

  X86_64 callstar(String op) {
    return emit("call *" + op);
  }

  X86_64 jmp(String s) {
    return emit("jmp " + s);
  }

  X86_64 jmpstar(String op) {
    return emit("jmp *" + op);
  }

  X86_64 cmpb(String op1, String op2) {
    return emit("cmpb " + op1 + ", " + op2);
  }

  X86_64 cmpb(int n, String op) {
    return cmpb("$" + n, op);
  }

  X86_64 cmpw(String op1, String op2) {
    return emit("cmpw " + op1 + ", " + op2);
  }

  X86_64 cmpw(int n, String op) {
    return cmpw("$" + n, op);
  }

  X86_64 cmpl(String op1, String op2) {
    return emit("cmpl " + op1 + ", " + op2);
  }

  X86_64 cmpl(int n, String op) {
    return cmpl("$" + n, op);
  }

  X86_64 cmpq(String op1, String op2) {
    return emit("cmpq " + op1 + ", " + op2);
  }

  X86_64 cmpq(int n, String op) {
    return cmpq("$" + n, op);
  }

  X86_64 testq(String op1, String op2) {
    return emit("testq " + op1 + ", " + op2);
  }

  X86_64 testq(int n, String op) {
    return testq("$" + n, op);
  }

  X86_64 je(String s) {
    return emit("je " + s);
  }

  X86_64 jz(String s) {
    return emit("jz " + s);
  }

  X86_64 jne(String s) {
    return emit("jne " + s);
  }

  X86_64 jnz(String s) {
    return emit("jnz " + s);
  }

  X86_64 js(String s) {
    return emit("js " + s);
  }

  X86_64 jns(String s) {
    return emit("jns " + s);
  }

  X86_64 jg(String s) {
    return emit("jg " + s);
  }

  X86_64 jge(String s) {
    return emit("jge " + s);
  }

  X86_64 jl(String s) {
    return emit("jl " + s);
  }

  X86_64 jle(String s) {
    return emit("jle " + s);
  }

  X86_64 ja(String s) {
    return emit("ja " + s);
  }

  X86_64 jae(String s) {
    return emit("jae " + s);
  }

  X86_64 jb(String s) {
    return emit("jb " + s);
  }

  X86_64 jbe(String s) {
    return emit("jbe " + s);
  }

  X86_64 sete(String s) {
    return emit("sete " + s);
  }

  X86_64 setz(String s) {
    return emit("setz " + s);
  }

  X86_64 setne(String s) {
    return emit("setne " + s);
  }

  X86_64 setnz(String s) {
    return emit("setnz " + s);
  }

  X86_64 sets(String s) {
    return emit("sets " + s);
  }

  X86_64 setns(String s) {
    return emit("setns " + s);
  }

  X86_64 setg(String s) {
    return emit("setg " + s);
  }

  X86_64 setge(String s) {
    return emit("setge " + s);
  }

  X86_64 setl(String s) {
    return emit("setl " + s);
  }

  X86_64 setle(String s) {
    return emit("setle " + s);
  }

  X86_64 seta(String s) {
    return emit("seta " + s);
  }

  X86_64 setae(String s) {
    return emit("setae " + s);
  }

  X86_64 setb(String s) {
    return emit("setb " + s);
  }

  X86_64 setbe(String s) {
    return emit("setbe " + s);
  }

  /** .data segment */

  private static String escaped(String s) {
    StringBuffer b = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\n')
        b.append("\\n");
      else
        b.append(c);
    }
    return b.toString();
  }

  /** add a label inside the .data segment */
  X86_64 dlabel(String s) {
    this.data.append(s + ":\n");
    return this;
  }

  X86_64 data(String s) {
    this.data.append("\t" + s + "\n");
    return this;
  }

  X86_64 string(String s) {
    return data(".string \"" + escaped(s) + "\"");
  }

  X86_64 space(int n) {
    return data(".space " + n);
  }

  X86_64 quad(long l) {
    return data(".quad " + l);
  }

  X86_64 globl(String l) {
    return emitNoTab(".globl " + l);
  }

  /** Print the assembly .s code into the folder */
  void printToFile(String file) {
    try {
      Writer writer = new FileWriter(file);
      writer.write("\t.text\n");
      for (LabelAsm lasm : this.text) {
        if (lasm instanceof Lab) {
          writer.write(lasm.s + ":\n");
        } else
          writer.write(lasm.s);
      }
      writer.write(this.inline.toString());
      writer.write("\t.data\n");
      writer.write(this.data.toString());
      writer.close();
    } catch (IOException e) {
      throw new Error("Cannot Write to " + file);
    }
  }

}
