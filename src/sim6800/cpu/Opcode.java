package sim6800.cpu;

// One row of the opcode table: what the byte means, how its operand is
// addressed, and how many bytes the whole instruction occupies.
public record Opcode(String mnemonic, Mode mode, int bytes) { }
