rem This script finds the files that are modified
echo off
pushd ..
cvs status . 2> test.txt | find "Locally Modified"
cvs status . 2> test.txt | find "Locally Added"
cvs status . 2> test.txt | find "Locally Removed"
del test.txt
popd
echo on
