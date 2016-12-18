make
FILEPATH="final_testcases/*.micro"
mkdir temp
for f in $FILEPATH
do
	g=$(basename $f)
	filename="${g%.*}"
	echo "Processing $filename"
	java -cp lib/antlr.jar:classes/ Micro $f > temp/$filename.out
	tiny final_testcases/$filename.out < final_testcases/$filename.input

	read -rsp $'Press any key to compile the file...\n' -n1 key

	tiny temp/$filename.out < final_testcases/$filename.input
	read -rsp $'Press any key to continue...\n' -n1 key
	clear
done
