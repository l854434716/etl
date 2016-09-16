echo off
rem ================================
rem	You must install Java Software Development Kit 1.5 or later!
rem	You must set the path variable UC_HOME and JDK_HOME before running it!
rem	You MAY NOT use spaces in the path names
rem	JDK_HOME is the full path name of the Java Software Development Kit's installation directory, such as D:\jdk1.5.0_19 
rem ================================

set UC_HOME=..

set JDK_HOME=C:\Program Files\Java\jdk1.7.0_45

set JAVA_HOME=%JDK_HOME%
if not exist %JDK_HOME%\lib\tools.jar goto NO_JDK_HOME
if not exist %UC_HOME%\lib goto NO_UC_HOME
set PATH=%UC_HOME%\bin;%PATH%;%JAVA_HOME%\bin
set CLASSPATH=%UC_HOME%\bin;%UC_HOME%\conf;%UC_HOME%\lib
for %%i in (%UC_HOME%\lib\*.jar) do call  appendcp.bat %%i
for %%i in (%UC_HOME%\lib\cdh5.5.1\*.jar) do call  appendcp.bat %%i
rem for %%i in (%UC_HOME%\lib\cdh5.0.0\*.jar) do call  appendcp.bat %%i
rem for %%i in (%UC_HOME%\lib\cdh4.2.0\*.jar) do call  appendcp.bat %%i

goto END

:NO_UC_HOME
echo     Error: UC_HOME variable does not point to your Ultra NMS directory
echo 		installation directory. Please edit your setEnv.bat script. 
pause
goto END

:NO_JDK_HOME
echo warnning: JDK_HOME variable does not point to your J2SDK1.5
echo 	 	installation directory. Please edit your setEnv.bat script. 
pause
goto END

:END
