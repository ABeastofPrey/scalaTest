#!/bin/bash
#
#	This script generates an ipk package for java files
#
#

if [[ "$#" -ne 2  || ( "$1" != "301" && "$1" != "Ubuntu" && "$1" != "702" && "$1" != "703" && "$1" != "302" ) ]]; then
        echo "Invalid arguments!! Usage:"
        echo "To generate ipk for Ubuntu: bash ./genipkg_java_files.sh Ubuntu"
        echo "To generate rpm for 301: bash ./genipkg_java_files.sh 301"
	echo "To generate rpm for 302: bash ./genipkg_java_files.sh 302"
        echo "To generate ipk for 702 (x86): bash ./genipkg_java_files.sh 702"
	echo "To generate rpm for 703: bash ./genipkg_java_files.sh 703"
	echo "SECOND ARGUMENT MUST BE THE VERSION!!!"
        exit 0
fi


if [ ! -e /usr/bin/dpkg-deb ]; then
	echo "debian packager is not available in this system"
	exit 0
fi 

TMP="/home/"$USER"/tmp"
if [ ! -e $TMP ]; then
	echo "Please create a tmp direcotry in your home direcotry"
	exit 0

fi

if [ "$1" == "702" ] ; then
	echo "Creating ipk package for x86 - DRPC"
fi

if [ "$1" == "Ubuntu" ] ; then
	echo "Creating ipk package for x86 - Ubuntu"
fi

if [ "$1" == "301" ] ; then
	echo "Creating rpm package for arm - iMX6"
fi

if [ "$1" == "703" ] ; then
	echo "Creating rpm package for LEX"
fi

if [ "$1" == "302" ] ; then
	echo "Creating rpm package for solidrun-iMX6"
fi

SRC_JAVA_WWW_FULL_PATH=$PWD/www/
SRC_JAVA_MODULE_FULL_PATH=$PWD/TPWebServer.jar
SRC_LIGHTTPD_CONF_FULL_PATH=$PWD/lighttpd.conf
SRC_WEBSERVER_INIT_SCRIPT_FULL_PATH_X86=$PWD/run_mc_webserver_x86.sh
SRC_WEBSERVER_INIT_SCRIPT_FULL_PATH_ARM=$PWD/run_mc_webserver_arm.sh
SRC_WEBSERVER_INIT_SCRIPT_FULL_PATH_LEX=$PWD/run_mc_webserver_lex.sh
SRC_WEBSERVER_STARTUP_SCRIPT_FULL_PATH=$PWD/java_webserver.sh
TARGET_JAVA_WWW_FULL_PATH=var/home/mc/www/
TARGET_JAVA_MODULE_FULL_PATH=usr/javaModules/
TARGET_LIGHTTPD_CONF_FULL_PATH=etc/lighttpd/
TARGET_WEBSERVER_INIT_SCRIPT_FULL_PATH=/usr/javaModules/run_java_webserver.sh
TARGET_WEBSERVER_STARTUP_SCRIPT_NAME=java_webserver.sh
TARGET_WEBSERVER_STARTUP_SCRIPT_FULL_PATH=/etc/init.d/$TARGET_WEBSERVER_STARTUP_SCRIPT_NAME

PKG_BASE_DIR=base
CONTROL=$PKG_BASE_DIR/DEBIAN/control

# copy sources
pushd $TMP

rm -rf $PKG_BASE_DIR/

mkdir -p $PKG_BASE_DIR
chmod 0755 $PKG_BASE_DIR			
mkdir -p $PKG_BASE_DIR/DEBIAN

echo	"Priority:	optional"	>>	$CONTROL
echo	"Version:       $2"		>>	$CONTROL
echo	"Section:  	base"		>>	$CONTROL
if [ "$1" == "301" ] ; then
       	echo    "Package:       java-files-301"         >>      $CONTROL
        echo    "Architecture: cortexa9hf_vfp_neon"     >>      $CONTROL
elif [ "$1" == "302" ] ; then
	echo    "Package:       java-files-302"         >>      $CONTROL
        echo    "Architecture: solidrun_imx6"     >>      $CONTROL
else
       	echo    "Package:       java-files"             >>      $CONTROL
        echo    "Architecture: i386 "           >>      $CONTROL
fi
echo	"Maintainer:	<Omri Soudry>"	>> 	$CONTROL	
echo	"Depends:"			>>	$CONTROL
echo	"Source: 	http://www.servotronix.com/">> $CONTROL
echo	"Description:	Java Web Server for softMC"		>> $CONTROL


#mkdir -p $PKG_BASE_DIR/$TARGET_JAVA_WWW_FULL_PATH
mkdir -p $PKG_BASE_DIR/$TARGET_JAVA_MODULE_FULL_PATH
mkdir -p $PKG_BASE_DIR/$TARGET_LIGHTTPD_CONF_FULL_PATH
mkdir -p $PKG_BASE_DIR/etc/init.d/

cp $SRC_JAVA_MODULE_FULL_PATH $PKG_BASE_DIR/$TARGET_JAVA_MODULE_FULL_PATH/TPWebServer.jar

cp $SRC_WEBSERVER_STARTUP_SCRIPT_FULL_PATH $PKG_BASE_DIR/$TARGET_WEBSERVER_STARTUP_SCRIPT_FULL_PATH
chmod 755 $PKG_BASE_DIR$TARGET_WEBSERVER_STARTUP_SCRIPT_FULL_PATH
echo "Webserver startup script is in /etc/init.d..."

if [ "$1" == "702" ] ; then
	cp $SRC_LIGHTTPD_CONF_FULL_PATH.DRPC $PKG_BASE_DIR/$TARGET_LIGHTTPD_CONF_FULL_PATH/lighttpd.conf
fi

if [ "$1" == "Ubuntu" ] ; then
        cp $SRC_LIGHTTPD_CONF_FULL_PATH.Ubuntu $PKG_BASE_DIR/$TARGET_LIGHTTPD_CONF_FULL_PATH/lighttpd.conf
fi

if [[ "$1" == "301" ||  "$1" == "302" ]] ; then
	mkdir -p $PKG_BASE_DIR/etc/rc5.d/
        cp $SRC_LIGHTTPD_CONF_FULL_PATH.arm $PKG_BASE_DIR/$TARGET_LIGHTTPD_CONF_FULL_PATH/lighttpd.conf
	cp $SRC_WEBSERVER_INIT_SCRIPT_FULL_PATH_ARM $PKG_BASE_DIR$TARGET_WEBSERVER_INIT_SCRIPT_FULL_PATH
	chmod 755 $PKG_BASE_DIR$TARGET_WEBSERVER_INIT_SCRIPT_FULL_PATH
	pushd $PKG_BASE_DIR/etc/rc5.d/
	ln -s ../init.d/$TARGET_WEBSERVER_STARTUP_SCRIPT_NAME  S99java
	touch S31java
	echo "Webserver symbolic link added to rc5.d..."
	popd
elif [ "$1" == "703" ] ; then
	cp $SRC_LIGHTTPD_CONF_FULL_PATH.LEX $PKG_BASE_DIR/$TARGET_LIGHTTPD_CONF_FULL_PATH/lighttpd.conf
	mkdir -p $PKG_BASE_DIR/etc/rc5.d/
	cp $SRC_WEBSERVER_INIT_SCRIPT_FULL_PATH_LEX $PKG_BASE_DIR$TARGET_WEBSERVER_INIT_SCRIPT_FULL_PATH
	chmod 755 $PKG_BASE_DIR$TARGET_WEBSERVER_INIT_SCRIPT_FULL_PATH
	pushd $PKG_BASE_DIR/etc/rc5.d/
	ln -s ../init.d/$TARGET_WEBSERVER_STARTUP_SCRIPT_NAME  S99java
	touch S71java
	echo "Webserver symbolic link added to rc5.d..."
	popd
else
	mkdir -p $PKG_BASE_DIR/etc/rc.d/
	cp $SRC_WEBSERVER_INIT_SCRIPT_FULL_PATH_X86 $PKG_BASE_DIR$TARGET_WEBSERVER_INIT_SCRIPT_FULL_PATH
	chmod 755 $PKG_BASE_DIR$TARGET_WEBSERVER_INIT_SCRIPT_FULL_PATH
	pushd $PKG_BASE_DIR/etc/rc.d/
	ln -s ../init.d/$TARGET_WEBSERVER_STARTUP_SCRIPT_NAME  S99java
	touch S50java
	echo "Webserver symbolic link added to rc.d..."
	popd
fi

fakeroot dpkg-deb --build $PKG_BASE_DIR

mv $PKG_BASE_DIR.deb $TMP/java_files-$2.ipk

if [[ "$1" == "301" || "$1" == "703" ||  "$1" == "302" ]] ; then
	cp $TMP/java_files-$2.ipk $TMP/java_files.deb
	alien -r $TMP/java_files.deb
fi

echo "java package is ready in directory ~tmp/"

popd


