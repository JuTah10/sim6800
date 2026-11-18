# Feel free to correct or add anything.

## Layout

```
src/sim6800/memory   Memory interface, Ram            (address space)
src/sim6800/cpu      Cpu: registers, flags, bus, stack, reset, step/run
src/sim6800/ui       Main: test driver                (uses the Cpu, is not part of it)
```

Run `sim6800.ui.Main`. Right now it only checks that reset works
(prints pc = 0010, ccr = D0, I = true).

## What we should follow

- All values are `int`. 8-bit things are masked `& 0xFF`, 16-bit `& 0xFFFF`. Never use `byte`.
- Never touch `ccr` directly. Use `flag(mask)`, `setFlag(mask, on)`, `setNZ8(v)`, `setNZ16(v)`.
- Never call `memory.read/write` from instruction code. Use `read8/read16/write8/write16/fetch8/fetch16`.
- Stack: `push8/pull8/push16/pull16` only. SP points at the next free byte.
- The 6800 is big-endian: high byte at the lower address. The helpers already do this.
- `git pull` before you start, commit only code that compiles, `git push` when a piece works.

### Alena - memory, loader, UI

### Vu - CPU core, control flow, interrupts (done)
- `OpcodeTable`: all 197 opcodes -> mnemonic, addressing mode, length (`Mode` enum, `Opcode` record).
- `Cpu.operandAddress()`: immediate, direct, extended, indexed, relative.
- `Cpu.step()`: fetch, decode, `execute()`. `run()` goes until WAI.
- Branches, JMP, JSR, BSR, RTS, stack ops, TAP/TPA, flag set/clear, NOP, SWI, WAI, RTI.
- SWI/WAI push the 7-byte frame in datasheet order (PC, X, A, B, CCR), RTI pulls it.
  The IRQ/NMI pins are not simulated, nothing in this phase can drive them.
- Illegal opcode throws `IllegalStateException` with the address; the UI should catch and show it.

### Gus - data, arithmetic, logic (and their flags)
Goes in `cpu/DataInstructions.java`. `Cpu.execute()` already sends every mnemonic it
does not own to `DataInstructions.execute(cpu, mnemonic, ea)` with the operand address
computed (`ea = -1` for accumulator forms like ASLA).
Operand: `cpu.read8(ea)` / `cpu.write8(ea, v)`. Registers: `cpu.a cpu.b cpu.x cpu.sp`.
Flags: `cpu.setFlag(Cpu.C, ...)`, `cpu.setNZ8(v)`. Keep 8-bit results `& 0xFF`.
- Loads/stores/transfers: LDAA LDAB STAA STAB LDX STX LDS STS TAB TBA.
- Arithmetic: ADDA ADDB ADCA ADCB ABA SUBA SUBB SBCA SBCB SBA CMPA CMPB CBA CPX INC DEC NEG DAA INX DEX.
- Logic/test: ANDA ANDB ORAA ORAB EORA EORB BITA BITB COM CLR TST.
- Shifts/rotates: ASL ASR LSR ROL ROR (accumulator and memory forms).
- H, V, C rules per instruction come from the datasheet condition-code column. Write a small .s19 test per group.

Everyone: test your own instructions with .s19 files through the UI, compare against the datasheet.
Sample test from the handout is in `../6800b1.asm` / `../6800b1.s19`.
