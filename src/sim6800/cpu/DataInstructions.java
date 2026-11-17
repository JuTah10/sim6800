package sim6800.cpu;

// Part 3: loads/stores/transfers, arithmetic, logic, shifts and rotates,
// with their condition codes. Cpu.execute() sends every mnemonic it does
// not handle itself here, with the operand address already computed
// (ea = -1 for inherent forms like ASLA, where the operand is the accumulator).
//
// Use the Cpu helpers: cpu.read8(ea) / cpu.write8(ea, v) for the operand,
// cpu.setFlag(Cpu.C, ...) / cpu.setNZ8(v) for flags. Registers are cpu.a,
// cpu.b, cpu.x, cpu.sp (package-private). Keep 8-bit results & 0xFF.
final class DataInstructions {

    static void execute(Cpu cpu, String mnemonic, int ea) {
        switch (mnemonic) {
            // TODO (Part 3): LDAA LDAB STAA STAB LDX STX LDS STS TAB TBA
            // TODO (Part 3): ADDA ADDB ADCA ADCB ABA SUBA SUBB SBCA SBCB SBA CMPA CMPB CBA CPX
            // TODO (Part 3): INCA INCB INC DECA DECB DEC NEGA NEGB NEG DAA INX DEX
            // TODO (Part 3): ANDA ANDB ORAA ORAB EORA EORB BITA BITB COMA COMB COM CLRA CLRB CLR TSTA TSTB TST
            // TODO (Part 3): ASLA ASLB ASL ASRA ASRB ASR LSRA LSRB LSR ROLA ROLB ROL RORA RORB ROR
            default:
                throw new UnsupportedOperationException("not implemented: " + mnemonic);
        }
    }

    private DataInstructions() { }
}
