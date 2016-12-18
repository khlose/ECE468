rm *.out
make
FILEPATH="self_testcases/*.micro"
mkdir self
for f in $FILEPATH
do
	g=$(basename $f)
	filename="${g%.*}"
	echo "Processing $filename"
	java -cp lib/antlr.jar:classes/ Micro $f > self/$filename.out
	tiny4R self/$filename.out < self_testcases/$filename.input
	read -rsp $'Press any key to continue...\n' -n1 key
	clear
done
