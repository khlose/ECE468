rm *.out
make
FILEPATH="../step6_tc/*.micro"
for f in $FILEPATH
do
	g=$(basename $f)
	filename="${g%.*}"
	echo "Processing $filename"
	java -cp lib/antlr.jar:classes/ Micro $f > $filename.out
	diff -y ../step6_tc/$filename.out $filename.out
	read -rsp $'Press any key to compile the file...\n' -n1 key
	tiny $filename.out < ../step6_tc/$filename.input
	read -rsp $'Press any key to continue...\n' -n1 key
done
