rm *.out
make
FILEPATH="../step7_tc/inputs/*.micro"
mkdir temp
for f in $FILEPATH
do
	g=$(basename $f)
	filename="${g%.*}"
	echo "Processing $filename"
	#cat $f
	#java -cp lib/antlr.jar:classes/ Micro $f
	java -cp lib/antlr.jar:classes/ Micro $f > temp/$filename.out
	#diff -y ../step7_tc/outputs/$filename.out $filename.out
	tiny4R ../step7_tc/outputs/$filename.out < ../step7_tc/inputs/$filename.input

	read -rsp $'Press any key to compile the file...\n' -n1 key

	tiny4R temp/$filename.out < ../step7_tc/inputs/$filename.input
	read -rsp $'Press any key to continue...\n' -n1 key
	clear
done
