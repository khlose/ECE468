rm *.out
make
FILEPATH="../step5_tc/input/*.micro"
for f in $FILEPATH
do
	g=$(basename $f)
	filename="${g%.*}"
	echo "Processing $filename"
	java -cp lib/antlr.jar:classes/ Micro $f > $filename.out
	tiny $filename.out < ../step5_tc/input/$filename.input
	read -rsp $'Press any key to continue...\n' -n1 key
done
