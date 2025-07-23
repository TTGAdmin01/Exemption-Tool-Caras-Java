cvs add *.properties
cvs add *.xml
cvs add *.bat

pushd ..\nbproject
cvs add *.properties
cvs add *.xml
cvs add *.bat

cd private
cvs add *.properties
cvs add *.xml
popd





pushd ..\lib
cvs add -kb *.jar
cvs add *.txt 
cvs add *.properties
cd CopyLibs
cvs add -kb *.jar
cd ..\hamcrest
cvs add -kb *.jar
cd ..\junit_4
cvs add -kb *.jar

popd

pushd ..\src
cvs add *.java
cd com
cvs add *.java
cd ttg
cvs add *.java
cd certificate_upload
cvs add *.java *.form
cd beans
cvs add *.java
cd ..\certcapture_api
cvs add *.java
cd ..\utils
cvs add *.java

pushd ..\scripts
cvs add *.bat
cvs add *.properties

popd


pushd ..\doc
cvs add -kb *.doc
cvs add -kb *.pdf 
cvs add -kb *.docx 
cvs add -kb *.xlsx

popd



pushd ..\scripts
cvs add *.bat
cvs add *.properties
popd
