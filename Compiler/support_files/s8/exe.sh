rm *.out
make
FILEPATH="../step7_tc/inputs/*.micro"
mkdir temp
for f in $FILEPATH
do
	g=$(basename $f)
	filename="${g%.*}"
	echo "Processing $filename"
	java -cp lib/antlr.jar:classes/ Micro $f > temp/$filename.out
	tiny4R final_testcases/$filename.out < final_testcases/$filename.input

	read -rsp $'Press any key to compile the file...\n' -n1 key

	tiny4R temp/$filename.out < final_testcases/$filename.input
	read -rsp $'Press any key to continue...\n' -n1 key
	clear
done
