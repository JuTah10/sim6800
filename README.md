# Feel free to correct or add anything.

## Layout

```
src/sim6800/memory   Memory interface, Ram (RAM + ROM + I/O hooks), IoPort, S19Loader
src/sim6800/cpu      Cpu, OpcodeTable, Mode, Opcode, DataInstructions, Part2Test
src/sim6800/ui       Main: menu driver (load, run, step, regs, mem, clear, reset, quit)
samples/             sample_rom.s19 (vector table + JMP $0000), sample_ram.s19 (handout program)
```

Run `sim6800.ui.Main`. Load `samples/sample_rom.s19` as ROM first, then a program as RAM.
`Part2Test` has its own main and checks the control-flow instructions by hand.

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
- Illegal opcode throws `IllegalStateException` with the address, the UI shows it as "CPU stopped".

### Gus - data, arithmetic, logic (and their flags) (done)
- `ALU`: one method per operation (LDA, ADD, SUB, CMP, INC, COM, ASL, ROL ...) that takes the
  operand values, sets the flags from the datasheet condition-code column and returns the result.
- `DataInstructions`: glue. Maps each mnemonic from the opcode table to the ALU method,
  picks A or B, reads the operand at `ea` and stores the result back in the register or memory.

The handout sample (`samples/sample_ram.s19`, same bytes as `6800b1.s19`) runs to WAI with 04 at 00B1.

## Memory, S19 Loader and UI
For the memory part, I updated the Ram class to support both RAM and ROM. The CPU can read and write to RAM, but it 
can't overwrite ROM normally. I also added basic support for I/O devices that will be used later.

The S19Loader class reads S19 files and loads the data into memory. It supports S0, S1 and S9 records.

For the UI, I made a simple text-based menu where the user can load S19 files, view memory, check CPU registers, clear RAM 
and reset the CPU. Run and step go until WAI, or stop with a message at the first instruction that is not implemented yet.

I tested the program using two sample S19 files.

The RAM file loaded 16 bytes, and address 00A0 showed 04 03 05.
The ROM file loaded 11 bytes, and address FC00 showed 7E 00 00.
After resetting the CPU, the PC showed FC00.
I also tested ROM protection by trying to overwrite a ROM address and clearing RAM. The ROM values stayed the same.

To load the sample files, use:

samples/sample_ram.s19 for RAM
samples/sample_rom.s19 for ROM


