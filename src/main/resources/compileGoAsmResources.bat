@echo off

REM Output file
SET file=com.zygateley.compiler
REM Creating a Newline variable (the two blank lines are required!)
set NLM=^


set NL=^^^%NLM%%NLM%^%NLM%%NLM%

if exist %file%.asm del %file%.asm
if exist %file%.obj del %file%.obj
if exist GoAsm\%file%.dll del %file%.dll

@echo Code Section >> %file%.tmp
@echo:


for /r %%i in (GoAsm\resources\*.asm) do (echo Include "%%i" & type "%%i" >> %file%.tmp & @echo: >> %file%.tmp & @echo: >> %file%.tmp)

move %file%.tmp %file%.asm


REM Compile w/ GoAsm
call "GoAsm/GoAsm" /fo GoAsm\%file%.obj %file%.asm
REM call "GoAsm/GoLink" /console /dll ./%file%.obj /fo GoAsm\%file%.dll kernel32.dll

del %file%.asm

echo:
echo Resource compilation complete!
echo:
