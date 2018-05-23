#!/bin/bash

update_package_json() {
    echo "Folder ${1}"
    echo "Server id ${1}"
    echo "Extension id ${1}"
    cd $1
    tmp=$(mktemp)
    jq ".dependencies.@theia/${2} = ${3}" package.json > "$tmp" && mv "$tmp" package.json
    npm version $3
}

set -e
workdir=`pwd`
sources=$workdir/sts4/theia-extensions/$extension_id
server_id=${extension_id#theia-}
ext_folder=$sources/$server_id

if [ -d "maven-cache" ]; then
    echo "Prepopulating maven cache"
    tar xzf maven-cache/*.tar.gz -C ${HOME}
else
   echo "!!!No stored maven cache found!!! "
   echo "!!!This may slow down the build!!!"
fi

cd "$ext_folder"

timestamp=`date -u +%Y%m%d%H%M`

base_version=`jq -r .version package.json`

# for snapshot build, work the timestamp into package.json version qualifier
qualified_version=${base_version}-${timestamp}
echo "Version: ${qualified_version}"
npm version ${qualified_version}
update_package_json "$sources"/browser-app $server_id $qualified_version
update_package_json "$sources"/electron-app $server_id $qualified_version
cd "$sources"
echo -e "\n\n*Version: ${qualified_version}*" >> README.md

cd "$sources"
./build.sh
cd "$ext_folder"
yarn pack

cp *.tar $workdir/out

