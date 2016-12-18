FILEPATH2="self_testcases/*.micro"
mkdir self
for f2 in $FILEPATH2
do
	g2=$(basename $f2)
	filename2="${g2%.*}"
	echo "Processing $filename2"
	java -cp lib/antlr.jar:classes/ Micro $f2 > self/$filename2.out
	tiny self/$filename2.out < self_testcases/$filename2.input
	read -rsp $'Press any key to continue...\n' -n1 key
	clear
done
