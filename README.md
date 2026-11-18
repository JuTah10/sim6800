# COSC 3P92 Final Project Phase 2 - Motorola 6800 simulator

Alena Mariam Binu, Gus Hearty, Vu Nguyen

## How to run

IntelliJ project, JDK 21. Open the folder and run `sim6800.ui.Main`. It is a numbered menu:

```
1  Load S19 into RAM        5  View CPU registers (A B X SP PC CCR)
2  Load S19 into ROM        6  View memory (hex start address, byte count)
3  Run (until WAI)          7  Clear RAM (ROM is kept)
4  Step one instruction     8  Reset CPU (PC from the vector at FFFE)
                            9  Quit
```

Type the number, then the file path or address when asked. Paths are relative to the project folder.
Run and step stop with a message on an illegal opcode.

Sample files in `samples/`:
- `sample_rom.s19` - load as ROM. Vector table at FFF8-FFFF, all pointing to FC00, where a `JMP $0000` starts the user program.
- `sample_ram.s19` - load as RAM. The handout test program (`6800b1.asm`): loads 04, branches, stores to 00B1, WAI.

Load the ROM file first, then the RAM file, then 3 to run. Registers show A = 04, PC = 0009, and memory 00B0 shows `FF 04 FF FF`.

## Where things are

```
src/sim6800/cpu      Cpu (registers, flags, bus, stack, step/run), OpcodeTable, Mode, Opcode,
                     DataInstructions (mnemonic -> ALU), ALU (operations and condition codes)
src/sim6800/memory   Memory interface, Ram (RAM, ROM at FC00-FFFF, I/O port hooks), IoPort, S19Loader
src/sim6800/ui       Main (the menu)
samples/             the two S19 files
```
