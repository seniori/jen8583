#!/usr/bin/env bash

FILENAME="jen8583-${1%-*}.xsd"

echo "Pushing $FILENAME"

exit 0

#cd target
#
#mkdir tmp
#
#cd tmp
#
#git clone https://github.com/chiknrice/chiknrice.github.io.git
#
#cp "../classes/jen8583.xsd" "chiknrice.github.io/schema/$FILENAME"
#
#cd chiknrice.github.io
#
#git add "schema/$FILENAME"
#
#git commit -m "added schema version $1"
#
#git push
