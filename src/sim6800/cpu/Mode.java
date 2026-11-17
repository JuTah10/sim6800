package sim6800.cpu;

// The six 6800 addressing modes (datasheet "Addressing Modes").
// Accumulator-only forms (ASLA, CLRB...) are treated as INH.
public enum Mode {
    INH,   // inherent: no operand byte
    IMM,   // immediate: operand is the next byte(s)
    DIR,   // direct: next byte is an address in page zero (0000-00FF)
    EXT,   // extended: next two bytes are the full 16-bit address
    IDX,   // indexed: next byte is an unsigned offset added to X
    REL    // relative: next byte is a signed offset added to PC (branches)
}
