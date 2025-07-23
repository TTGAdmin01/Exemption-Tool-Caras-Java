::
:: Wesco Certificate Upload
::

@echo on
setlocal

::
:: Set Java path
:: Must be jdk 1.8 or higher
::
set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_231"



::
:: Set Java Classpath used by programs
::
set "CLASSPATH=.\TTGCertificateUpload.properties;..\lib\*"
::../lib/commons-codec-1.10.jar;../lib/commons-logging-1.2.jar;../lib/curvesapi-1.03.jar;../lib/junit-4.12.jar;../lib/log4j-1.2.17.jar;../lib/poi-3.14-20160307.jar;../lib/poi-excelant-3.14-20160307.jar;../lib/poi-ooxml-3.14-20160307.jar;../lib/poi-ooxml-schemas-3.14-20160307.jar;../lib/poi-scratchpad-3.14-20160307.jar;../lib/super-csv-2.4.0.jar;../lib/xmlbeans-2.6.0.jar

::
:: Run the upload process
:: Note: Must be running Java 1.8 or higher
::
@echo on
"%JAVA_HOME%\bin\java" -Xms128m -Xmx512m -cp "%CLASSPATH%" ^
     -Dlog4j.configurationFile=..\lib\log4j2.xml ^
	com.ttg.certificate_upload.CertificateUploadUI


endlocal

