package sim6800.cpu;

// glue between the opcode table and ALU: picks the accumulator, fetches the
// operand, calls the ALU method and stores the result. Cpu.execute() sends
// every mnemonic it does not handle itself here.
// ea is the operand address, or -1 for accumulator forms like ASLA.
final class DataInstructions {

    static void execute(Cpu cpu, String m, int ea) {
        ALU alu = cpu.alu;
        switch (m) {
            // loads, stores, transfers
            case "LDAA": cpu.a = alu.LDA(ea); break;
            case "LDAB": cpu.b = alu.LDA(ea); break;
            case "STAA": alu.STAA(ea); break;
            case "STAB": alu.STAB(ea); break;
            case "LDX":  cpu.x = alu.LDX(ea); break;
            case "STX":  alu.STX(ea); break;
            case "LDS":  cpu.sp = alu.LDS(ea); break;
            case "STS":  alu.STS(ea); break;
            case "TAB":  cpu.b = alu.TAB(cpu.a); break;
            case "TBA":  cpu.a = alu.TBA(cpu.b); break;

            // arithmetic with a memory operand
            case "ADDA": cpu.a = alu.ADD(cpu.a, cpu.read8(ea)); break;
            case "ADDB": cpu.b = alu.ADD(cpu.b, cpu.read8(ea)); break;
            case "ADCA": cpu.a = alu.ADC(cpu.a, cpu.read8(ea)); break;
            case "ADCB": cpu.b = alu.ADC(cpu.b, cpu.read8(ea)); break;
            case "SUBA": cpu.a = alu.SUB(cpu.a, cpu.read8(ea)); break;
            case "SUBB": cpu.b = alu.SUB(cpu.b, cpu.read8(ea)); break;
            case "SBCA": cpu.a = alu.SBC(cpu.a, cpu.read8(ea)); break;
            case "SBCB": cpu.b = alu.SBC(cpu.b, cpu.read8(ea)); break;
            case "CMPA": alu.CMP(cpu.a, cpu.read8(ea)); break;
            case "CMPB": alu.CMP(cpu.b, cpu.read8(ea)); break;
            case "CPX":  alu.CPX(cpu.x, cpu.read16(ea)); break;

            // arithmetic between registers
            case "ABA":  cpu.a = alu.ABA(cpu.a, cpu.b); break;
            case "SBA":  cpu.a = alu.SBA(cpu.a, cpu.b); break;
            case "CBA":  alu.CBA(cpu.a, cpu.b); break;
            case "DAA":  cpu.a = alu.DAA(cpu.a); break;
            case "INX":  cpu.x = alu.INX(cpu.x); break;
            case "DEX":  cpu.x = alu.DEX(cpu.x); break;

            // logic with a memory operand
            case "ANDA": cpu.a = alu.AND(cpu.a, cpu.read8(ea)); break;
            case "ANDB": cpu.b = alu.AND(cpu.b, cpu.read8(ea)); break;
            case "ORAA": cpu.a = alu.ORA(cpu.a, cpu.read8(ea)); break;
            case "ORAB": cpu.b = alu.ORA(cpu.b, cpu.read8(ea)); break;
            case "EORA": cpu.a = alu.EOR(cpu.a, cpu.read8(ea)); break;
            case "EORB": cpu.b = alu.EOR(cpu.b, cpu.read8(ea)); break;
            case "BITA": alu.BIT(cpu.a, cpu.read8(ea)); break;
            case "BITB": alu.BIT(cpu.b, cpu.read8(ea)); break;

            // single operand: A form, B form, memory form
            case "INCA": cpu.a = alu.INC(cpu.a); break;
            case "INCB": cpu.b = alu.INC(cpu.b); break;
            case "INC":  cpu.write8(ea, alu.INC(cpu.read8(ea))); break;
            case "DECA": cpu.a = alu.DEC(cpu.a); break;
            case "DECB": cpu.b = alu.DEC(cpu.b); break;
            case "DEC":  cpu.write8(ea, alu.DEC(cpu.read8(ea))); break;
            case "NEGA": cpu.a = alu.NEG(cpu.a); break;
            case "NEGB": cpu.b = alu.NEG(cpu.b); break;
            case "NEG":  cpu.write8(ea, alu.NEG(cpu.read8(ea))); break;
            case "COMA": cpu.a = alu.COM(cpu.a); break;
            case "COMB": cpu.b = alu.COM(cpu.b); break;
            case "COM":  cpu.write8(ea, alu.COM(cpu.read8(ea))); break;
            case "CLRA": cpu.a = alu.CLR(cpu.a); break;
            case "CLRB": cpu.b = alu.CLR(cpu.b); break;
            case "CLR":  cpu.write8(ea, alu.CLR(0)); break;
            case "TSTA": alu.TST(cpu.a); break;
            case "TSTB": alu.TST(cpu.b); break;
            case "TST":  alu.TST(cpu.read8(ea)); break;

            // shifts and rotates
            case "ASLA": cpu.a = alu.ASL(cpu.a); break;
            case "ASLB": cpu.b = alu.ASL(cpu.b); break;
            case "ASL":  cpu.write8(ea, alu.ASL(cpu.read8(ea))); break;
            case "ASRA": cpu.a = alu.ASR(cpu.a); break;
            case "ASRB": cpu.b = alu.ASR(cpu.b); break;
            case "ASR":  cpu.write8(ea, alu.ASR(cpu.read8(ea))); break;
            case "LSRA": cpu.a = alu.LSR(cpu.a); break;
            case "LSRB": cpu.b = alu.LSR(cpu.b); break;
            case "LSR":  cpu.write8(ea, alu.LSR(cpu.read8(ea))); break;
            case "ROLA": cpu.a = alu.ROL(cpu.a); break;
            case "ROLB": cpu.b = alu.ROL(cpu.b); break;
            case "ROL":  cpu.write8(ea, alu.ROL(cpu.read8(ea))); break;
            case "RORA": cpu.a = alu.ROR(cpu.a); break;
            case "RORB": cpu.b = alu.ROR(cpu.b); break;
            case "ROR":  cpu.write8(ea, alu.ROR(cpu.read8(ea))); break;

            default:
                throw new UnsupportedOperationException("not implemented: " + m);
        }
    }

    private DataInstructions() { }
}
